#!/usr/bin/env bash
set -euo pipefail

TEST_CLASS="com.app.miklink.e2e.LiveProbeE2ETest"
RUN_DIR="artifacts/live-probe-e2e/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RUN_DIR"

if ! command -v adb >/dev/null 2>&1; then
  echo "FAIL: adb not found"
  exit 10
fi

if [[ ! -x "./gradlew" ]]; then
  echo "FAIL: ./gradlew not found or not executable"
  exit 11
fi

adb devices -l > "$RUN_DIR/adb_devices.txt"
DEVICE_COUNT="$(awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }' "$RUN_DIR/adb_devices.txt")"

if [[ "$DEVICE_COUNT" -eq 0 ]]; then
  cat > "$RUN_DIR/summary.txt" <<EOF
status=NOT_RUN
reason=no_adb_device
exit_code=1
test_class=$TEST_CLASS
run_dir=$RUN_DIR
visible_result_found=no
trace_pulled=no
crash_found=no
EOF
  cat "$RUN_DIR/summary.txt"
  exit 1
fi

if [[ "$DEVICE_COUNT" -gt 1 && -z "${ANDROID_SERIAL:-}" ]]; then
  cat > "$RUN_DIR/summary.txt" <<EOF
status=NOT_RUN
reason=multiple_devices_set_android_serial
exit_code=1
test_class=$TEST_CLASS
run_dir=$RUN_DIR
visible_result_found=no
trace_pulled=no
crash_found=no
EOF
  cat "$RUN_DIR/summary.txt"
  exit 1
fi

ADB=(adb)
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB+=( -s "$ANDROID_SERIAL" )
fi

"${ADB[@]}" get-state >/dev/null 2>&1 || {
  cat > "$RUN_DIR/summary.txt" <<EOF
status=NOT_RUN
reason=device_not_ready
exit_code=1
test_class=$TEST_CLASS
run_dir=$RUN_DIR
visible_result_found=no
trace_pulled=no
crash_found=no
EOF
  cat "$RUN_DIR/summary.txt"
  exit 1
}

"${ADB[@]}" logcat -c || true

set +e
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="$TEST_CLASS" \
  > "$RUN_DIR/gradle_stdout.txt" \
  2> "$RUN_DIR/gradle_stderr.txt"
EXIT_CODE=$?
set -e

"${ADB[@]}" logcat -d -v time \
  | grep -E "MIKLINK_E2E|AndroidRuntime|FATAL EXCEPTION|ANR|AssertionError|Exception" \
  > "$RUN_DIR/logcat_filtered.txt" || true

grep "MIKLINK_E2E" "$RUN_DIR/logcat_filtered.txt" > "$RUN_DIR/miklink_e2e_raw.txt" || true

TRACE_PATH="$(grep "MIKLINK_E2E_TRACE" "$RUN_DIR/miklink_e2e_raw.txt" | tail -1 | sed -E 's/.*"path":"([^"]+)".*/\1/' || true)"
TRACE_PULLED="no"
if [[ -n "$TRACE_PATH" ]]; then
  if "${ADB[@]}" pull "$TRACE_PATH" "$RUN_DIR/debug_trace.ndjson" >/dev/null 2>&1; then
    TRACE_PULLED="yes"
  fi
fi

VISIBLE_RESULT_FOUND="no"
if grep "MIKLINK_E2E_VISIBLE_RESULT" "$RUN_DIR/miklink_e2e_raw.txt" >/dev/null 2>&1; then
  VISIBLE_RESULT_FOUND="yes"
fi

CRASH_FOUND="no"
if grep -E "AndroidRuntime|FATAL EXCEPTION|ANR" "$RUN_DIR/logcat_filtered.txt" >/dev/null 2>&1; then
  CRASH_FOUND="yes"
fi

NOT_RUN_MARKER="no"
if grep "MIKLINK_E2E_END" "$RUN_DIR/miklink_e2e_raw.txt" | grep '"status":"NOT_RUN"' >/dev/null 2>&1; then
  NOT_RUN_MARKER="yes"
fi

required_events=(
  run_started
  profile_loaded
  test_enabled_state
  thresholds_loaded
  mikrotik_raw_response
  parsed_response
  normalized_result
  threshold_evaluation
  test_decision
  run_finished
)

TRACE_EVENTS_OK="yes"
if [[ "$TRACE_PULLED" != "yes" ]]; then
  TRACE_EVENTS_OK="no"
else
  for event_name in "${required_events[@]}"; do
    if ! grep "\"event\":\"$event_name\"" "$RUN_DIR/debug_trace.ndjson" >/dev/null 2>&1; then
      TRACE_EVENTS_OK="no"
      break
    fi
  done
fi

STATUS="PASS"
REASON="ok"

if [[ "$NOT_RUN_MARKER" == "yes" ]]; then
  STATUS="NOT_RUN"
  REASON="$(grep "MIKLINK_E2E_END" "$RUN_DIR/miklink_e2e_raw.txt" | tail -1 | sed -E 's/.*"reason":"([^"]+)".*/\1/' || true)"
  if [[ -z "$REASON" ]]; then
    REASON="not_run_marker"
  fi
elif [[ "$EXIT_CODE" -ne 0 ]]; then
  STATUS="FAIL"
  REASON="instrumentation_failed"
elif [[ "$VISIBLE_RESULT_FOUND" != "yes" ]]; then
  STATUS="FAIL"
  REASON="missing_visible_result"
elif [[ "$TRACE_PULLED" != "yes" ]]; then
  STATUS="FAIL"
  REASON="missing_debug_trace"
elif [[ "$TRACE_EVENTS_OK" != "yes" ]]; then
  STATUS="FAIL"
  REASON="missing_required_trace_events"
elif [[ "$CRASH_FOUND" == "yes" ]]; then
  STATUS="FAIL"
  REASON="crash_or_anr_found"
elif ! grep "MIKLINK_E2E_END" "$RUN_DIR/miklink_e2e_raw.txt" | grep '"status":"PASS"' >/dev/null 2>&1; then
  STATUS="FAIL"
  REASON="missing_pass_end_marker"
fi

cat > "$RUN_DIR/summary.txt" <<EOF
status=$STATUS
reason=$REASON
exit_code=$EXIT_CODE
test_class=$TEST_CLASS
run_dir=$RUN_DIR
visible_result_found=$VISIBLE_RESULT_FOUND
trace_pulled=$TRACE_PULLED
crash_found=$CRASH_FOUND
EOF

cat "$RUN_DIR/summary.txt"

if [[ "$STATUS" != "PASS" ]]; then
  exit 1
fi

exit 0
