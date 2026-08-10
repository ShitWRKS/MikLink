# Quickstart: Native Agent Testing (Post-Implementation)

This is the acceptance workflow the implementation must make valid. It deliberately
uses standard Gradle and Android tools rather than a repository shell wrapper.

## 1. Select one device explicitly

```text
adb devices -l
adb -s <serial> shell getprop ro.build.version.sdk
adb -s <serial> shell getprop ro.product.model
```

Continue only when the selected device state is `device` and API is at least 30.
Never omit `<serial>` when more than one target is connected.

## 2. Run probe-independent regression

Build both APKs with the platform Gradle launcher, then update them with standard
adb install and invoke AndroidJUnitRunner directly. `-r` preserves the installed
application data; if update/signature/version compatibility fails, stop and report
NOT_RUN rather than uninstalling the package:

```text
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb -s <serial> install -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <serial> shell am instrument -w -r \
  -e sessionId native-<timestamp> \
  com.app.miklink.test/androidx.test.runner.AndroidJUnitRunner
```

To rerun one maintained class or method, pass AndroidJUnitRunner's standard `class`
argument directly:

```text
adb -s <serial> shell am instrument -w -r \
  -e sessionId native-<timestamp> \
  -e class com.app.miklink.e2e.catalog.ClientScenarioTest \
  com.app.miklink.test/androidx.test.runner.AndroidJUnitRunner
```

Pull the manifest-listed session directory from the device into
`app/build/outputs/agent-tests/`. Each result links a session manifest and applicable
screenshots, hierarchy, trace, and generated files.

`connectedDebugAndroidTest` remains suitable only for a device whose local state is
explicitly disposable. Some device/installer combinations uninstall packages while
cleaning up a rejected test-APK install, which also erases app data; therefore it is
not the default when `disposableLocalState=false`.

## 3. Perform an ad-hoc UI investigation (debug only)

Build/install debug once with the same preserving update, then use direct adb for
lifecycle and observation:

```text
./gradlew :app:assembleDebug
adb -s <serial> install -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> shell run-as com.app.miklink id
adb -s <serial> shell am force-stop com.app.miklink
adb -s <serial> shell am start -W -n com.app.miklink/.MainActivity
adb -s <serial> shell uiautomator dump /sdcard/miklink-window.xml
adb -s <serial> pull /sdcard/miklink-window.xml <session-dir>/ui-hierarchy.xml
adb -s <serial> exec-out screencap -p > <session-dir>/screenshot.png
```

Use stable resource IDs/text/content descriptions from the hierarchy for selection;
perform bounded `adb shell input tap|text|swipe|keyevent` actions and recapture state
after each assertion. Record the final result using
`contracts/scenario-result.schema.json`. Do not place passwords in commands or saved
hierarchies. If the debuggability check fails, stop: agent mode MUST NOT be attempted
against production/release and the session is NOT_RUN.

## 4. Run live-probe coverage

Configure the intended probe in MikLink on the device first. Do not pass credentials
as Gradle/instrumentation arguments. Run the targeted live catalog with the selected
serial. If probe reachability/authentication/interface/speed-server prerequisites are
absent, verify the result is NOT_RUN rather than PASS or product FAIL.

Wi-Fi loss/recovery is excluded unless the session separately declares both
`allowWifiDisruption=true` and retained host control. After an opted-in run, verify
the original Wi-Fi state is restored before accepting the result.

## 5. Verify release isolation

With release signing configured:

```text
./gradlew :app:assembleRelease
```

Install the exact release APK with adb, launch it, externally attempt the documented
forbidden activation paths, perform minimal black-box navigation, and inspect the
package/artifacts. Acceptance requires no agent-mode activation path, no active
agent-control component, no enhanced debug trace, and no credential exposure.

## 6. Migration gate

Compare the produced evidence against every row of the parity table in `research.md`.
Do not delete `run_live_probe_e2e.ps1` or `run_live_probe_e2e.sh` until all rows pass
on the designated physical device and removal is explicitly accepted.
