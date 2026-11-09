package fr.julien.quievreux.droidplane2.ui.mindmap

import fr.julien.quievreux.droidplane2.ui.media.DEFAULT_IMAGE_SCALE
import fr.julien.quievreux.droidplane2.ui.media.ImageResizeCorner
import fr.julien.quievreux.droidplane2.ui.media.MAX_IMAGE_SCALE
import fr.julien.quievreux.droidplane2.ui.media.MIN_IMAGE_SCALE
import fr.julien.quievreux.droidplane2.ui.media.computeResizedImageScale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val FLOAT_TOLERANCE = 0.0001f

/**
 * Unit tests for [computeResizedImageScale] and [ImageResizeCorner] scale calculations.
 */
class NodeImageResizeCalculationTest {

    @Test
    fun `GIVEN initial scale 1_0f, WHEN dragging BOTTOM_RIGHT corner outward by 120f and 90f dp, THEN scale increases to 2_0f`() {
        val updatedScale = computeResizedImageScale(
            initialScale = DEFAULT_IMAGE_SCALE,
            corner = ImageResizeCorner.BOTTOM_RIGHT,
            dragDeltaXDp = 120f,
            dragDeltaYDp = 90f
        )

        assertEquals(2.0f, updatedScale, FLOAT_TOLERANCE)
        assertTrue(updatedScale > DEFAULT_IMAGE_SCALE)
    }

    @Test
    fun `GIVEN initial scale 1_0f, WHEN dragging TOP_LEFT corner outward and inward, THEN scale increases to 2_0f and decreases to 0_5f`() {
        val expandedScale = computeResizedImageScale(
            initialScale = DEFAULT_IMAGE_SCALE,
            corner = ImageResizeCorner.TOP_LEFT,
            dragDeltaXDp = -120f,
            dragDeltaYDp = -90f
        )
        val shrunkScale = computeResizedImageScale(
            initialScale = DEFAULT_IMAGE_SCALE,
            corner = ImageResizeCorner.TOP_LEFT,
            dragDeltaXDp = 60f,
            dragDeltaYDp = 45f
        )

        assertEquals(2.0f, expandedScale, FLOAT_TOLERANCE)
        assertEquals(0.5f, shrunkScale, FLOAT_TOLERANCE)
        assertTrue(shrunkScale < DEFAULT_IMAGE_SCALE)
    }

    @Test
    fun `GIVEN initial scale 1_0f, WHEN dragging TOP_RIGHT and BOTTOM_LEFT corners outward, THEN both increase scale to 2_0f`() {
        val topRightScale = computeResizedImageScale(
            initialScale = DEFAULT_IMAGE_SCALE,
            corner = ImageResizeCorner.TOP_RIGHT,
            dragDeltaXDp = 120f,
            dragDeltaYDp = -90f
        )
        val bottomLeftScale = computeResizedImageScale(
            initialScale = DEFAULT_IMAGE_SCALE,
            corner = ImageResizeCorner.BOTTOM_LEFT,
            dragDeltaXDp = -120f,
            dragDeltaYDp = 90f
        )

        assertEquals(2.0f, topRightScale, FLOAT_TOLERANCE)
        assertEquals(2.0f, bottomLeftScale, FLOAT_TOLERANCE)
    }

    @Test
    fun `GIVEN initial scale 1_0f, WHEN dragging far inward or outward, THEN scale clamps to MIN_IMAGE_SCALE and MAX_IMAGE_SCALE`() {
        val minClampedScale = computeResizedImageScale(
            initialScale = DEFAULT_IMAGE_SCALE,
            corner = ImageResizeCorner.BOTTOM_RIGHT,
            dragDeltaXDp = -600f,
            dragDeltaYDp = -450f
        )
        val maxClampedScale = computeResizedImageScale(
            initialScale = DEFAULT_IMAGE_SCALE,
            corner = ImageResizeCorner.BOTTOM_RIGHT,
            dragDeltaXDp = 600f,
            dragDeltaYDp = 450f
        )

        assertEquals(MIN_IMAGE_SCALE, minClampedScale, FLOAT_TOLERANCE)
        assertEquals(0.2f, minClampedScale, FLOAT_TOLERANCE)
        assertEquals(MAX_IMAGE_SCALE, maxClampedScale, FLOAT_TOLERANCE)
        assertEquals(4.0f, maxClampedScale, FLOAT_TOLERANCE)
        assertTrue(minClampedScale in MIN_IMAGE_SCALE..MAX_IMAGE_SCALE)
        assertTrue(maxClampedScale in MIN_IMAGE_SCALE..MAX_IMAGE_SCALE)
    }
}
