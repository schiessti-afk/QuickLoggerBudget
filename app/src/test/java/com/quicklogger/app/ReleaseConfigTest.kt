package com.quicklogger.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseConfigTest {
    private val repoRoot = File("..").canonicalFile
    private val appBuildGradle = File("build.gradle.kts").readText()
    private val gitignore = File(repoRoot, ".gitignore").readText()
    private val license = File(repoRoot, "LICENSE").readText()
    private val releaseWorkflow = File(repoRoot, ".github/workflows/release.yml").readText()

    @Test
    fun gitignoreExcludesKeystoreAndSigningPasswords() {
        assertTrue(gitignore.contains("keystore.properties"))
        assertTrue(gitignore.contains("*.jks"))
        assertTrue(gitignore.contains("*.keystore"))
        assertTrue(gitignore.contains("*.p12"))
        assertTrue(gitignore.contains("upload-keystore.p12"))
    }

    @Test
    fun licenseIsMitCopyrightMichaSchiess() {
        assertTrue(license.contains("MIT License"))
        assertTrue(license.contains("Micha Schiess"))
    }

    @Test
    fun releaseBuildEnablesR8Optimization() {
        // AGP 9.3: optimization { enable = true } turns on code + resource shrinking.
        // https://developer.android.com/topic/performance/app-optimization/enable-app-optimization
        assertTrue(appBuildGradle.contains("optimization"))
        assertTrue(appBuildGradle.contains("enable = true"))
    }

    @Test
    fun assembleReleaseFailsClosedWithoutSigningCredentials() {
        assertTrue(appBuildGradle.contains("requireReleaseSigning"))
        assertTrue(appBuildGradle.contains("outputs.upToDateWhen"))
        assertTrue(
            appBuildGradle.contains("Refusing to produce an unsigned APK"),
        )
        assertTrue(appBuildGradle.contains("keystore.properties"))
        assertTrue(appBuildGradle.contains("QUICKLOGGER_STORE_FILE"))
        assertTrue(appBuildGradle.contains("QUICKLOGGER_STORE_PASSWORD"))
        assertTrue(appBuildGradle.contains("QUICKLOGGER_KEY_ALIAS"))
        assertTrue(appBuildGradle.contains("QUICKLOGGER_KEY_PASSWORD"))
    }

    @Test
    fun versionCanBeOverriddenFromTheTag() {
        assertTrue(appBuildGradle.contains("versionName"))
        assertTrue(appBuildGradle.contains("versionCode"))
        assertTrue(appBuildGradle.contains("versionNameOverride") || appBuildGradle.contains("gradleProperty(\"versionName\")"))
        assertTrue(appBuildGradle.contains("versionCodeOverride") || appBuildGradle.contains("gradleProperty(\"versionCode\")"))
    }

    @Test
    fun releaseWorkflowRunsOnVersionTagsAndUploadsASignedApk() {
        assertTrue(releaseWorkflow.contains("tags:"))
        assertTrue(releaseWorkflow.contains("v*"))
        assertTrue(releaseWorkflow.contains("assembleRelease"))
        assertTrue(releaseWorkflow.contains("gh release create"))
        assertTrue(releaseWorkflow.contains("permissions:"))
        assertTrue(releaseWorkflow.contains("contents: write"))
    }

    @Test
    fun releaseWorkflowUsesActionsSecretsAndDoesNotEmbedPasswords() {
        assertTrue(releaseWorkflow.contains("secrets.RELEASE_KEYSTORE_BASE64"))
        assertTrue(releaseWorkflow.contains("secrets.RELEASE_KEYSTORE_PASSWORD"))
        assertTrue(releaseWorkflow.contains("secrets.RELEASE_KEY_ALIAS"))
        assertTrue(releaseWorkflow.contains("secrets.RELEASE_KEY_PASSWORD"))
        assertFalse(
            "workflow must not contain a password assignment",
            Regex("""(storePassword|keyPassword|KEYSTORE_PASSWORD)\s*[:=]\s*['\"][^'\"]+['\"]""")
                .containsMatchIn(releaseWorkflow),
        )
    }

    @Test
    fun keepRulesLiveInTheAgp93SourceSet() {
        val keepDir = File("src/main/keepRules")
        assertTrue(keepDir.isDirectory)
        assertTrue(keepDir.listFiles().orEmpty().any { it.name.endsWith(".keep") })
    }
}
