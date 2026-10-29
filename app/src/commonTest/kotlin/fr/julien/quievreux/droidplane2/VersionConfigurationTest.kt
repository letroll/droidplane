package fr.julien.quievreux.droidplane2

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VersionConfigurationTest {

    private fun findProjectRoot(): File {
        var current: File? = File(".").canonicalFile
        while (current != null) {
            if (File(current, "settings.gradle.kts").exists()) {
                return current
            }
            current = current.parentFile
        }
        return File(".").canonicalFile
    }

    @Test
    fun `gradle defines appVersionCode and appVersionName and manifest does not define them`() {
        val root = findProjectRoot()
        val gradleFile = File(root, "app/build.gradle.kts")
        val manifestFile = File(root, "app/src/androidMain/AndroidManifest.xml")

        assertTrue(gradleFile.exists(), "app/build.gradle.kts must exist")
        assertTrue(manifestFile.exists(), "app/src/androidMain/AndroidManifest.xml must exist")

        val gradleContent = gradleFile.readText()
        val manifestContent = manifestFile.readText()

        // Verify Gradle declares version variables
        assertTrue(gradleContent.contains("val appVersionCode"), "app/build.gradle.kts must declare val appVersionCode")
        assertTrue(gradleContent.contains("val appVersionName"), "app/build.gradle.kts must declare val appVersionName")
        assertTrue(gradleContent.contains("versionCode = appVersionCode"), "defaultConfig must use appVersionCode")
        assertTrue(gradleContent.contains("versionName = appVersionName"), "defaultConfig must use appVersionName")

        // Extract values
        val versionCodeRegex = Regex("""val\s+appVersionCode\s*=\s*(\d+)""")
        val versionNameRegex = Regex("""val\s+appVersionName\s*=\s*"([^"]+)"""")

        val codeMatch = versionCodeRegex.find(gradleContent)
        val nameMatch = versionNameRegex.find(gradleContent)

        assertNotNull(codeMatch, "Must find appVersionCode in build.gradle.kts")
        assertNotNull(nameMatch, "Must find appVersionName in build.gradle.kts")

        val code = codeMatch.groupValues[1].toInt()
        val name = nameMatch.groupValues[1]

        assertTrue(code > 0, "appVersionCode must be greater than 0")
        assertEquals("1.0.$code", name, "appVersionName must equal 1.0.<appVersionCode>")

        // Verify AndroidManifest.xml has NO hardcoded versionCode or versionName
        assertFalse(manifestContent.contains("android:versionCode"), "AndroidManifest.xml must not contain android:versionCode")
        assertFalse(manifestContent.contains("android:versionName"), "AndroidManifest.xml must not contain android:versionName")
    }
}
