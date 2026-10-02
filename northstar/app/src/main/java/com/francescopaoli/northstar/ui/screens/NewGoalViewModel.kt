package com.francescopaoli.northstar.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.francescopaoli.northstar.data.Area
import com.francescopaoli.northstar.data.Criterion
import com.francescopaoli.northstar.voice.VoiceGuide
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Stato del percorso guidato: sopravvive alla rotazione dello schermo. */
class NewGoalViewModel(app: Application) : AndroidViewModel(app) {

    val voice = VoiceGuide(app)

    /** 0..5 = i 6 criteri, 6 = anteprima della frase finale. */
    var step by mutableIntStateOf(0)
        private set
    var area by mutableStateOf(Area.PERSONALE)
    val answers = mutableStateMapOf<Criterion, String>()
    var deadline by mutableStateOf(LocalDate.now().plusMonths(1))
    var typing by mutableStateOf(false)
    var voiceGuideOn = true

    /** Secondi al passaggio automatico alla domanda dopo (0 = nessun conto alla rovescia). */
    var autoNextIn by mutableIntStateOf(0)
        private set
    private var autoJob: kotlinx.coroutines.Job? = null

    /** Segnale nascosto per la profilazione: quante volte ha rifatto una risposta. */
    var revisions = 0
        private set

    val criterion: Criterion? get() = Criterion.entries.getOrNull(step)
    val isSummary get() = step == Criterion.entries.size

    init {
        voice.setOnResult { text ->
            val c = criterion ?: return@setOnResult
            val old = answers[c].orEmpty()
            answers[c] = if (old.isBlank()) text.replaceFirstChar { it.uppercase() } else "$old $text"
            // risposta a voce arrivata: avanti da solo tra 3 secondi (annullabile)
            startAutoNext()
        }
    }

    fun canContinue(): Boolean = when (val c = criterion) {
        null -> true
        Criterion.CONTESTUALIZZATO -> true // la data c'è sempre; il contesto è facoltativo
        else -> answers[c].orEmpty().isNotBlank()
    }

    /** All'ingresso di ogni domanda la voce guida la legge (e resta pronta a farsi interrompere). */
    fun enterStep() {
        val c = criterion ?: return voice.silence()
        if (typing || !voiceGuideOn) return
        voice.speakAndListen("${c.question.replace('\n', ' ')} ${c.hint}")
    }

    fun next() { cancelAutoNext(); voice.silence(); if (step < Criterion.entries.size) step++ }
    fun back() { cancelAutoNext(); voice.silence(); if (step > 0) step-- }

    private fun startAutoNext() {
        if (isSummary || typing || !canContinue()) return
        cancelAutoNext()
        autoJob = viewModelScope.launch {
            for (s in 3 downTo 1) { autoNextIn = s; kotlinx.coroutines.delay(1000) }
            autoNextIn = 0
            autoJob = null
            next()
        }
    }

    fun cancelAutoNext() {
        autoJob?.cancel()
        autoJob = null
        autoNextIn = 0
    }

    fun setAnswer(text: String) {
        val c = criterion ?: return
        cancelAutoNext()
        // svuotare del tutto una risposta già data conta come "ripensamento"
        if (text.isBlank() && answers[c].orEmpty().length > 3) revisions++
        answers[c] = text
    }

    fun micTap() {
        val c = criterion ?: return
        cancelAutoNext()
        val v = voice.state.value
        when {
            // la guida sta parlando: la interrompo e ascolto subito
            v.speaking -> voice.startListening()
            // sta già ascoltando: se hai già detto qualcosa chiudo, altrimenti riparto pulito
            v.listening -> if (v.partial.isNotBlank()) voice.stopListening() else voice.startListening()
            answers[c].orEmpty().isNotBlank() -> { revisions++; answers[c] = ""; voice.startListening() }
            else -> voice.startListening()
        }
    }

    fun toggleTyping() {
        cancelAutoNext()
        typing = !typing
        if (typing) voice.silence() else enterStep()
    }

    override fun onCleared() = voice.release()
}
