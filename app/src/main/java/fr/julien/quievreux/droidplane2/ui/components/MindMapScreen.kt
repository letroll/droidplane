package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import fr.julien.quievreux.droidplane2.helper.FakeDataSource

@Composable
fun MindMapScreen() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()

//    MindMap(screenWidth = screenWidth, screenHeight = screenHeight)
    MindMap(rootNode = FakeDataSource.fakeRootNode(), fetchText = { node ->
        node.text
    })
}