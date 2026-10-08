package fr.julien.quievreux.droidplane2.ui.mindmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.data.model.CloudProperties
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

data class CloudEnclosure(
    val node: Node,
    val cloud: CloudProperties,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

@Composable
fun MindMapCloudLayer(
    clouds: List<CloudEnclosure>,
    modifier: Modifier = Modifier,
) {
    if (clouds.isEmpty()) return

    Canvas(modifier = modifier) {
        // Draw larger clouds first so nested clouds render on top
        val sortedClouds = clouds.sortedByDescending { (it.right - it.left) * (it.bottom - it.top) }

        sortedClouds.forEach { enclosure ->
            val cloudColor = parseHexColor(enclosure.cloud.color.orEmpty()) ?: Color(0xFFFFCC80)
            val left = enclosure.left
            val top = enclosure.top
            val width = enclosure.right - enclosure.left
            val height = enclosure.bottom - enclosure.top

            if (width <= 0 || height <= 0) return@forEach

            val cornerRadius = when (enclosure.cloud.shape) {
                "ARC" -> CornerRadius(32.dp.toPx(), 32.dp.toPx())
                "RECT" -> CornerRadius(8.dp.toPx(), 8.dp.toPx())
                "STAR" -> CornerRadius(16.dp.toPx(), 16.dp.toPx())
                else -> CornerRadius(24.dp.toPx(), 24.dp.toPx())
            }

            // 1. Soft translucent background fill
            drawRoundRect(
                color = cloudColor.copy(alpha = 0.22f),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = cornerRadius,
                style = Fill
            )

            // 2. Crisp boundary stroke
            val strokeWidth = (enclosure.cloud.width ?: 2).toFloat() * density
            val pathEffect = if (enclosure.cloud.shape == "STAR") {
                PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
            } else {
                null
            }

            drawRoundRect(
                color = cloudColor.copy(alpha = 0.85f),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = cornerRadius,
                style = Stroke(
                    width = strokeWidth.coerceAtLeast(2f),
                    pathEffect = pathEffect
                )
            )
        }
    }
}

private fun parseHexColor(hex: String): Color? {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> 0xFF000000.toInt() or clean.toInt(16)
            8 -> clean.toLong(16).toInt()
            else -> return null
        }
        Color(colorInt)
    } catch (e: Exception) {
        null
    }
}

@Preview(name = "Cloud Layer Preview", showBackground = true)
@Composable
fun PreviewMindMapCloudLayer() {
    ContrastAwareReplyTheme {
        val sampleClouds = listOf(
            CloudEnclosure(
                node = Node(
                    parentNode = null,
                    id = "demo_node",
                    numericId = 1,
                    text = "Demo Cloud Node",
                    creationDate = 0L,
                    modificationDate = 0L
                ),
                cloud = CloudProperties(color = "#FFAA00", shape = "ROUND_RECT", width = 2),
                left = 50f,
                top = 50f,
                right = 350f,
                bottom = 250f
            )
        )

        MindMapCloudLayer(
            clouds = sampleClouds,
            modifier = Modifier.fillMaxSize()
        )
    }
}
