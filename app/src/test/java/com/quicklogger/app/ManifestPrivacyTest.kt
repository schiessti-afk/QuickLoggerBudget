package com.quicklogger.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestPrivacyTest {
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun doesNotDeclareInternetPermission() {
        assertFalse(manifest.contains("android.permission.INTERNET"))
    }

    @Test
    fun doesNotDeclareCameraPermission() {
        assertFalse(manifest.contains("android.permission.CAMERA"))
    }

    @Test
    fun allowBackupIsFalse() {
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
    }

    @Test
    fun doesNotDeclareAnyMediaOrStoragePermission() {
        // TakePicture and PickVisualMedia delegate to system apps. Declaring these
        // would force a runtime prompt before either contract could run.
        listOf(
            "READ_MEDIA_IMAGES",
            "READ_MEDIA_VIDEO",
            "READ_MEDIA_VISUAL_USER_SELECTED",
            "READ_EXTERNAL_STORAGE",
            "WRITE_EXTERNAL_STORAGE",
        ).forEach { permission ->
            assertFalse("manifest must not declare $permission", manifest.contains(permission))
        }
    }

    @Test
    fun fileProviderIsNotExportedAndGrantsPerUri() {
        assertTrue(manifest.contains("androidx.core.content.FileProvider"))
        assertTrue(manifest.contains("android:authorities=\"\${applicationId}.fileprovider\""))
        assertTrue(manifest.contains("android:exported=\"false\""))
        assertTrue(manifest.contains("android:grantUriPermissions=\"true\""))
    }

    @Test
    fun fileProviderExposesOnlyTheReceiptsAndExportsDirectories() {
        val paths = File("src/main/res/xml/file_paths.xml").readText()

        assertTrue(paths.contains("<files-path"))
        assertTrue(paths.contains("path=\"receipts/\""))
        assertTrue("CSV export needs a cache-path entry (ARCHITECTURE §7.3)", paths.contains("<cache-path"))
        assertTrue(paths.contains("path=\"exports/\""))
        assertFalse("external storage must never be exposed", paths.contains("external-path"))
        assertFalse("the whole files dir must not be exposed", paths.contains("path=\".\""))
    }

    @Test
    fun pointsAtDenyAllBackupRules() {
        assertTrue(manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        val backupRules = File("src/main/res/xml/backup_rules.xml").readText()
        val extractionRules = File("src/main/res/xml/data_extraction_rules.xml").readText()
        assertTrue(backupRules.contains("<exclude domain=\"database\""))
        assertTrue(extractionRules.contains("<cloud-backup>"))
        assertTrue(extractionRules.contains("<device-transfer>"))
    }
}
