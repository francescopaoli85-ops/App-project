package com.francescopaoli.northstar.domain

import com.francescopaoli.northstar.data.CalendarMode
import com.francescopaoli.northstar.data.Criterion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DomainTest {

    private val today = LocalDate.of(2026, 9, 29)

    @Test
    fun summaryCombinesAnswers() {
        val s = SummaryBuilder.build(
            mapOf(
                Criterion.POSITIVO to "Voglio presentare il piano del negozio.",
                Criterion.SPECIFICO to "Tre azioni già avviate",
                Criterion.VERIFICABILE to "Quando il team lo approva",
            ),
            LocalDate.of(2026, 11, 24),
            today,
        )
        assertEquals(
            "Entro il 24 novembre voglio presentare il piano del negozio: tre azioni già avviate — lo saprò quando il team lo approva.",
            s,
        )
    }

    @Test
    fun summaryShowsYearWhenDifferent() {
        val s = SummaryBuilder.build(mapOf(Criterion.POSITIVO to "Correre 5 km"), LocalDate.of(2027, 3, 1), today)
        assertTrue(s.startsWith("Entro il 1 marzo 2027 correre 5 km"))
    }

    @Test
    fun plannerGetsConfirmMode() {
        val signals = PersonalityProfiler.Signals(
            mapOf(
                Criterion.CONTROLLO to "Lo pianifico ogni sera in agenda, alle ore 7",
                Criterion.CONTESTUALIZZATO to "al parco con Marco",
            ),
        )
        assertEquals(CalendarMode.CONFIRM, PersonalityProfiler.calendarMode(signals))
    }

    @Test
    fun easyGoingGetsAutoMode() {
        val signals = PersonalityProfiler.Signals(
            mapOf(Criterion.CONTROLLO to "boh, quando capita", Criterion.ECOLOGICO to "mi adatto"),
        )
        assertEquals(CalendarMode.AUTO, PersonalityProfiler.calendarMode(signals))
    }
}

class AdPlacementTest {
    @Test
    fun noAdInEmptyList() = assertEquals(null, com.francescopaoli.northstar.ads.AdPlacement.slot(0, 2))

    @Test
    fun adAtEndOfShortList() = assertEquals(1, com.francescopaoli.northstar.ads.AdPlacement.slot(1, 2))

    @Test
    fun adAfterSecondItem() = assertEquals(2, com.francescopaoli.northstar.ads.AdPlacement.slot(5, 2))
}
