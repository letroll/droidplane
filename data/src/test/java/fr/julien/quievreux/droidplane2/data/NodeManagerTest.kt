package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.Node
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.xmlpull.v1.XmlPullParser
import java.util.Stack

class NodeManagerTest : KStringSpec() {

    init {
        coroutineTestScope = true

        //todo Tests
        /*
        *  parsing document without node should throw an error?
        *  parsing document should finish with mindmap processing
         */
        "parsing document without start should throw an error" {
            val fakeXmlPullParser: XmlPullParser = mockk(relaxed = true)
            coEvery { fakeXmlPullParser.eventType }.returns(XmlPullParser.END_TAG)
            coEvery { fakeXmlPullParser.next() }.returns(XmlPullParser.END_DOCUMENT)
            var possibleException: Exception? = null
            val nodeManager = initNodeManager()
            val job = launch {
                nodeManager.loadMindMap(
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
                nodeManager.loadMindMap(
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

        "at end of parsing we should obtain a mindmap node" {
            val fakeXmlPullParser: XmlPullParser = mockk(relaxed = true)
//            coEvery { fakeXmlPullParser.eventType }.returns(XmlPullParser.END_TAG)
//            coEvery { fakeXmlPullParser.next() }.returns(XmlPullParser.END_DOCUMENT)
            coEvery { fakeXmlPullParser.getAttributeValue(any(),any()) }.returns(null)
            coEvery { fakeXmlPullParser.getAttributeValue(any(),any()) }.returns(null)
//            var finishWithProcessing = false
            val nodeManager = initNodeManager()
            val nodeStack = Stack<Node>()
            var nodeResult : Node? = null
            val job = launch {
                nodeManager.parseNode(
                    nodeStack = nodeStack,
                    xpp = fakeXmlPullParser,
                    onParentNodeUpdate = { result ->
                        nodeResult = result
                    }
                )
            }

            job.join()
            job.cancel()

            nodeResult shouldNotBe null
        }

        /*
         test idea:
         * "at end of parsing we should obtain a mindmap node"
         * "parse a node should end up add it to the mindmap"

         */

    }

    private fun initNodeManager(nodeUtils:NodeUtils= NodeUtilsDefaultImpl()): NodeManager {
        return NodeManager(
            logger = mockk(relaxed = true),
            nodeUtils = nodeUtils,
            xmlParseUtils = XmlParseUtilsDefaultImpl(nodeUtils),
            coroutineScope = TestScope(),
        )
    }

    private fun getFakeNodeUtils(): NodeUtils = object :NodeUtils{
        override fun loadRichContentNodes(xpp: XmlPullParser): String {
            TODO("Not yet implemented")
        }

        override fun fillArrowLinks(nodesById: Map<String, Node>?) {
            TODO("Not yet implemented")
        }

        override fun loadAndIndexNodesByIds(root: Node?): MindmapIndexes {
            TODO("Not yet implemented")
        }

        override fun parseNodeTag(xpp: XmlPullParser, parentNode: Node?): Node {
            TODO("Not yet implemented")
        }

    }
}
