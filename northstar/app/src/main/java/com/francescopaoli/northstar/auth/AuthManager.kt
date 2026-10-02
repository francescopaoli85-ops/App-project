package com.francescopaoli.northstar.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.francescopaoli.northstar.data.SettingsStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Chi sta usando l'app. */
sealed interface Session {
    val name: String

    /** Account Firebase: dati su cloud, sincronizzati tra dispositivi. */
    data class Cloud(val uid: String, override val name: String, val email: String?) : Session

    /** Modalità locale (Firebase non configurato): dati solo su questo telefono. */
    data class Local(override val name: String) : Session
}

class AuthManager(private val context: Context, private val settings: SettingsStore) {

    /** true se c'è google-services.json e Firebase si è inizializzato. */
    val firebaseEnabled: Boolean = FirebaseApp.getApps(context).isNotEmpty()

    private val auth: FirebaseAuth? = if (firebaseEnabled) FirebaseAuth.getInstance() else null

    /** Emette null quando nessuno è loggato. */
    val session: Flow<Session?> =
        if (auth != null) callbackFlow {
            val l = FirebaseAuth.AuthStateListener { fa -> trySend(fa.currentUser?.toSession()) }
            auth.addAuthStateListener(l)
            awaitClose { auth.removeAuthStateListener(l) }
        } else settings.settings.map { s -> s.localName?.let { Session.Local(it) } }

    /** Sessione attuale senza aspettare (usata dal worker in background). */
    suspend fun currentSession(): Session? =
        if (auth != null) auth.currentUser?.toSession()
        else settings.current().localName?.let { Session.Local(it) }

    private fun com.google.firebase.auth.FirebaseUser.toSession() = Session.Cloud(
        uid = uid,
        name = displayName?.substringBefore(' ')?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@') ?: "Ciao",
        email = email,
    )

    /** Client ID OAuth "web" generato dal plugin google-services. */
    private fun webClientId(): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id != 0) context.getString(id) else null
    }

    /** Accesso rapido con Google tramite Credential Manager (un tocco). */
    suspend fun signInWithGoogle(activity: Activity): Result<Unit> = runCatching {
        val fa = auth ?: error("Firebase non configurato")
        val clientId = webClientId() ?: error("default_web_client_id mancante")
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val cred = CredentialManager.create(activity).getCredential(activity, request).credential
        check(cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Credenziale non supportata"
        }
        val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
        fa.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    /** Email e password: se l'account non esiste lo crea al volo (meno click). */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        val fa = auth ?: error("Firebase non configurato")
        try {
            fa.signInWithEmailAndPassword(email.trim(), password).await()
        } catch (e: FirebaseAuthInvalidUserException) {
            fa.createUserWithEmailAndPassword(email.trim(), password).await()
        }
    }

    suspend fun enterLocal(name: String) = settings.setLocalName(name.trim().ifBlank { "amico" })

    suspend fun signOut() {
        auth?.signOut()
        settings.setLocalName(null)
    }
}
