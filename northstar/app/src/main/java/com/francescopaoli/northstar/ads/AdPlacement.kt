package com.francescopaoli.northstar.ads

/**
 * Dove inserire UNA card sponsorizzata in una lista.
 * Mai in cima (prima i contenuti dell'utente), mai in una lista vuota.
 */
object AdPlacement {
    /** Home: dopo il 2° obiettivo. */
    const val HOME_AFTER = 2

    /** Traguardi: dopo il 3° traguardo. */
    const val ACHIEVEMENTS_AFTER = 3

    /** Indice dopo cui mettere l'annuncio, o null se la lista è vuota. */
    fun slot(listSize: Int, after: Int): Int? = if (listSize <= 0) null else minOf(after, listSize)
}
