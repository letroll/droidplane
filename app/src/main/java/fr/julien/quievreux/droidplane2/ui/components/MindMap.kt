package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import fr.julien.quievreux.droidplane2.data.FakeDataSource
import fr.julien.quievreux.droidplane2.data.model.Node
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

@Composable
fun MindMap(
    rootNode: Node,
    fetchText: (Node) -> String?,
    modifier: Modifier = Modifier,
    paddingStart: Dp = 16.dp,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    var rootNodeSize by remember { mutableStateOf(IntSize.Zero) }
    val parentCenterY by remember { derivedStateOf { (parentSize.height / 2).toFloat() } }
    val paddingLeftPx = with(LocalDensity.current) { paddingStart.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.onBackground)
            .onSizeChanged {
                parentSize = it
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    offsetX += pan.x
                    offsetY += pan.y
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                }
            }
            .graphicsLayer(
                translationX = offsetX,
                translationY = offsetY,
                scaleX = scale,
                scaleY = scale,
            )
    ) {
        val rootText = fetchText(rootNode) ?: "Root"
        val rootSizePx = calculateCellSizePx(fetchText(rootNode) ?: "Root")

        Cell(
            text = rootText,
            modifier = Modifier
                .graphicsLayer(translationX = paddingLeftPx, translationY = parentCenterY - rootSizePx / 2)
                .onSizeChanged {
                    rootNodeSize = it
                }
        )

        val childCount = rootNode.childNodes.size
        val totalSpacing = 20.dp.toPx() * (childCount - 1) // Total space between children
        val totalChildHeight = rootNode.childNodes.size * rootNodeSize.height
        val availableSpace = totalSpacing + totalChildHeight
        val topOffset = parentCenterY - availableSpace / 2
        val verticalSpacing = 40.dp.toPx()

        Canvas(modifier = Modifier.fillMaxSize()) {
            rootNode.childNodes.forEachIndexed { index, childNode ->
                val startX = paddingLeftPx + rootNodeSize.width
                val startY = parentCenterY
                val endX = startX + verticalSpacing// Horizontal spacing
                val endY = topOffset + index * (rootNodeSize.height) + (index* (rootNodeSize.height / 2)) // Consistent spacing

                drawConnection(
                    startX = startX,
                    startY = startY,
                    endX = endX,
                    endY = endY,
                )
            }
        }

        rootNode.childNodes.forEachIndexed { index, childNode ->
            val childText = fetchText(childNode) ?: "Child ${index + 1}"
            val gapFromRoot = 40.dp.toPx() // Gap between root and children
            val translationX = paddingLeftPx + rootNodeSize.width + gapFromRoot // Horizontal spacing
            val translationY = topOffset + index * (rootNodeSize.height + 20.dp.toPx())

            Box(
                modifier = Modifier
                    .graphicsLayer(
                        translationX = translationX,
                        translationY = translationY,
                    )
            ) {
                Cell(
                    text = childText,
                )
            }
        }
    }
}

@Composable
fun Dp.toPx(): Float {
    return with(LocalDensity.current) { this@toPx.toPx() }
}

fun calculateCellSizePx(
    text: String,
): Float {
    val textPaint = android.text.TextPaint().apply {
        textSize = 16f
    }
    val textWidth = textPaint.measureText(text)
    val padding = 32f // Padding in pixels
    return textWidth + padding
}

fun calculateCellHeightPx(
    text: String,
    callback: (Float) -> Unit
) {
    val textPaint = android.text.TextPaint().apply {
        textSize = 16f
    }
    val lineHeight = textPaint.fontMetrics.run { descent - ascent + leading }
    val padding = 32f // Padding in pixels
    callback(lineHeight + padding)
}

@Composable
fun Cell(
    text: String,
    modifier: Modifier = Modifier,
) {
    var textSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .wrapContentSize()
            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
            .border(2.dp, Color.Black, shape = RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(fontSize = 16.sp),
            modifier = Modifier
                .padding(8.dp)
                .onSizeChanged { size ->
                    textSize = size
                },
        )
    }
}

fun DrawScope.drawConnection(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
) {
    drawLine(
        color = Color.Black,
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = 4f
    )
}

@Preview(
    showBackground = true,
    widthDp = 800,
    heightDp = 800,
)
@Composable
fun PreviewMindMap() {
    MindMap(
        rootNode = FakeDataSource.fakeRootNode(),
        fetchText = { node ->
            node.text
        },
        paddingStart = 16.dp
    )
}
