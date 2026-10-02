package com.francescopaoli.northstar.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Ambito di vita dell'obiettivo. */
enum class Area(val label: String) {
    LAVORO("Lavoro"), SALUTE("Salute"), RELAZIONI("Relazioni"), PERSONALE("Personale")
}

/** I 6 criteri PNL dell'obiettivo ben formato, nell'ordine del percorso guidato. */
enum class Criterion(val label: String, val question: String, val hint: String) {
    POSITIVO(
        "Positivo",
        "Cosa vuoi ottenere?",
        "Dillo in positivo: cosa vuoi, non cosa vuoi evitare."
    ),
    SPECIFICO(
        "Specifico",
        "Descrivilo nel concreto.",
        "Cosa, quanto, come: più dettagli dai, più diventa reale."
    ),
    VERIFICABILE(
        "Verificabile",
        "Come farai a sapere\ndi averlo raggiunto?",
        "Un segnale concreto che puoi vedere, sentire o misurare."
    ),
    CONTROLLO(
        "Sotto il tuo controllo",
        "Cosa dipende da te\nper arrivarci?",
        "Le azioni che puoi fare tu. Ti piace programmarle nel dettaglio o le fai quando capita?"
    ),
    ECOLOGICO(
        "Ecologico",
        "Come si incastra\ncon il resto della tua vita?",
        "Tempo, energie, persone vicine. Di solito ti organizzi in anticipo o ti adatti giorno per giorno?"
    ),
    CONTESTUALIZZATO(
        "Contestualizzato",
        "Entro quando?",
        "Scegli la data e, se vuoi, dove e con chi."
    );
}

enum class GoalStatus { ACTIVE, POSTPONED, ACHIEVED }

/** Modalità di creazione eventi su Google Calendar. */
enum class CalendarMode { AUTO, CONFIRM }

/** Un passo concreto verso la scadenza (serve per la percentuale di avanzamento). */
data class GoalAction(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val done: Boolean = false,
    /** Momento in cui è stata spuntata: serve per "passo della settimana" e serie. */
    val doneAt: Long? = null,
)

data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val area: Area = Area.PERSONALE,
    val answers: Map<Criterion, String> = emptyMap(),
    val summary: String = "",
    /** Scadenza in epochDay (LocalDate.toEpochDay) per restare semplice da salvare. */
    val deadlineEpochDay: Long = LocalDate.now().plusMonths(1).toEpochDay(),
    val status: GoalStatus = GoalStatus.ACTIVE,
    val actions: List<GoalAction> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val achievedAt: Long? = null,
    val postponedCount: Int = 0,
    /** Indice del prossimo criterio da richiamare nel check-in (ruota sui 6). */
    val nextCheckinIndex: Int = 0,
    val lastCheckinAt: Long? = null,
    /** Check-in inviato (notifica) e non ancora risposto: resta in Home finché non rispondi. */
    val checkinPending: Boolean = false,
    val calendarEventId: String? = null,
) {
    val title: String get() = answers[Criterion.POSITIVO].orEmpty().ifBlank { "Nuovo obiettivo" }
    val deadline: LocalDate get() = LocalDate.ofEpochDay(deadlineEpochDay)
    val isOpen: Boolean get() = status != GoalStatus.ACHIEVED

    fun daysLeft(today: LocalDate = LocalDate.now()): Long = ChronoUnit.DAYS.between(today, deadline)

    /** Scadenza passata e obiettivo ancora aperto: l'app deve chiedere "l'hai raggiunto?". */
    fun isDue(today: LocalDate = LocalDate.now()): Boolean = isOpen && !deadline.isAfter(today)

    /** 0..1: quota di azioni completate. */
    val progress: Float
        get() = if (actions.isEmpty()) 0f else actions.count { it.done }.toFloat() / actions.size
}
