package fr.julien.quievreux.droidplane2.data.model

data class GenericHookElement(
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    val textContent: String? = null,
    val rawXml: String? = null,
)
