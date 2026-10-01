package com.francescopaoli.northstar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

/**
 * Musica adattiva: un solo AudioTrack, un mixer che somma tutte le tracce allineate al campione.
 *
 * - Le tracce (10 strumenti del percorso + 2 sottofondi) girano in loop sullo stesso orologio,
 *   quindi restano sempre a tempo: cambiare scena o canzone è solo una dissolvenza.
 * - Le tracce si decodificano in memoria solo quando servono e si liberano quando non suonano più.
 * - Nessun suono con l'app in secondo piano, e rispetto della musica di altre app.
 */
class MusicEngine(private val context: Context) {

    private class Track(val asset: String, val frames: Int, val isFlow: Boolean, val enterStep: Int = 0) {
        @Volatile var data: ShortArray? = null
        @Volatile var loading = false
        var gain = 0f
        var target = 0f
        var fade = MusicPlan.SCENE_FADE_S
        /** Nuovo volume in attesa del prossimo inizio di battuta (-1 = nessuno). */
        var pendingTarget = -1f
        var pendingAt = 0L
        /** Frame da cui la traccia è muta (per liberarla dopo un po'). */
        var silentSince = -1L
    }

    private val audio = context.getSystemService(AudioManager::class.java)
    private val flow = MusicPlan.FLOW_STEMS.map { (stem, step) ->
        Track(MusicPlan.flowAsset(stem), MusicPlan.FLOW_FRAMES, isFlow = true, enterStep = step)
    }
    private val ambient = AmbientSong.entries.associateWith { Track(it.asset, MusicPlan.AMBIENT_FRAMES, isFlow = false) }
    private val all = flow + ambient.values

    private val commands = ConcurrentLinkedQueue<() -> Unit>()
    private val decoder = Executors.newSingleThreadExecutor { Thread(it, "music-decode").apply { priority = Thread.MIN_PRIORITY } }
    private val lock = Object()

    // stato voluto (scritto dal thread principale)
    @Volatile private var enabled = true
    @Volatile private var foreground = false
    @Volatile private var focusLost = false
    @Volatile private var otherAppPlaying = false
    @Volatile private var volume = 0.4f
    @Volatile private var ducked = false

    // stato del mixer (solo thread audio)
    private var scene: Scene = Scene.Ambient
    private var song = AmbientSong.ENERGY
    private var clock = 0L
    private var duckGain = 1f
    private var masterGain = 0f
    private var thread: Thread? = null
    private var track: AudioTrack? = null
    private var focusRequest: AudioFocusRequest? = null

    // ---------------- comandi (thread principale) ----------------

    fun setScene(s: Scene) = post { if (scene != s) { scene = s; retarget(musical = true) } }

    fun setSong(s: AmbientSong) = post { if (song != s) { song = s; retarget(musical = false) } }

    fun setDucked(v: Boolean) { ducked = v }

    fun setVolume(v: Float) { volume = v.coerceIn(0f, 1f) }

    fun setEnabled(v: Boolean) {
        if (enabled == v) return
        enabled = v
        refreshPlayback()
    }

    /** App visibile o in secondo piano: fuori dall'app la musica si ferma del tutto. */
    fun setForeground(v: Boolean) {
        if (foreground == v) return
        foreground = v
        // al rientro: se c'è già musica di un'altra app (Spotify...) non la copriamo
        if (v) { otherAppPlaying = thread == null && audio.isMusicActive; focusLost = false }
        refreshPlayback()
    }

    private fun post(cmd: () -> Unit) {
        commands.add(cmd)
        synchronized(lock) { lock.notifyAll() }
    }

    private fun shouldPlay() = enabled && foreground && !focusLost && !otherAppPlaying

    private fun refreshPlayback() {
        if (shouldPlay()) start() else stop()
    }

    // ---------------- avvio / arresto ----------------

    private fun start() {
        if (thread != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    // telefonata, altra app che suona: silenzio
                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { focusLost = true; refreshPlayback() }
                    AudioManager.AUDIOFOCUS_GAIN -> { focusLost = false; refreshPlayback() }
                }
            }
            .build()
        if (audio.requestAudioFocus(req) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        focusRequest = req

        val minBuf = AudioTrack.getMinBufferSize(MusicPlan.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val t = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(MusicPlan.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
            )
            .setBufferSizeInBytes(max(minBuf, MusicPlan.SAMPLE_RATE / 5 * 4)) // ~200 ms
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        masterGain = 0f // ogni ripartenza sfuma da zero
        val th = Thread({ runMixer(t) }, "music-mixer").apply { priority = Thread.MAX_PRIORITY }
        thread = th
        t.play()
        th.start()
        post { retarget(musical = false) }
    }

    private fun stop() {
        val th = thread ?: return
        thread = null
        synchronized(lock) { lock.notifyAll() }
        th.interrupt()
        runCatching { th.join(300) }
        track?.let { runCatching { it.pause(); it.flush(); it.release() } }
        track = null
        focusRequest?.let { audio.abandonAudioFocusRequest(it) }
        focusRequest = null
        // in secondo piano liberiamo la memoria del percorso (si ricarica al bisogno)
        if (!foreground) flow.forEach { it.data = null }
    }

    // ---------------- mixer (thread audio) ----------------

    /** Ricalcola i volumi voluti per la scena attuale. */
    private fun retarget(musical: Boolean) {
        // il percorso parte solo quando i suoi strumenti sono pronti: intanto resta il sottofondo
        val wantFlow = scene is Scene.Flow
        val flowReady = flow.all { it.data != null }
        if (wantFlow && !flowReady) flow.forEach { load(it) }
        val effective = if (wantFlow && !flowReady) Scene.Ambient else scene
        load(ambient.getValue(song))

        val bar = MusicPlan.nextBar(clock)
        for (t in flow) setTarget(t, MusicPlan.flowTarget(t.enterStep, effective), MusicPlan.FLOW_FADE_S, if (musical) bar else -1)
        for ((s, t) in ambient) setTarget(t, MusicPlan.ambientTarget(s, song, effective), MusicPlan.SCENE_FADE_S, if (musical) bar else -1)
    }

    private fun setTarget(t: Track, v: Float, fade: Float, at: Long) {
        t.fade = fade
        if (v > t.target && at >= 0) { // gli strumenti nuovi entrano sulla battuta
            t.pendingTarget = v; t.pendingAt = at
        } else {
            t.pendingTarget = -1f; t.target = v
        }
    }

    private fun load(t: Track) {
        if (t.data != null || t.loading) return
        t.loading = true
        decoder.execute {
            val pcm = runCatching { decode(t.asset, t.frames) }
                .onFailure { Log.w("Music", "decodifica fallita ${t.asset}", it) }.getOrNull()
            t.data = pcm
            t.loading = false
            post { retarget(musical = true) }
        }
    }

    private fun runMixer(out: AudioTrack) {
        val n = BLOCK
        val mix = FloatArray(n * 2)
        val pcm = ShortArray(n * 2)
        while (thread === Thread.currentThread() && !Thread.currentThread().isInterrupted) {
            while (true) { (commands.poll() ?: break).invoke() }

            // volumi: dissolvenze per blocco, interpolate dentro il blocco
            val masterTo = volume * MASTER
            val duckTo = if (ducked) MusicPlan.VOICE_DUCK else 1f
            val m0 = masterGain * duckGain
            masterGain = MusicPlan.approach(masterGain, masterTo, n, 0.6f)
            duckGain = MusicPlan.approach(duckGain, duckTo, n, if (ducked) 0.25f else 1.0f)
            val m1 = masterGain * duckGain

            java.util.Arrays.fill(mix, 0f)
            for (t in all) {
                if (t.pendingTarget >= 0 && clock >= t.pendingAt) { t.target = t.pendingTarget; t.pendingTarget = -1f }
                val g0 = t.gain
                val g1 = MusicPlan.approach(g0, t.target, n, t.fade)
                t.gain = g1
                val data = t.data
                if (g0 == 0f && g1 == 0f) {
                    if (t.silentSince < 0) t.silentSince = clock
                    // dopo 10 s di silenzio si libera la memoria delle tracce che non servono più
                    val unused = if (t.isFlow) scene !is Scene.Flow else t !== ambient[song]
                    if (data != null && unused && clock - t.silentSince > UNLOAD_AFTER) t.data = null
                    continue
                }
                t.silentSince = -1
                if (data == null) continue
                var pos = ((clock % t.frames).toInt())
                for (i in 0 until n) {
                    val g = g0 + (g1 - g0) * i / n
                    mix[2 * i] += data[2 * pos] * g
                    mix[2 * i + 1] += data[2 * pos + 1] * g
                    if (++pos == t.frames) pos = 0
                }
            }
            for (i in 0 until n) {
                val g = (m0 + (m1 - m0) * i / n) / 32768f
                pcm[2 * i] = softClip(mix[2 * i] * g)
                pcm[2 * i + 1] = softClip(mix[2 * i + 1] * g)
            }
            if (out.write(pcm, 0, pcm.size) < 0) break
            clock += n
        }
    }

    /** Limita i picchi senza distorcere: lineare fino a 0,8, poi curva morbida. */
    private fun softClip(x: Float): Short {
        val a = kotlin.math.abs(x)
        val y = if (a <= 0.8f) a else 0.8f + 0.2f * kotlin.math.tanh((a - 0.8f) / 0.2f)
        return (Math.copySign(min(y, 1f), x) * 32767f).toInt().toShort()
    }

    // ---------------- decodifica Opus → PCM 16 bit stereo ----------------

    private fun decode(asset: String, frames: Int): ShortArray {
        val out = ShortArray(frames * 2)
        var written = 0
        val afd = context.assets.openFd(asset)
        val ex = MediaExtractor()
        try {
            ex.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            ex.selectTrack(0)
            val fmt = ex.getTrackFormat(0)
            val codec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
            try {
                codec.configure(fmt, null, null, 0)
                codec.start()
                var channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val info = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                while (!outputDone) {
                    if (!inputDone) {
                        val i = codec.dequeueInputBuffer(10_000)
                        if (i >= 0) {
                            val buf = codec.getInputBuffer(i)!!
                            val size = ex.readSampleData(buf, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(i, 0, size, ex.sampleTime, 0)
                                ex.advance()
                            }
                        }
                    }
                    val o = codec.dequeueOutputBuffer(info, 10_000)
                    when {
                        o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ->
                            channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        o >= 0 -> {
                            val sb = codec.getOutputBuffer(o)!!.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            sb.position(info.offset / 2)
                            val samples = info.size / 2
                            if (channels == 2) {
                                val take = min(samples, out.size - written)
                                sb.get(out, written, take); written += take
                            } else {
                                repeat(min(samples, (out.size - written) / 2)) {
                                    val s = sb.get(); out[written++] = s; out[written++] = s
                                }
                            }
                            codec.releaseOutputBuffer(o, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 || written >= out.size) outputDone = true
                        }
                    }
                }
            } finally {
                runCatching { codec.stop() }
                codec.release()
            }
        } finally {
            ex.release()
            afd.close()
        }
        return out
    }

    private companion object {
        const val BLOCK = 480 // 10 ms
        /** Volume massimo della musica: tenuto basso, è un sottofondo. */
        const val MASTER = 0.55f
        const val UNLOAD_AFTER = 10L * MusicPlan.SAMPLE_RATE
    }
}
