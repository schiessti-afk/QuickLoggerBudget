# Sprint 9 tasks

- [x] Task 1: Release config tests
  - Acceptance: Tests fail without `release.yml` / R8 / fail-closed Gradle; gitignore still excludes keystores; LICENSE still names Micha Schiess
  - Verify: `ReleaseConfigTest` 8/8 green
  - Files: `app/src/test/java/com/quicklogger/app/ReleaseConfigTest.kt`

- [x] Task 2: Release Gradle
  - Acceptance: Release uses AGP 9.3 `optimization { enable = true }`; signing from `keystore.properties` or `QUICKLOGGER_STORE_*`; assemble/bundle/package Release fail if credentials are missing; debug does not
  - Verify: `assembleRelease` fails without a keystore; throwaway-keystore `assembleRelease` succeeded (v2-signed); `requireReleaseSigning` is never UP-TO-DATE
  - Files: `app/build.gradle.kts`, `app/src/main/keepRules/libraries.keep`, `app/proguard-rules.pro`

- [x] Task 3: Tag workflow
  - Acceptance: Push of `v*` runs `assembleRelease` with secrets, uploads the APK via `gh release create`, refuses empty secrets
  - Verify: `ReleaseConfigTest` reads the workflow; no password literals in the YAML
  - Files: `.github/workflows/release.yml`

- [x] Task 4: Docs
  - Acceptance: ARCHITECTURE §13 names secrets, fail-closed, version mapping, and the 9.3 optimization DSL. README says how to sign locally and which Actions secrets to set. Sprint 9 status matches what landed.
  - Verify: docs match Gradle/workflow
  - Files: `docs/ARCHITECTURE.md`, `docs/Sprint.md`, `README.md`

## Checkpoint: Sprint 9 done
- [x] `.\gradlew.bat lint`
- [x] `.\gradlew.bat test` — 221 tests, 0 failures (8 added)
- [x] `.\gradlew.bat assembleDebug`
- [x] `.\gradlew.bat assembleRelease` fails without secrets
- [x] Throwaway-keystore `assembleRelease` succeeds; no `INTERNET` in the packaged release manifest; `minifyReleaseWithR8` + `mapping.txt`
- [x] No keystore / `keystore.properties` / passwords in the working tree that would be committed
- [ ] Human: set the four GitHub Actions secrets and push `v1.0.0`
- [ ] Human/device: cold-start the minified APK to the focused amount field
