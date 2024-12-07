package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import fr.julien.quievreux.droidplane2.helper.FakeDataSource
import kotlin.math.cos
import kotlin.math.sin
import fr.julien.quievreux.droidplane2.data.model.Node
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlin.math.PI

@Composable
fun MindMap(
    rootNode: Node,
    fetchText: (Node) -> String?,
) {
    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    val parentCenterX by remember { derivedStateOf { (parentSize.width / 2).toFloat() } }
    val parentCenterY by remember { derivedStateOf { (parentSize.height / 2).toFloat() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                parentSize = it
            }
    ) {
        // Calculate the minimum radius to prevent overlapping
        val rootText = fetchText(rootNode) ?: "Root"
        val rootSizePx = with(LocalDensity.current) { calculateCellSizePx(rootText) }
        val childSizesPx = rootNode.childNodes.map { fetchText(it)?.let { text -> calculateCellSizePx(text) } ?: rootSizePx }
        val maxChildSizePx = childSizesPx.maxOrNull() ?: rootSizePx

        // Calculate the radius to avoid overlaps with shortest connections
        val minRadius = rootSizePx / 2 + maxChildSizePx / 2 + 16.dp.toPx()
        val radius = maxOf(parentSize.width.coerceAtMost(parentSize.height) / 3f, minRadius)

        // Calculate positions for child nodes
        val childPositions = calculateCircularPositions(parentCenterX, parentCenterY, radius, rootNode.childNodes.size)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2  // Dynamically calculate the center X
            val centerY = size.height / 2 // Dynamically calculate the center Y

            // Draw lines connecting the root to its children
            childPositions.forEachIndexed { index, (childX, childY) ->
                drawConnection(
                    startX = centerX,
                    startY = centerY,
                    endX = childX,
                    endY = childY
                )
            }
        }

        // Root node
        Box(
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Cell(
                text = rootText,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Child nodes positioned in a circular layout
        rootNode.childNodes.forEachIndexed { index, childNode ->
            val (childX, childY) = childPositions[index]
            val childText = fetchText(childNode) ?: "Child ${index + 1}"
            val childSizePx = calculateCellSizePx(childText)

            Box(
                modifier = Modifier
                    .graphicsLayer(
                        translationX = childX - (childSizePx / 2),
                        translationY = childY - (childSizePx / 2)
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

@Composable fun calculateCellSizePx(
    text: String,
): Float {
    val textPaint = android.text.TextPaint().apply {
        textSize = 16.sp.toPx()
    }
    val textWidth = textPaint.measureText(text)
    val padding = 32f // Padding in pixels
    return textWidth + padding
}

@Composable fun TextUnit.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }

// Function to calculate circular positions for nodes
fun calculateCircularPositions(
    centerX: Float,
    centerY: Float,
    radius: Float,
    count: Int,
): List<Pair<Float, Float>> {
    val positions = mutableListOf<Pair<Float, Float>>()
    for (i in 0 until count) {
        val angle = 2 * PI * i / count
        val xOffset = (radius * cos(angle)).toFloat()
        val yOffset = (radius * sin(angle)).toFloat()
        val x = centerX + xOffset
        val y = centerY + yOffset
        positions.add(Pair(x, y))
    }
    return positions
}

@Composable //keep here, put in another module break preview
fun Dp.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }

// Function to draw a connection between two points
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
    )
}