package com.francescopaoli.northstar.calendar

import android.content.Context
import android.content.Intent
import com.francescopaoli.northstar.data.Goal
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Google Calendar via REST: crea un evento "giornata intera" alla scadenza dell'obiettivo.
 * Il permesso viene chiesto con l'Authorization API di Google (nessuna libreria pesante).
 */
class CalendarSync(private val context: Context) {

    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/calendar.events")))
        .build()

    private val client get() = Identity.getAuthorizationClient(context)

    /** Primo passo: se serve il consenso, result.pendingIntent va lanciato dalla UI. */
    suspend fun authorize(): AuthorizationResult = client.authorize(request).await()

    fun resultFromIntent(data: Intent?): AuthorizationResult =
        client.getAuthorizationResultFromIntent(data)

    /** Token senza mostrare nulla: null se l'utente non ha ancora dato il consenso. */
    suspend fun tokenSilently(): String? = runCatching {
        val r = authorize()
        if (r.hasResolution()) null else r.accessToken
    }.getOrNull()

    /** Crea o aggiorna l'evento. Ritorna l'id dell'evento, o null se fallisce. */
    suspend fun upsertEvent(token: String, goal: Goal): String? = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("summary", "Northstar · ${goal.title}")
            .put("description", goal.summary)
            .put("start", JSONObject().put("date", goal.deadline.toString()))
            .put("end", JSONObject().put("date", goal.deadline.plusDays(1).toString()))
        val base = "https://www.googleapis.com/calendar/v3/calendars/primary/events"
        val (url, method) = goal.calendarEventId
            ?.let { "$base/$it" to "PUT" } ?: (base to "POST")
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            if (conn.responseCode !in 200..299) return@runCatching null
            JSONObject(conn.inputStream.bufferedReader().readText()).optString("id").ifBlank { null }
        }.getOrNull()
    }
}
