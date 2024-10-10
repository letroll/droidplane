package ch.benediktkoeppel.code.droidplane.controller

import ch.benediktkoeppel.code.droidplane.MainViewModel
import ch.benediktkoeppel.code.droidplane.model.MindmapNode

interface OnRootNodeLoadedListener {
    fun rootNodeLoaded(viewModel: MainViewModel, rootNode: MindmapNode?)
}
