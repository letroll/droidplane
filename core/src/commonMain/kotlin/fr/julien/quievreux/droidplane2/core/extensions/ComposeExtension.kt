package fr.julien.quievreux.droidplane2.core.extensions

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

// Extension function to convert pixels to DP
fun Float.toDp(density: Density): Dp = with(density) { this@toDp.toDp() }
