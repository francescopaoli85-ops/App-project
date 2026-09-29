package com.francescopaoli.northstar.ads

import android.app.Activity
import android.content.Context
import com.francescopaoli.northstar.data.SettingsStore
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pubblicità: prima il consenso GDPR (UMP di Google, obbligatorio in UE),
 * poi l'avvio di AdMob. Niente annunci per chi ha comprato "Rimuovi pubblicità".
 */
class AdsManager(private val context: Context, settings: SettingsStore) {

    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val started = AtomicBoolean(false)
    private val sdkReady = MutableStateFlow(false)

    /** true quando si possono mostrare annunci in questo momento. */
    val showAds: Flow<Boolean> = combine(sdkReady, settings.settings) { ready, s -> ready && !s.adFree }

    /** Da chiamare a ogni avvio dall'Activity: mostra il modulo consenso solo se serve. */
    fun gatherConsent(activity: Activity) {
        consent.requestConsentInfoUpdate(
            activity, ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { startIfAllowed() } },
            { startIfAllowed() }, // errore di rete: si usa il consenso già salvato, se c'è
        )
        startIfAllowed() // consenso già dato in una sessione precedente: parti subito
    }

    private fun startIfAllowed() {
        if (!consent.canRequestAds() || started.getAndSet(true)) return
        MobileAds.initialize(context) { sdkReady.value = true }
    }

    /** In UE va offerto un modo per rivedere il consenso: lo mostriamo in Impostazioni. */
    val privacyOptionsRequired: Boolean
        get() = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) = UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
}
