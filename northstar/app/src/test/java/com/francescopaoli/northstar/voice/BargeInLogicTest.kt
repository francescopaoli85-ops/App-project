package com.francescopaoli.northstar.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BargeInLogicTest {

    /** Eco della voce guida che oscilla tra 55 e 65 dB (sillabe). */
    private fun echo(i: Int) = 55.0 + (i % 7) * 1.5

    private fun run(logic: BargeInLogic, frames: List<Double>) = frames.any { logic.onFrame(it) }

    @Test
    fun echoAloneNeverTriggers() {
        assertFalse(run(BargeInLogic(), List(500) { echo(it) }))
    }

    @Test
    fun echoLouderLaterStillIgnoredWithinMargin() {
        // la guida alza un po' il tono dopo la taratura: +5 dB non basta
        assertFalse(run(BargeInLogic(), List(35) { echo(it) } + List(200) { echo(it) + 5 }))
    }

    @Test
    fun userVoiceTriggers() {
        val frames = List(35) { echo(it) } + List(40) { echo(it) } + List(20) { 80.0 }
        assertTrue(run(BargeInLogic(), frames))
    }

    @Test
    fun shortNoiseDoesNotTrigger() {
        // un colpo di tosse / oggetto che cade: 100 ms forti e basta
        val frames = List(35) { echo(it) } + List(5) { 85.0 } + List(100) { echo(it) }
        assertFalse(run(BargeInLogic(), frames))
    }

    @Test
    fun speechWithSmallPausesTriggers() {
        // voce con brevissime pause tra le parole
        val voice = List(24) { if (it % 6 == 5) 50.0 else 80.0 }
        assertTrue(run(BargeInLogic(), List(35) { echo(it) } + voice))
    }
}
