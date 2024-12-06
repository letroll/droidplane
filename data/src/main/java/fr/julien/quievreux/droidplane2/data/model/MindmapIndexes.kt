package fr.julien.quievreux.droidplane2.data.model

class MindmapIndexes(
    val nodesByIdIndex: Map<String, Node>,
    val nodesByNumericIndex: Map<Int, Node>,
)
