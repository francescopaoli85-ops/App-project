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

class CheckinOneTapTest {
    private val goal = com.francescopaoli.northstar.data.Goal(
        answers = mapOf(Criterion.POSITIVO to "Correre 10 km"),
        deadlineEpochDay = LocalDate.now().plusDays(60).toEpochDay(),
    )

    @Test fun `check-in notificato resta in Home finché non rispondi`() {
        assertTrue(Checkins.needsAnswer(goal.copy(checkinPending = true, lastCheckinAt = System.currentTimeMillis())))
    }

    @Test fun `sì tutto ok fa avanzare il criterio e chiude il check-in`() {
        val g = Checkins.confirmed(goal.copy(checkinPending = true), now = 1000L)
        assertEquals(1, g.nextCheckinIndex)
        assertEquals(1000L, g.lastCheckinAt)
        assertEquals(false, g.checkinPending)
        assertEquals(goal.answers, g.answers)
        assertEquals(false, Checkins.needsAnswer(g, now = 2000L))
    }

    @Test fun `il nuovo campo sopravvive al salvataggio`() {
        val back = com.francescopaoli.northstar.data.GoalMapper.fromMap(
            com.francescopaoli.northstar.data.GoalMapper.toMap(goal.copy(checkinPending = true)),
        )
        assertEquals(true, back?.checkinPending)
    }
}

class FocusAndStatsTest {
    private val today = LocalDate.of(2026, 10, 2)
    private fun ms(d: LocalDate) = d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun goal(title: String, created: LocalDate, deadline: LocalDate, done: Int, total: Int) =
        com.francescopaoli.northstar.data.Goal(
            answers = mapOf(Criterion.POSITIVO to title),
            createdAt = ms(created), deadlineEpochDay = deadline.toEpochDay(),
            actions = (1..total).map { com.francescopaoli.northstar.data.GoalAction(text = "a$it", done = it <= done) },
        )

    @Test fun `il focus va sull'obiettivo più in ritardo e sulla sua prossima azione`() {
        val inTime = goal("In orario", today.minusDays(10), today.plusDays(10), done = 3, total = 4)
        val late = goal("In ritardo", today.minusDays(18), today.plusDays(2), done = 1, total = 4)
        val p = Focus.pick(listOf(inTime, late), today)!!
        assertEquals("In ritardo", p.goal.title)
        assertEquals("a2", p.action?.text)
        assertTrue(p.behind)
    }

    @Test fun `un obiettivo senza azioni non ruba il focus a uno con un passo pronto`() {
        val noActions = goal("Vuoto", today.minusDays(20), today.plusDays(1), done = 0, total = 0)
        val ready = goal("Pronto", today.minusDays(5), today.plusDays(30), done = 0, total = 2)
        assertEquals("Pronto", Focus.pick(listOf(noActions, ready), today)!!.goal.title)
    }

    @Test fun `senza obiettivi aperti nessun focus`() {
        assertEquals(null, Focus.pick(emptyList(), today))
    }

    @Test fun `statistiche dei traguardi`() {
        val a = goal("A", today.minusDays(30), today, 2, 2).copy(
            status = com.francescopaoli.northstar.data.GoalStatus.ACHIEVED, achievedAt = ms(today.minusDays(10)),
            area = com.francescopaoli.northstar.data.Area.SALUTE,
        )
        val b = goal("B", today.minusDays(10), today, 1, 3)
        val s = Stats.of(listOf(a, b), today)
        assertEquals(1, s.achieved)
        assertEquals(20, s.avgDays)
        assertEquals(com.francescopaoli.northstar.data.Area.SALUTE, s.topArea)
        assertEquals(3, s.stepsDone)
        assertEquals(1, s.onTime)
    }
}
