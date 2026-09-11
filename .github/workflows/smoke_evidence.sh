#!/bin/sh
# Capture on-device state after a failed :app:connectedDebugAndroidTest run.
#
# Why this exists: the emulator-smoke jobs are the only red gate in the repo, and the
# Gradle exception they surface ("There were failing tests") says nothing about *which*
# test failed or *why*. The raw job log lives on a host that a sandboxed reviewer cannot
# reach, so the evidence has to travel as a workflow artifact instead. Everything the
# device knows about the run — install state, instrumentation registration, logcat,
# crash buffer, ANR traces — is written under <out>/ and uploaded by the workflow.
#
# Usage: smoke_evidence.sh [out-dir] [label] [gradle-log]
# Best effort by design: no `set -e`, every probe is guarded, and a missing adb still
# leaves the Gradle log triage in place. Exits 0 unless the evidence directory could
# not be created at all (the caller must never let diagnostics mask the real exit code).

set -u

OUT="${1:-smoke-evidence}"
LABEL="${2:-Smoke}"
GRADLE_LOG="${3:-connected.log}"
PKG="${PKG:-com.usbmediaexplorer.debug}"
HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

mkdir -p "$OUT" || exit 1

{
    echo "label=$LABEL"
    echo "date_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || echo unknown)"
    echo "adb=$(command -v adb 2>/dev/null || echo MISSING)"
    echo "package=$PKG"
    echo "gradle_log=$GRADLE_LOG"
} > "$OUT/summary.txt"

# probe <relative-output-file> <command...> — records the command, its output and its
# exit status, so an absent/failed probe is itself visible in the artifact.
probe() {
    file="$1"
    shift
    {
        echo "\$ $*"
        "$@" 2>&1
        echo "exit=$?"
    } > "$OUT/$file"
}

if command -v adb >/dev/null 2>&1; then
    probe devices.txt adb devices -l
    probe props.txt adb shell getprop
    probe packages.txt adb shell pm list packages -f
    probe instrumentation.txt adb shell pm list instrumentation
    probe package-dump.txt adb shell dumpsys package "$PKG"
    probe crash-buffer.txt adb logcat -d -b crash -v threadtime
    probe logcat.txt adb logcat -d -v threadtime
    probe crash-dirs.txt adb shell ls -la /data/anr /data/tombstones
    # Drop the huge buffers: the artifact is for triage, not for archival.
    probe meminfo.txt adb shell dumpsys meminfo "$PKG"
else
    echo "adb not on PATH - no device evidence collected" > "$OUT/adb-missing.txt"
fi

# Surface the cause as a check annotation (the log host may be unreachable). The
# triage script mines the Gradle log and, when present, the device logcat.
if command -v python3 >/dev/null 2>&1; then
    python3 "$HERE/report_failure.py" "$GRADLE_LOG" "$LABEL" "$OUT/logcat.txt" || true
fi

exit 0
