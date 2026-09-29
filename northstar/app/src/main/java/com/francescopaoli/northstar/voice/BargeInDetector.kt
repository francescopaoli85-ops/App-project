package com.francescopaoli.northstar.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import kotlin.concurrent.thread
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Ascolta il microfono MENTRE la voce guida parla e avvisa appena l'utente inizia a parlare.
 * Usa la sorgente VOICE_COMMUNICATION con cancellazione dell'eco (la voce guida esce
 * sullo stesso canale "chiamata", vedi VoiceGuide) e [BargeInLogic] per scartare l'eco residua.
 */
class BargeInDetector(private val onVoice: () -> Unit) {

    @Volatile private var running = false
    private var worker: Thread? = null

    @SuppressLint("MissingPermission") // il permesso è verificato da VoiceGuide prima di chiamare start()
    fun start() {
        if (running) return
        running = true
        worker = thread(name = "barge-in") {
            val rate = 16_000
            val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION, rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2,
                )
            }.getOrNull()
            if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) { running = false; return@thread }

            val aec = if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(rec.audioSessionId) else null
            val ns = if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(rec.audioSessionId) else null
            aec?.enabled = true
            ns?.enabled = true

            val frame = ShortArray(rate / 50) // 20 ms
            val logic = BargeInLogic()
            rec.startRecording()
            try {
                while (running) {
                    val n = rec.read(frame, 0, frame.size)
                    if (n <= 0) continue
                    if (logic.onFrame(rmsDb(frame, n))) {
                        running = false
                        onVoice()
                    }
                }
            } finally {
                runCatching { rec.stop() }
                rec.release(); aec?.release(); ns?.release()
            }
        }
    }

    fun stop() {
        running = false
        worker?.join(300)
        worker = null
    }

    private fun rmsDb(buf: ShortArray, n: Int): Double {
        var sum = 0.0
        for (i in 0 until n) sum += buf[i].toDouble() * buf[i]
        val rms = sqrt(sum / n)
        return if (rms < 1) 0.0 else 20 * log10(rms)
    }
}
