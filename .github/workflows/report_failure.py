#!/usr/bin/env python3
"""CI failure triage: extract the cause excerpt from a Gradle log.

Writes the full excerpt to the run summary and emits ONE `::error::`
annotation (visible via the Checks API) so failures can be diagnosed
without opening the raw log.

Usage: report_failure.py <gradle-log> <label>
"""

import glob
import os
import re
import sys
import xml.etree.ElementTree as ET


def main() -> None:
    log_path = sys.argv[1] if len(sys.argv) > 1 else "build.log"
    label = sys.argv[2] if len(sys.argv) > 2 else "Failure"

    key: list[str] = []
    extra: list[str] = []
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
        frames: list[str] = []
        for line in log:
            m = re.match(r"at ([\w$.]+)\(", line.strip())
            if m and ("com.android" in m.group(1) or "org.jetbrains.kotlin" in m.group(1)):
                frame = "FRAME: " + m.group(1)
                if frame not in frames and len(frames) < 80:
                    frames.append(frame)
        key += [f for f in frames[-12:] if f not in key]
        if frames:
            extra.append("--- deepest compiler/lint frames (oldest first) ---")
            extra += frames
        m = next((i for i, line in enumerate(log) if line.strip() == "What went wrong:"), None)
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
    for path in glob.glob("app/build/test-results/testDebugUnitTest/*.xml"):
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        for tc in tree.getroot().iter("testcase"):
            node = tc.find("failure")
            if node is None:
                node = tc.find("error")
            if node is not None:
                msg = (node.get("message") or "")[:160]
                failed_tests.append(f"{tc.get('classname')}.{tc.get('name')}: {msg}")
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

    print("::error::" + esc(" | ".join((key or lines)[:16])))


if __name__ == "__main__":
    main()
