package com.stretchify.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.stretchify.MainActivity
import com.stretchify.R
import com.stretchify.StretchifyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SessionService : Service()
{
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var sessionController: SessionController
    private var wakeLock: PowerManager.WakeLock? = null
    private var completionStopJob: Job? = null
    private var isGracefulStop = false

    override fun onCreate()
    {
        super.onCreate()
        sessionController = (application as StretchifyApplication).sessionController
        createNotificationChannel()
        startInForeground(sessionController.state.value)
        serviceScope.launch {
            sessionController.state.collectLatest { sessionState ->
                updateWakeLock(sessionState.phase.isActive)
                notificationManager().notify(NOTIFICATION_ID, createNotification(sessionState))
                if (sessionState.phase == SessionPhase.Completed)
                {
                    completionStopJob?.cancel()
                    completionStopJob = launch {
                        delay(COMPLETION_SERVICE_DELAY_MILLIS)
                        isGracefulStop = true
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        when (intent?.action)
        {
            ACTION_PAUSE -> sessionController.pause()
            ACTION_RESUME -> sessionController.resume()
            ACTION_STOP ->
            {
                sessionController.stop()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?)
    {
        sessionController.stop()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy()
    {
        completionStopJob?.cancel()
        releaseWakeLock()
        serviceScope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (!isGracefulStop && sessionController.state.value.phase != SessionPhase.NotStarted)
        {
            sessionController.stop()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground(sessionState: SessionState)
    {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(sessionState),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
            else
            {
                0
            }
        )
    }

    private fun createNotification(sessionState: SessionState): Notification
    {
        val openAppIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN_APP,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseOrResumeAction = if (sessionState.phase == SessionPhase.Paused)
        {
            NotificationCompat.Action(0, getString(R.string.resume), servicePendingIntent(ACTION_RESUME, 1))
        }
        else
        {
            NotificationCompat.Action(0, getString(R.string.pause), servicePendingIntent(ACTION_PAUSE, 2))
        }
        val phaseText = when (sessionState.phase)
        {
            SessionPhase.Stretching -> getString(R.string.notification_stretching)
            SessionPhase.Resting -> getString(R.string.notification_resting)
            SessionPhase.Paused -> getString(R.string.notification_paused)
            SessionPhase.Completed -> getString(R.string.notification_completed)
            else -> getString(R.string.notification_preparing)
        }
        val timeText = String.format("%d:%02d", sessionState.remainingSeconds / 60, sessionState.remainingSeconds % 60)
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(sessionState.routine.title)
            .setContentText("$phaseText • $timeText")
            .setContentIntent(openAppIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(sessionState.phase != SessionPhase.Completed)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (sessionState.phase != SessionPhase.Countdown)
        {
            builder.addAction(pauseOrResumeAction)
        }
        return builder
            .addAction(NotificationCompat.Action(0, getString(R.string.stop), servicePendingIntent(ACTION_STOP, 3)))
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent
    {
        return PendingIntent.getService(
            this,
            requestCode,
            Intent(this, SessionService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateWakeLock(shouldHoldWakeLock: Boolean)
    {
        if (shouldHoldWakeLock && wakeLock?.isHeld != true)
        {
            wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                .apply { acquire() }
        }
        else if (!shouldHoldWakeLock)
        {
            releaseWakeLock()
        }
    }

    private fun releaseWakeLock()
    {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun createNotificationChannel()
    {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.session_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = getString(R.string.session_notification_channel_description)
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager
    {
        return getSystemService(NotificationManager::class.java)
    }

    companion object
    {
        const val ACTION_START = "com.stretchify.session.START"
        const val ACTION_PAUSE = "com.stretchify.session.PAUSE"
        const val ACTION_RESUME = "com.stretchify.session.RESUME"
        const val ACTION_STOP = "com.stretchify.session.STOP"

        private const val NOTIFICATION_CHANNEL_ID = "active_session"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN_APP = 100
        private const val WAKE_LOCK_TAG = "Stretchify:ActiveSession"
        private const val COMPLETION_SERVICE_DELAY_MILLIS = 1500L
    }
}
