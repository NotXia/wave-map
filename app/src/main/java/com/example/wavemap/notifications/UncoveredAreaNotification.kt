package com.example.wavemap.notifications

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.wavemap.R
import com.example.wavemap.ui.main.MainActivity

class UncoveredAreaNotification {

    companion object {
        private var last_uncovered_area_notification_time : Long = 0
        private const val NOTIFICATION_DELAY_MS = 30000

        fun build(context: Context) : Notification {
            val channel_id = "ch_uncovered_area"
            val channel_name: CharSequence = context.getString(R.string.notification_uncovered_area)
            val channel_desc = context.getString(R.string.notification_uncovered_area_desc)
            val channel = NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = channel_desc
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            val notification_builder = NotificationCompat.Builder(context, channel_id)

            notification_builder.setContentTitle(context.getString(R.string.notification_uncovered_area))
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_uncovered_area_text)))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(TaskStackBuilder.create(context).run {
                    addNextIntentWithParentStack(Intent(context, MainActivity::class.java))
                    getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                })

            return notification_builder.build()
        }

        fun send(context: Context, id: Int=1) {
            if ( ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("Missing POST_NOTIFICATIONS permission")
            }

            if (System.currentTimeMillis() - last_uncovered_area_notification_time < NOTIFICATION_DELAY_MS) {
                return // To avoid too pedantic notifications
            } else {
                NotificationManagerCompat.from(context).notify(id, build(context))
                last_uncovered_area_notification_time = System.currentTimeMillis()
            }
        }

    }

}