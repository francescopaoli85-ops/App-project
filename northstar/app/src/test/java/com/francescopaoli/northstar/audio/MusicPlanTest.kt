package com.francescopaoli.northstar.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPlanTest {

    @Test fun `il percorso aggiunge strumenti a ogni passo fino all'apoteosi`() {
        val counts = (1..7).map { lvl -> MusicPlan.FLOW_STEMS.count { MusicPlan.flowTarget(it.second, Scene.Flow(lvl)) == 1f } }
        assertEquals(listOf(4, 5, 6, 7, 7, 9, 10), counts)
    }

    @Test fun `nel sottofondo il percorso tace e suona solo la canzone scelta`() {
        MusicPlan.FLOW_STEMS.forEach { assertEquals(0f, MusicPlan.flowTarget(it.second, Scene.Ambient)) }
        assertEquals(1f, MusicPlan.ambientTarget(AmbientSong.CALM, AmbientSong.CALM, Scene.Ambient))
        assertEquals(0f, MusicPlan.ambientTarget(AmbientSong.ENERGY, AmbientSong.CALM, Scene.Ambient))
        assertEquals(0f, MusicPlan.ambientTarget(AmbientSong.CALM, AmbientSong.CALM, Scene.Flow(3)))
    }

    @Test fun `i sottofondi durano esattamente due giri del percorso`() {
        assertEquals(2L * MusicPlan.FLOW_FRAMES, MusicPlan.AMBIENT_FRAMES.toLong())
    }

    @Test fun `gli strumenti entrano sulla battuta successiva`() {
        val bar = MusicPlan.BAR_FRAMES
        assertEquals(bar.toLong(), MusicPlan.nextBar(0))
        assertEquals((2 * bar).toLong(), MusicPlan.nextBar(bar.toLong() + 10))
        assertTrue(MusicPlan.nextBar(12_345) > 12_345)
    }

    @Test fun `la dissolvenza arriva a destinazione senza superarla`() {
        var g = 0f
        repeat(48_000 * 4 / 480) { g = MusicPlan.approach(g, 1f, 480, MusicPlan.FLOW_FADE_S) }
        assertTrue(g > 0.95f && g <= 1f)
        assertEquals(1f, MusicPlan.approach(0f, 1f, 480, 0f))
    }

    @Test fun `canzone sconosciuta = energica`() {
        assertEquals(AmbientSong.ENERGY, AmbientSong.byId("boh"))
        assertEquals(AmbientSong.CALM, AmbientSong.byId("calm"))
    }

    @Test fun `il cambio rapido cade sul battito, prima della battuta`() {
        val c = 12_345L
        assertTrue(MusicPlan.nextBeat(c) > c)
        assertTrue(MusicPlan.nextBeat(c) <= MusicPlan.nextBar(c))
        assertTrue(MusicPlan.nextBeat(c) - c <= (MusicPlan.BAR_FRAMES / 4).toLong() + 1)
    }
}
