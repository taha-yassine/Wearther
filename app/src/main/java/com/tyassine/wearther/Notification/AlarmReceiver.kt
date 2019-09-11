package com.tyassine.wearther.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.tyassine.wearther.CHANNEL_ID
import com.tyassine.wearther.DAILY_REMINDER_REQUEST_CODE
import com.tyassine.wearther.MainActivity
import com.tyassine.wearther.R


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = TaskStackBuilder.create(context!!).run {
            addNextIntentWithParentStack(notificationIntent)
            getPendingIntent(DAILY_REMINDER_REQUEST_CODE, PendingIntent.FLAG_UPDATE_CURRENT)
        }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setContentTitle("Wearther")
                setContentText("Don't forget to check the weather !")
                priority = NotificationCompat.PRIORITY_DEFAULT
                setAutoCancel(true)
                setSmallIcon(R.drawable.notification_icon)
                setContentIntent(pendingIntent)
            }

            with(NotificationManagerCompat.from(context)) {
                notify(DAILY_REMINDER_REQUEST_CODE, builder.build())
            }
        }
}