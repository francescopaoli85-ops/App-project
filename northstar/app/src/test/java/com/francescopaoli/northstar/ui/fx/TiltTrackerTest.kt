package com.francescopaoli.northstar.ui.fx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TiltTrackerTest {
    @Test fun `inclinare sposta il cielo in modo ben visibile`() {
        val t = TiltTracker()
        t.update(0f, 0f)
        var o = t.update(0.2f, -0.2f)
        repeat(20) { o = t.update(0.2f, -0.2f) }
        assertTrue("x=${o.x}", o.x > 0.6f)
        assertTrue("y=${o.y}", o.y < -0.6f)
    }

    @Test fun `tenendolo storto torna piano al centro`() {
        val t = TiltTracker()
        t.update(0f, 0f)
        repeat(50 * 30) { t.update(0.3f, 0f) } // 30 s a 50 letture al secondo
        assertEquals(0f, t.update(0.3f, 0f).x, 0.05f)
    }
}
