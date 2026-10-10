package fr.julien.quievreux.droidplane2.ui.mindmap

import fr.julien.quievreux.droidplane2.data.model.Node

enum class BranchDirection {
    ROOT,
    LEFT,
    RIGHT,
}

data class MindMapNodeLayout(
    val node: Node,
    val x: Float, // Center X coordinate (in pixels)
    val y: Float, // Center Y coordinate (in pixels)
    val width: Float, // Measured width (in pixels)
    val height: Float, // Measured height (in pixels)
    val branchDirection: BranchDirection,
    val isCollapsed: Boolean,
    val isSelected: Boolean,
)

data class BranchConnector(
    val parentId: String,
    val childId: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val direction: BranchDirection,
    val color: String? = null,
    val style: String? = null,
    val width: String? = null,
)

data class MindMapLayoutResult(
    val nodes: List<MindMapNodeLayout>,
    val connectors: List<BranchConnector>,
    val boundsMinX: Float,
    val boundsMaxX: Float,
    val boundsMinY: Float,
    val boundsMaxY: Float,
)

object MindMapLayoutEngine {

    const val DEFAULT_HORIZONTAL_SPACING_DP = 56f
    const val DEFAULT_VERTICAL_SPACING_DP = 16f
    const val CLOUD_EXTRA_VERTICAL_MARGIN_DP = 32f
    const val CLOUD_EXTRA_HORIZONTAL_MARGIN_DP = 24f

    private fun hasCloudInSubtree(node: Node, collapsedNodeIds: Set<String>): Boolean {
        if (node.cloud != null) return true
        if (collapsedNodeIds.contains(node.id)) return false
        return node.childNodes.toList().any { hasCloudInSubtree(it, collapsedNodeIds) }
    }

    fun computeLayout(
        rootNode: Node,
        collapsedNodeIds: Set<String> = emptySet(),
        selectedNodeId: String? = null,
        density: Float = 1f,
        horizontalSpacingDp: Float = DEFAULT_HORIZONTAL_SPACING_DP,
        verticalSpacingDp: Float = DEFAULT_VERTICAL_SPACING_DP,
        fetchText: (Node) -> String? = { it.text },
    ): MindMapLayoutResult {
        val horizontalSpacing = horizontalSpacingDp * density
        val verticalSpacing = verticalSpacingDp * density

        val rootSize = measureNode(rootNode, density, fetchText)
        val isRootSelected = selectedNodeId == rootNode.id
        val isRootCollapsed = collapsedNodeIds.contains(rootNode.id)

        val rootLayout = MindMapNodeLayout(
            node = rootNode,
            x = 0f,
            y = 0f,
            width = rootSize.first,
            height = rootSize.second,
            branchDirection = BranchDirection.ROOT,
            isCollapsed = isRootCollapsed,
            isSelected = isRootSelected,
        )

        val nodes = mutableListOf<MindMapNodeLayout>()
        nodes.add(rootLayout)

        val connectors = mutableListOf<BranchConnector>()

        if (!isRootCollapsed && rootNode.childNodes.isNotEmpty()) {
            val (leftChildren, rightChildren) = partitionFirstLevelChildren(rootNode.childNodes)

            // Layout right subtrees
            layoutBranches(
                parentLayout = rootLayout,
                children = rightChildren,
                direction = BranchDirection.RIGHT,
                collapsedNodeIds = collapsedNodeIds,
                selectedNodeId = selectedNodeId,
                density = density,
                horizontalSpacing = horizontalSpacing,
                verticalSpacing = verticalSpacing,
                fetchText = fetchText,
                outNodes = nodes,
                outConnectors = connectors,
            )

            // Layout left subtrees
            layoutBranches(
                parentLayout = rootLayout,
                children = leftChildren,
                direction = BranchDirection.LEFT,
                collapsedNodeIds = collapsedNodeIds,
                selectedNodeId = selectedNodeId,
                density = density,
                horizontalSpacing = horizontalSpacing,
                verticalSpacing = verticalSpacing,
                fetchText = fetchText,
                outNodes = nodes,
                outConnectors = connectors,
            )
        }

        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        nodes.forEach { n ->
            minX = minOf(minX, n.x - n.width / 2)
            maxX = maxOf(maxX, n.x + n.width / 2)
            minY = minOf(minY, n.y - n.height / 2)
            maxY = maxOf(maxY, n.y + n.height / 2)
        }

        return MindMapLayoutResult(
            nodes = nodes,
            connectors = connectors,
            boundsMinX = minX,
            boundsMaxX = maxX,
            boundsMinY = minY,
            boundsMaxY = maxY,
        )
    }

    private fun partitionFirstLevelChildren(
        children: List<Node>
    ): Pair<List<Node>, List<Node>> {
        val left = mutableListOf<Node>()
        val right = mutableListOf<Node>()
        val unassigned = mutableListOf<Node>()

        children.toList().forEach { child ->
            when (child.position?.lowercase()) {
                "left", "top_or_left" -> left.add(child)
                "right", "bottom_or_right" -> right.add(child)
                else -> unassigned.add(child)
            }
        }

        if (unassigned.isNotEmpty()) {
            // Balance unassigned children evenly between right and left
            val rightNeeded = (unassigned.size + 1) / 2
            right.addAll(unassigned.take(rightNeeded))
            left.addAll(unassigned.drop(rightNeeded))
        }

        return Pair(left, right)
    }

    private fun layoutBranches(
        parentLayout: MindMapNodeLayout,
        children: List<Node>,
        direction: BranchDirection,
        collapsedNodeIds: Set<String>,
        selectedNodeId: String?,
        density: Float,
        horizontalSpacing: Float,
        verticalSpacing: Float,
        fetchText: (Node) -> String?,
        outNodes: MutableList<MindMapNodeLayout>,
        outConnectors: MutableList<BranchConnector>,
    ) {
        if (children.isEmpty()) return

        // Compute total height required for all child subtrees
        val subtreeHeights = children.map { child ->
            computeSubtreeHeight(child, collapsedNodeIds, density, verticalSpacing, fetchText)
        }
        val totalSpan = subtreeHeights.sum() + (children.size - 1) * verticalSpacing

        var currentTopY = parentLayout.y - totalSpan / 2

        children.forEachIndexed { index, child ->
            val childSubtreeHeight = subtreeHeights[index]
            val childY = currentTopY + childSubtreeHeight / 2
            val childSize = measureNode(child, density, fetchText)
            val isCollapsed = collapsedNodeIds.contains(child.id)
            val isSelected = selectedNodeId == child.id

            val childHasCloud = child.cloud != null || hasCloudInSubtree(child, collapsedNodeIds)
            val extraHorizontal = if (childHasCloud) CLOUD_EXTRA_HORIZONTAL_MARGIN_DP * density else 0f

            val childX = if (direction == BranchDirection.RIGHT) {
                parentLayout.x + (parentLayout.width / 2) + horizontalSpacing + extraHorizontal + (childSize.first / 2)
            } else {
                parentLayout.x - (parentLayout.width / 2) - horizontalSpacing - extraHorizontal - (childSize.first / 2)
            }

            val childLayout = MindMapNodeLayout(
                node = child,
                x = childX,
                y = childY,
                width = childSize.first,
                height = childSize.second,
                branchDirection = direction,
                isCollapsed = isCollapsed,
                isSelected = isSelected,
            )

            outNodes.add(childLayout)

            // Connecting curve endpoints
            val startX = if (direction == BranchDirection.RIGHT) {
                parentLayout.x + parentLayout.width / 2
            } else {
                parentLayout.x - parentLayout.width / 2
            }
            val startY = parentLayout.y

            val endX = if (direction == BranchDirection.RIGHT) {
                childLayout.x - childLayout.width / 2
            } else {
                childLayout.x + childLayout.width / 2
            }
            val endY = childLayout.y

            outConnectors.add(
                BranchConnector(
                    parentId = parentLayout.node.id,
                    childId = child.id,
                    startX = startX,
                    startY = startY,
                    endX = endX,
                    endY = endY,
                    direction = direction,
                    color = child.edge?.color,
                    style = child.edge?.style,
                    width = child.edge?.width,
                )
            )

            if (!isCollapsed && child.childNodes.isNotEmpty()) {
                layoutBranches(
                    parentLayout = childLayout,
                    children = child.childNodes.toList(),
                    direction = direction,
                    collapsedNodeIds = collapsedNodeIds,
                    selectedNodeId = selectedNodeId,
                    density = density,
                    horizontalSpacing = horizontalSpacing,
                    verticalSpacing = verticalSpacing,
                    fetchText = fetchText,
                    outNodes = outNodes,
                    outConnectors = outConnectors,
                )
            }

            currentTopY += childSubtreeHeight + verticalSpacing
        }
    }

    private fun computeSubtreeHeight(
        node: Node,
        collapsedNodeIds: Set<String>,
        density: Float,
        verticalSpacing: Float,
        fetchText: (Node) -> String?,
    ): Float {
        val selfHeight = measureNode(node, density, fetchText).second
        val cloudMargin = if (node.cloud != null) CLOUD_EXTRA_VERTICAL_MARGIN_DP * density else 0f

        if (collapsedNodeIds.contains(node.id) || node.childNodes.isEmpty()) {
            return selfHeight + cloudMargin
        }

        val children = node.childNodes.toList()
        val childHeights = children.sumOf { child ->
            computeSubtreeHeight(child, collapsedNodeIds, density, verticalSpacing, fetchText).toDouble()
        }.toFloat() + (children.size - 1) * verticalSpacing

        return maxOf(selfHeight, childHeights) + cloudMargin
    }

    fun measureNode(
        node: Node,
        density: Float = 1f,
        fetchText: (Node) -> String? = { it.text },
    ): Pair<Float, Float> {
        val text = fetchText(node)?.ifEmpty { " " } ?: node.text.orEmpty().ifEmpty { " " }
        val isRoot = node.parentNode == null
        val hasChildren = node.childNodes.isNotEmpty()

        val fontSize = node.fontSize?.toFloat() ?: (if (isRoot) 15f else 13f)
        val charWidthDp = fontSize * (if (node.isBold || isRoot) 0.65f else 0.58f)
        val lineHeightDp = fontSize * 1.38f
        val horizontalPaddingDp = 28f
        val foldIndicatorDp = if (hasChildren && !isRoot) 28f else 0f
        val verticalPaddingDp = 20f
        val maxTextWidthDp = if (isRoot) 280f else 260f

        val rawLines = text.split("\n")
        var totalLines = 0
        var maxLineWidthDp = 0f

        rawLines.forEach { rawLine ->
            if (rawLine.isBlank()) {
                totalLines += 1
            } else {
                val words = rawLine.split(" ")
                var currentLineWidth = 0f
                var lineCountForParagraph = 1

                words.forEach { word ->
                    val wordWidth = word.length * charWidthDp
                    val spaceWidth = charWidthDp
                    if (currentLineWidth + wordWidth <= maxTextWidthDp) {
                        currentLineWidth += wordWidth + spaceWidth
                    } else {
                        lineCountForParagraph += 1
                        currentLineWidth = wordWidth + spaceWidth
                    }
                    maxLineWidthDp = maxOf(maxLineWidthDp, minOf(currentLineWidth, maxTextWidthDp))
                }
                totalLines += lineCountForParagraph
            }
        }

        val displayLines = totalLines.coerceAtLeast(1)

        val badgesWidthDp = (if (node.cloud != null) 16f else 0f) +
            (node.iconNames.size * 18f) +
            (if (node.link != null) 16f else 0f) +
            (if (!node.noteText.isNullOrBlank()) 16f else 0f) +
            (if (!node.detailsText.isNullOrBlank()) 16f else 0f)

        val totalWidthDp = (maxLineWidthDp + horizontalPaddingDp + foldIndicatorDp + badgesWidthDp)
            .coerceIn(70f, maxTextWidthDp + horizontalPaddingDp + foldIndicatorDp + badgesWidthDp)
        val totalHeightDp = (displayLines * lineHeightDp + verticalPaddingDp).coerceAtLeast(38f)

        return Pair(totalWidthDp * density, totalHeightDp * density)
    }
}
