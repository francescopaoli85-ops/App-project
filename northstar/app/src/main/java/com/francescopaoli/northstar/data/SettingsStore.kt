package com.francescopaoli.northstar.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    /** null = non ancora dedotta dal profilo. */
    val calendarMode: CalendarMode? = null,
    /** true se l'utente l'ha cambiata a mano: il profilo non la sovrascrive più. */
    val calendarModeManual: Boolean = false,
    val calendarConnected: Boolean = false,
    val calendarPromptSeen: Boolean = false,
    val voiceGuide: Boolean = true,
    val checkins: Boolean = true,
    /** Nome per la modalità locale (senza login). */
    val localName: String? = null,
    /** Ha acquistato "Rimuovi pubblicità". */
    val adFree: Boolean = false,
    /** Meno effetti grafici: niente shader, parallasse e scintille (risparmia batteria). */
    val reducedEffects: Boolean = false,
    /** Tema scelto (id di Palettes). */
    val theme: String = "neon",
    /** Giorno (epochDay) fino a cui il banner Calendar resta nascosto. */
    val calendarBannerHiddenUntil: Long = 0,
    /** Temi per cui l'utente ha scelto lo sfondo statico. */
    val staticThemes: Set<String> = emptySet(),
    /** Musica di sottofondo e del percorso. */
    val musicOn: Boolean = true,
    /** Canzone di sottofondo: "energy" o "calm". */
    val ambientSong: String = "energy",
    /** Volume musica 0..100 (basso di default: è un sottofondo). */
    val musicVolume: Int = 45,
    val sfxOn: Boolean = true,
    /** true = sfondi animati anche col risparmio energetico attivo. */
    val animateOnPowerSave: Boolean = false,
)

/** Preferenze salvate sul dispositivo (DataStore). */
class SettingsStore(private val context: Context) {

    private object K {
        val mode = stringPreferencesKey("calendar_mode")
        val modeManual = booleanPreferencesKey("calendar_mode_manual")
        val connected = booleanPreferencesKey("calendar_connected")
        val promptSeen = booleanPreferencesKey("calendar_prompt_seen")
        val voice = booleanPreferencesKey("voice_guide")
        val checkins = booleanPreferencesKey("checkins")
        val localName = stringPreferencesKey("local_name")
        val adFree = booleanPreferencesKey("ad_free")
        val reducedFx = booleanPreferencesKey("reduced_fx")
        val theme = stringPreferencesKey("theme")
        val bannerHidden = longPreferencesKey("cal_banner_hidden_until")
        val staticThemes = androidx.datastore.preferences.core.stringSetPreferencesKey("static_themes")
        val musicOn = booleanPreferencesKey("music_on")
        val ambientSong = stringPreferencesKey("ambient_song")
        val musicVolume = androidx.datastore.preferences.core.intPreferencesKey("music_volume")
        val sfxOn = booleanPreferencesKey("sfx_on")
        val animPowerSave = booleanPreferencesKey("animate_on_power_save")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p -> p.toSettings() }

    suspend fun current(): Settings = settings.first()

    private fun Preferences.toSettings() = Settings(
        calendarMode = this[K.mode]?.let { runCatching { CalendarMode.valueOf(it) }.getOrNull() },
        calendarModeManual = this[K.modeManual] ?: false,
        calendarConnected = this[K.connected] ?: false,
        calendarPromptSeen = this[K.promptSeen] ?: false,
        voiceGuide = this[K.voice] ?: true,
        checkins = this[K.checkins] ?: true,
        localName = this[K.localName],
        adFree = this[K.adFree] ?: false,
        reducedEffects = this[K.reducedFx] ?: false,
        theme = this[K.theme] ?: "neon",
        calendarBannerHiddenUntil = this[K.bannerHidden] ?: 0,
        staticThemes = this[K.staticThemes] ?: emptySet(),
        musicOn = this[K.musicOn] ?: true,
        ambientSong = this[K.ambientSong] ?: "energy",
        musicVolume = this[K.musicVolume] ?: 45,
        sfxOn = this[K.sfxOn] ?: true,
        animateOnPowerSave = this[K.animPowerSave] ?: false,
    )

    /** Impostata dal profilo nascosto: non tocca una scelta fatta a mano. */
    suspend fun setProfiledMode(mode: CalendarMode) = context.dataStore.edit {
        if (it[K.modeManual] != true) it[K.mode] = mode.name
    }

    suspend fun setManualMode(mode: CalendarMode) = context.dataStore.edit {
        it[K.mode] = mode.name
        it[K.modeManual] = true
    }

    suspend fun setCalendarConnected(v: Boolean) = context.dataStore.edit { it[K.connected] = v }
    suspend fun setCalendarPromptSeen() = context.dataStore.edit { it[K.promptSeen] = true }
    suspend fun setVoiceGuide(v: Boolean) = context.dataStore.edit { it[K.voice] = v }
    suspend fun setCheckins(v: Boolean) = context.dataStore.edit { it[K.checkins] = v }
    /** Ultimo periodo (settimana o giorno) in cui è partita la notifica [tag]. */
    suspend fun lastSent(tag: String): Long? = context.dataStore.data.first()[longPreferencesKey("sent_$tag")]
    suspend fun markSent(tag: String, period: Long) = context.dataStore.edit { it[longPreferencesKey("sent_$tag")] = period }

    suspend fun setThemeAnimated(id: String, animated: Boolean) = context.dataStore.edit {
        val cur = it[K.staticThemes] ?: emptySet()
        it[K.staticThemes] = if (animated) cur - id else cur + id
    }
    suspend fun setTheme(id: String) = context.dataStore.edit { it[K.theme] = id }
    suspend fun hideCalendarBanner(untilEpochDay: Long) = context.dataStore.edit { it[K.bannerHidden] = untilEpochDay }
    suspend fun setReducedEffects(v: Boolean) = context.dataStore.edit { it[K.reducedFx] = v }
    suspend fun setMusicOn(v: Boolean) = context.dataStore.edit { it[K.musicOn] = v }
    suspend fun setAmbientSong(id: String) = context.dataStore.edit { it[K.ambientSong] = id }
    suspend fun setMusicVolume(v: Int) = context.dataStore.edit { it[K.musicVolume] = v.coerceIn(0, 100) }
    suspend fun setAnimateOnPowerSave(v: Boolean) = context.dataStore.edit { it[K.animPowerSave] = v }
    suspend fun setSfxOn(v: Boolean) = context.dataStore.edit { it[K.sfxOn] = v }
    suspend fun setAdFree(v: Boolean) = context.dataStore.edit { it[K.adFree] = v }
    suspend fun setLocalName(v: String?) = context.dataStore.edit {
        if (v == null) it.remove(K.localName) else it[K.localName] = v
    }
}
