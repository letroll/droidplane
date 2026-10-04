package fr.julien.quievreux.droidplane2.data.search

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.Node
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

class SearchManagerTest : KStringSpec() {
    init {
        coroutineTestScope = true

        "search should filter nodes case-insensitively by plain text" {
            val testScope = TestScope()
            val logger = mockk<Logger>(relaxed = true)
            val rootNode = Node(
                parentNode = null,
                id = "ID_1",
                numericId = 1,
                text = "Root Node",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val child1 = Node(
                parentNode = rootNode,
                id = "ID_2",
                numericId = 2,
                text = "Architecture",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val child2 = Node(
                parentNode = rootNode,
                id = "ID_3",
                numericId = 3,
                text = "Navigation",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            rootNode.childNodes.addAll(listOf(child1, child2))

            val nodes = listOf(rootNode, child1, child2)
            val searchManager = SearchManager(
                scope = testScope,
                logger = mockk(relaxed = true),
                nodesSource = MutableStateFlow(nodes).asStateFlow(),
                fetchText = { it.text },
            )

            val collectJob = launch { searchManager.getSearchResult().collect { } }
            testScope.advanceUntilIdle()

            searchManager.search("arch") {}
            testScope.advanceUntilIdle()
            searchManager.getResultCount() shouldBe 1
            searchManager.getSearchResult().value.first().text shouldBe "Architecture"

            searchManager.search("ARCH") {}
            testScope.advanceUntilIdle()
            searchManager.getResultCount() shouldBe 1

            searchManager.search("nav") {}
            testScope.advanceUntilIdle()
            searchManager.getResultCount() shouldBe 1
            searchManager.getSearchResult().value.first().text shouldBe "Navigation"

            collectJob.cancel()
        }

        "search should return all nodes when query is blank" {
            val testScope = TestScope()
            val logger = mockk<Logger>(relaxed = true)
            val rootNode = Node(
                parentNode = null,
                id = "ID_1",
                numericId = 1,
                text = "Root Node",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val child1 = Node(
                parentNode = rootNode,
                id = "ID_2",
                numericId = 2,
                text = "Child 1",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            rootNode.childNodes.add(child1)

            val nodes = listOf(rootNode, child1)
            val searchManager = SearchManager(
                scope = testScope,
                logger = logger,
                nodesSource = MutableStateFlow(nodes).asStateFlow(),
                fetchText = { it.text },
            )

            val collectJob = launch { searchManager.getSearchResult().collect { } }
            testScope.advanceUntilIdle()

            searchManager.search("") {}
            testScope.advanceUntilIdle()
            searchManager.getResultCount() shouldBe 2

            collectJob.cancel()
        }

        "search should return empty results when no matches found" {
            val testScope = TestScope()
            val rootNode = Node(
                parentNode = null,
                id = "ID_1",
                numericId = 1,
                text = "Root Node",
                creationDate = 1000L,
                modificationDate = 1000L,
            )

            val nodes = listOf(rootNode)
            val searchManager = SearchManager(
                scope = testScope,
                logger = mockk<Logger>(relaxed = true),
                nodesSource = MutableStateFlow(nodes).asStateFlow(),
                fetchText = { it.text },
            )

            val collectJob = launch { searchManager.getSearchResult().collect { } }
            testScope.advanceUntilIdle()

            searchManager.search("nonexistent") {}
            testScope.advanceUntilIdle()
            searchManager.getResultCount() shouldBe 0
            searchManager.getSearchResult().value.isEmpty() shouldBe true

            collectJob.cancel()
        }

        "search should handle rich text content by stripping HTML" {
            val testScope = TestScope()
            val logger = mockk<Logger>(relaxed = true)
            val rootNode = Node(
                parentNode = null,
                id = "ID_1",
                numericId = 1,
                text = null,
                richTextContents = mutableListOf("<html><body><p>Rich <b>Content</b></p></body></html>"),
                creationDate = 1000L,
                modificationDate = 1000L,
            )

            val nodes = listOf(rootNode)
            val searchManager = SearchManager(
                scope = testScope,
                logger = logger,
                nodesSource = MutableStateFlow(nodes).asStateFlow(),
                fetchText = { node ->
                    node.richTextContents.firstOrNull()
                        ?.replace("<[^>]*>".toRegex(), "")
                        ?.replace("&[^;]+;".toRegex(), "")
                        ?: node.text
                },
            )

            val collectJob = launch { searchManager.getSearchResult().collect { } }
            testScope.advanceUntilIdle()

            searchManager.search("content") {}
            testScope.advanceUntilIdle()
            searchManager.getResultCount() shouldBe 1

            collectJob.cancel()
        }

        "onResultFound callback should be invoked when matches are found" {
            val testScope = TestScope()
            val rootNode = Node(
                parentNode = null,
                id = "ID_1",
                numericId = 1,
                text = "Test Node",
                creationDate = 1000L,
                modificationDate = 1000L,
            )

            val nodes = listOf(rootNode)
            val searchManager = SearchManager(
                scope = testScope,
                logger = mockk<Logger>(relaxed = true),
                nodesSource = MutableStateFlow(nodes).asStateFlow(),
                fetchText = { it.text },
            )

            val collectJob = launch { searchManager.getSearchResult().collect { } }
            testScope.advanceUntilIdle()

            var callbackInvoked = false
            searchManager.search("test") {
                callbackInvoked = true
            }
            testScope.advanceUntilIdle()
            callbackInvoked shouldBe true

            collectJob.cancel()
        }
    }
}
