package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.NodeManager.Companion.FILE_EXTENSION
import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.RichContent
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import kotlin.io.path.createTempDirectory

class NodeManagerTest : KStringSpec() {

    init {
        coroutineTestScope = true

        "parsing document without start should throw an error" {
            val fakeXmlPullParser: XmlPullParser = mockk(relaxed = true)
            coEvery { fakeXmlPullParser.eventType }.returns(XmlPullParser.END_TAG)
            coEvery { fakeXmlPullParser.next() }.returns(XmlPullParser.END_DOCUMENT)
            var possibleException: Exception? = null
            val nodeManager = initNodeManager()
            val job = launch {
                nodeManager.loadMindMapFromXml(
                    xpp = fakeXmlPullParser,
                    onParentNodeUpdate = {},
                    onError = { exception ->
                        possibleException = exception
                    },
                    onReadFinish = {},
                )
            }

            job.join()
            job.cancel()

            possibleException shouldNotBe null
        }

        "parsing document should finish with mindmap processing" {
            val fakeXmlPullParser: XmlPullParser = mockk(relaxed = true)
            coEvery { fakeXmlPullParser.eventType }.returns(XmlPullParser.END_TAG)
            coEvery { fakeXmlPullParser.next() }.returns(XmlPullParser.END_DOCUMENT)
            var finishWithProcessing = false
            val nodeManager = initNodeManager()
            val job = launch {
                nodeManager.loadMindMapFromXml(
                    xpp = fakeXmlPullParser,
                    onParentNodeUpdate = {},
                    onError = { },
                    onReadFinish = {
                        finishWithProcessing = true
                    },
                )
            }

            job.join()
            job.cancel()

            finishWithProcessing shouldBe true
        }

        "serialize a mindmap with incorrect destination" should {
            val nodeManager = initNodeManager()
            var possibleException: Exception? = null
            val validFilePath = "/storage/"
            val validFileName = "mindmap.mm"

            "fail on empty filePath" {
                val job = launch {
                    nodeManager.serializeMindmap(
                        filePath = "", filename = validFileName, onError = { exception ->
                            possibleException = exception
                        }, onSaveFinished = {}
                    )
                }

                job.join()
                job.cancel()

                possibleException shouldNotBe null
            }

            "fail on blank filePath" {
                val job = launch {
                    nodeManager.serializeMindmap(
                        filePath = "   ", filename = validFileName, onError = { exception ->
                            possibleException = exception
                        }, onSaveFinished = {}
                    )
                }

                job.join()
                job.cancel()

                possibleException shouldNotBe null
            }

            "fail on filePath without path separator" {
                val job = launch {
                    nodeManager.serializeMindmap(
                        filePath = "lkjqsdf", filename = validFileName, onError = { exception ->
                            possibleException = exception
                        }, onSaveFinished = {}
                    )
                }

                job.join()
                job.cancel()

                possibleException shouldNotBe null
            }

            "fail on filePath not beginning with path separator" {
                val job = launch {
                    nodeManager.serializeMindmap(
                        filePath = "lkjqsdf/", filename = validFileName, onError = { exception ->
                            possibleException = exception
                        }, onSaveFinished = {}
                    )
                }

                job.join()
                job.cancel()

                possibleException shouldNotBe null
            }

            "fail on fileName empty" {
                val job = launch {
                    nodeManager.serializeMindmap(
                        filePath = validFilePath, filename = "", onError = { exception ->
                            possibleException = exception
                        }, onSaveFinished = {}
                    )
                }

                job.join()
                job.cancel()

                possibleException shouldNotBe null
            }

            "fail on fileName blank" {
                val job = launch {
                    nodeManager.serializeMindmap(
                        filePath = validFilePath, filename = "    ", onError = { exception ->
                            possibleException = exception
                        }, onSaveFinished = {}
                    )
                }

                job.join()
                job.cancel()

                possibleException shouldNotBe null
            }

            "fail on fileName without extension" {
                val job = launch {
                    nodeManager.serializeMindmap(
                        filePath = validFilePath, filename = "filename", onError = { exception ->
                            possibleException = exception
                        }, onSaveFinished = {}
                    )
                }

                job.join()
                job.cancel()

                possibleException shouldNotBe null
            }

            "fail on fileName with only an extension" {
                val job = launch {
                    nodeManager.serializeMindmap(
                        filePath = validFilePath, filename = FILE_EXTENSION, onError = { exception ->
                            possibleException = exception
                        }, onSaveFinished = {}
                    )
                }

                job.join()
                job.cancel()

                possibleException shouldNotBe null
            }
        }

        "generateNodeNumericID should be positive" {
            var resultId = -1
            val job = launch {
                val nodeManager = initNodeManager()
                resultId = nodeManager.generateNodeNumericID()
            }

            job.join()
            job.cancel()

            resultId shouldBeGreaterThanOrEqual 0
        }

        "generateNodeNumericID should generate id not contained in mindmap nodes" {
            val nodeManager = initNodeManager()
            val fakeNode = FakeDataSource.getFakeRootNode()
            nodeManager.updateRootNode(fakeNode)
            val existingNodeIds = nodeManager.allNodesId.first().toSet()

            // Act
            val generatedId: Int = runBlocking {
                nodeManager.generateNodeNumericID()
            }

            // Assert
            generatedId shouldNotBeIn existingNodeIds
        }

        "Add node with blank text should do nothing and so return an null node id" {
            val nodeManager = initNodeManager()
            nodeManager.addNodeToMindmap("") shouldBe null
            nodeManager.addNodeToMindmap(" ") shouldBe null
        }

        "Add node should give Id not blank" {
            val nodeManager = initNodeManager()
            nodeManager.addNodeToMindmap("fakeValue") shouldNotBe null
        }

        "Add node increase allNodeIds size" {
            val nodeManager = initNodeManager()
            val sizeBefore = nodeManager.allNodesId.first().size
            nodeManager.addNodeToMindmap("fakeValue")
            val sizeAfter = nodeManager.allNodesId.first().size
            sizeAfter shouldBeGreaterThan sizeBefore
        }

        "Add node increase mindmapIndexes size" {
            val nodeManager = initNodeManager()
            val sizeBefore = nodeManager.allNodesId.first().size
            nodeManager.addNodeToMindmap("fakeValue")
            val sizeAfter = nodeManager.allNodesId.first().size
            sizeAfter shouldBeGreaterThan sizeBefore
        }

        "Add node with parent should also change it's parent child list" {
            val nodeManager = initNodeManager()
            val deferredDadNodeId: Deferred<Int?> = async { nodeManager.addNodeToMindmap("fakeDad") }
            val dadNodeId : Int? = deferredDadNodeId.await()

            dadNodeId shouldNotBe null
            dadNodeId?.shouldBeGreaterThanOrEqual(0)

            dadNodeId?.let {
                var dadNode = nodeManager.getNodeByNumericId(dadNodeId)
                dadNode shouldNotBe null

                val deferredChildNodeId : Deferred<Int?> = async { nodeManager.addNodeToMindmap("fakeChild", parentNode = dadNode) }
                val childNodeId = deferredChildNodeId.await()

                childNodeId shouldNotBe null

                childNodeId?.let {
                    val childNode = nodeManager.getNodeByNumericId(childNodeId)
                    dadNode = nodeManager.getNodeByNumericId(dadNodeId)
                    childNode?.parentNode?.id shouldBe dadNode?.id
                    dadNode?.childNodes?.first()?.id shouldBe childNode?.id
                }
            }
        }

        //TODO jqx look if with freeplane, when we modify a child if parent modification date change also and if it's recursif

        "loadMindMapFromInputStream should parse test_map.mm and build correct hierarchy" {
            val nodeManager = initNodeManager()
            val inputStream: InputStream = java.lang.ClassLoader.getSystemResourceAsStream("test_map.mm")
            inputStream shouldNotBe null
            
            var loadFinished = false
            var capturedRootNode: Node? = null
            var capturedError: Exception? = null
            
            val job = launch {
                nodeManager.loadMindMapFromInputStream(
                    inputStream = inputStream!!,
                    onError = { capturedError = it },
                    onParentNodeUpdate = { capturedRootNode = it },
                    onLoadFinished = { loadFinished = true }
                )
            }
            
            job.join()
            job.cancel()
            
            capturedError shouldBe null
            loadFinished shouldBe true
            capturedRootNode shouldNotBe null
            capturedRootNode?.text shouldBe "Droidplane Root"
            capturedRootNode?.id shouldBe "ID_1000"
            
            // Verify all nodes are indexed
            val indexes = nodeManager.getNodeByIdIndex()
            indexes shouldNotBe null
            indexes!!.size shouldBe 11  // Root + 10 children from test_map.mm
            
            // Verify root node is in index
            indexes["ID_1000"] shouldNotBe null
            indexes["ID_1000"]?.text shouldBe "Droidplane Root"
            indexes["ID_1000"]?.parentNode shouldBe null
            
            // Verify child nodes are in index with correct parent references
            val architectureNode = indexes["ID_1001"]
            architectureNode shouldNotBe null
            architectureNode?.text shouldBe "Architecture"
            architectureNode?.parentNode?.id shouldBe "ID_1000"
            
            val navigationNode = indexes["ID_1005"]
            navigationNode shouldNotBe null
            navigationNode?.text shouldBe "Navigation"
            navigationNode?.parentNode?.id shouldBe "ID_1000"
            
            val searchNode = indexes["ID_1008"]
            searchNode shouldNotBe null
            searchNode?.text shouldBe "Search"
            searchNode?.parentNode?.id shouldBe "ID_1000"
            
            // Verify grand-children
            val coreModuleNode = indexes["ID_1002"]
            coreModuleNode shouldNotBe null
            coreModuleNode?.text shouldBe ":core Module"
            coreModuleNode?.parentNode?.id shouldBe "ID_1001"
            
            val upActionNode = indexes["ID_1006"]
            upActionNode shouldNotBe null
            upActionNode?.text shouldBe "Up Action"
            upActionNode?.parentNode?.id shouldBe "ID_1005"
            
            // Verify rootNode property is set
            nodeManager.rootNode shouldNotBe null
            nodeManager.rootNode?.id shouldBe "ID_1000"
            nodeManager.rootNode?.text shouldBe "Droidplane Root"
        }

        "loadMindMapFromInputStream should populate _allNodes StateFlow with all nodes" {
            val nodeManager = initNodeManager()
            val inputStream: InputStream = java.lang.ClassLoader.getSystemResourceAsStream("test_map.mm")
            inputStream shouldNotBe null
            
            var loadFinished = false
            var capturedError: Exception? = null
            
            val job = launch {
                nodeManager.loadMindMapFromInputStream(
                    inputStream = inputStream!!,
                    onError = { capturedError = it },
                    onParentNodeUpdate = { },
                    onLoadFinished = { loadFinished = true }
                )
            }
            
            job.join()
            job.cancel()
            
            capturedError shouldBe null
            loadFinished shouldBe true
            
            // Verify _allNodes StateFlow has all nodes
            val allNodes = nodeManager.allNodes.first()
            allNodes.size shouldBe 11
            allNodes.map { it.id } shouldContainExactlyInAnyOrder listOf(
                "ID_1000", "ID_1001", "ID_1002", "ID_1003", "ID_1004",
                "ID_1005", "ID_1006", "ID_1007", "ID_1008", "ID_1009", "ID_1010"
            )
        }

        "getNodeParent should return correct parent for a given node ID" {
            val nodeManager = initNodeManager()
            val inputStream: InputStream = java.lang.ClassLoader.getSystemResourceAsStream("test_map.mm")
            inputStream shouldNotBe null
            
            var loadFinished = false
            var capturedError: Exception? = null
            val job = launch {
                nodeManager.loadMindMapFromInputStream(
                    inputStream = inputStream!!,
                    onError = { capturedError = it },
                    onParentNodeUpdate = { },
                    onLoadFinished = { loadFinished = true }
                )
            }
            job.join()
            job.cancel()
            capturedError shouldBe null
            loadFinished shouldBe true
            
            // Test getNodeParent with numeric IDs
            val parentOfCoreModule = nodeManager.getNodeParent(1002)
            parentOfCoreModule shouldNotBe null
            parentOfCoreModule?.id shouldBe "ID_1001"
            
            val parentOfArchitecture = nodeManager.getNodeParent(1001)
            parentOfArchitecture shouldNotBe null
            parentOfArchitecture?.id shouldBe "ID_1000"
            
            val parentOfRoot = nodeManager.getNodeParent(1000)
            parentOfRoot shouldBe null
        }

        "updateNodeText should update node, its parent childNodes entry and indexes" {
            val nodeManager = loadedNodeManager()
            val before = nodeManager.getNodeByID("ID_1002")!!.modificationDate!!

            val updated = nodeManager.updateNodeText("ID_1002", "edited text")
            updated shouldNotBe null
            updated!!.text shouldBe "edited text"
            val newModificationDate = updated!!.modificationDate!!
            (newModificationDate >= before) shouldBe true


            // index lookup returns the updated node
            nodeManager.getNodeByID("ID_1002")?.text shouldBe "edited text"

            // parent's childNodes entry is replaced, not appended
            val parent = nodeManager.getNodeByID("ID_1001")
            parent shouldNotBe null
            parent?.childNodes?.count { it.id == "ID_1002" } shouldBe 1
            parent?.childNodes?.first { it.id == "ID_1002" }?.text shouldBe "edited text"

            // allNodes stream carries the update, without duplicating the node
            val allNodes = nodeManager.allNodes.first()
            allNodes.count { it.id == "ID_1002" } shouldBe 1
            allNodes.first { it.id == "ID_1002" }.text shouldBe "edited text"
        }

        "updateNodeText should return null for unknown node id" {
            val nodeManager = loadedNodeManager()
            nodeManager.updateNodeText("ID_does_not_exist", "nope") shouldBe null
        }

        "addNodeToMindmap should append child to parent and register it in indexes" {
            val nodeManager = loadedNodeManager()
            val parentBefore = nodeManager.getNodeByID("ID_1001")!!.childNodes.size

            val newNumericId = nodeManager.addNodeToMindmap("brand new child", nodeManager.getNodeByID("ID_1001"))
            newNumericId shouldNotBe null

            val newNode = nodeManager.getNodeByNumericId(newNumericId!!)
            newNode shouldNotBe null
            newNode?.text shouldBe "brand new child"
            newNode?.parentNode?.id shouldBe "ID_1001"
            newNode?.creationDate shouldNotBe null

            val parentAfter = nodeManager.getNodeByID("ID_1001")
            parentAfter?.childNodes?.size shouldBe parentBefore + 1
            parentAfter?.childNodes?.last()?.id shouldBe newNode?.id

            nodeManager.allNodes.first().count { it.id == newNode?.id } shouldBe 1
        }

        "addNodeToMindmap should reject blank text" {
            val nodeManager = loadedNodeManager()
            nodeManager.addNodeToMindmap("   ", nodeManager.getNodeByID("ID_1001")) shouldBe null
        }

        "serializeMindmap should round-trip a document preserving text and hierarchy" {
            val nodeManager = loadedNodeManager()
            val outDir = createTempDirectory(prefix = "droidplane-serialize")

            var saved: java.io.File? = null
            nodeManager.serializeMindmap(
                filePath = outDir.toAbsolutePath().toString(),
                filename = "round_trip.mm",
                onError = { throw it },
                onSaveFinished = { saved = it },
            )
            saved shouldNotBe null
            val savedFile = saved!!
            savedFile.name shouldBe "round_trip.mm"

            val xml = savedFile.readText()
            xml shouldContain "ID_1000"
            xml shouldContain "Droidplane Root"
            xml shouldContain "ID_1002"
            // formatting tags must survive the round-trip
            xml shouldContain "<icon"
            xml shouldContain "<font"

            // reload the saved document into a fresh manager: structure must survive
            val reloaded = initNodeManager()
            reloaded.loadMindMapFromInputStream(
                inputStream = savedFile.inputStream(),
                onError = { throw it },
                onParentNodeUpdate = {},
                onLoadFinished = {},
            )
            reloaded.allNodes.first().size shouldBe 11
            reloaded.rootNode?.text shouldBe "Droidplane Root"
            reloaded.getNodeByID("ID_1002")?.parentNode?.id shouldBe "ID_1001"
            reloaded.getNodeByID("ID_1002")?.parentNode?.childNodes?.size shouldBe nodeManager.getNodeByID("ID_1001")?.childNodes?.size
        }
    }

    /** Loads test_map.mm and fails loudly if anything goes wrong. */
    private suspend fun loadedNodeManager(): NodeManager {
        val nodeManager = initNodeManager()
        val inputStream: InputStream? = java.lang.ClassLoader.getSystemResourceAsStream("test_map.mm")
        inputStream shouldNotBe null
        nodeManager.loadMindMapFromInputStream(
            inputStream = inputStream!!,
            onError = { throw it },
            onParentNodeUpdate = {},
            onLoadFinished = {},
        )
        return nodeManager
    }

    private fun initNodeManager(
        nodeUtils: NodeUtils = NodeUtilsDefaultImpl(),
        logger: Logger = mockk(relaxed = true),
    ): NodeManager {
        return NodeManager(
            logger = logger,
            nodeUtils = nodeUtils,
            xmlParseUtils = XmlParseUtilsDefaultImpl(nodeUtils, logger),
            coroutineScope = TestScope(),
        )
    }

    private fun getFakeNodeUtils(): NodeUtils = object : NodeUtils {
        override fun loadRichContent(xpp: XmlPullParser): Result<RichContent> = Result.failure(Exception())

        override fun fillArrowLinks(nodesById: Map<String, Node>?) {}

        override fun loadAndIndexNodesByIds(root: Node?): MindmapIndexes = MindmapIndexes(emptyMap(), emptyMap())

        override fun parseNodeTag(xpp: XmlPullParser, parentNode: Node?): Result<Node> = Result.failure(Exception())
    }
}
