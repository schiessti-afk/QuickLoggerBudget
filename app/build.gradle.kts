import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val versionNameOverride = providers.gradleProperty("versionName").orNull
val versionCodeOverride = providers.gradleProperty("versionCode").orNull?.toInt()
internal val releaseSigning = loadReleaseSigning()

android {
    namespace = "com.quicklogger.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.quicklogger.app"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeOverride ?: 1
        versionName = versionNameOverride ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    releaseSigning?.let { signing ->
        signingConfigs {
            create("release") {
                storeFile = signing.storeFile
                storePassword = signing.storePassword
                keyAlias = signing.keyAlias
                keyPassword = signing.keyPassword
            }
        }
    }

    buildTypes {
        release {
            // AGP 9.3: enables R8 code optimization and resource shrinking together.
            // https://developer.android.com/topic/performance/app-optimization/enable-app-optimization
            optimization {
                enable = true
            }
            if (releaseSigning != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        // MigrationTestHelper reads each version's committed schema JSON from
        // assets — same directory KSP already writes them to (§7.1 below).
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        // ARCHITECTURE pins compileSdk/targetSdk 36. Newer AndroidX artifacts that
        // require API 37 stay off the classpath until that pin changes.
        disable += setOf(
            "OldTargetApi",
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "ObsoleteSdkInt",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    // ARCHITECTURE §7.1: exportSchema = true with the JSON committed under app/schemas/
    // so sprint 4 onwards can write migrations against a recorded v1.
    arg("room.schemaLocation", "$projectDir/schemas")
}

// `androidx.room:room-migration:2.8.4` (used by `MigrationTestHelper`) is built
// against `kotlinx-serialization-json:1.8.1`, whose generated `$$serializer`
// classes require `GeneratedSerializer.typeParametersSerializers()` — a method
// that only exists from `kotlinx-serialization-core` 1.7.0 onward. A separate,
// stricter constraint elsewhere in Room 2.8.4's own metadata otherwise pins core
// down to 1.7.3 in a way that resolves inconsistently and throws
// `AbstractMethodError` the instant `MigrationTestHelper` parses a schema JSON.
// Forcing the whole serialization trio to the same known-good version keeps
// json and core mutually consistent.
configurations.matching { it.name.contains("AndroidTest") }.configureEach {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1")
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

val requireReleaseSigning = tasks.register("requireReleaseSigning") {
    // Credentials live in the environment / an untracked file. Without this,
    // a successful signed build would mark the task UP-TO-DATE and the next
    // assembleRelease with no secrets could package an unsigned APK.
    outputs.upToDateWhen { false }
    doLast {
        if (releaseSigning == null) {
            throw GradleException(
                "assembleRelease requires signing credentials " +
                    "(keystore.properties or QUICKLOGGER_STORE_FILE / " +
                    "QUICKLOGGER_STORE_PASSWORD / QUICKLOGGER_KEY_ALIAS / " +
                    "QUICKLOGGER_KEY_PASSWORD). Refusing to produce an unsigned APK.",
            )
        }
    }
}

tasks.configureEach {
    if (name == "assembleRelease" || name == "bundleRelease" || name == "packageRelease") {
        dependsOn(requireReleaseSigning)
    }
}

internal data class ReleaseSigning(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

internal fun loadReleaseSigning(): ReleaseSigning? {
    fun env(name: String): String? =
        providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }

    val envFile = env("QUICKLOGGER_STORE_FILE")
    val envPassword = env("QUICKLOGGER_STORE_PASSWORD")
    val envAlias = env("QUICKLOGGER_KEY_ALIAS")
    val envKeyPassword = env("QUICKLOGGER_KEY_PASSWORD")
    val envPresent = listOf(envFile, envPassword, envAlias, envKeyPassword).count { it != null }
    if (envPresent == 4) {
        return ReleaseSigning(
            storeFile = resolveStoreFile(envFile!!),
            storePassword = envPassword!!,
            keyAlias = envAlias!!,
            keyPassword = envKeyPassword!!,
        )
    }
    if (envPresent > 0) {
        throw GradleException(
            "Release signing env vars are incomplete. Set all of QUICKLOGGER_STORE_FILE, " +
                "QUICKLOGGER_STORE_PASSWORD, QUICKLOGGER_KEY_ALIAS, QUICKLOGGER_KEY_PASSWORD.",
        )
    }

    val propsFile = rootProject.file("keystore.properties")
    if (!propsFile.isFile) return null
    val props = Properties()
    propsFile.inputStream().use { props.load(it) }
    val storeFile = props.getProperty("storeFile")?.takeIf { it.isNotBlank() }
    val storePassword = props.getProperty("storePassword")?.takeIf { it.isNotBlank() }
    val keyAlias = props.getProperty("keyAlias")?.takeIf { it.isNotBlank() }
    val keyPassword = props.getProperty("keyPassword")?.takeIf { it.isNotBlank() }
    val present = listOf(storeFile, storePassword, keyAlias, keyPassword).count { it != null }
    if (present == 0) return null
    if (present < 4) {
        throw GradleException(
            "keystore.properties is incomplete. Need storeFile, storePassword, keyAlias, keyPassword.",
        )
    }
    return ReleaseSigning(
        storeFile = resolveStoreFile(storeFile!!),
        storePassword = storePassword!!,
        keyAlias = keyAlias!!,
        keyPassword = keyPassword!!,
    )
}

internal fun resolveStoreFile(path: String): File {
    val file = File(path)
    return if (file.isAbsolute) file else rootProject.file(path)
}
