package com.francescopaoli.northstar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.francescopaoli.northstar.notify.Notifications
import com.francescopaoli.northstar.ui.NorthstarRoot
import com.francescopaoli.northstar.ui.theme.NorthstarTheme

class MainActivity : ComponentActivity() {

    /** Schermata da aprire quando si tocca una notifica. */
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        pendingRoute.value = intent.getStringExtra(Notifications.EXTRA_ROUTE)
        val container = (application as NorthstarApp).container
        container.ads.gatherConsent(this)
        container.billing.connect()
        // i tasti volume regolano la musica
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        setContent {
            NorthstarTheme {
                NorthstarRoot(
                    container = container,
                    deepLink = pendingRoute.value,
                    onDeepLinkHandled = { pendingRoute.value = null },
                )
            }
        }
    }

    /** Suoni solo con l'app davanti: tasto Home, altra app o schermo spento = silenzio. */
    override fun onStart() {
        super.onStart()
        (application as NorthstarApp).container.sound.setForeground(true)
    }

    override fun onStop() {
        // girare il telefono ricrea la schermata ma non è un'uscita: la musica continua
        if (!isChangingConfigurations) (application as NorthstarApp).container.sound.setForeground(false)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Notifications.EXTRA_ROUTE)?.let { pendingRoute.value = it }
    }
}
