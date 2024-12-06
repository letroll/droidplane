package fr.julien.quievreux.droidplane2.model

import fr.julien.quievreux.droidplane2.data.model.Node

sealed class ContextMenuAction {
    data class CopyText(val text: String) : ContextMenuAction()
    data class Edit(
        val node: Node,
    ) : ContextMenuAction()
    data class NodeLink(
        val node: Node,
    ) : ContextMenuAction()
}

data class ContextMenuDropDownItem(
    val text: String,
    val action: ContextMenuAction? = null,
)