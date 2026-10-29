package fr.julien.quievreux.droidplane2.core.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun Dispatchers.default() : CoroutineScope = CoroutineScope(Main + SupervisorJob())
