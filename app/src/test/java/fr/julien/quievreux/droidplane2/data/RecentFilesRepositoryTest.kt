package fr.julien.quievreux.droidplane2.data

import android.content.SharedPreferences
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

class RecentFilesRepositoryTest : KStringSpec() {
    init {
        "addRecentFile stores entry and returns in getRecentFiles" {
            val fakeStorage = mutableMapOf<String, String>()
            val editor: SharedPreferences.Editor = mockk(relaxed = true)
            val prefs: SharedPreferences = mockk(relaxed = true)

            every { prefs.getString(any(), any()) } answers {
                val key = firstArg<String>()
                fakeStorage[key] ?: secondArg()
            }
            every { editor.putString(any(), any()) } answers {
                val key = firstArg<String>()
                val value = secondArg<String>()
                fakeStorage[key] = value
                editor
            }
            every { prefs.edit() } returns editor

            val repo = RecentFilesRepositoryImpl(prefs)

            repo.getRecentFiles().shouldBeEmpty()

            repo.addRecentFile("content://test/file1.mm", "file1.mm")
            val recents = repo.getRecentFiles()

            recents shouldHaveSize 1
            recents.first().uriString shouldBe "content://test/file1.mm"
            recents.first().displayName shouldBe "file1.mm"
        }

        "getRecentFiles filters out files that no longer exist" {
            val fakeStorage = mutableMapOf<String, String>()
            val editor: SharedPreferences.Editor = mockk(relaxed = true)
            val prefs: SharedPreferences = mockk(relaxed = true)

            every { prefs.getString(any(), any()) } answers { fakeStorage[firstArg()] ?: secondArg() }
            every { editor.putString(any(), any()) } answers {
                fakeStorage[firstArg()] = secondArg()
                editor
            }
            every { prefs.edit() } returns editor

            val repo = RecentFilesRepositoryImpl(prefs)

            repo.addRecentFile("content://test/existing.mm", "existing.mm")
            repo.addRecentFile("content://test/deleted.mm", "deleted.mm")

            val filtered = repo.getRecentFiles(checker = { uri -> uri.contains("existing") })

            filtered shouldHaveSize 1
            filtered.first().displayName shouldBe "existing.mm"
        }

        "addRecentFile deduplicates and moves most recent to front" {
            val fakeStorage = mutableMapOf<String, String>()
            val editor: SharedPreferences.Editor = mockk(relaxed = true)
            val prefs: SharedPreferences = mockk(relaxed = true)

            every { prefs.getString(any(), any()) } answers { fakeStorage[firstArg()] ?: secondArg() }
            every { editor.putString(any(), any()) } answers {
                fakeStorage[firstArg()] = secondArg()
                editor
            }
            every { prefs.edit() } returns editor

            val repo = RecentFilesRepositoryImpl(prefs)

            repo.addRecentFile("content://test/file1.mm", "File 1")
            repo.addRecentFile("content://test/file2.mm", "File 2")
            repo.addRecentFile("content://test/file1.mm", "File 1 Updated")

            val recents = repo.getRecentFiles()

            recents shouldHaveSize 2
            recents[0].displayName shouldBe "File 1 Updated"
            recents[1].displayName shouldBe "File 2"
        }
    }
}
