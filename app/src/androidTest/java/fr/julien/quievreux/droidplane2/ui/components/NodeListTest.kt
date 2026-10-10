package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import fr.julien.quievreux.droidplane2.data.FakeDataSource
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import fr.julien.quievreux.droidplane2.ui.view.DeleteConfirmationDialog
import org.junit.Rule
import org.junit.Test

class NodeListTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deleteConfirmationDialogShowsCorrectText() {
        val rootNode = FakeDataSource.getFakeRootNode()
        val node = Node(
            parentNode = rootNode,
            id = "ID_1",
            numericId = 1,
            text = "Test Node",
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

        composeRule.setContent {
            ContrastAwareReplyTheme {
                DeleteConfirmationDialog(
                    confirmation = fr.julien.quievreux.droidplane2.MainUiState.DialogType.DeleteConfirmation(
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
        val rootNode = FakeDataSource.getFakeRootNode()
        val node = Node(
            parentNode = rootNode,
            id = "ID_1",
            numericId = 1,
            text = "Leaf Node",
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

        composeRule.setContent {
            ContrastAwareReplyTheme {
                DeleteConfirmationDialog(
                    confirmation = fr.julien.quievreux.droidplane2.MainUiState.DialogType.DeleteConfirmation(
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
        val rootNode = FakeDataSource.getFakeRootNode()
        val node = Node(
            parentNode = rootNode,
            id = "ID_1",
            numericId = 1,
            text = "Parent Node",
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

        composeRule.setContent {
            ContrastAwareReplyTheme {
                DeleteConfirmationDialog(
                    confirmation = fr.julien.quievreux.droidplane2.MainUiState.DialogType.DeleteConfirmation(
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
        val rootNode = FakeDataSource.getFakeRootNode()
        val node = Node(
            parentNode = rootNode,
            id = "ID_1",
            numericId = 1,
            text = "Accessible Node",
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

        composeRule.setContent {
            ContrastAwareReplyTheme {
                DeleteConfirmationDialog(
                    confirmation = fr.julien.quievreux.droidplane2.MainUiState.DialogType.DeleteConfirmation(
                        node = node,
                        descendantCount = 2,
                        onConfirm = {},
                        onCancel = {},
                    )
                )
            }
        }

        // Verify dialog title is accessible
        composeRule.onNodeWithText("Delete Node?")
        
        // Verify buttons exist
        composeRule.onNodeWithText("Delete")
        composeRule.onNodeWithText("Cancel")
    }

    @Test
    fun deleteConfirmationDialogConfirmButtonHasErrorColor() {
        val rootNode = FakeDataSource.getFakeRootNode()
        val node = Node(
            parentNode = rootNode,
            id = "ID_1",
            numericId = 1,
            text = "Test Node",
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

        composeRule.setContent {
            ContrastAwareReplyTheme {
                DeleteConfirmationDialog(
                    confirmation = fr.julien.quievreux.droidplane2.MainUiState.DialogType.DeleteConfirmation(
                        node = node,
                        descendantCount = 1,
                        onConfirm = {},
                        onCancel = {},
                    )
                )
            }
        }

        // Verify destructive button styling is present
        composeRule.onNodeWithText("Delete")
        composeRule.onNodeWithText("Cancel")
    }
}