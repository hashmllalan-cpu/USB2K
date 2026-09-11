#!/usr/bin/env python3
"""CI failure triage: extract the cause excerpt from a Gradle log.

Writes the full excerpt to the run summary and emits ONE `::error::`
annotation (visible via the Checks API) so failures can be diagnosed
without opening the raw log.

Usage: report_failure.py <gradle-log> <label> [device-logcat ...]

Optional device logcat files (dumped by smoke_evidence.sh) are mined for
FATAL EXCEPTION / instrumentation / install failures — an instrumented-test
failure only reports "There were failing tests" at the Gradle level, so the
device-side trace is what actually names the cause.
"""

import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

# Device-side failure markers, in two tiers. An instrumented run only reports "There were
# failing tests" at the Gradle level, so the device trace is what names the real cause —
# but an emulator logcat is full of unrelated noise (system_server ANRs, WebViewLoader
# "has died", HAL restarts). Run 34651240282 proved the cost of one loose pattern: all six
# slots filled with noise and the real signal never reached the annotation. So: tier 1 is
# app/test-specific, tier 2 (generic ANR/died) is used only when tier 1 found nothing.
CRASH_STRONG = re.compile(
    r"FATAL EXCEPTION|AndroidRuntime: FATAL|Unable to find instrumentation"
    r"|INSTRUMENTATION_FAILED|INSTRUMENTATION_STATUS|INSTALL_FAILED"
    r"|Test running failed|Process crashed|am_crash"
)
CRASH_WEAK = re.compile(r"ANR in|has died")
# The package under test — a block mentioning it is always more relevant than one that
# does not, whatever tier matched.
APP_PAT = re.compile(r"usbmediaexplorer|ApplicationSmokeTest")
# Gradle/UTP verdict lines worth putting in the annotation verbatim.
TEST_SIGNAL = re.compile(
    r"There were failing tests|Test running failed|failed to complete|Unable to install"
    r"|INSTALL_FAILED|Process crashed|Instrumentation run failed|tests? (?:finished|failed)"
    r"|^com\.usbmediaexplorer.*FAILED|Execution failed for task"
)


def _device_lines(path: str) -> list[str]:
    try:
        with open(path, errors="replace") as f:
            lines = f.read().splitlines()
    except OSError:
        return []
    # smoke_evidence.sh wraps each probe with the command line it ran and its exit
    # status; those bookkeeping lines are not part of the device trace.
    return [
        line for line in lines
        if not line.startswith("$ ") and not re.fullmatch(r"exit=-?\d+", line.strip())
    ]


def device_crashes(paths: list[str], max_blocks: int = 4) -> list[str]:
    """Mine device logcat dumps for crash / instrumentation / install failures."""
    strong: list[str] = []
    weak: list[str] = []
    for path in paths:
        if not os.path.exists(path):
            continue
        device = _device_lines(path)
        for index, line in enumerate(device):
            strong_hit = bool(CRASH_STRONG.search(line))
            if not strong_hit and not CRASH_WEAK.search(line):
                continue
            window = device[index:index + 12]
            block = " ".join(part.strip() for part in window if part.strip())
            if not block:
                continue
            # App-specific blocks first: they are the ones that explain our failure.
            bucket = strong if strong_hit else weak
            if APP_PAT.search(block):
                bucket.insert(0, block[:600])
            elif block[:600] not in bucket:
                bucket.append(block[:600])
    found = (strong or weak)[:max_blocks]
    return list(dict.fromkeys(found))


def main() -> None:
    log_path = sys.argv[1] if len(sys.argv) > 1 else "build.log"
    label = sys.argv[2] if len(sys.argv) > 2 else "Failure"
    crash_blocks = device_crashes(sys.argv[3:])

    key: list[str] = []
    extra: list[str] = []
    frames: list[str] = []
    signal: list[str] = []
    if os.path.exists(log_path):
        with open(log_path, errors="replace") as f:
            log = f.read().splitlines()
        key += [line.strip() for line in log if re.search(r"> Task .* FAILED", line)][:6]
        for line in log:
            stripped = line.strip()
            if re.match(r"e: .*\.kt", stripped) and stripped not in key:
                key.append(stripped[:300])
            elif stripped.startswith("Caused by:") and stripped not in key:
                key.append(stripped[:300])
        # Stack frames anywhere in the log (dumb global collect — cause-positioning
        # proved brittle). The log lists outer-wrapper stacks first, so the annotation
        # keeps the LAST frames (deepest cause); the summary keeps them all.
        for line in log:
            m = re.match(r"at ([\w$.]+)\(", line.strip())
            if m and ("com.android" in m.group(1) or "org.jetbrains.kotlin" in m.group(1)):
                frame = "FRAME: " + m.group(1)
                if frame not in frames and len(frames) < 80:
                    frames.append(frame)
        if frames:
            extra.append("--- deepest compiler/lint frames (oldest first) ---")
            extra += frames
        # Gradle prints it as "* What went wrong:" — the leading asterisk meant this
        # block was never found before.
        m = next(
            (i for i, line in enumerate(log) if line.strip().lstrip("*").strip() == "What went wrong:"),
            None,
        )
        # Verdict lines: the only ones in a connected-test log that say something
        # actionable, and the annotation is the one channel readable without the raw log.
        signal += [line.strip()[:300] for line in log if TEST_SIGNAL.search(line.strip())][:10]
        if m is not None:
            extra += ["What went wrong:"] + [line.strip() for line in log[m + 1:m + 9] if line.strip()]
        pat = re.compile(
            r"minCompileSdk|AAR metadata|Could not (resolve|find)|Dependency .* requires"
            r"|Lint [Ee]rror|Lint found|Unexpected failure|requires .*lint|ObsoleteLint|NoSuchMethod|NoClassDefFound|error:|FAILED"
        )
        for line in log:
            stripped = line.strip()
            if pat.search(stripped) and stripped not in key and stripped not in extra:
                extra.append(stripped)
                if len(extra) >= 20:
                    break
    # Device traces first: they name the failing test/crash; the Gradle frames that
    # follow only describe how the task gave up.
    key += [f"DEVICE CRASH: {c}" for c in crash_blocks if c not in key]
    # The "What went wrong:" block and any test-runner verdict line: these are the only
    # parts of a connected-test failure that say something actionable, and the annotation
    # is the single channel readable without the raw log.
    if m is not None:
        key += ["What went wrong:"] + [e for e in extra[extra.index("What went wrong:") + 1:][:6] if e not in key]
    key += [g for g in signal if g not in key][:8]
    key += [f for f in frames[-6:] if f not in key]
    # Lint crashes also leave details in the text report when it was written.
    for report in glob.glob("app/build/reports/lint-results-*.txt"):
        try:
            with open(report, errors="replace") as f:
                report_lines = [line.strip() for line in f.read().splitlines() if line.strip()]
            extra.append(f"--- {report} ({len(report_lines)} lines) ---")
            extra += report_lines[:25]
        except OSError:
            continue
    failed_tests: list[str] = []
    result_globs = [
        "app/build/test-results/testDebugUnitTest/*.xml",
        "app/build/outputs/androidTest-results/**/*.xml",
        "app/build/reports/androidTests/**/*.xml",
    ]
    result_files = list(dict.fromkeys(p for g in result_globs for p in glob.glob(g, recursive=True)))
    for path in result_files:
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        for tc in tree.getroot().iter("testcase"):
            node = tc.find("failure")
            if node is None:
                node = tc.find("error")
            if node is not None:
                # A test that dies in @Before / instrumentation startup often has an
                # empty message attribute; the element text then carries the trace.
                msg = (node.get("message") or "").strip()
                if not msg:
                    text_lines = (node.text or "").strip().splitlines()
                    msg = text_lines[0].strip() if text_lines else ""
                failed_tests.append(f"{tc.get('classname')}.{tc.get('name')}: {msg[:160]}")
    key += [f"FAILING TEST: {t}" for t in failed_tests[:15]]
    lines = key + [e for e in extra if e not in key]
    if not lines:
        lines = [f"{label} step failed but no cause excerpt matched (see raw log)."]

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a") as f:
            f.write(f"### {label} failure excerpt\n```\n" + "\n".join(lines[:60]) + "\n```\n")

    def esc(s: str) -> str:
        return s.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")[:6000]

    print("::error::" + esc(" | ".join((key or lines)[:30])))


if __name__ == "__main__":
    main()
