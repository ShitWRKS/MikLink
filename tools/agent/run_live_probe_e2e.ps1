#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

$TestClass = 'com.app.miklink.e2e.LiveProbeE2ETest'
$Timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$RunDir = Join-Path (Join-Path 'artifacts' 'live-probe-e2e') $Timestamp

New-Item -ItemType Directory -Path $RunDir -Force | Out-Null

$SummaryPath = Join-Path $RunDir 'summary.txt'
$AdbDevicesPath = Join-Path $RunDir 'adb_devices.txt'
$GradleStdoutPath = Join-Path $RunDir 'gradle_stdout.txt'
$GradleStderrPath = Join-Path $RunDir 'gradle_stderr.txt'
$LogcatFilteredPath = Join-Path $RunDir 'logcat_filtered.txt'
$MiklinkRawPath = Join-Path $RunDir 'miklink_e2e_raw.txt'
$DebugTracePath = Join-Path $RunDir 'debug_trace.ndjson'

foreach ($path in @(
    $SummaryPath,
    $AdbDevicesPath,
    $GradleStdoutPath,
    $GradleStderrPath,
    $LogcatFilteredPath,
    $MiklinkRawPath,
    $DebugTracePath
)) {
    Set-Content -Path $path -Value ''
}

$adbPath = 'NA'
$androidSerial = if ($env:ANDROID_SERIAL) { $env:ANDROID_SERIAL } else { '' }
$gradleExitCode = 'NA'
$visibleResultFound = 'no'
$traceMarkerFound = 'no'
$tracePulled = 'no'
$requiredTraceEventsPresent = 'no'
$crashFound = 'no'

function Write-Summary {
    param(
        [string]$Status,
        [string]$Reason
    )

    $lines = @(
        "status=$Status",
        "reason=$Reason",
        "exit_code=$gradleExitCode",
        "test_class=$TestClass",
        "run_dir=$RunDir",
        "adb=$adbPath",
        "android_serial=$androidSerial",
        "visible_result_found=$visibleResultFound",
        "trace_marker_found=$traceMarkerFound",
        "trace_pulled=$tracePulled",
        "required_trace_events_present=$requiredTraceEventsPresent",
        "crash_found=$crashFound"
    )

    Set-Content -Path $SummaryPath -Value $lines
}

function Finish-Run {
    param(
        [string]$Status,
        [string]$Reason,
        [int]$ExitCode
    )

    Write-Summary -Status $Status -Reason $Reason
    Write-Host "Final status: $Status"
    Write-Host "Final reason: $Reason"
    exit $ExitCode
}

Write-Host "Artifacts: $RunDir"

$adbCommand = Get-Command adb.exe -ErrorAction SilentlyContinue | Select-Object -First 1
if ($null -ne $adbCommand -and $adbCommand.Source) {
    $adbPath = $adbCommand.Source
} else {
    if (-not [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
        $fallbackAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
        if (Test-Path -LiteralPath $fallbackAdb) {
            $adbPath = (Resolve-Path -LiteralPath $fallbackAdb).Path
        }
    }
}

if ($adbPath -eq 'NA') {
    Write-Host 'ADB: not found'
    Finish-Run -Status 'NOT_RUN' -Reason 'adb_not_found' -ExitCode 1
}

Write-Host "ADB: $adbPath"

$gradlewPath = Join-Path (Get-Location) 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradlewPath)) {
    Write-Host 'Gradle wrapper: .\\gradlew.bat not found'
    Finish-Run -Status 'NOT_RUN' -Reason 'gradlew_not_found' -ExitCode 1
}

$adbDevicesOutput = & $adbPath devices -l
$adbDevicesOutput | Set-Content -Path $AdbDevicesPath

$deviceLines = @($adbDevicesOutput | Where-Object { $_ -match '^\S+\s+device(\s|$)' })
$deviceCount = $deviceLines.Count

if ($deviceCount -eq 0) {
    Write-Host 'Device: none'
    Finish-Run -Status 'NOT_RUN' -Reason 'no_adb_device' -ExitCode 1
}

if ($deviceCount -gt 1 -and [string]::IsNullOrWhiteSpace($androidSerial)) {
    Write-Host 'Device: multiple connected, ANDROID_SERIAL required'
    Finish-Run -Status 'NOT_RUN' -Reason 'multiple_devices_set_ANDROID_SERIAL' -ExitCode 1
}

if ([string]::IsNullOrWhiteSpace($androidSerial)) {
    $androidSerial = ($deviceLines[0] -split '\s+')[0]
}

$selectedDeviceLine = $deviceLines | Where-Object { $_ -match "^$([regex]::Escape($androidSerial))\s+device(\s|$)" } | Select-Object -First 1
if (-not $selectedDeviceLine) {
    Write-Host "Device: serial '$androidSerial' not in state device"
    Finish-Run -Status 'NOT_RUN' -Reason 'no_adb_device' -ExitCode 1
}

$adbArgs = @()
if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_SERIAL)) {
    $adbArgs += '-s'
    $adbArgs += $env:ANDROID_SERIAL
}

Write-Host "Device: $androidSerial"

& $adbPath @adbArgs logcat -c *> $null

$gradleArgs = @(
    ':app:connectedDebugAndroidTest',
    "-Pandroid.testInstrumentationRunnerArguments.class=$TestClass"
)

if (-not [string]::IsNullOrWhiteSpace($env:MIKLINK_CLIENT_ID)) {
    $gradleArgs += "-Pandroid.testInstrumentationRunnerArguments.clientId=$($env:MIKLINK_CLIENT_ID)"
}

if (-not [string]::IsNullOrWhiteSpace($env:MIKLINK_PROFILE_ID)) {
    $gradleArgs += "-Pandroid.testInstrumentationRunnerArguments.profileId=$($env:MIKLINK_PROFILE_ID)"
}

$ErrorActionPreference = 'Continue'
& $gradlewPath @gradleArgs 1> $GradleStdoutPath 2> $GradleStderrPath
$gradleExitCode = $LASTEXITCODE
$ErrorActionPreference = 'Stop'

$logcatOutput = & $adbPath @adbArgs logcat -d
$filteredLines = @($logcatOutput | Where-Object {
    $_ -match 'MIKLINK_E2E' -or
    $_ -match 'AndroidRuntime' -or
    $_ -match 'FATAL EXCEPTION' -or
    $_ -match 'ANR' -or
    $_ -match 'AssertionError' -or
    $_ -match 'Exception'
})
$filteredLines | Set-Content -Path $LogcatFilteredPath

$miklinkLines = @($filteredLines | Where-Object { $_ -match 'MIKLINK_E2E' })
$miklinkLines | Set-Content -Path $MiklinkRawPath

if ($miklinkLines | Where-Object { $_ -match 'MIKLINK_E2E_VISIBLE_RESULT' } | Select-Object -First 1) {
    $visibleResultFound = 'yes'
}

$traceLine = $miklinkLines | Where-Object { $_ -match 'MIKLINK_E2E_TRACE' } | Select-Object -Last 1
$tracePath = ''
if ($traceLine) {
    $traceMarkerFound = 'yes'
    $traceMatch = [regex]::Match($traceLine, '"path":"([^"]+)"')
    if ($traceMatch.Success) {
        $tracePath = $traceMatch.Groups[1].Value
    }
}

if ($traceMarkerFound -eq 'yes' -and -not [string]::IsNullOrWhiteSpace($tracePath)) {
    $ErrorActionPreference = 'Continue'
    & $adbPath @adbArgs pull $tracePath $DebugTracePath *> $null
    $pullExitCode = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'

    if ($pullExitCode -eq 0 -and (Test-Path -LiteralPath $DebugTracePath) -and (Get-Item -LiteralPath $DebugTracePath).Length -gt 0) {
        $tracePulled = 'yes'
    }
}

$requiredEvents = @(
    'run_started',
    'profile_loaded',
    'test_enabled_state',
    'thresholds_loaded',
    'mikrotik_raw_response',
    'parsed_response',
    'normalized_result',
    'threshold_evaluation',
    'test_decision',
    'run_finished'
)

if ($tracePulled -eq 'yes') {
    $traceContent = Get-Content -Path $DebugTracePath
    $missingEvent = $false
    foreach ($eventName in $requiredEvents) {
        $eventPattern = '"event":"' + [regex]::Escape($eventName) + '"'
        if (-not ($traceContent | Where-Object { $_ -match $eventPattern } | Select-Object -First 1)) {
            $missingEvent = $true
            break
        }
    }

    if (-not $missingEvent) {
        $requiredTraceEventsPresent = 'yes'
    }
}

if ($filteredLines | Where-Object { $_ -match 'AndroidRuntime|FATAL EXCEPTION|ANR' } | Select-Object -First 1) {
    $crashFound = 'yes'
}

$endNotRunLine = $miklinkLines | Where-Object { $_ -match 'MIKLINK_E2E_END' -and $_ -match '"status":"NOT_RUN"' } | Select-Object -Last 1
$endPassFound = if ($miklinkLines | Where-Object { $_ -match 'MIKLINK_E2E_END' -and $_ -match '"status":"PASS"' } | Select-Object -First 1) { $true } else { $false }

$status = 'PASS'
$reason = 'ok'

if ($endNotRunLine) {
    $status = 'NOT_RUN'
    $reasonMatch = [regex]::Match($endNotRunLine, '"reason":"([^"]+)"')
    if ($reasonMatch.Success -and -not [string]::IsNullOrWhiteSpace($reasonMatch.Groups[1].Value)) {
        $reason = $reasonMatch.Groups[1].Value
    } else {
        $reason = 'unknown_failure'
    }
} elseif ($gradleExitCode -ne 0) {
    $status = 'FAIL'
    $reason = 'gradle_failed'
} elseif ($visibleResultFound -ne 'yes') {
    $status = 'FAIL'
    $reason = 'missing_visible_result'
} elseif ($traceMarkerFound -ne 'yes') {
    $status = 'FAIL'
    $reason = 'missing_trace_marker'
} elseif ($tracePulled -ne 'yes') {
    $status = 'FAIL'
    $reason = 'trace_pull_failed'
} elseif ($requiredTraceEventsPresent -ne 'yes') {
    $status = 'FAIL'
    $reason = 'missing_required_trace_events'
} elseif ($crashFound -eq 'yes') {
    $status = 'FAIL'
    $reason = 'crash_or_anr_found'
} elseif (-not $endPassFound) {
    $status = 'FAIL'
    $reason = 'missing_pass_end_marker'
}

if ($status -eq 'PASS') {
    Finish-Run -Status $status -Reason $reason -ExitCode 0
}

if ($status -eq 'NOT_RUN') {
    Finish-Run -Status $status -Reason $reason -ExitCode 1
}

if ($reason -eq '') {
    $reason = 'unknown_failure'
}
Finish-Run -Status 'FAIL' -Reason $reason -ExitCode 1
