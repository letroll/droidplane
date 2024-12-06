package fr.julien.quievreux.droidplane2.controller

import fr.julien.quievreux.droidplane2.data.model.Node

sealed class NodeChange{
    data class RichContentChanged(val node: Node): NodeChange()
    data class NodeStyleChanged(val node: Node): NodeChange()
    data class SubscribeNodeRichContentChanged(val node: Node): NodeChange()
}