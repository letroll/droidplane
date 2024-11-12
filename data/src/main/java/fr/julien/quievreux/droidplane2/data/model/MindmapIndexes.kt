package fr.julien.quievreux.droidplane2.data.model

class MindmapIndexes(
    val nodesByIdIndex: Map<String, MindmapNode>,
    val nodesByNumericIndex: Map<Int, MindmapNode>,
)
