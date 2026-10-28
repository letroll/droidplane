package fr.julien.quievreux.droidplane2.ui.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import fr.julien.quievreux.droidplane2.MainUiState
import fr.julien.quievreux.droidplane2.data.FakeDataSource
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import org.junit.Rule
import org.junit.Test

class DeleteConfirmationDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun createSampleNode(text: String): Node {
        val rootNode = FakeDataSource.getFakeRootNode()
        return Node(
            parentNode = rootNode,
            id = "ID_1",
            numericId = 1,
            text = text,
            childNodes = mutableListOf(),
            link = null,
            treeIdAttribute = null,
            richTextContents = mutableListOf(),
            richContentType = null,
            iconNames = mutableListOf(),
            creationDate = 0,
            modificationDate = 0,
            isBold = false,
            isItalic = false,
            position = null,
            arrowLinkDestinationIds = mutableListOf(),
            arrowLinkDestinationNodes = mutableListOf(),
            arrowLinkIncomingNodes = mutableListOf(),
        )
    }

    @Test
    fun deleteConfirmationDialogShowsCorrectText() {
        val node = createSampleNode("Test Node")

        composeRule.setContent {
            ContrastAwareReplyTheme {
                DeleteConfirmationDialog(
                    confirmation = MainUiState.DialogType.DeleteConfirmation(
                        node = node,
                        descendantCount = 3,
                        onConfirm = {},
                        onCancel = {},
                    )
                )
            }
        }

        composeRule.onNodeWithText("Delete Node?")
        composeRule.onNodeWithText("Delete \"Test Node\" and 3 descendants? This action cannot be undone.")
        composeRule.onNodeWithText("Delete")
        composeRule.onNodeWithText("Cancel")
    }

    @Test
    fun deleteConfirmationDialogShowsNoDescendantsText() {
        val node = createSampleNode("Leaf Node")

        composeRule.setContent {
            ContrastAwareReplyTheme {
                DeleteConfirmationDialog(
                    confirmation = MainUiState.DialogType.DeleteConfirmation(
                        node = node,
                        descendantCount = 0,
                        onConfirm = {},
                        onCancel = {},
                    )
                )
            }
        }

        composeRule.onNodeWithText("Delete \"Leaf Node\" and no descendants? This action cannot be undone.")
    }

    @Test
    fun deleteConfirmationDialogShowsOneDescendantText() {
        val node = createSampleNode("Parent Node")

        composeRule.setContent {
            ContrastAwareReplyTheme {
                DeleteConfirmationDialog(
                    confirmation = MainUiState.DialogType.DeleteConfirmation(
                        node = node,
                        descendantCount = 1,
                        onConfirm = {},
                        onCancel = {},
                    )
                )
            }
        }

        composeRule.onNodeWithText("Delete \"Parent Node\" and 1 descendant? This action cannot be undone.")
    }

    @Test
    fun deleteConfirmationDialogHasAccessibilityContentDescriptions() {
        val node = createSampleNode("Accessible Node")

        composeRule.setContent {
            ContrastAwareReplyTheme {
                DeleteConfirmationDialog(
                    confirmation = MainUiState.DialogType.DeleteConfirmation(
                        node = node,
                        descendantCount = 2,
                        onConfirm = {},
                        onCancel = {},
                    )
                )
            }
        }

        composeRule.onNodeWithText("Delete Node?")
        composeRule.onNodeWithText("Delete")
        composeRule.onNodeWithText("Cancel")
    }

    @Test
    fun deleteConfirmationDialogConfirmButtonHasErrorColor() {
        val node = createSampleNode("Test Node")

        composeRule.setContent {
            ContrastAwareReplyTheme {
                DeleteConfirmationDialog(
                    confirmation = MainUiState.DialogType.DeleteConfirmation(
                        node = node,
                        descendantCount = 1,
                        onConfirm = {},
                        onCancel = {},
                    )
                )
            }
        }

        composeRule.onNodeWithText("Delete")
        composeRule.onNodeWithText("Cancel")
    }
}
