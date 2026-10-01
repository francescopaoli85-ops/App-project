package com.francescopaoli.northstar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import com.francescopaoli.northstar.R
import com.francescopaoli.northstar.data.Settings

/** Effetti sonori brevi e morbidi (arpa, legno, aria), tutti in Do maggiore come la musica. */
class Sfx(context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val tapId = pool.load(context, R.raw.sfx_tap, 1)
    private val swooshId = pool.load(context, R.raw.sfx_swoosh, 1)
    private val doneId = pool.load(context, R.raw.sfx_done, 1)
    private val successId = pool.load(context, R.raw.sfx_success, 1)
    private val stepIds = listOf(
        R.raw.sfx_step1, R.raw.sfx_step2, R.raw.sfx_step3, R.raw.sfx_step4, R.raw.sfx_step5, R.raw.sfx_step6,
    ).map { pool.load(context, it, 1) }

    @Volatile var enabled = true
    @Volatile var foreground = true
        set(v) { field = v; if (v) pool.autoResume() else pool.autoPause() }

    private fun play(id: Int, volume: Float = VOLUME) {
        if (enabled && foreground) pool.play(id, volume, volume, 1, 0, 1f)
    }

    /** Tocco leggero su un bottone. */
    fun tap() = play(tapId, VOLUME * 0.8f)
    /** Cambio di sezione dalla barra in basso. */
    fun swoosh() = play(swooshId, VOLUME * 0.8f)
    /** Risposta data nel percorso: una nota d'arpa che sale a ogni passo (1..6). */
    fun step(n: Int) = play(stepIds[(n - 1).coerceIn(0, stepIds.lastIndex)])
    /** Azione segnata come fatta. */
    fun done() = play(doneId)
    /** Obiettivo creato o raggiunto. */
    fun success() = play(successId)

    private companion object { const val VOLUME = 0.6f }
}

/** Tutti i suoni dell'app: musica adattiva + effetti, guidati dalle impostazioni. */
class SoundManager(context: Context) {
    val music = MusicEngine(context)
    val sfx = Sfx(context)

    fun apply(s: Settings) {
        music.setEnabled(s.musicOn)
        music.setVolume(s.musicVolume / 100f)
        music.setSong(AmbientSong.byId(s.ambientSong))
        sfx.enabled = s.sfxOn
    }

    /** App visibile o in secondo piano: fuori dall'app nessun suono. */
    fun setForeground(v: Boolean) {
        music.setForeground(v)
        sfx.foreground = v
    }
}

/** Accesso ai suoni dalle schermate (null nei test e nelle anteprime: niente audio). */
val LocalSound = staticCompositionLocalOf<SoundManager?> { null }
