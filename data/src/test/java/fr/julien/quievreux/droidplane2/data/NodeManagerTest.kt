package fr.julien.quievreux.droidplane2.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.xmlpull.v1.XmlPullParser

class NodeManagerTest : StringSpec() {

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
                    onParentNode = {},
                    onError = { exception ->
                        possibleException = exception
                    },
                    onReadFinished = {},
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
                    onParentNode = {},
                    onError = { },
                    onReadFinished = {
                        finishWithProcessing = true
                    },
                )
            }

            job.join()
            job.cancel()

            finishWithProcessing shouldBe true

        }

    }

    private fun initNodeManager(): NodeManager = NodeManager(
        logger = mockk(relaxed = true),
        coroutineScope = TestScope()
    )
}
