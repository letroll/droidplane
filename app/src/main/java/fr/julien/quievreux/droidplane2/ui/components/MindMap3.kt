//package fr.julien.quievreux.droidplane2.ui.components
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.gestures.detectTransformGestures
//import androidx.compose.foundation.gestures.rememberTransformableState
//import androidx.compose.foundation.horizontalScroll
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.drawscope.DrawScope
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.layout.onSizeChanged
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.unit.*
//import fr.julien.quievreux.droidplane2.data.FakeDataSource
//import kotlin.math.cos
//import kotlin.math.sin
//import fr.julien.quievreux.droidplane2.data.model.Node
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.unit.sp
//import kotlin.math.PI
//
//@Composable
//fun MindMap(
//    rootNode: Node,
//    fetchText: (Node) -> String?,
//) {
//    var offsetX by remember { mutableStateOf(0f) }
//    var offsetY by remember { mutableStateOf(0f) }
//    var scale by remember { mutableStateOf(1f) }
//    var parentSize by remember { mutableStateOf(IntSize.Zero) }
//    val parentCenterX by remember { derivedStateOf { (parentSize.width / 2).toFloat() } }
//    val parentCenterY by remember { derivedStateOf { (parentSize.height / 2).toFloat() } }
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(color = MaterialTheme.colorScheme.onBackground)
//            .onSizeChanged {
//                parentSize = it
//            }
//            .pointerInput(Unit) {
//                detectTransformGestures { _, pan, zoom, _ ->
//                    offsetX += pan.x
//                    offsetY += pan.y
//                    scale = (scale * zoom).coerceIn(0.5f, 3f)
//                }
//            }
//            .graphicsLayer(
//                translationX = offsetX,
//                translationY = offsetY,
//                scaleX = scale,
//                scaleY = scale,
//            )
//    ) {
//        // Calculate the minimum radius to prevent overlapping
//        val rootText = fetchText(rootNode) ?: "Root"
//        val rootSizePx = with(LocalDensity.current) { calculateCellSizePx(rootText) }
//        val childSizesPx = rootNode.childNodes.map { fetchText(it)?.let { text -> calculateCellSizePx(text) } ?: rootSizePx }
//        val maxChildSizePx = childSizesPx.maxOrNull() ?: rootSizePx
//
//        // Calculate the radius to avoid overlaps with shortest connections
//        val minRadius = rootSizePx / 2 + maxChildSizePx / 2 + 16.dp.toPx()
//        val radius = maxOf(parentSize.width.coerceAtMost(parentSize.height) / 3f, minRadius)
//
//        // Calculate positions for child nodes
//        val childPositions = calculateCircularPositions(parentCenterX, parentCenterY, radius, rootNode.childNodes.size)
//
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            childPositions.forEachIndexed { index, (childX, childY) ->
//                drawConnection(
//                    startX = size.width / 2 + offsetX,
//                    startY = size.height / 2 + offsetY,
//                    endX = childX + offsetX,
//                    endY = childY + offsetY,
//                )
//            }
//        }
//
//        Cell(
//            text = rootText,
//            modifier = Modifier
//                .graphicsLayer(translationX = offsetX, translationY = offsetY)
//                .align(Alignment.Center)
//        )
//
//        // Child nodes positioned in a circular layout
//        rootNode.childNodes.forEachIndexed { index, childNode ->
//            val (childX, childY) = childPositions[index]
//            val childText = fetchText(childNode) ?: "Child ${index + 1}"
//            val childSizePx = calculateCellSizePx(childText)
//            Cell(
//                text = childText,
//                modifier = Modifier
//                    .graphicsLayer(
//                        translationX = childX + offsetX - (childSizePx / 2),
//                        translationY = childY + offsetY - (childSizePx / 2)
//                    )
//            )
//        }
//    }
//}
//
//@Composable
//fun Cell(
//    text: String,
//    modifier: Modifier = Modifier,
//) {
//    var textSize by remember { mutableStateOf(IntSize.Zero) }
//
//    Box(
//        modifier = modifier
//            .wrapContentSize()
//            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
//            .border(2.dp, Color.Black, shape = RoundedCornerShape(8.dp))
//            .padding(16.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            text = text,
//            style = TextStyle(fontSize = 16.sp),
//            modifier = Modifier
//                .padding(8.dp)
//                .onSizeChanged { size ->
//                    textSize = size
//                },
//        )
//    }
//}
//
//@Composable fun calculateCellSizePx(
//    text: String,
//): Float {
//    val textPaint = android.text.TextPaint().apply {
//        textSize = 16.sp.toPx()
//    }
//    val textWidth = textPaint.measureText(text)
//    val padding = 32f // Padding in pixels
//    return textWidth + padding
//}
//
//@Composable fun TextUnit.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }
//
//// Function to calculate circular positions for nodes
//fun calculateCircularPositions(
//    centerX: Float,
//    centerY: Float,
//    radius: Float,
//    count: Int,
//): List<Pair<Float, Float>> {
//    val positions = mutableListOf<Pair<Float, Float>>()
//    for (i in 0 until count) {
//        val angle = 2 * PI * i / count
//        val x = centerX + (radius * cos(angle)).toFloat()
//        val y = centerY + (radius * sin(angle)).toFloat()
//        positions.add(Pair(x, y))
//    }
//    return positions
//}
//
//@Composable //keep here, put in another module break preview
//fun Dp.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }
//
//// Function to draw a connection between two points
//fun DrawScope.drawConnection(
//    startX: Float,
//    startY: Float,
//    endX: Float,
//    endY: Float,
//) {
//    drawLine(
//        color = Color.Black,
//        start = Offset(startX, startY),
//        end = Offset(endX, endY),
//        strokeWidth = 4f
//    )
//}
//
//@Preview(
//    showBackground = true,
//    widthDp = 800,
//    heightDp = 800,
//)
//@Composable
//fun PreviewMindMap() {
//    MindMap(
//        rootNode = FakeDataSource.fakeRootNode(),
//        fetchText = { node ->
//            node.text
//        },
//    )
//}