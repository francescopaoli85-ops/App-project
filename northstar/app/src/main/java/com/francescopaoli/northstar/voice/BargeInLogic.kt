package com.francescopaoli.northstar.voice

/**
 * Decide se nel microfono c'è la voce dell'utente o solo l'eco della voce guida.
 * Logica pura (niente Android) così si può testare.
 *
 * 1. Periodo di taratura: nei primi [calibrationFrames] frame la guida sta già parlando,
 *    quindi misuriamo il livello dell'eco (il picco).
 * 2. Poi scatta solo se il livello supera l'eco di [marginDb] per almeno
 *    [sustainFrames] frame (con qualche buco concesso tra una sillaba e l'altra).
 */
class BargeInLogic(
    private val calibrationFrames: Int = 35, // 35 x 20 ms = 0,7 s
    private val marginDb: Double = 10.0,
    private val sustainFrames: Int = 15,     // 0,3 s di voce
    private val maxGapFrames: Int = 3,
    private val minAbsoluteDb: Double = 50.0,
) {
    private var frames = 0
    private var echoPeak = 0.0
    private var loud = 0
    private var gap = 0

    /** Passa il livello (dB) di un frame da 20 ms. true = l'utente sta parlando. */
    fun onFrame(db: Double): Boolean {
        if (frames < calibrationFrames) {
            frames++
            if (db > echoPeak) echoPeak = db
            return false
        }
        val threshold = maxOf(echoPeak + marginDb, minAbsoluteDb)
        if (db > threshold) {
            loud++; gap = 0
        } else if (loud > 0 && ++gap > maxGapFrames) {
            loud = 0; gap = 0
        }
        return loud >= sustainFrames
    }
}
