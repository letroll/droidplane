package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun MindMapScreen() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()

//    MindMap(screenWidth = screenWidth, screenHeight = screenHeight)
    MindMap()
}