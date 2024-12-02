package fr.julien.quievreux.droidplane2.core.async

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.stateIn

class CoroutineDispatchers(
    val main: CoroutineDispatcher,
    val io: CoroutineDispatcher,
    val computing: CoroutineDispatcher,
)

fun <T> SharedFlow<T>.onFirstSubscription(block: suspend () -> Unit): SharedFlow<T> {
    var first = true
    return this.onSubscription {
        if (first) {
            first = false
            block()
        }
    }
}

fun <T> Flow<T>.uiStateIn(scope: CoroutineScope, initialValue: T): StateFlow<T> = this.stateIn(
    scope,
    SharingStarted.WhileSubscribed(5000),//it will allow the StateFlow survive 5 seconds before it been canceled
    initialValue = initialValue,
)
