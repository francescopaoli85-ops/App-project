package com.francescopaoli.northstar.audio

import kotlin.math.exp
import kotlin.math.floor

/** Cosa deve suonare in questo momento. */
sealed interface Scene {
    /** Sottofondo dell'app (canzone scelta nelle impostazioni). */
    data object Ambient : Scene
    /** Percorso guidato: [level] 1..6 = domande, 7 = riepilogo e festa (apoteosi). */
    data class Flow(val level: Int) : Scene
}

/** Le due canzoni di sottofondo tra cui l'utente sceglie. */
enum class AmbientSong(val id: String, val label: String, val asset: String) {
    ENERGY("energy", "Energica", "music/ambient_energy.webm"),
    CALM("calm", "Calma", "music/ambient_calm.webm");

    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id } ?: ENERGY
    }
}

/**
 * Regole della musica, senza Android: quali tracce suonano, a che volume e quando entrano.
 *
 * Tutte le musiche sono a 130,7 BPM e allineate sul battito:
 * il loop del percorso dura 16 battute, i sottofondi 32 (due giri esatti).
 */
object MusicPlan {
    const val SAMPLE_RATE = 48_000
    const val FLOW_FRAMES = 1_410_723
    const val AMBIENT_FRAMES = 2 * FLOW_FRAMES
    const val BAR_FRAMES = FLOW_FRAMES / 16.0

    /** Strumenti della canzone del percorso e passo in cui entrano (configurazione scelta nella demo). */
    val FLOW_STEMS: List<Pair<String, Int>> = listOf(
        "piano" to 1, "acoustic_guitar" to 1, "bass" to 1, "synth" to 1,
        "percussion" to 2,
        "drum_kit" to 3,
        "string_section" to 4,
        "electric_guitar" to 6, "sound_effects" to 6,
        "brass_section" to 7,
    )

    /** Dissolvenze (secondi): ingresso strumenti nel percorso e cambio di scena. */
    const val FLOW_FADE_S = 3.5f
    const val SCENE_FADE_S = 2.5f
    /** Volume della musica mentre la voce guida parla o ascolta. */
    const val VOICE_DUCK = 0.10f

    fun flowAsset(stem: String) = "music/flow_$stem.webm"

    /** Volume (0/1) di uno strumento del percorso nella scena data. */
    fun flowTarget(enterStep: Int, scene: Scene): Float =
        if (scene is Scene.Flow && enterStep <= scene.level) 1f else 0f

    /** Volume (0/1) di una canzone di sottofondo nella scena data. */
    fun ambientTarget(song: AmbientSong, chosen: AmbientSong, scene: Scene): Float =
        if (scene == Scene.Ambient && song == chosen) 1f else 0f

    /** Primo inizio di battuta dopo [clock] (in frame): lì entrano gli strumenti nuovi, a tempo. */
    fun nextBar(clock: Long): Long = (floor(clock / BAR_FRAMES + 1.0) * BAR_FRAMES).toLong()

    /**
     * Passo di una dissolvenza esponenziale: dopo [frames] il volume [from] si avvicina a [to].
     * Con [fadeSeconds] si arriva a circa il 95% (tre costanti di tempo).
     */
    fun approach(from: Float, to: Float, frames: Int, fadeSeconds: Float): Float {
        if (fadeSeconds <= 0f) return to
        val tc = fadeSeconds / 3f * SAMPLE_RATE
        val k = exp(-frames / tc)
        val v = to + (from - to) * k
        return if (kotlin.math.abs(v - to) < 1e-4f) to else v
    }
}
