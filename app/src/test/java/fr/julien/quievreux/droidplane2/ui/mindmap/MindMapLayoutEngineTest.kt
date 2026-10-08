package fr.julien.quievreux.droidplane2.ui.mindmap

import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.Node
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MindMapLayoutEngineTest : KStringSpec() {
    init {
        "computeLayout for single root node places it at origin with no connectors" {
            val root = Node(
                parentNode = null,
                id = "root_1",
                numericId = 1,
                text = "Central Idea",
                creationDate = 0L,
                modificationDate = 0L,
            )

            val layout = MindMapLayoutEngine.computeLayout(
                rootNode = root,
                collapsedNodeIds = emptySet(),
                selectedNodeId = null,
            )

            layout.nodes shouldHaveSize 1
            layout.connectors.shouldBeEmpty()

            val rootLayout = layout.nodes.first()
            rootLayout.node.id shouldBe "root_1"
            rootLayout.x shouldBe 0f
            rootLayout.y shouldBe 0f
            rootLayout.branchDirection shouldBe BranchDirection.ROOT
            rootLayout.isSelected shouldBe false
        }

        "computeLayout partitions explicit left and right children correctly" {
            val root = Node(
                parentNode = null,
                id = "root",
                numericId = 1,
                text = "Root",
                creationDate = 0L,
                modificationDate = 0L,
            )
            val leftChild = Node(
                parentNode = root,
                id = "left_child",
                numericId = 2,
                text = "Left Topic",
                position = "left",
                creationDate = 0L,
                modificationDate = 0L,
            )
            val rightChild = Node(
                parentNode = root,
                id = "right_child",
                numericId = 3,
                text = "Right Topic",
                position = "right",
                creationDate = 0L,
                modificationDate = 0L,
            )
            root.childNodes.addAll(listOf(leftChild, rightChild))

            val layout = MindMapLayoutEngine.computeLayout(rootNode = root)

            layout.nodes shouldHaveSize 3
            layout.connectors shouldHaveSize 2

            val leftLayout = layout.nodes.first { it.node.id == "left_child" }
            leftLayout.branchDirection shouldBe BranchDirection.LEFT
            leftLayout.x shouldBeLessThan 0f

            val rightLayout = layout.nodes.first { it.node.id == "right_child" }
            rightLayout.branchDirection shouldBe BranchDirection.RIGHT
            rightLayout.x shouldBeGreaterThan 0f
        }

        "computeLayout balances unspecified position children evenly" {
            val root = Node(
                parentNode = null,
                id = "root",
                numericId = 1,
                text = "Root",
                creationDate = 0L,
                modificationDate = 0L,
            )
            val child1 = Node(parentNode = root, id = "c1", numericId = 2, text = "Child 1", creationDate = 0L, modificationDate = 0L)
            val child2 = Node(parentNode = root, id = "c2", numericId = 3, text = "Child 2", creationDate = 0L, modificationDate = 0L)
            root.childNodes.addAll(listOf(child1, child2))

            val layout = MindMapLayoutEngine.computeLayout(rootNode = root)

            layout.nodes shouldHaveSize 3
            val c1Layout = layout.nodes.first { it.node.id == "c1" }
            val c2Layout = layout.nodes.first { it.node.id == "c2" }

            c1Layout.branchDirection shouldBe BranchDirection.RIGHT
            c2Layout.branchDirection shouldBe BranchDirection.LEFT
        }

        "computeLayout excludes children of collapsed nodes" {
            val root = Node(
                parentNode = null,
                id = "root",
                numericId = 1,
                text = "Root",
                creationDate = 0L,
                modificationDate = 0L,
            )
            val branch = Node(
                parentNode = root,
                id = "branch",
                numericId = 2,
                text = "Branch",
                position = "right",
                creationDate = 0L,
                modificationDate = 0L,
            )
            val subChild = Node(
                parentNode = branch,
                id = "sub_child",
                numericId = 3,
                text = "Sub Child",
                creationDate = 0L,
                modificationDate = 0L,
            )
            branch.childNodes.add(subChild)
            root.childNodes.add(branch)

            val layoutCollapsed = MindMapLayoutEngine.computeLayout(
                rootNode = root,
                collapsedNodeIds = setOf("branch"),
            )

            layoutCollapsed.nodes shouldHaveSize 2 // root + branch (sub_child excluded)
            layoutCollapsed.nodes.any { it.node.id == "sub_child" } shouldBe false
            val branchLayout = layoutCollapsed.nodes.first { it.node.id == "branch" }
            branchLayout.isCollapsed shouldBe true
        }

        "computeLayout adjusts bounding box when subtree is collapsed" {
            val root = Node(
                parentNode = null,
                id = "root",
                numericId = 1,
                text = "Root",
                creationDate = 0L,
                modificationDate = 0L,
            )
            val branch = Node(
                parentNode = root,
                id = "branch",
                numericId = 2,
                text = "Branch Node with Wide Children",
                position = "right",
                creationDate = 0L,
                modificationDate = 0L,
            )
            val deepChild = Node(
                parentNode = branch,
                id = "deep",
                numericId = 3,
                text = "Deep Child Node with lots of text extending rightwards",
                creationDate = 0L,
                modificationDate = 0L,
            )
            branch.childNodes.add(deepChild)
            root.childNodes.add(branch)

            val expandedLayout = MindMapLayoutEngine.computeLayout(rootNode = root)
            val collapsedLayout = MindMapLayoutEngine.computeLayout(
                rootNode = root,
                collapsedNodeIds = setOf("branch"),
            )

            collapsedLayout.boundsMaxX shouldBeLessThan expandedLayout.boundsMaxX
        }

        "computeLayout marks selected node correctly" {
            val root = Node(
                parentNode = null,
                id = "root",
                numericId = 1,
                text = "Root",
                creationDate = 0L,
                modificationDate = 0L,
            )
            val child = Node(
                parentNode = root,
                id = "child",
                numericId = 2,
                text = "Child",
                creationDate = 0L,
                modificationDate = 0L,
            )
            root.childNodes.add(child)

            val layout = MindMapLayoutEngine.computeLayout(
                rootNode = root,
                selectedNodeId = "child",
            )

            layout.nodes.first { it.node.id == "child" }.isSelected shouldBe true
            layout.nodes.first { it.node.id == "root" }.isSelected shouldBe false
        }

        "measureNode scales dimensions dynamically with text content and density" {
            val shortNode = Node(parentNode = null, id = "s", numericId = 1, text = "Short", creationDate = 0L, modificationDate = 0L)
            val longNode = Node(parentNode = null, id = "l", numericId = 2, text = "This is a significantly longer node text with many characters", creationDate = 0L, modificationDate = 0L)

            val shortSize = MindMapLayoutEngine.measureNode(shortNode, density = 1f)
            val longSize = MindMapLayoutEngine.measureNode(longNode, density = 1f)

            longSize.first shouldBeGreaterThan shortSize.first

            val scaledShortSize = MindMapLayoutEngine.measureNode(shortNode, density = 2f)
            scaledShortSize.first shouldBe (shortSize.first * 2f)
            scaledShortSize.second shouldBe (shortSize.second * 2f)
        }

        "measureNode expands height for long multi-line paragraphs beyond 4 lines without truncation" {
            val multiLineText = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5\nLine 6\nLine 7"
            val multiLineNode = Node(parentNode = null, id = "m", numericId = 1, text = multiLineText, creationDate = 0L, modificationDate = 0L)
            val fourLineText = "Line 1\nLine 2\nLine 3\nLine 4"
            val fourLineNode = Node(parentNode = null, id = "f", numericId = 2, text = fourLineText, creationDate = 0L, modificationDate = 0L)

            val multiLineSize = MindMapLayoutEngine.measureNode(multiLineNode, density = 1f)
            val fourLineSize = MindMapLayoutEngine.measureNode(fourLineNode, density = 1f)

            multiLineSize.second shouldBeGreaterThan fourLineSize.second
        }

        "measureNode scales height and width when custom fontSize is applied" {
            val normalNode = Node(parentNode = null, id = "n", numericId = 1, text = "Font Scaling Text", creationDate = 0L, modificationDate = 0L)
            val largeFontNode = Node(parentNode = null, id = "l", numericId = 2, text = "Font Scaling Text", fontSize = 20, creationDate = 0L, modificationDate = 0L)

            val normalSize = MindMapLayoutEngine.measureNode(normalNode, density = 1f)
            val largeSize = MindMapLayoutEngine.measureNode(largeFontNode, density = 1f)

            largeSize.first shouldBeGreaterThan normalSize.first
            largeSize.second shouldBeGreaterThan normalSize.second
        }
    }
}
