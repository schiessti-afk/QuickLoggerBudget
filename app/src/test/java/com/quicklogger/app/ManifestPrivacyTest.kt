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
