package fr.julien.quievreux.droidplane2.data.model

data class ConnectorLink(
    val destinationId: String,
    val color: String? = null,
    val startArrow: String? = null,
    val endArrow: String? = null,
    val startInclination: String? = null,
    val endInclination: String? = null,
    val sourceLabel: String? = null,
    val middleLabel: String? = null,
    val targetLabel: String? = null,
    val edgeLike: Boolean = false,
)
