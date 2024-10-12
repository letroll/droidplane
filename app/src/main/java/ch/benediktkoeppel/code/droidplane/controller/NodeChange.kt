package ch.benediktkoeppel.code.droidplane.controller

import ch.benediktkoeppel.code.droidplane.model.MindmapNode

sealed class NodeChange{
    data class RichContentChanged(val node: MindmapNode): NodeChange()
    data class NodeStyleChanged(val node: MindmapNode): NodeChange()
    data class SubscribeNodeRichContentChanged(val node: MindmapNode): NodeChange()
}