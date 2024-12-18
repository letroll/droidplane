package fr.julien.quievreux.droidplane2.helper

import android.content.Context
import android.util.TypedValue

data class Dp(val dpVal: Float) {
    operator fun plus(dpValue: Dp): Dp = Dp(dpVal + dpValue.dpVal)

    operator fun minus(dpValue: Dp): Dp = Dp(dpVal - dpValue.dpVal)

    operator fun times(dpValue: Dp): Dp = Dp(dpVal * dpValue.dpVal)

    operator fun div(dpValue: Dp): Dp = Dp(dpVal / dpValue.dpVal)

    operator fun rem(dpValue: Dp): Dp = Dp(dpVal % dpValue.dpVal)

    operator fun plus(value: Float): Dp = Dp(dpVal + value)

    operator fun minus(value: Float): Dp = Dp(dpVal - value)

    operator fun times(value: Float): Dp = Dp(dpVal * value)

    operator fun div(value: Float): Dp = Dp(dpVal / value)

    operator fun rem(value: Float): Dp = Dp(dpVal % value)

    operator fun plus(value: Int): Dp = Dp(dpVal + value)

    operator fun minus(value: Int): Dp = Dp(dpVal - value)

    operator fun times(value: Int): Dp = Dp(dpVal * value)

    operator fun div(value: Int): Dp = Dp(dpVal / value)

    operator fun rem(value: Int): Dp = Dp(dpVal % value)
}

data class Px(val pxVal: Float) {
    operator fun plus(pxValue: Px): Px = Px(pxVal + pxValue.pxVal)

    operator fun minus(pxValue: Px): Px = Px(pxVal - pxValue.pxVal)

    operator fun times(pxValue: Px): Px = Px(pxVal * pxValue.pxVal)

    operator fun div(pxValue: Px): Px = Px(pxVal / pxValue.pxVal)

    operator fun rem(pxValue: Px): Px = Px(pxVal % pxValue.pxVal)

    operator fun plus(value: Float): Px = Px(pxVal + value)

    operator fun minus(value: Float): Px = Px(pxVal - value)

    operator fun times(value: Float): Px = Px(pxVal * value)

    operator fun div(value: Float): Px = Px(pxVal / value)

    operator fun rem(value: Float): Px = Px(pxVal % value)

    operator fun plus(value: Int): Px = Px(pxVal + value)

    operator fun minus(value: Int): Px = Px(pxVal - value)

    operator fun times(value: Int): Px = Px(pxVal * value)

    operator fun div(value: Int): Px = Px(pxVal / value)

    operator fun rem(value: Int): Px = Px(pxVal % value)
}

fun Dp.toPx(context: Context): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dpVal,
    context.resources
        .displayMetrics,
)

fun Px.toDp(context: Context): Float {
    val scale = context.resources.displayMetrics.density
    return pxVal / scale
}
