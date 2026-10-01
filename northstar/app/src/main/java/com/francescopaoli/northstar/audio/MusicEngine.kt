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
        /** Frame già decodificati: la traccia può suonare mentre si decodifica il resto. */
        @Volatile var ready = 0
        @Volatile var complete = false
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
    /** Parte solo dopo aver letto le impostazioni (così suona subito la canzone giusta). */
    @Volatile private var configured = false
    @Volatile private var stoppedAt = 0L
    @Volatile private var foreground = false
    @Volatile private var focusLost = false
    @Volatile private var otherAppPlaying = false
    @Volatile private var volume = 0.4f
    /** Volume voluto per la voce guida: 1 = niente, più basso mentre parla o ascolta. */
    @Volatile private var voiceDuck = 1f
    /** Il riconoscimento vocale ha preso il focus audio: la musica si abbassa, non si ferma. */
    @Volatile private var focusDuck = false
    /** Siamo nel percorso guidato (dove voce e microfono prendono il focus audio). */
    @Volatile private var inFlow = false

    // stato del mixer (solo thread audio)
    private var scene: Scene = Scene.Ambient
    private var song = AmbientSong.ENERGY
    /** Canzone di sottofondo che suona davvero: cambia solo quando la nuova è pronta. */
    private var playingSong: AmbientSong? = null
    private var clock = 0L
    private var duckGain = 1f
    private var masterGain = 0f
    private var thread: Thread? = null
    private var track: AudioTrack? = null
    private var focusRequest: AudioFocusRequest? = null

    // ---------------- comandi (thread principale) ----------------

    fun setScene(s: Scene) {
        inFlow = s is Scene.Flow
        post { if (scene != s) { scene = s; retarget(musical = true) } }
    }

    fun setSong(s: AmbientSong) = post { if (song != s) { song = s; retarget(musical = true) } }

    /** Voce guida: mentre parla la musica scende al 10%, mentre ascolta al 25%. */
    fun setVoice(speaking: Boolean, listening: Boolean) {
        voiceDuck = when {
            speaking -> MusicPlan.VOICE_DUCK
            listening -> MusicPlan.LISTEN_DUCK
            else -> 1f
        }
    }

    fun setVolume(v: Float) { volume = v.coerceIn(0f, 1f) }

    /** Impostazioni dell'utente: la prima volta fa anche partire la musica. */
    fun configure(enabled: Boolean, volume: Float, song: AmbientSong) {
        setVolume(volume)
        if (!configured) post { this.song = song; playingSong = song } else setSong(song)
        configured = true
        this.enabled = enabled
        refreshPlayback()
    }

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
        if (v) {
            // musica di un'altra app? (non la coda della nostra appena fermata)
            val justStopped = System.currentTimeMillis() - stoppedAt < 3000
            otherAppPlaying = thread == null && !justStopped && audio.isMusicActive
            focusLost = false
        }
        refreshPlayback()
    }

    private fun post(cmd: () -> Unit) {
        commands.add(cmd)
        synchronized(lock) { lock.notifyAll() }
    }

    private fun phoneCall() = audio.mode == AudioManager.MODE_IN_CALL || audio.mode == AudioManager.MODE_RINGTONE

    private fun shouldPlay() = configured && enabled && foreground && !focusLost && !otherAppPlaying

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
                    AudioManager.AUDIOFOCUS_GAIN -> { focusLost = false; focusDuck = false; refreshPlayback() }
                    // nel percorso il focus lo prende il nostro microfono: basta abbassare
                    else -> if (inFlow && !phoneCall()) focusDuck = true
                    // telefonata o un'altra app che suona: silenzio
                    else { focusLost = true; refreshPlayback() }
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
        stoppedAt = System.currentTimeMillis()
        synchronized(lock) { lock.notifyAll() }
        th.interrupt()
        runCatching { th.join(300) }
        track?.let { runCatching { it.pause(); it.flush(); it.release() } }
        track = null
        focusRequest?.let { audio.abandonAudioFocusRequest(it) }
        focusRequest = null
        // in secondo piano liberiamo la memoria del percorso (si ricarica al bisogno)
        if (!foreground) flow.filter { !it.loading }.forEach { it.data = null; it.complete = false; it.ready = 0 }
    }

    // ---------------- mixer (thread audio) ----------------

    /** Ricalcola i volumi voluti per la scena attuale. */
    private fun retarget(musical: Boolean) {
        // le due canzoni di sottofondo restano sempre pronte: il cambio è immediato
        load(ambient.getValue(song))
        AmbientSong.entries.forEach { load(ambient.getValue(it)) }
        // la canzone scelta entra quando è decodificata (o subito, se non suona ancora niente)
        if (playingSong == null || ambient.getValue(song).complete) playingSong = song
        val current = playingSong ?: song

        // il percorso parte solo quando i suoi strumenti sono pronti: intanto resta il sottofondo
        val wantFlow = scene is Scene.Flow
        val flowReady = flow.all { it.complete }
        if (wantFlow && !flowReady) flow.forEach { load(it) }
        val effective = if (wantFlow && !flowReady) Scene.Ambient else scene

        val bar = MusicPlan.nextBar(clock)
        for (t in flow) setTarget(t, MusicPlan.flowTarget(t.enterStep, effective), MusicPlan.FLOW_FADE_S, if (musical) bar else -1)
        for ((s, t) in ambient) setTarget(t, MusicPlan.ambientTarget(s, current, effective), MusicPlan.SCENE_FADE_S, if (musical) bar else -1)
    }

    private fun setTarget(t: Track, v: Float, fade: Float, at: Long) {
        t.fade = fade
        if (v != t.target && at >= 0) { // entrate e uscite cadono insieme sulla battuta: dissolvenza incrociata
            t.pendingTarget = v; t.pendingAt = at
        } else {
            t.pendingTarget = -1f; t.target = v
        }
    }

    private fun load(t: Track) {
        if (t.data != null || t.loading) return
        t.loading = true
        t.ready = 0
        t.complete = false
        t.data = ShortArray(t.frames * 2)
        decoder.execute {
            val ok = runCatching { decode(t) }
                .onFailure { Log.w("Music", "decodifica fallita ${t.asset}", it) }.isSuccess
            if (!ok) t.data = null
            t.complete = ok
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
            val duckTo = min(voiceDuck, if (focusDuck) MusicPlan.LISTEN_DUCK else 1f)
            val m0 = masterGain * duckGain
            masterGain = MusicPlan.approach(masterGain, masterTo, n, 0.6f)
            // giù veloce quando parte la voce, su morbido quando finisce
            duckGain = MusicPlan.approach(duckGain, duckTo, n, if (duckTo < duckGain) 0.25f else 1.0f)
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
                    // (solo il percorso: i sottofondi restano pronti per cambiare canzone al volo)
                    if (t.isFlow && data != null && t.complete && scene !is Scene.Flow && clock - t.silentSince > UNLOAD_AFTER) {
                        t.data = null; t.complete = false; t.ready = 0
                    }
                    continue
                }
                t.silentSince = -1
                if (data == null) continue
                val ready = if (t.complete) t.frames else t.ready
                var pos = ((clock % t.frames).toInt())
                for (i in 0 until n) {
                    if (pos < ready) {
                        val g = g0 + (g1 - g0) * i / n
                        mix[2 * i] += data[2 * pos] * g
                        mix[2 * i + 1] += data[2 * pos + 1] * g
                    }
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

    private fun decode(t: Track) {
        val out = t.data ?: return
        var written = 0
        val afd = context.assets.openFd(t.asset)
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
                                t.ready = written / 2
                            } else {
                                repeat(min(samples, (out.size - written) / 2)) {
                                    val s = sb.get(); out[written++] = s; out[written++] = s
                                }
                                t.ready = written / 2
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
    }

    private companion object {
        const val BLOCK = 480 // 10 ms
        /** Volume massimo della musica: tenuto basso, è un sottofondo. */
        const val MASTER = 0.55f
        const val UNLOAD_AFTER = 10L * MusicPlan.SAMPLE_RATE
    }
}
