package fr.julien.quievreux.droidplane2.controller

import fr.julien.quievreux.droidplane2.model.MindmapNode

sealed class NodeChange{
    data class RichContentChanged(val node: MindmapNode): NodeChange()
    data class NodeStyleChanged(val node: MindmapNode): NodeChange()
    data class SubscribeNodeRichContentChanged(val node: MindmapNode): NodeChange()
}