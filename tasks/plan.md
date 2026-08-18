# Implementation Plan: Sprint 9 — Signed release

## Overview

A git tag `v*` produces a minified, signed APK on GitHub Releases. Secrets never enter git. `assembleRelease` fails closed when credentials are missing. Debug/lint/test stay usable without a keystore.

## Architecture Decisions

- **AGP 9.3 optimization DSL.** The project is on AGP 9.3.0. Official docs enable R8 with `optimization { enable = true }` on the release build type, which turns on code optimization and optimized resource shrinking together and includes the default Android keep rules. Source: [Enable app optimization with R8](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization) and [AGP 9.3.0 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes).
- **Keep rules only as required.** Room, Kotlin, Hilt, and Compose ship consumer rules. Official R8 guidance treats manual `-keep class * extends androidx.room.RoomDatabase` as redundant and harmful. App keep files live under `src/main/keepRules/*.keep`. We do not copy library rules into the app.
- **Fail closed.** AGP produces an unsigned APK when release has no `signingConfig`. `assembleRelease` / `bundleRelease` / `packageRelease` depend on a `requireReleaseSigning` task that fails if credentials are incomplete. Debug, lint, and JVM tests do not require a keystore.
- **Credentials.** Local: `keystore.properties` at the repo root (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). CI: env `QUICKLOGGER_STORE_FILE` / `QUICKLOGGER_STORE_PASSWORD` / `QUICKLOGGER_KEY_ALIAS` / `QUICKLOGGER_KEY_PASSWORD`. Env wins when all four are set. Incomplete sets fail at configuration.
- **GitHub secrets (never in git).** `RELEASE_KEYSTORE_BASE64` (binary storeFile, [Base64 secret workaround](https://docs.github.com/en/actions/security-for-github-actions/security-guides/using-secrets-in-github-actions#storing-base64-binary-blobs-as-secrets)), `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Unset secrets are empty strings; the workflow checks that before assemble.
- **Versioning.** `versionName` from the tag with the leading `v` stripped (`v1.0.0` → `1.0.0`), passed as `-PversionName`. `versionCode` is `${{ github.run_number }}` (ARCHITECTURE §13 allows run number). Untagged local defaults stay `0.1.0` / `1`.
- **Release upload.** `gh release create` with `permissions: contents: write` and `GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}`. GitHub CLI is preinstalled on hosted runners ([Using GitHub CLI in workflows](https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/using-github-cli-in-workflows), [gh release create](https://cli.github.com/manual/gh_release_create)). No third-party release action.
- **Tag `v1.0.0`.** Wired in this sprint; the human tag push landed and produced [QuickLogger 1.0.0](https://github.com/schiessti-afk/QuickLoggerBudget/releases/tag/v1.0.0).

## Task List

### Phase 1: Gradle contract
- [x] Task 1: JVM tests for gitignore, license, R8-on-release, fail-closed wording, workflow shape
- [x] Task 2: `optimization { enable = true }`, signing from env/properties, `requireReleaseSigning`, keepRules source set

### Checkpoint: Fail closed
- [x] `assembleRelease` without credentials fails
- [x] `assembleDebug`, `lint`, `test` still succeed without a keystore

### Phase 2: CI + docs
- [x] Task 3: `.github/workflows/release.yml` on tags `v*`
- [x] Task 4: ARCHITECTURE §13, README release section, Sprint 9 status

### Checkpoint: Sprint complete
- [x] Tests cover the new files; `lint` / `test` / `assembleDebug` green
- [x] Throwaway-keystore `assembleRelease` succeeds; merged release manifest has no `INTERNET`

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Incomplete signingConfig still packages an unsigned APK | High (exit criterion) | Dedicated `requireReleaseSigning` task as a dependency of package/assemble/bundle Release |
| R8 strips Hilt/Room | High (cold start crash) | First minify build with a throwaway keystore; add keep rules only if the build or runtime proves they are required |
| Tagging without secrets | Medium | Workflow fails closed on empty secrets; do not tag from this change |
| `optimization { enable }` DSL mismatch | Medium | Docs pin it for AGP 9.3; fall back to legacy `isMinifyEnabled` only if configure fails |

## Open Questions

None. `v1.0.0` is on GitHub Releases.
