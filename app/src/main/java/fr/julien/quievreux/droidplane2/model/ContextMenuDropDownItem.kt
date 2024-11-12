package fr.julien.quievreux.droidplane2.model

import fr.julien.quievreux.droidplane2.data.model.MindmapNode

sealed class ContextMenuAction {
    data class CopyText(val text: String) : ContextMenuAction()
    data class Edit(
        val node: MindmapNode,
    ) : ContextMenuAction()
    data class NodeLink(
        val node: MindmapNode,
    ) : ContextMenuAction()
}

data class ContextMenuDropDownItem(
    val text: String,
    val action: ContextMenuAction? = null,
)