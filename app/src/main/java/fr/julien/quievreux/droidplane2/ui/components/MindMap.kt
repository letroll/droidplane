package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MindMap() {
    val childrenCount = 10  // Number of children around the root
    val cellSizeDp = 100.dp  // Size of each cell in DP

    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    val parentCenterX by remember { derivedStateOf { (parentSize.width / 2).toFloat() } }
    val parentCenterY by remember { derivedStateOf { (parentSize.height / 2).toFloat() } }

    if (LocalInspectionMode.current) {

    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                parentSize = it
            }
    ) {
        // Convert cell size to pixels
        val cellSizePx = cellSizeDp.toPx()

        // Calculate the minimum radius to prevent overlapping
        val minRadius = (cellSizePx / 2) / sin(PI / childrenCount).toFloat() + cellSizePx

        // Radius proportional to the current size of the parent but at least minRadius
        val radius = maxOf(parentSize.width.coerceAtMost(parentSize.height) / 3f, minRadius)

        // Calculate positions for child nodes
        val childPositions = calculateCircularPositions(parentCenterX, parentCenterY, radius, childrenCount)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2  // Dynamically calculate the center X
            val centerY = size.height / 2 // Dynamically calculate the center Y

            // Draw lines connecting the root to its children
            childPositions.forEach { (childX, childY) ->
                drawConnection(
                    startX = centerX,
                    startY = centerY,
                    endX = childX,
                    endY = childY
                )
            }
        }

        // Root node
        Cell(
            text = "Root",
            modifier = Modifier
                .align(Alignment.Center)
                .size(cellSizeDp)
        )

        // Child nodes positioned in a circular layout
        childPositions.forEachIndexed { index, (childX, childY) ->
            Box(
                modifier = Modifier
                    .graphicsLayer(
                        translationX = childX - (cellSizeDp.toPx() / 2),
                        translationY = childY - (cellSizeDp.toPx() / 2)
                    )
                    .size(cellSizeDp)
            ) {
                Cell(text = "Child ${index + 1}")
            }
        }
    }
}

@Composable //keep here, put in another module break preview
fun Dp.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }

// Function to calculate circular positions for nodes
fun calculateCircularPositions(centerX: Float, centerY: Float, radius: Float, count: Int): List<Pair<Float, Float>> {
    val positions = mutableListOf<Pair<Float, Float>>()
    val axeXDirection = 1
    val axeYDirection = 1
    val rotationDirection = 1
    for (i in 0 until count) {
        val angle = (2 * rotationDirection) * PI * i / count// Angle for this item
        val xOffset = (radius * cos(angle)* axeXDirection).toFloat()
        val yOffset = (radius * sin(angle * axeYDirection)).toFloat()
        val x = centerX + xOffset
        val y = centerY + yOffset
        positions.add(Pair(x, y))
    }
    return positions
}

// Function to draw a connection between two points
fun DrawScope.drawConnection(startX: Float, startY: Float, endX: Float, endY: Float) {
    drawLine(
        color = Color.Black,
        start = androidx.compose.ui.geometry.Offset(startX, startY),
        end = androidx.compose.ui.geometry.Offset(endX, endY),
        strokeWidth = 4f
    )
}


@Preview(showBackground = true, widthDp = 800, heightDp = 800)
@Composable
fun PreviewMindMap() {
    MindMap()
}