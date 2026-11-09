package fr.julien.quievreux.droidplane2.ui.mindmap

import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.ExternalObjectProperties
import fr.julien.quievreux.droidplane2.data.model.Node
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

        "measureNode expands width when badges such as icons or notes are attached" {
            val plainNode = Node(parentNode = null, id = "p", numericId = 1, text = "Topic", creationDate = 0L, modificationDate = 0L)
            val badgedNode = Node(
                parentNode = null,
                id = "b",
                numericId = 2,
                text = "Topic",
                iconNames = mutableListOf("idea", "yes"),
                noteText = "Note text",
                detailsText = "Details",
                creationDate = 0L,
                modificationDate = 0L,
            )

            val plainSize = MindMapLayoutEngine.measureNode(plainNode, density = 1f)
            val badgedSize = MindMapLayoutEngine.measureNode(badgedNode, density = 1f)

            badgedSize.first shouldBeGreaterThan plainSize.first
        }

        "measureNode returns standard text-only dimensions when externalObject is null or has blank uri" {
            val plainNode = Node(
                parentNode = null,
                id = "plain",
                numericId = 1,
                text = "Topic",
                externalObject = null,
                creationDate = 0L,
                modificationDate = 0L,
            )
            val blankUriNode = Node(
                parentNode = null,
                id = "blank_uri",
                numericId = 2,
                text = "Topic",
                externalObject = ExternalObjectProperties(uri = "   "),
                creationDate = 0L,
                modificationDate = 0L,
            )

            val plainSize = MindMapLayoutEngine.measureNode(plainNode, density = 1f)
            val blankUriSize = MindMapLayoutEngine.measureNode(blankUriNode, density = 1f)

            assertEquals(plainSize.first, blankUriSize.first)
            assertEquals(plainSize.second, blankUriSize.second)
        }

        "measureNode expands height and width for externalObject with default scale when size is null" {
            val density = 2f
            val plainNode = Node(
                parentNode = null,
                id = "plain",
                numericId = 1,
                text = "Short",
                externalObject = null,
                creationDate = 0L,
                modificationDate = 0L,
            )
            val imageNode = Node(
                parentNode = null,
                id = "img",
                numericId = 2,
                text = "Short",
                externalObject = ExternalObjectProperties(
                    uri = "content://media/external/images/media/42",
                    size = null,
                ),
                creationDate = 0L,
                modificationDate = 0L,
            )

            val plainSize = MindMapLayoutEngine.measureNode(plainNode, density = density)
            val imageSize = MindMapLayoutEngine.measureNode(imageNode, density = density)

            val expectedMinWidth = (120f + 28f) * density
            val expectedHeight = plainSize.second + (90f + 8f) * density

            assertTrue(imageSize.first >= expectedMinWidth)
            assertEquals(expectedMinWidth, imageSize.first)
            assertEquals(expectedHeight, imageSize.second)
        }

        "measureNode scales image dimensions with custom size and clamps out-of-range scales" {
            val density = 1.5f
            val plainNode = Node(
                parentNode = null,
                id = "plain",
                numericId = 1,
                text = "Short",
                externalObject = null,
                creationDate = 0L,
                modificationDate = 0L,
            )
            val customScaledNode = Node(
                parentNode = null,
                id = "scaled_2x",
                numericId = 2,
                text = "Short",
                externalObject = ExternalObjectProperties(
                    uri = "images/diagram.png",
                    size = 2.0f,
                ),
                creationDate = 0L,
                modificationDate = 0L,
            )
            val minClampedNode = Node(
                parentNode = null,
                id = "clamped_min",
                numericId = 3,
                text = "Short",
                externalObject = ExternalObjectProperties(
                    uri = "images/diagram.png",
                    size = 0.05f,
                ),
                creationDate = 0L,
                modificationDate = 0L,
            )
            val exactMinNode = Node(
                parentNode = null,
                id = "exact_min",
                numericId = 4,
                text = "Short",
                externalObject = ExternalObjectProperties(
                    uri = "images/diagram.png",
                    size = 0.2f,
                ),
                creationDate = 0L,
                modificationDate = 0L,
            )
            val maxClampedNode = Node(
                parentNode = null,
                id = "clamped_max",
                numericId = 5,
                text = "Short",
                externalObject = ExternalObjectProperties(
                    uri = "images/diagram.png",
                    size = 10.0f,
                ),
                creationDate = 0L,
                modificationDate = 0L,
            )
            val exactMaxNode = Node(
                parentNode = null,
                id = "exact_max",
                numericId = 6,
                text = "Short",
                externalObject = ExternalObjectProperties(
                    uri = "images/diagram.png",
                    size = 4.0f,
                ),
                creationDate = 0L,
                modificationDate = 0L,
            )

            val plainSize = MindMapLayoutEngine.measureNode(plainNode, density = density)
            val customScaledSize = MindMapLayoutEngine.measureNode(customScaledNode, density = density)
            val minClampedSize = MindMapLayoutEngine.measureNode(minClampedNode, density = density)
            val exactMinSize = MindMapLayoutEngine.measureNode(exactMinNode, density = density)
            val maxClampedSize = MindMapLayoutEngine.measureNode(maxClampedNode, density = density)
            val exactMaxSize = MindMapLayoutEngine.measureNode(exactMaxNode, density = density)

            assertEquals((240f + 28f) * density, customScaledSize.first, absoluteTolerance = 0.01f)
            assertEquals(plainSize.second + (180f + 8f) * density, customScaledSize.second, absoluteTolerance = 0.01f)

            assertEquals(exactMinSize.first, minClampedSize.first, absoluteTolerance = 0.01f)
            assertEquals(exactMinSize.second, minClampedSize.second, absoluteTolerance = 0.01f)
            assertEquals(plainSize.second + (90f * 0.2f + 8f) * density, minClampedSize.second, absoluteTolerance = 0.01f)

            assertEquals(exactMaxSize.first, maxClampedSize.first, absoluteTolerance = 0.01f)
            assertEquals(exactMaxSize.second, maxClampedSize.second, absoluteTolerance = 0.01f)
            assertEquals((120f * 4.0f + 28f) * density, maxClampedSize.first, absoluteTolerance = 0.01f)
            assertEquals(plainSize.second + (90f * 4.0f + 8f) * density, maxClampedSize.second, absoluteTolerance = 0.01f)
        }
    }
}
