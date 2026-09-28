package com.stretchify.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.stretchify.MainActivity
import com.stretchify.R
import com.stretchify.data.StretchifyRepository
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderReceiver : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        val repository = StretchifyRepository(context)
        schedule(context, repository)
        if (intent.action != ACTION_REMIND || !repository.loadRemindersEnabled())
        {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED)
        {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Stretch reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time for a stretch")
            .setContentText("A short routine can help you reset.")
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object
    {
        private const val ACTION_REMIND = "com.stretchify.action.REMIND"
        private const val CHANNEL_ID = "stretch_reminders"
        private const val NOTIFICATION_ID = 2001

        fun schedule(context: Context, repository: StretchifyRepository = StretchifyRepository(context))
        {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val reminderIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(reminderIntent)
            if (!repository.loadRemindersEnabled())
            {
                return
            }

            val now = LocalDateTime.now()
            var nextReminder = now.toLocalDate().atTime(repository.loadReminderHour(), 0)
            if (!nextReminder.isAfter(now))
            {
                nextReminder = nextReminder.plusDays(1)
            }
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextReminder.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                reminderIntent
            )
        }
    }
}
