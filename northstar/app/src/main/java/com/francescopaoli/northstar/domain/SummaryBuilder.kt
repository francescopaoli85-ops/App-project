package com.francescopaoli.northstar.domain

import com.francescopaoli.northstar.data.Criterion
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Compone la frase riassuntiva finale a partire dalle 6 risposte.
 * Esempio: "Entro il 24 novembre voglio presentare il piano del negozio:
 * tre azioni già avviate — lo saprò quando il team lo approva."
 */
object SummaryBuilder {

    private val dateFmt = DateTimeFormatter.ofPattern("d MMMM", Locale.ITALIAN)
    private val dateFmtYear = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN)

    fun formatDate(date: LocalDate, today: LocalDate = LocalDate.now()): String =
        date.format(if (date.year == today.year) dateFmt else dateFmtYear)

    fun build(
        answers: Map<Criterion, String>,
        deadline: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): String {
        val positivo = clean(answers[Criterion.POSITIVO]).lowerFirst()
        val specifico = clean(answers[Criterion.SPECIFICO]).lowerFirst()
        val verifica = clean(answers[Criterion.VERIFICABILE])
            .removePrefixIgnoreCase("quando ").lowerFirst()
        val contesto = clean(answers[Criterion.CONTESTUALIZZATO]).lowerFirst()

        return buildString {
            append("Entro il ").append(formatDate(deadline, today))
            if (contesto.isNotEmpty()) append(", ").append(contesto).append(",")
            append(" ").append(positivo.ifEmpty { "raggiungerò il mio obiettivo" })
            if (specifico.isNotEmpty() && !positivo.contains(specifico, ignoreCase = true)) {
                append(": ").append(specifico)
            }
            if (verifica.isNotEmpty()) append(" — lo saprò quando ").append(verifica)
            append(".")
        }
    }

    /** Toglie spazi e punteggiatura finale, così le parti si incastrano. */
    private fun clean(s: String?): String = s.orEmpty().trim().trimEnd('.', '!', ';', ',').trim()

    /** Minuscola iniziale, ma lascia intatte sigle tipo "PC" o "UX". */
    private fun String.lowerFirst(): String =
        if (length >= 2 && this[1].isUpperCase()) this
        else replaceFirstChar { it.lowercase(Locale.ITALIAN) }

    private fun String.removePrefixIgnoreCase(prefix: String): String =
        if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this
}
