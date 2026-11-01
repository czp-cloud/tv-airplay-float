package io.github.jqssun.airplay.floating

import org.junit.Assert.*
import org.junit.Test

class OverlayGeometryTest {
    private fun geometry(aspect: Double) = OverlayGeometry(1920, 1080, aspect, 240)

    @Test fun portraitFollowsPhoneInsteadOfLeavingLandscapeFrame() {
        assertEquals(222 to 480, geometry(334.0 / 720).size(480))
        assertEquals(480 to 270, geometry(16.0 / 9).size(480))
    }

    @Test fun rotationKeepsLongestEdgeWhenItFits() {
        for (aspect in listOf(16.0 / 9, 334.0 / 720, 1.0, 720.0 / 334)) {
            val (w, h) = geometry(aspect).size(480)
            assertEquals(480, maxOf(w, h))
            assertTrue(kotlin.math.abs(w - h * aspect) < maxOf(1.0, aspect))
        }
    }

    @Test fun oversizedPortraitFitsAndDoesNotDestroyLandscapePreference() {
        val preferred = 1306
        val portrait = geometry(334.0 / 720)
        assertEquals(864, portrait.maxLongEdge)
        assertEquals(400 to 864, portrait.size(preferred))
        assertEquals(1306 to 734, geometry(16.0 / 9).size(preferred))
    }

    @Test fun sizeLimitsRespectBothScreenAxes() {
        for (aspect in listOf(0.1, 334.0 / 720, 1.0, 16.0 / 9, 10.0)) {
            val g = geometry(aspect)
            for (requested in listOf(Int.MIN_VALUE, 1, 240, 480, 16384, Int.MAX_VALUE)) {
                val (w, h) = g.size(requested)
                assertTrue(w in 1..1536)
                assertTrue(h in 1..864)
                assertTrue(maxOf(w, h) in g.minLongEdge..g.maxLongEdge)
            }
        }
    }

    @Test fun smallScreenNeverHasInvertedLimits() {
        val g = OverlayGeometry(100, 60, 9.0 / 16, 240)
        assertEquals(48, g.minLongEdge)
        assertEquals(27 to 48, g.size(320))
    }

    @Test fun legacyWidthStillMeansWidth() {
        val g = geometry(0.5)
        assertEquals(300 to 600, g.size(g.longEdgeForWidth(300)))
        assertEquals(700 to 393, geometry(16.0 / 9).size(700))
    }

    @Test fun invalidAspectFallsBackToLandscape() {
        for (aspect in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(320 to 180, geometry(aspect).size(320))
        }
    }
}
