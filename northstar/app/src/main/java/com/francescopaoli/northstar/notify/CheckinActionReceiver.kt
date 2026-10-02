package com.francescopaoli.northstar.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.francescopaoli.northstar.NorthstarApp
import com.francescopaoli.northstar.domain.Checkins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** "Sì, tutto ok" toccato nella notifica del check-in: risposta registrata senza aprire l'app. */
class CheckinActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val goalId = intent.getStringExtra(EXTRA_GOAL) ?: return
        NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NOTIF, 0))
        val pending = goAsync()
        val c = (context.applicationContext as NorthstarApp).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = c.backgroundRepository() ?: return@launch
                repo.getGoals().firstOrNull { it.id == goalId }?.let { repo.upsert(Checkins.confirmed(it)) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_GOAL = "goal"
        const val EXTRA_NOTIF = "notif"
    }
}
