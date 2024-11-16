package fr.julien.quievreux.droidplane2.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun Dispatchers.default() : CoroutineScope = CoroutineScope(Main + SupervisorJob())
