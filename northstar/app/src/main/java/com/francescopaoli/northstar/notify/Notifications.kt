package com.francescopaoli.northstar.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.francescopaoli.northstar.MainActivity
import com.francescopaoli.northstar.R

object Notifications {
    const val CHANNEL = "checkin"
    const val EXTRA_ROUTE = "route"

    fun createChannel(context: Context) {
        val ch = NotificationChannel(CHANNEL, context.getString(R.string.channel_checkin), NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = context.getString(R.string.channel_checkin_desc)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    /** Notifica che, toccata, apre direttamente la schermata indicata da [route]. */
    fun show(context: Context, id: Int, title: String, text: String, route: String, checkinGoalId: String? = null) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_star)
            .setColor(0xFF7C3AED.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
        // check-in: si risponde direttamente dalla notifica
        if (checkinGoalId != null) {
            val yes = Intent(context, CheckinActionReceiver::class.java)
                .putExtra(CheckinActionReceiver.EXTRA_GOAL, checkinGoalId)
                .putExtra(CheckinActionReceiver.EXTRA_NOTIF, id)
            val yesPi = PendingIntent.getBroadcast(context, id, yes, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            b.addAction(0, "Sì, tutto ok", yesPi)
            b.addAction(0, "Qualcosa è cambiato", pi)
        }
        NotificationManagerCompat.from(context).notify(id, b.build())
    }
}
