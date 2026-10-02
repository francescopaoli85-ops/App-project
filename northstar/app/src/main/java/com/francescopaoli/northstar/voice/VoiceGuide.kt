package com.francescopaoli.northstar.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class VoiceState(
    val speaking: Boolean = false,
    val listening: Boolean = false,
    /** Testo riconosciuto mentre l'utente parla. */
    val partial: String = "",
    /** Livello voce 0..1 per animare le barre. */
    val level: Float = 0f,
    val available: Boolean = true,
)

/**
 * Voce guida (TextToSpeech) + ascolto (SpeechRecognizer) con priorità all'ascolto:
 * se l'utente parla mentre la guida sta parlando, la guida si zittisce subito
 * e parte il riconoscimento, come in una conversazione vera.
 *
 * Per non scambiare la voce del telefono per quella dell'utente:
 *  - la guida esce sul canale "chiamata in vivavoce", così il filtro anti-eco del telefono la toglie dal microfono;
 *  - [BargeInLogic] scatta solo con una voce chiaramente più forte dell'eco residua;
 *  - se l'interruzione era falsa (poi l'utente non dice nulla) la domanda viene riletta
 *    e per questa sessione la guida non si fa più interrompere.
 */
class VoiceGuide(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private val _state = MutableStateFlow(VoiceState(available = SpeechRecognizer.isRecognitionAvailable(context)))
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private var ttsReady = false
    private var pendingSpeech: String? = null
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts.language = Locale.ITALIAN
            tts.setSpeechRate(1.05f)
            // stesso canale di una chiamata: il filtro anti-eco ha il riferimento giusto
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            pendingSpeech?.let { t -> main.post { speak(t, listenAfter) } }
            pendingSpeech = null
        }
    }
    private var recognizer: SpeechRecognizer? = null
    private val bargeIn = BargeInDetector { main.post { onUserStartedTalking() } }

    private var onFinalText: ((String) -> Unit)? = null
    private var lastQuestion: String? = null

    /** Diventa false dopo un'interruzione falsa: da lì la guida finisce sempre di parlare. */
    private var bargeInEnabled = true
    /** true se l'ascolto in corso è partito da un'interruzione. */
    private var listeningAfterBargeIn = false
    private var commMode = false
    /** false = legge la domanda e basta (modalità "scrivo io"): niente microfono dopo. */
    private var listenAfter = true
    /** true con l'app in secondo piano: niente voce e niente ascolto finché non si rientra. */
    @Volatile private var paused = false

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                _state.update { it.copy(speaking = true) }
                // il rilevatore parte quando la guida parla davvero: così tara l'eco reale
                if (bargeInEnabled && hasMic() && _state.value.available) bargeIn.start()
            }
            override fun onDone(id: String?) {
                main.post {
                    bargeIn.stop()
                    _state.update { it.copy(speaking = false) }
                    if (!paused && listenAfter) startListening() // finita la domanda, ascolto
                    if (!listenAfter) leaveSpeakerphone()
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                main.post { bargeIn.stop(); _state.update { it.copy(speaking = false) } }
            }
            override fun onStop(id: String?, interrupted: Boolean) {
                bargeIn.stop()
                _state.update { it.copy(speaking = false) }
            }
        })
    }

    private fun hasMic() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    /** Imposta chi riceve il testo finale riconosciuto. */
    fun setOnResult(block: (String) -> Unit) { onFinalText = block }

    /** Legge la domanda; nel frattempo resta in ascolto per l'interruzione. */
    fun speakAndListen(text: String) = speak(text, listen = true)

    /** Legge la domanda senza poi aprire il microfono (per chi risponde scrivendo). */
    fun speakOnly(text: String) = speak(text, listen = false)

    private fun speak(text: String, listen: Boolean) {
        if (paused) return
        stopListening()
        listenAfter = listen
        lastQuestion = text
        if (!ttsReady) { pendingSpeech = text; return }
        // senza microfono non serve la modalità chiamata (anti-eco): audio normale
        if (listen) enterSpeakerphone()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "q-${System.nanoTime()}")
    }

    private fun onUserStartedTalking() {
        if (!_state.value.speaking) return
        tts.stop()
        _state.update { it.copy(speaking = false) }
        listeningAfterBargeIn = true
        startListening()
    }

    fun startListening() {
        if (paused || !hasMic() || !_state.value.available) return
        bargeIn.stop()
        if (tts.isSpeaking) tts.stop()
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            })
        }
        _state.update { it.copy(listening = true, partial = "") }
    }

    fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        _state.update { it.copy(listening = false, level = 0f) }
    }

    /** Ferma tutto (voce e ascolto), es. quando l'utente passa a scrivere o esce. */
    fun silence() {
        bargeIn.stop()
        tts.stop()
        stopListening()
        leaveSpeakerphone()
        _state.update { it.copy(speaking = false) }
    }

    /** App in secondo piano: silenzio totale e audio del telefono rimesso a posto. */
    fun pause() {
        paused = true
        pendingSpeech = null
        main.removeCallbacksAndMessages(null)
        silence()
    }

    /** Rientro nell'app: si può di nuovo parlare, ma la guida non riparte da sola. */
    fun resume() { paused = false }

    fun release() {
        silence()
        tts.shutdown()
    }

    /**
     * Modalità "chiamata in vivavoce": serve al filtro anti-eco.
     * Se ci sono cuffie o auricolari il sistema li usa da solo e non forziamo l'altoparlante.
     */
    @Suppress("DEPRECATION")
    private fun enterSpeakerphone() {
        if (commMode) return
        commMode = true
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        if (Build.VERSION.SDK_INT >= 31) {
            val current = audio.communicationDevice
            if (current == null || current.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
                audio.availableCommunicationDevices
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    ?.let { audio.setCommunicationDevice(it) }
            }
        } else if (!audio.isWiredHeadsetOn && !audio.isBluetoothScoOn) {
            audio.isSpeakerphoneOn = true
        }
    }

    @Suppress("DEPRECATION")
    private fun leaveSpeakerphone() {
        if (!commMode) return
        commMode = false
        if (Build.VERSION.SDK_INT >= 31) audio.clearCommunicationDevice() else audio.isSpeakerphoneOn = false
        audio.mode = AudioManager.MODE_NORMAL
    }

    /** Interruzione falsa: rileggo la domanda intera e non mi faccio più interrompere. */
    private fun onFalseBargeIn() {
        bargeInEnabled = false
        lastQuestion?.let { q -> main.post { speakAndListen(q) } }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) =
            _state.update { it.copy(level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)) }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() = _state.update { it.copy(level = 0f) }
        override fun onError(error: Int) {
            _state.update { it.copy(listening = false, level = 0f) }
            val nothingSaid = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            if (listeningAfterBargeIn && nothingSaid) onFalseBargeIn()
            listeningAfterBargeIn = false
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!t.isNullOrBlank()) _state.update { it.copy(partial = t) }
        }
        override fun onResults(results: Bundle?) {
            val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            _state.update { it.copy(listening = false, partial = "", level = 0f) }
            if (listeningAfterBargeIn && t.isBlank()) onFalseBargeIn()
            listeningAfterBargeIn = false
            if (t.isNotBlank()) onFinalText?.invoke(t)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
