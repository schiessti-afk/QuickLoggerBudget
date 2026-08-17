# Sprint 1 tasks

- [x] Task 1: Gradle wrapper, `libs.versions.toml`, `:app` module
  - Acceptance: `settings.gradle.kts` includes `:app`; JDK 17; `minSdk` 26; `compileSdk`/`targetSdk` 36; `applicationId` `com.quicklogger.app`
  - Verify: Gradle sync / `assembleDebug` after later tasks
  - Files: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, wrapper, `local.properties` (gitignored)

- [x] Task 2: Manifest privacy and backup deny-all
  - Acceptance: no `INTERNET`/`CAMERA`/media permissions; `allowBackup=false`; deny-all backup XML
  - Verify: `.\gradlew.bat :app:testDebugUnitTest --tests com.quicklogger.app.ManifestPrivacyTest`

- [x] Task 3: Light-only Material 3 theme from DESIGN §5.1
  - Acceptance: cream paper, sealing-wax primary; `dynamicColor` off; no dark scheme
  - Verify: `.\gradlew.bat :app:testDebugUnitTest --tests com.quicklogger.app.BrandColorsTest`

- [x] Task 4: Navigation shell — Log start, History push/pop, amount focused + IME
  - Acceptance: debug APK opens on Log; History from top-bar list action pops back
  - Verify: `assembleDebug`; human install; JVM `LogViewModelTest`

- [x] Task 5: Layer package holders + Room/Coil/Hilt classpath
  - Acceptance: domain/data/presentation packages exist; Room KSP and Coil on classpath; no entities/use cases
  - Verify: project compiles; domain has no `android.*` imports

- [x] Task 6: GitHub Actions CI
  - Acceptance: on `main` and PRs: `lint`, `test`, `assembleDebug`
  - Verify: workflow file present; local `lint`/`test`/`assembleDebug` green

## Checkpoint: Sprint 1 done
- [x] `.\gradlew.bat lint`
- [x] `.\gradlew.bat test`
- [x] `.\gradlew.bat assembleDebug`
- [ ] Opening the debug APK lands on Log with amount focused and IME shown (needs a device)
- [ ] History is reachable and pops back (needs a device)
- [x] Merged manifest has no `INTERNET`; `allowBackup` is false
