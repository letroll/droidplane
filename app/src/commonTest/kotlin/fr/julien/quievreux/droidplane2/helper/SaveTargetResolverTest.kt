package fr.julien.quievreux.droidplane2.helper

import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SaveTargetResolverTest : KStringSpec() {

    init {
        "isSameOrAutoIncrementedFileName returns true for exact match and Android SAF auto-incremented (n) suffixes" {
            assertTrue(SaveTargetResolver.isSameOrAutoIncrementedFileName("project.mm", "project.mm"))
            assertTrue(SaveTargetResolver.isSameOrAutoIncrementedFileName("project.mm", "project (1).mm"))
            assertTrue(SaveTargetResolver.isSameOrAutoIncrementedFileName("project.mm", "project(1).mm"))
            assertTrue(SaveTargetResolver.isSameOrAutoIncrementedFileName("project.mm", "project (12).mm"))
            assertTrue(SaveTargetResolver.isSameOrAutoIncrementedFileName("My Notes.mm", "My Notes (2).mm"))
            assertTrue(SaveTargetResolver.isSameOrAutoIncrementedFileName("My Notes.mm", "My Notes (1)"))
            assertTrue(SaveTargetResolver.isSameOrAutoIncrementedFileName("My Notes.mm", "My Notes.mm (1)"))
        }

        "isSameOrAutoIncrementedFileName returns false for genuinely different filenames" {
            assertFalse(SaveTargetResolver.isSameOrAutoIncrementedFileName("project.mm", "project_v2.mm"))
            assertFalse(SaveTargetResolver.isSameOrAutoIncrementedFileName("project.mm", "other (1).mm"))
            assertFalse(SaveTargetResolver.isSameOrAutoIncrementedFileName("project.mm", "project (0).mm"))
        }

        "resolve returns WriteNewFile when originalUriString is null (brand new mindmap)" {
            val result = SaveTargetResolver.resolve(
                originalUriString = null,
                originalDisplayName = null,
                pickedUriString = "content://docs/new_map",
                pickedDisplayName = "new_map.mm",
            )

            val writeNew = assertIs<SaveResolution.WriteNewFile>(result)
            assertEquals("content://docs/new_map", writeNew.targetUriString)
            assertEquals("new_map.mm", writeNew.displayName)
        }

        "resolve returns WriteNewFile when originalUriString uses android.resource scheme (demo mindmap)" {
            val result = SaveTargetResolver.resolve(
                originalUriString = "android.resource://pkg/raw/example.mm",
                originalDisplayName = "example.mm",
                pickedUriString = "content://docs/example_1",
                pickedDisplayName = "example (1).mm",
            )

            val writeNew = assertIs<SaveResolution.WriteNewFile>(result)
            assertEquals("content://docs/example_1", writeNew.targetUriString)
            assertEquals("example (1).mm", writeNew.displayName)
        }

        "resolve returns DirectOverwrite when pickedUriString equals originalUriString" {
            val result = SaveTargetResolver.resolve(
                originalUriString = "content://docs/42",
                originalDisplayName = "roadmap.mm",
                pickedUriString = "content://docs/42",
                pickedDisplayName = "roadmap.mm",
            )

            val direct = assertIs<SaveResolution.DirectOverwrite>(result)
            assertEquals("content://docs/42", direct.targetUriString)
            assertEquals("roadmap.mm", direct.displayName)
        }

        "resolve returns ConfirmOverwrite when picker created a (n) duplicate of originalDisplayName" {
            val result = SaveTargetResolver.resolve(
                originalUriString = "content://docs/42",
                originalDisplayName = "roadmap.mm",
                pickedUriString = "content://docs/43",
                pickedDisplayName = "roadmap (1).mm",
            )

            val confirm = assertIs<SaveResolution.ConfirmOverwrite>(result)
            assertEquals("content://docs/42", confirm.originalUriString)
            assertEquals("roadmap.mm", confirm.originalDisplayName)
            assertEquals("content://docs/43", confirm.duplicatePickerUriString)
            assertEquals("roadmap (1).mm", confirm.duplicateDisplayName)
        }

        "resolve returns WriteNewFile when user enters a different filename in the picker" {
            val result = SaveTargetResolver.resolve(
                originalUriString = "content://docs/42",
                originalDisplayName = "roadmap.mm",
                pickedUriString = "content://docs/44",
                pickedDisplayName = "roadmap_2027.mm",
            )

            val writeNew = assertIs<SaveResolution.WriteNewFile>(result)
            assertEquals("content://docs/44", writeNew.targetUriString)
            assertEquals("roadmap_2027.mm", writeNew.displayName)
        }
    }
}
