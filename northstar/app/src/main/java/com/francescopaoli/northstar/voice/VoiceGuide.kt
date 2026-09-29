package com.francescopaoli.northstar.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
 */
class VoiceGuide(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(VoiceState(available = SpeechRecognizer.isRecognitionAvailable(context)))
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private var ttsReady = false
    private var pendingSpeech: String? = null
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts.language = Locale.ITALIAN
            tts.setSpeechRate(1.05f)
            pendingSpeech?.let { speakAndListen(it) }
            pendingSpeech = null
        }
    }
    private var recognizer: SpeechRecognizer? = null
    private val bargeIn = BargeInDetector { main.post { onUserStartedTalking() } }

    private var onFinalText: ((String) -> Unit)? = null

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = _state.update { it.copy(speaking = true) }
            override fun onDone(id: String?) {
                main.post {
                    bargeIn.stop()
                    _state.update { it.copy(speaking = false) }
                    startListening() // finita la domanda, ascolto
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                main.post { bargeIn.stop(); _state.update { it.copy(speaking = false) } }
            }
            override fun onStop(id: String?, interrupted: Boolean) = _state.update { it.copy(speaking = false) }
        })
    }

    private fun hasMic() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    /** Imposta chi riceve il testo finale riconosciuto. */
    fun setOnResult(block: (String) -> Unit) { onFinalText = block }

    /** Legge la domanda; nel frattempo resta in ascolto per l'interruzione. */
    fun speakAndListen(text: String) {
        stopListening()
        if (!ttsReady) { pendingSpeech = text; return }
        if (hasMic() && _state.value.available) bargeIn.start()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "q-${System.nanoTime()}")
    }

    private fun onUserStartedTalking() {
        if (!_state.value.speaking) return
        tts.stop()
        _state.update { it.copy(speaking = false) }
        startListening()
    }

    fun startListening() {
        if (!hasMic() || !_state.value.available) return
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

    /** Ferma tutto (voce e ascolto), es. quando l'utente passa a scrivere. */
    fun silence() {
        bargeIn.stop()
        tts.stop()
        stopListening()
        _state.update { it.copy(speaking = false) }
    }

    fun release() {
        silence()
        tts.shutdown()
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) =
            _state.update { it.copy(level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)) }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() = _state.update { it.copy(level = 0f) }
        override fun onError(error: Int) = _state.update { it.copy(listening = false, level = 0f) }
        override fun onPartialResults(partialResults: Bundle?) {
            val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!t.isNullOrBlank()) _state.update { it.copy(partial = t) }
        }
        override fun onResults(results: Bundle?) {
            val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            _state.update { it.copy(listening = false, partial = "", level = 0f) }
            if (t.isNotBlank()) onFinalText?.invoke(t)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
