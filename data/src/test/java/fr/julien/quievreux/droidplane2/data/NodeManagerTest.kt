package fr.julien.quievreux.droidplane2.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.xmlpull.v1.XmlPullParser

class NodeManagerTest : StringSpec() {

    init {
        coroutineTestScope = true
        /*
        Tests todo:
        *
         */
        "parsing document without start should throw an error" {
            val fakeXmlPullParser: XmlPullParser = mockk(relaxed = true)
            coEvery { fakeXmlPullParser.eventType }.returns(XmlPullParser.END_TAG)
            var possibleException: Exception? = null
            val nodeManager = initNodeManager()
            nodeManager.loadMindMap(
                xpp = fakeXmlPullParser,
                onParentNode = {},
                onError = { exception ->
//                    possibleException = exception
                },
                onReadFinished = {},
            )

           possibleException shouldNotBe null
        }

        "loadMindMap" {
//            val nodeManager = initNodeManager()
//            nodeManager.loadMindMap {  }
        }

    }

    private fun initNodeManager(): NodeManager = NodeManager(
        logger = mockk(relaxed = true),
        coroutineScope = TestScope()
    )
}
