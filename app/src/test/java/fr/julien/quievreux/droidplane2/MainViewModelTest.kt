package fr.julien.quievreux.droidplane2

import app.cash.turbine.test
import fr.julien.quievreux.droidplane2.model.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.MainUiState.DialogUiState
import fr.julien.quievreux.droidplane2.MainUiState.SearchUiState
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class MainViewModelTest : KStringSpec() {
    init {
        coroutineTestScope = true

//        lateinit var mindMapStream: InputStream

        beforeSpec {
//            mindMapStream = FakeDataSource.mindMap.byteInputStream()
        }

        "check default state" {
            val viewModel = initMainViewModel()
            viewModel.uiState.test {
                awaitItem().apply {
                    loading shouldBe false
                    leaving shouldBe false
                    canGoBack shouldBe false
                    title shouldBe ""
                    error shouldBe ""
                    nodeCurrentlyDisplayed shouldBe null
                    errorAction shouldBe null
                    viewIntentNode shouldBe null
                    contentNodeType shouldBe Classic
                    searchUiState shouldBe SearchUiState()
                    dialogUiState shouldBe DialogUiState()
                }
            }
        }

//            "load mind map should finish with a callback" {
//            val viewModel = initMainViewModel()
//            viewModel.uiState.test {
//                awaitItem().apply {
////                    var finished = false
//                    viewModel.loadMindMap(mindMapStream)
//
//                    loading shouldBe false
////                    finished shouldBe true
//
//                    cancelAndIgnoreRemainingEvents()
//                }
//            }
//        }
    }

    private fun initMainViewModel(): MainViewModel {
//        val nodeUtils = NodeUtilsDefaultImpl()
        val logger = mockk<Logger>(relaxed = true)
        return MainViewModel(
            logger = logger
        )
    }
}