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

class EngagementTest {
    private val zone = java.time.ZoneId.systemDefault()
    private fun at(d: LocalDate) = d.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val wed = LocalDate.of(2026, 9, 30) // mercoledì

    private fun goalWithDone(vararg days: LocalDate) = com.francescopaoli.northstar.data.Goal(
        actions = days.mapIndexed { i, d ->
            com.francescopaoli.northstar.data.GoalAction(id = "$i", text = "a$i", done = true, doneAt = at(d))
        } + com.francescopaoli.northstar.data.GoalAction(id = "next", text = "prossimo"),
    )

    @Test
    fun streakCountsConsecutiveWeeks() {
        val g = goalWithDone(wed, wed.minusWeeks(1), wed.minusWeeks(2), wed.minusWeeks(4))
        assertEquals(3, Engagement.streakWeeks(listOf(g), wed))
    }

    @Test
    fun currentWeekWithoutStepsDoesNotBreakStreak() {
        val g = goalWithDone(wed.minusWeeks(1), wed.minusWeeks(2))
        assertEquals(2, Engagement.streakWeeks(listOf(g), wed))
    }

    @Test
    fun missedWeekResetsStreak() {
        val g = goalWithDone(wed.minusWeeks(2))
        assertEquals(0, Engagement.streakWeeks(listOf(g), wed))
    }

    @Test
    fun weeklyStepIsFirstUndoneAndDoneThisWeekFiltersByWeek() {
        val g = goalWithDone(wed.minusDays(1), wed.minusWeeks(1))
        assertEquals("next", Engagement.weeklyStep(g)?.id)
        assertEquals(1, Engagement.doneThisWeek(listOf(g), wed).size)
    }
}
