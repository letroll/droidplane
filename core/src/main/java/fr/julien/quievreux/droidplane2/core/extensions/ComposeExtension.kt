package fr.julien.quievreux.droidplane2.core.extensions

import android.content.res.Resources
import android.util.TypedValue.applyDimension
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

// Extension function to convert pixels to DP
// val density = LocalDensity.current.density
fun Float.toDp(density: Density): Dp = with(density) { this@toDp.toDp() }

// Extension function to convert DP to PX
//@Composable
//fun Int.dpToPx(): Float {
//    val density = LocalDensity.current.density
//    return this * density
//}

//@Composable
//fun Dp.toPx(): Float {
//    return with(LocalDensity.current) { this@toPx.toPx() }
//}

//@Composable
//fun Dp.toPx() = with(LocalDensity.current) { this@toPx.toPx() }


//@Composable
//fun Int.toDp() = with(LocalDensity.current) { this@toDp.toDp() }

//public fun Dp.toPx(
//    resources: Resources,
//): Int = applyDimension(
//    android.util.TypedValue.COMPLEX_UNIT_DIP,
//    value,
//    resources.displayMetrics,
//).toInt()