package com.francescopaoli.northstar.ads

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.francescopaoli.northstar.BuildConfig
import com.francescopaoli.northstar.R
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Card sponsorizzata nello stile Notte Neon.
 * Se l'annuncio non arriva (niente rete, niente riempimento) non occupa spazio.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var ad by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        var disposed = false
        AdLoader.Builder(ctx, BuildConfig.ADMOB_NATIVE_ID)
            .forNativeAd { loaded -> if (disposed) loaded.destroy() else ad = loaded }
            .withNativeAdOptions(
                NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build(),
            )
            .build()
            .loadAd(AdRequest.Builder().build())
        onDispose { disposed = true; ad?.destroy() }
    }

    val current = ad ?: return
    AndroidView(
        modifier = modifier,
        factory = { LayoutInflater.from(it).inflate(R.layout.ad_native_card, null) as NativeAdView },
        update = { view -> bind(view, current) },
    )
}

/** Collega i dati dell'annuncio alle view (serve ad AdMob per contare click e impression). */
private fun bind(view: NativeAdView, ad: NativeAd) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val cta = view.findViewById<Button>(R.id.ad_cta)

    headline.text = ad.headline
    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
    ad.icon?.drawable?.let { icon.setImageDrawable(it); icon.visibility = android.view.View.VISIBLE }
        ?: run { icon.visibility = android.view.View.GONE }
    cta.text = ad.callToAction ?: "Scopri"

    view.headlineView = headline
    view.bodyView = body
    view.iconView = icon
    view.callToActionView = cta
    view.setNativeAd(ad)
}
