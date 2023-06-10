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

class BackgroundScanningNotification {

    companion object {

        fun build(context: Context) : Notification {
            val channel_id = "ch_background_scan"
            val channel_name: CharSequence = context.getString(R.string.notification_background)
            val channel_desc = context.getString(R.string.notification_background_desc)
            val channel = NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = channel_desc
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            val notification_builder = NotificationCompat.Builder(context, channel_id)

            notification_builder.setContentTitle(context.getString(R.string.notification_background))
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_background_text)))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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

            NotificationManagerCompat.from(context).notify(id, build(context))
        }

    }

}