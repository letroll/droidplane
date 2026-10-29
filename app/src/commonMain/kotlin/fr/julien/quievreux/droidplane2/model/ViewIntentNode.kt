package fr.julien.quievreux.droidplane2.model

import fr.julien.quievreux.droidplane2.data.model.Node

data class ViewIntentNode(
    val uriString: String? = null,
    val mimeType: String? = null,
    val node: Node,
)
