package io.github.jqssun.airplay.floating

import kotlin.math.floor

/** Size in physical pixels; the user's preferred longest edge is independent of orientation. */
internal class OverlayGeometry(screenWidth: Int, screenHeight: Int, aspect: Double, minimum: Int) {
    val aspect = aspect.takeIf { it.isFinite() && it > 0 } ?: (16.0 / 9.0)
    private val widthFactor = minOf(1.0, this.aspect)
    private val heightFactor = minOf(1.0, 1.0 / this.aspect)
    val maxLongEdge = floor(minOf(screenWidth * 0.8 / widthFactor, screenHeight * 0.8 / heightFactor))
        .toInt().coerceAtLeast(1)
    val minLongEdge = minimum.coerceIn(1, maxLongEdge)

    fun size(preferredLongEdge: Int): Pair<Int, Int> {
        val edge = preferredLongEdge.coerceIn(minLongEdge, maxLongEdge)
        return maxOf(1, floor(edge * widthFactor).toInt()) to
            maxOf(1, floor(edge * heightFactor).toInt())
    }

    fun longEdgeForWidth(width: Int): Int = (width / widthFactor).toInt().coerceIn(1, 16384)
}
