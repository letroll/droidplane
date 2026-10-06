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
        val strokeWidth = strokeWidthDp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        connectors.forEach { connector ->
            val path = Path().apply {
                moveTo(connector.startX, connector.startY)
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
            drawPath(path = path, color = color, style = stroke)
        }
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
            ),
            BranchConnector(
                parentId = "root",
                childId = "c2",
                startX = 100f,
                startY = 100f,
                endX = 250f,
                endY = 160f,
                direction = BranchDirection.RIGHT,
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
