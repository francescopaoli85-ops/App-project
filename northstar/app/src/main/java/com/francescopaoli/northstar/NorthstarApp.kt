package com.francescopaoli.northstar

import android.app.Application
import com.francescopaoli.northstar.notify.CheckinWorker
import com.francescopaoli.northstar.notify.Notifications
import com.google.firebase.FirebaseApp

class NorthstarApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Ritorna null (senza crash) se google-services.json non c'è: si va in modalità locale.
        FirebaseApp.initializeApp(this)
        container = AppContainer(this)
        Notifications.createChannel(this)
        CheckinWorker.schedule(this)
    }
}
