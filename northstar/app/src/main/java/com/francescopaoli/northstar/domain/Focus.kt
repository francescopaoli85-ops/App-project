package com.francescopaoli.northstar.domain

import com.francescopaoli.northstar.data.Area
import com.francescopaoli.northstar.data.Goal
import com.francescopaoli.northstar.data.GoalAction
import com.francescopaoli.northstar.data.GoalStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * "Focus di oggi": UNA sola cosa da fare, scelta per te.
 * Meno scelte = si agisce di più (e la stessa scelta la usa il widget "Passo di oggi").
 */
object Focus {

    data class Pick(val goal: Goal, val action: GoalAction?, val behind: Boolean)

    /**
     * Quanto un obiettivo è "indietro": tempo trascorso meno lavoro fatto (0..1 ciascuno).
     * Positivo = sei in ritardo rispetto al calendario.
     */
    fun lag(goal: Goal, today: LocalDate = LocalDate.now()): Float {
        val start = Engagement.dateOf(goal.createdAt)
        val total = ChronoUnit.DAYS.between(start, goal.deadline).coerceAtLeast(1)
        val elapsed = ChronoUnit.DAYS.between(start, today).coerceIn(0, total)
        return elapsed.toFloat() / total - goal.progress
    }

    /**
     * L'obiettivo su cui concentrarsi oggi: prima quelli arrivati a scadenza,
     * poi quelli con un'azione pronta, poi il più in ritardo sul calendario, a parità quello che scade prima.
     */
    fun pick(goals: List<Goal>, today: LocalDate = LocalDate.now()): Pick? {
        val g = goals.filter { it.isOpen }.sortedWith(
            compareByDescending<Goal> { it.isDue(today) }
                // prima chi ha un'azione pronta da fare: il focus deve essere qualcosa da spuntare
                .thenByDescending { Engagement.weeklyStep(it) != null }
                .thenByDescending { lag(it, today) }
                .thenBy { it.deadlineEpochDay },
        ).firstOrNull() ?: return null
        return Pick(g, Engagement.weeklyStep(g), lag(g, today) > 0.15f)
    }
}

/** Numeri dei Traguardi: per vedere quanta strada hai fatto. */
data class Stats(
    val achieved: Int,
    val achievedThisYear: Int,
    /** Giorni medi dalla creazione al traguardo (null se nessun traguardo). */
    val avgDays: Int?,
    /** Area con più traguardi (null se nessuno). */
    val topArea: Area?,
    /** Passi spuntati in tutto, su tutti gli obiettivi. */
    val stepsDone: Int,
    /** Traguardi raggiunti entro la data scelta (senza rinvii). */
    val onTime: Int,
) {
    companion object {
        fun of(goals: List<Goal>, today: LocalDate = LocalDate.now()): Stats {
            val done = goals.filter { it.status == GoalStatus.ACHIEVED }
            val days = done.mapNotNull { g -> g.achievedAt?.let { ChronoUnit.DAYS.between(Engagement.dateOf(g.createdAt), Engagement.dateOf(it)) } }
            return Stats(
                achieved = done.size,
                achievedThisYear = done.count { g -> g.achievedAt?.let { Engagement.dateOf(it).year == today.year } == true },
                avgDays = days.takeIf { it.isNotEmpty() }?.average()?.toInt()?.coerceAtLeast(1),
                topArea = done.groupingBy { it.area }.eachCount().maxByOrNull { it.value }?.key,
                stepsDone = goals.sumOf { g -> g.actions.count { it.done } },
                onTime = done.count { it.postponedCount == 0 },
            )
        }
    }
}
