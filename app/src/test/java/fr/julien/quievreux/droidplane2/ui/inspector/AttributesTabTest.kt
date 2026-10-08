package fr.julien.quievreux.droidplane2.ui.inspector

import fr.julien.quievreux.droidplane2.InspectorTab
import fr.julien.quievreux.droidplane2.MainViewModel
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.data.NodeUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.XmlParseUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.model.NodeAttributeEntry
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope

class AttributesTabTest : KStringSpec() {

    init {
        coroutineTestScope = true

        "adding, modifying, and deleting attributes persists on node update" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]

            // 1. Add attributes to child
            val updatedWithAttrs = child.copy(
                attributes = mutableListOf(
                    NodeAttributeEntry("Priority", "Urgent"),
                    NodeAttributeEntry("Owner", "Bob")
                ),
                iconNames = mutableListOf("flag-red", "button_ok")
            )

            viewModel.onSaveNodeProperties(updatedWithAttrs)

            eventually {
                val stored = nodeManager.getNodeByID(child.id)
                stored shouldNotBe null
                stored!!.attributes.size shouldBe 2
                stored.attributes[0].name shouldBe "Priority"
                stored.attributes[0].value shouldBe "Urgent"
                stored.iconNames.size shouldBe 2
            }

            // 2. Modify and delete an attribute
            val modifiedChild = nodeManager.getNodeByID(child.id)!!.copy(
                attributes = mutableListOf(
                    NodeAttributeEntry("Priority", "Medium")
                )
            )

            viewModel.onSaveNodeProperties(modifiedChild)

            eventually {
                val stored = nodeManager.getNodeByID(child.id)
                stored shouldNotBe null
                stored!!.attributes.size shouldBe 1
                stored.attributes[0].value shouldBe "Medium"
            }
        }
    }

    private suspend fun eventually(
        timeoutMs: Long = 3000,
        condition: suspend () -> Unit,
    ) {
        val start = System.currentTimeMillis()
        var lastError: Throwable? = null
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                condition()
                return
            } catch (e: Throwable) {
                lastError = e
                delay(10)
            }
        }
        throw lastError ?: AssertionError("condition never became true")
    }

    private suspend fun realNodeManager(): NodeManager {
        val logger = mockk<Logger>(relaxed = true)
        val nodeUtils = NodeUtilsDefaultImpl()
        val nodeManager = NodeManager(
            logger = logger,
            nodeUtils = nodeUtils,
            xmlParseUtils = XmlParseUtilsDefaultImpl(nodeUtils, logger),
            coroutineScope = TestScope(),
        )
        val rootId = nodeManager.addNodeToMindmap("root node", null)
        rootId shouldNotBe null
        nodeManager.addNodeToMindmap("child node", nodeManager.getNodeByNumericId(rootId!!))
        return nodeManager
    }
}
