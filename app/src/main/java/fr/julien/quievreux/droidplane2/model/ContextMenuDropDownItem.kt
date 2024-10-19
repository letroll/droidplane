package fr.julien.quievreux.droidplane2.model

sealed class ContextMenuAction {
    data class CopyText(val text:String) : ContextMenuAction()
    data object Edit : ContextMenuAction()
    data class NodeLink(
        val node: MindmapNode,
    ) : ContextMenuAction()
}

data class ContextMenuDropDownItem(
    val text: String,
    val action: ContextMenuAction,
)