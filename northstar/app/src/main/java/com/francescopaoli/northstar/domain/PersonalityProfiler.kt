package com.francescopaoli.northstar.domain

import com.francescopaoli.northstar.data.CalendarMode
import com.francescopaoli.northstar.data.Criterion

/**
 * Profilazione implicita "chi ama controllare tutto" vs "chi lascia fare in automatico".
 *
 * Non ci sono domande esplicite: i suggerimenti di alcuni criteri contengono
 * domande indirette ("le programmi nel dettaglio o le fai quando capita?")
 * e qui leggiamo i segnali nelle risposte e nel comportamento:
 *  - lessico di pianificazione vs. lessico di flessibilità
 *  - quanto sono dettagliate le risposte
 *  - quante volte l'utente ha rifatto una risposta
 *  - se ha aggiunto il contesto opzionale (dove / con chi)
 *
 * Punteggio alto = controllore → crea eventi SOLO con conferma.
 * Punteggio basso = delega → crea eventi in automatico.
 */
object PersonalityProfiler {

    private val controlWords = listOf(
        "pianific", "programm", "organizz", "agenda", "calendar", "orario", "alle ore",
        "ogni giorno", "ogni mattina", "ogni sera", "precis", "lista", "schema",
        "tabella", "controll", "decido io", "in anticipo", "dettagli", "metodo", "routine",
    )
    private val flowWords = listOf(
        "quando capita", "vediamo", "più o meno", "boh", "non so", "lascio",
        "automatic", "improvvis", "flessib", "giorno per giorno", "mi adatto",
        "a sentimento", "come viene", "all'occorrenza", "spontane",
    )

    data class Signals(
        val answers: Map<Criterion, String>,
        /** Quante volte in totale ha cancellato/ridetto una risposta. */
        val revisions: Int = 0,
    )

    fun score(s: Signals): Int {
        val text = s.answers.values.joinToString(" ").lowercase()
        var score = 0
        score += controlWords.count { it in text }.coerceAtMost(3)
        score -= flowWords.count { it in text }.coerceAtMost(3)

        val words = s.answers.values.map { it.trim().split(Regex("\\s+")).filter(String::isNotBlank).size }
        val avg = if (words.isEmpty()) 0.0 else words.average()
        if (avg >= 12) score += 1
        if (avg in 0.1..4.0) score -= 1

        if (s.revisions >= 2) score += 1
        if (s.answers[Criterion.CONTESTUALIZZATO].orEmpty().isNotBlank()) score += 1
        return score
    }

    fun calendarMode(s: Signals): CalendarMode =
        if (score(s) >= 2) CalendarMode.CONFIRM else CalendarMode.AUTO
}
