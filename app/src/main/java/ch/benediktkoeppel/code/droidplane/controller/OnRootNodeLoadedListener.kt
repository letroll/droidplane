package ch.benediktkoeppel.code.droidplane.controller

import ch.benediktkoeppel.code.droidplane.model.Mindmap
import ch.benediktkoeppel.code.droidplane.model.MindmapNode

interface OnRootNodeLoadedListener {
    fun rootNodeLoaded(mindmap: Mindmap?, rootNode: MindmapNode?)
}
