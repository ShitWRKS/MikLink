# Quickstart Validation: Test Profile Tabbed UX

## Prerequisites

- Repository checked out on `develop`
- Java 17 and Android SDK configured for the existing project
- For device acceptance only: exactly one authorized Android device, API 30 or newer, selected explicitly

## Local gates

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lint
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleDebugAndroidTest
```

Focused quality and domain classes are included in `:app:testDebugUnitTest`; their exact observed outcomes are recorded in [verification.md](verification.md).

## Manual acceptance

1. Open Profiles and create a profile.
2. Enter name and description in General.
3. Visit Link; configure a preset or valid custom minimum rate and confirm the discrete threshold explanation changes.
4. Visit Ping; enable it, configure target/count and a Ping threshold, switch away and back, and confirm unsaved state remains.
5. Visit Speed test; edit download/upload and confirm the comparison uses Mbps only while ping/jitter/loss remain separate.
6. Disable and re-enable Ping or Speed test and confirm configuration is retained.
7. Save, reopen, visit all four tabs, and confirm values round-trip.
8. Edit, save, reopen, then delete through the profile list UI.

## Device functional acceptance

Use the repository-preserving installation and explicit-serial workflow in `docs/reference/testing.md`. Run `ProfileCrudUiTest` first; run `FunctionalAcceptanceSuite` only after the focused journey passes and the environment allows it. Unavailable or unauthorized device state is `NOT_RUN`, never PASS.
