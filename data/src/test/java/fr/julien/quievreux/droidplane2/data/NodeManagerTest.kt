package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.NodeManager.Companion.FILE_EXTENSION
import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.RichContent
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import org.xmlpull.v1.XmlPullParser

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
                    childNode?.parentNode shouldBe dadNode
                    dadNode = nodeManager.getNodeByNumericId(dadNodeId)
                    dadNode?.childNodes?.first()?.id shouldBe childNode?.id
                }
            }
        }

        //TODO jqx look if with freeplane, when we modify a child if parent modification date change also and if it's recursif

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
