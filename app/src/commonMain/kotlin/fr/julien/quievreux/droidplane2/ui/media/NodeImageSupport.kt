package fr.julien.quievreux.droidplane2.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

const val MIN_IMAGE_SCALE = 0.2f
const val MAX_IMAGE_SCALE = 4.0f
const val DEFAULT_IMAGE_SCALE = 1.0f
const val DEFAULT_IMAGE_BASE_WIDTH_DP = 120f
const val DEFAULT_IMAGE_BASE_HEIGHT_DP = 90f
const val IMAGE_VERTICAL_SPACING_DP = 8f

enum class ImageResizeCorner(
    val horizontalSign: Float,
    val verticalSign: Float
) {
    TOP_LEFT(horizontalSign = -1f, verticalSign = -1f),
    TOP_RIGHT(horizontalSign = 1f, verticalSign = -1f),
    BOTTOM_LEFT(horizontalSign = -1f, verticalSign = 1f),
    BOTTOM_RIGHT(horizontalSign = 1f, verticalSign = 1f)
}

/**
 * Computes the updated image scale factor when dragging a corner anchor by [dragDeltaXDp] and [dragDeltaYDp].
 * Outward drag relative to [corner] increases the scale; inward drag decreases the scale.
 * Result is always clamped to [MIN_IMAGE_SCALE]..[MAX_IMAGE_SCALE].
 */
fun computeResizedImageScale(
    initialScale: Float,
    corner: ImageResizeCorner,
    dragDeltaXDp: Float,
    dragDeltaYDp: Float,
    baseWidthDp: Float = DEFAULT_IMAGE_BASE_WIDTH_DP,
    baseHeightDp: Float = DEFAULT_IMAGE_BASE_HEIGHT_DP
): Float {
    val safeBaseWidth = if (baseWidthDp > 0f) baseWidthDp else DEFAULT_IMAGE_BASE_WIDTH_DP
    val safeBaseHeight = if (baseHeightDp > 0f) baseHeightDp else DEFAULT_IMAGE_BASE_HEIGHT_DP
    val horizontalScaleDelta = (dragDeltaXDp * corner.horizontalSign) / safeBaseWidth
    val verticalScaleDelta = (dragDeltaYDp * corner.verticalSign) / safeBaseHeight
    val combinedDelta = (horizontalScaleDelta + verticalScaleDelta) / 2f
    return (initialScale + combinedDelta).coerceIn(MIN_IMAGE_SCALE, MAX_IMAGE_SCALE)
}

@Composable
expect fun rememberImageFilePicker(onUriSelected: (String) -> Unit): () -> Unit

@Composable
expect fun rememberNodeImageBitmap(uri: String): ImageBitmap?
