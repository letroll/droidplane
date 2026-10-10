package fr.julien.quievreux.droidplane2.ui.mindmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import kotlin.math.sqrt

@Composable
fun MindMapBranchLayer(
    connectors: List<BranchConnector>,
    color: Color = MaterialTheme.colorScheme.outline,
    strokeWidthDp: Dp = 2.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val defaultStrokeWidth = strokeWidthDp.toPx()

        connectors.toList().forEach { connector ->
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

data class ArrowLinkConnector(
    val sourceId: String,
    val destinationId: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val color: String? = null,
    val middleLabel: String? = null,
    val startArrow: Boolean = false,
    val endArrow: Boolean = true,
)

@Composable
fun MindMapArrowLinkLayer(
    arrowLinks: List<ArrowLinkConnector>,
    defaultColor: Color = Color(0xFFD32F2F),
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        arrowLinks.toList().forEach { link ->
            val linkColor = link.color?.let { parseHexColor(it) } ?: defaultColor
            val stroke = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            val dx = link.endX - link.startX
            val dy = link.endY - link.startY
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < 1f) return@forEach

            // Normal offset for smooth curve
            val nx = -dy / dist
            val ny = dx / dist
            val curveOffset = (dist * 0.2f).coerceIn(0f, 80f)

            val ctrlX = (link.startX + link.endX) / 2f + nx * curveOffset
            val ctrlY = (link.startY + link.endY) / 2f + ny * curveOffset

            val path = Path().apply {
                moveTo(link.startX, link.startY)
                quadraticTo(ctrlX, ctrlY, link.endX, link.endY)
            }
            drawPath(path = path, color = linkColor, style = stroke)

            // Draw End Arrowhead
            if (link.endArrow) {
                drawArrowHead(
                    tipX = link.endX,
                    tipY = link.endY,
                    fromX = ctrlX,
                    fromY = ctrlY,
                    color = linkColor,
                    size = 14f * density
                )
            }

            // Draw Start Arrowhead
            if (link.startArrow) {
                drawArrowHead(
                    tipX = link.startX,
                    tipY = link.startY,
                    fromX = ctrlX,
                    fromY = ctrlY,
                    color = linkColor,
                    size = 14f * density
                )
            }

            // Draw Middle Label
            if (!link.middleLabel.isNullOrBlank()) {
                val midCurveX = 0.25f * link.startX + 0.5f * ctrlX + 0.25f * link.endX
                val midCurveY = 0.25f * link.startY + 0.5f * ctrlY + 0.25f * link.endY

                val measuredText = textMeasurer.measure(
                    text = AnnotatedString(link.middleLabel),
                    style = TextStyle(fontSize = 11.sp, color = linkColor)
                )

                val padding = 4f * density
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(midCurveX - measuredText.size.width / 2f - padding, midCurveY - measuredText.size.height / 2f - padding),
                    size = Size(measuredText.size.width + padding * 2f, measuredText.size.height + padding * 2f),
                    cornerRadius = CornerRadius(4f * density, 4f * density)
                )

                drawText(
                    textLayoutResult = measuredText,
                    topLeft = Offset(midCurveX - measuredText.size.width / 2f, midCurveY - measuredText.size.height / 2f)
                )
            }
        }
    }
}

private fun DrawScope.drawArrowHead(
    tipX: Float,
    tipY: Float,
    fromX: Float,
    fromY: Float,
    color: Color,
    size: Float,
) {
    val dx = tipX - fromX
    val dy = tipY - fromY
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1f) return

    val ux = dx / len
    val uy = dy / len

    val arrowPath = Path().apply {
        moveTo(tipX, tipY)
        lineTo(tipX - size * ux + size * 0.45f * uy, tipY - size * uy - size * 0.45f * ux)
        lineTo(tipX - size * ux - size * 0.45f * uy, tipY - size * uy + size * 0.45f * ux)
        close()
    }
    drawPath(path = arrowPath, color = color)
}

@Preview(name = "Arrow Link Layer Preview", showBackground = true)
@Composable
fun PreviewMindMapArrowLinkLayer() {
    ContrastAwareReplyTheme {
        val sampleArrowLinks = listOf(
            ArrowLinkConnector(
                sourceId = "c1",
                destinationId = "c2",
                startX = 50f,
                startY = 50f,
                endX = 250f,
                endY = 150f,
                color = "#D32F2F",
                middleLabel = "relates to",
                endArrow = true
            )
        )
        MindMapArrowLinkLayer(
            arrowLinks = sampleArrowLinks,
            modifier = Modifier.size(300.dp, 200.dp)
        )
    }
}
