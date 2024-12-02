package fr.julien.quievreux.droidplane2.core.testutils

import fr.julien.quievreux.droidplane2.core.async.CoroutineDispatchers
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

fun coroutineScope(): Triple<TestScope, TestDispatcher, CoroutineDispatchers> {
    val testCoroutineScope = TestScope()
    val testDispatcher = StandardTestDispatcher(testCoroutineScope.testScheduler)
    val dispatchers = CoroutineDispatchers(
        main = testDispatcher,
        io = testDispatcher,
        computing = testDispatcher,
    )

    return Triple(testCoroutineScope, testDispatcher, dispatchers)
}

@OptIn(ExperimentalCoroutinesApi::class)
open class KStringSpec(private val setMain: Boolean = true) : StringSpec() {
    protected val testCoroutineScope: TestScope = TestScope()
    protected val testDispatcher: TestDispatcher = StandardTestDispatcher(testCoroutineScope.testScheduler)
    protected val dispatchers: CoroutineDispatchers = CoroutineDispatchers(
        main = testDispatcher,
        io = testDispatcher,
        computing = testDispatcher,
    )

    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerTest

    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        if (setMain) {
            Dispatchers.setMain(testDispatcher)
        }
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        if (setMain) {
            Dispatchers.resetMain()
            testCoroutineScope.testScheduler.cancelChildren()
        }

        if (result.isFailure) {
            println("Test failed: ${testCase.name}")
            println("Running coroutines:")
            println(testCoroutineScope.testScheduler.toString())
        }
    }
}