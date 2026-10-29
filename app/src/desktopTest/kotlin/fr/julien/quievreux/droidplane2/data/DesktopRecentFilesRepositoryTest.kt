package fr.julien.quievreux.droidplane2.data

import java.io.File
import java.nio.file.Files
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopRecentFilesRepositoryTest {

    private lateinit var prefs: Preferences
    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        prefs = Preferences.userRoot().node("fr/julien/quievreux/droidplane2/test_${System.nanoTime()}")
        tempDir = Files.createTempDirectory("droidplane_desktop_test").toFile()
    }

    @AfterTest
    fun tearDown() {
        runCatching { prefs.removeNode() }
        tempDir.deleteRecursively()
    }

    @Test
    fun `addRecentFile stores and retrieves recent files in MRU order`() {
        val repo = DesktopRecentFilesRepository(preferences = prefs)

        repo.addRecentFile("file:///tmp/first.mm", "first.mm")
        Thread.sleep(5)
        repo.addRecentFile("file:///tmp/second.mm", "second.mm")

        val recent = repo.getRecentFiles(checker = { true })
        assertEquals(2, recent.size)
        assertEquals("second.mm", recent[0].displayName)
        assertEquals("first.mm", recent[1].displayName)
    }

    @Test
    fun `removeRecentFile deletes matching entry`() {
        val repo = DesktopRecentFilesRepository(preferences = prefs)

        repo.addRecentFile("file:///tmp/first.mm", "first.mm")
        repo.removeRecentFile("file:///tmp/first.mm")

        assertTrue(repo.getRecentFiles(checker = { true }).isEmpty())
    }

    @Test
    fun `createDesktopFileChecker verifies file URI and direct path existence`() {
        val existingFile = File(tempDir, "map.mm").apply { writeText("<map/>") }
        val checker = createDesktopFileChecker()

        assertTrue(checker(existingFile.toURI().toString()))
        assertTrue(checker(existingFile.absolutePath))
        assertFalse(checker(File(tempDir, "missing.mm").toURI().toString()))
    }
}
