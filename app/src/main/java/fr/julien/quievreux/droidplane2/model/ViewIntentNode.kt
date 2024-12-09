package fr.julien.quievreux.droidplane2.model

import android.content.Intent
import fr.julien.quievreux.droidplane2.data.model.Node

data class ViewIntentNode(
    val intent: Intent,
    val node: Node,
)