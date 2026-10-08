package fr.julien.quievreux.droidplane2.ui.mindmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

@Composable
fun MindMapBranchLayer(
    connectors: List<BranchConnector>,
    color: Color = MaterialTheme.colorScheme.outline,
    strokeWidthDp: Dp = 2.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val defaultStrokeWidth = strokeWidthDp.toPx()

        connectors.forEach { connector ->
            // Skip hidden edges
            if (connector.style == "hide_edge") return@forEach

            val edgeColor = connector.color?.let { parseHexColor(it) } ?: color
            val edgeStrokeWidth = connector.width?.toFloatOrNull()?.let { it * density } ?: defaultStrokeWidth
            val stroke = Stroke(width = edgeStrokeWidth, cap = StrokeCap.Round)

            val path = Path().apply {
                moveTo(connector.startX, connector.startY)
                if (connector.style == "linear" || connector.style == "sharp-linear") {
                    lineTo(connector.endX, connector.endY)
                } else {
                    val midX = connector.startX + (connector.endX - connector.startX) * 0.5f
                    cubicTo(
                        x1 = midX,
                        y1 = connector.startY,
                        x2 = midX,
                        y2 = connector.endY,
                        x3 = connector.endX,
                        y3 = connector.endY,
                    )
                }
            }
            drawPath(path = path, color = edgeColor, style = stroke)
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

@Preview(name = "Branch Layer Preview", showBackground = true)
@Composable
fun PreviewMindMapBranchLayer() {
    ContrastAwareReplyTheme {
        val sampleConnectors = listOf(
            BranchConnector(
                parentId = "root",
                childId = "c1",
                startX = 100f,
                startY = 100f,
                endX = 250f,
                endY = 40f,
                direction = BranchDirection.RIGHT,
                style = "bezier",
                color = "#0033AA"
            ),
            BranchConnector(
                parentId = "root",
                childId = "c2",
                startX = 100f,
                startY = 100f,
                endX = 250f,
                endY = 160f,
                direction = BranchDirection.RIGHT,
                style = "linear",
                color = "#D32F2F"
            ),
            BranchConnector(
                parentId = "root",
                childId = "c3",
                startX = 50f,
                startY = 100f,
                endX = -100f,
                endY = 100f,
                direction = BranchDirection.LEFT,
            ),
        )

        MindMapBranchLayer(
            connectors = sampleConnectors,
            modifier = Modifier.size(300.dp, 200.dp),
        )
    }
}
