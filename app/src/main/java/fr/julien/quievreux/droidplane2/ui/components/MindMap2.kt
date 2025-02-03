package fr.julien.quievreux.droidplane2.ui.components

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.mindsync.library.MindMapView
import com.mindsync.library.NodeClickListener
import com.mindsync.library.data.CircleNodeData
import com.mindsync.library.data.CirclePath
import com.mindsync.library.data.NodeData
import com.mindsync.library.data.RectangleNodeData
import com.mindsync.library.data.RectanglePath
import com.mindsync.library.data.Tree
import com.mindsync.library.util.Dp
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.FakeDataSource
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.model.Circle
import fr.julien.quievreux.droidplane2.ui.model.CircleNode
import fr.julien.quievreux.droidplane2.ui.model.Dot
import fr.julien.quievreux.droidplane2.ui.model.RectangleNode
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import fr.julien.quievreux.droidplane2.ui.view.MindMapUiState.DialogType
import fr.julien.quievreux.droidplane2.ui.view.MindMapUiState.DialogType.*
import fr.julien.quievreux.droidplane2.ui.view.MindMapViewModel
import fr.julien.quievreux.droidplane2.ui.model.Node as NodeView

@Composable
fun MindMap2(
    node: Node,
    logger: Logger,
    modifier: Modifier = Modifier,
    fetchText: (Node) -> String?,
    viewModel: MindMapViewModel,
//    onNodeClick: (
//        NodeData<*>?,
//        MindMapView,
//    ) -> Unit,
) {

    val state = viewModel.uiState.collectAsState()
    val selectedNode = viewModel.selectedNode.collectAsState()
    var mindMapView: MindMapView? = null

    when (val dialog = state.value.dialogUiState.dialogType) {
        Add -> {
            CustomDialog(
                titre = stringResource(R.string.add),
                value = "",
                onDismiss = {
                    viewModel.setDialogState(None)
                }
            ) { newValue ->
                logger.e("addNode($newValue)")
                viewModel.addNode(newValue)
            }
        }

        is Edit -> {
            CustomDialog(
                titre = stringResource(R.string.edit),
                value = dialog.oldValue,
                onDismiss = {
                    viewModel.setDialogState(None)
                }
            ) { newValue ->
//                viewModel.updateNodeText(dialog.node, newValue)
            }
        }

        None -> {
            /* unused but needed with stateless view... */
        }
    }

    val nodeToAdd = viewModel.nodeToAdd.collectAsState()

    nodeToAdd.value?.let {
        logger.e("add node in view: $it")
        mindMapView?.addNode(it)
        viewModel.addNode(null)
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .windowInsetsPadding(WindowInsets.systemBars.add(WindowInsets.navigationBars))
    ) {
        AndroidView(
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.onBackground)
                .weight(1f),
            factory = { context ->
                // Creates view
                MindMapView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    logger.e("mindMap2 create")
                    val tree = Tree<Node>(
                        context = context
//                        nodes = getNodeTree(
//                            node = node,
//                            fetchText = fetchText,
//                            view = this@apply,
//                        )
                    )
                    setTree(tree)
                    initialize()
                    viewModel.setManager(getMindMapManager())

                    setNodeClickListener(object : NodeClickListener {
                        override fun onClickListener(node: NodeData<*>?) {
                            logger.e("onClickListener $node")
                            viewModel.setSelectedNode(createNode(node))
//                            onNodeClick(
//                                node,
//                                this@apply
//                            )
                        }
                    })

                    mindMapView = this

//                    addNode("toto")
//             editNodeText(description)
//                fitScreen()
                    // Sets up listeners for View -> Compose communication
//                                            setOnClickListener {
//                                                selectedItem = 1
//                                            }
                }
            },
            update = { mindMapView ->
                // View's been inflated or state read in this block has been updated
                // Add logic here if necessary
                logger.e("mindMap2 update: ${mindMapView.width}x${mindMapView.height}")

                // As selectedItem is read here, AndroidView will recompose
                // whenever the state changes
                // Example of Compose -> View communication
//                                        view.selectedItem = selectedItem

            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Red.copy(alpha = 0.5f)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            selectedNode.value?.let {
                AppButton(
                    text = stringResource(R.string.add),
                    onClick = {
                        viewModel.setDialogState(Add)
                    }
                )

                AppButton(
                    text = stringResource(R.string.edit),
                    onClick = {
//                        viewModel.setDialogState(Edit())
                    }
                )
            }
        }
    }
}

fun getNodeTree(
    node: Node,
    fetchText: (Node) -> String?,
    view: MindMapView,
): Map<String, NodeData<*>> = mapOf(
    node.id to node.toCircleNodeData(
        fetchText = fetchText,
        circle = Circle(
            dot = Dot(view.width / 2f, view.height / 2f),
            radius = 200f,
        )
    )
)

//private fun Node.toNodeData(
//    fetchText: (Node) -> String?,
//): NodeData<*> = when {
//    parentNode == null -> this.toCircleNodeData(fetchText)
//    else -> this.toRectangleNodeData(fetchText)
//}
//
private fun Node.toCircleNodeData(
    fetchText: (Node) -> String?,
    circle: Circle,
): CircleNodeData = CircleNodeData(
    id = id,
    parentId = null,
    path = CirclePath(
        Dp(circle.dot.x),
        Dp(circle.dot.y),
        Dp(circle.radius)
    ),
    description = fetchText(this).orEmpty(),
    children = childNodes.mapNotNull { fetchText(it) },
//    alpha = alpha,
//    isAnimating = isAnimating,
//    isDrawingLine = isDrawingLine,
//    strokeWidth = strokeWidth

)
//
//private fun Node.toRectangleNodeData(
//    fetchText: (Node) -> String?,
//): RectangleNodeData = RectangleNodeData(
//    id = id,
//    parentId = parentNode?.id.orEmpty(),
//    path = path,
//    description = fetchText(this).orEmpty(),
//    children = childNodes.mapNotNull { fetchText(it) },
////    alpha = alpha,
////    isAnimating = isAnimating,
////    isDrawingLine = isDrawingLine,
////    strokeWidth = strokeWidth
//)

private fun createNode(node: NodeData<*>?): NodeView? = when (node) {
    is CircleNodeData -> CircleNode(
        node.id,
        node.parentId,
        CirclePath(
            Dp(node.path.centerX.dpVal),
            Dp(node.path.centerY.dpVal),
            Dp(node.path.radius.dpVal)
        ),
        node.description,
        node.children
    )

    is RectangleNodeData -> RectangleNode(
        node.id,
        node.parentId,
        RectanglePath(
            Dp(node.path.centerX.dpVal),
            Dp(node.path.centerY.dpVal),
            Dp(node.path.width.dpVal),
            Dp(node.path.height.dpVal)
        ),
        node.description,
        node.children
    )

    else -> null
}

//@PreviewScreenSizes
@Preview(showSystemUi = true)
@Composable
private fun MindMap2Preview() {
    val logger = object : Logger {
        override fun e(message: String) {}

        override fun e(tag: String, message: String) {}

        override fun w(message: String) {}

        override fun w(tag: String, message: String) {}

        override fun d(message: String) {}

        override fun d(tag: String, message: String) {}
    }
    ContrastAwareReplyTheme {
        MindMap2(
            fetchText = { it.text },
            logger = logger,
            node = FakeDataSource.fakeRootNode(),
            viewModel = MindMapViewModel(
                logger = logger,
            )
        )
    }
}