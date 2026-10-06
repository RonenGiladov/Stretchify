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
import com.stretchify.data.StretchifyRepository
import com.stretchify.model.RoutineStepGoal
import com.stretchify.model.RoutineType
import com.stretchify.model.RepetitionDetectorType
import com.stretchify.motion.AndroidMotionSensorSource
import com.stretchify.motion.RepetitionDetectionEvent
import com.stretchify.motion.RepetitionDetector
import com.stretchify.motion.RepetitionDetectorFactory
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
    private lateinit var repository: StretchifyRepository
    private lateinit var motionSensorSource: AndroidMotionSensorSource
    private var repetitionDetector: RepetitionDetector? = null
    private var isMotionTracking = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var completionStopJob: Job? = null
    private var isGracefulStop = false

    override fun onCreate()
    {
        super.onCreate()
        sessionController = (application as StretchifyApplication).sessionController
        repository = StretchifyRepository(this)
        motionSensorSource = AndroidMotionSensorSource(this)
        createNotificationChannel()
        startInForeground(sessionController.state.value)
        serviceScope.launch {
            sessionController.state.collectLatest { sessionState ->
                syncMotionTracking(sessionState)
                updateWakeLock(shouldHoldWakeLock(sessionState))
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
            ACTION_UNDO_REP -> sessionController.adjustRepetitionCount(-1)
            ACTION_FINISH -> if (sessionController.state.value.repetitionCount > 0)
            {
                sessionController.finishRepetitionSession()
            }
            else
            {
                sessionController.stop()
                stopSelf()
            }
            ACTION_STOP ->
            {
                sessionController.stop()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?)
    {
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy()
    {
        completionStopJob?.cancel()
        stopMotionTracking()
        releaseWakeLock()
        serviceScope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
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
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
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
            SessionPhase.Stretching -> if (sessionState.routine.routineType == RoutineType.Workout)
            {
                getString(R.string.notification_exercising)
            }
            else
            {
                getString(R.string.notification_stretching)
            }
            SessionPhase.TrackingRepetitions -> when (sessionState.repetitionTrackingStatus)
            {
                RepetitionTrackingStatus.Calibrating -> getString(R.string.notification_calibrating)
                RepetitionTrackingStatus.CheckingPosition,
                RepetitionTrackingStatus.Preparing -> getString(R.string.notification_checking_position)
                RepetitionTrackingStatus.Manual -> getString(R.string.notification_manual_counting)
                RepetitionTrackingStatus.CalibrationFailed -> getString(R.string.notification_calibration_failed)
                RepetitionTrackingStatus.SensorUnavailable -> getString(R.string.notification_sensor_unavailable)
                else -> getString(R.string.notification_counting_reps)
            }
            SessionPhase.Resting -> getString(R.string.notification_resting)
            SessionPhase.Paused -> getString(R.string.notification_paused)
            SessionPhase.Completed -> getString(R.string.notification_completed)
            else -> getString(R.string.notification_preparing)
        }
        val progressText = if (sessionState.currentStep.goal is RoutineStepGoal.SensorRepetitions)
        {
            resources.getQuantityString(R.plurals.repetition_count, sessionState.repetitionCount,
                sessionState.repetitionCount)
        }
        else
        {
            String.format("%d:%02d", sessionState.remainingSeconds / 60, sessionState.remainingSeconds % 60)
        }
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(sessionState.routine.title)
            .setContentText("$phaseText • $progressText")
            .setContentIntent(openAppIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(sessionState.phase != SessionPhase.Completed)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (sessionState.phase != SessionPhase.Countdown && sessionState.phase != SessionPhase.Completed)
        {
            builder.addAction(pauseOrResumeAction)
        }
        if (sessionState.currentStep.goal is RoutineStepGoal.SensorRepetitions &&
            sessionState.phase != SessionPhase.Completed)
        {
            if (sessionState.repetitionCount > 0)
            {
                builder.addAction(NotificationCompat.Action(0, getString(R.string.undo_rep),
                    servicePendingIntent(ACTION_UNDO_REP, 4)))
            }
            return builder
                .addAction(NotificationCompat.Action(0, getString(R.string.finish),
                    servicePendingIntent(ACTION_FINISH, 5)))
                .build()
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

    private fun shouldHoldWakeLock(sessionState: SessionState): Boolean
    {
        if (!sessionState.phase.isActive)
        {
            return false
        }
        if (sessionState.currentStep.goal !is RoutineStepGoal.SensorRepetitions)
        {
            return true
        }
        return !sessionState.isManualCounting && when (sessionState.repetitionTrackingStatus)
        {
            RepetitionTrackingStatus.Preparing,
            RepetitionTrackingStatus.Calibrating,
            RepetitionTrackingStatus.CheckingPosition,
            RepetitionTrackingStatus.Counting -> true
            else -> false
        }
    }

    private fun syncMotionTracking(sessionState: SessionState)
    {
        val repetitionGoal = sessionState.currentStep.goal as? RoutineStepGoal.SensorRepetitions
        val shouldTrack = sessionState.phase == SessionPhase.TrackingRepetitions &&
            !sessionState.isManualCounting && repetitionGoal != null
        if (!shouldTrack)
        {
            stopMotionTracking()
            return
        }
        if (isMotionTracking && sessionState.repetitionTrackingStatus == RepetitionTrackingStatus.Preparing)
        {
            stopMotionTracking()
        }
        if (isMotionTracking || sessionState.repetitionTrackingStatus != RepetitionTrackingStatus.Preparing)
        {
            return
        }
        if (!motionSensorSource.isAvailable)
        {
            sessionController.updateRepetitionTracking(
                RepetitionTrackingStatus.SensorUnavailable,
                sensorError = getString(R.string.motion_sensor_unavailable)
            )
            return
        }
        val profile = repository.loadPullUpCalibrationProfile()
        repetitionDetector = RepetitionDetectorFactory.create(repetitionGoal.detector, profile)
        sessionController.updateRepetitionTracking(
            if (profile == null) RepetitionTrackingStatus.Calibrating
            else RepetitionTrackingStatus.CheckingPosition
        )
        isMotionTracking = motionSensorSource.start { sample ->
            val events = repetitionDetector?.process(sample).orEmpty()
            if (events.isNotEmpty())
            {
                serviceScope.launch {
                    events.forEach(::handleDetectionEvent)
                }
            }
        }
        if (!isMotionTracking)
        {
            motionSensorSource.stop()
            sessionController.updateRepetitionTracking(
                RepetitionTrackingStatus.SensorUnavailable,
                sensorError = getString(R.string.motion_sensor_start_failed)
            )
        }
    }

    private fun handleDetectionEvent(event: RepetitionDetectionEvent)
    {
        when (event)
        {
            RepetitionDetectionEvent.Ready -> sessionController.updateRepetitionTracking(
                RepetitionTrackingStatus.Counting
            )
            is RepetitionDetectionEvent.CalibrationProgress -> sessionController.updateRepetitionTracking(
                RepetitionTrackingStatus.Calibrating,
                calibrationProgress = event.completedRepetitions
            )
            is RepetitionDetectionEvent.CalibrationComplete ->
            {
                repository.savePullUpCalibrationProfile(event.profile)
                repetitionDetector = RepetitionDetectorFactory.create(
                    RepetitionDetectorType.PullUp,
                    event.profile
                )
                sessionController.updateRepetitionTracking(RepetitionTrackingStatus.CheckingPosition)
            }
            is RepetitionDetectionEvent.CalibrationFailed ->
            {
                stopMotionTracking()
                sessionController.updateRepetitionTracking(
                    RepetitionTrackingStatus.CalibrationFailed,
                    calibrationProgress = 0,
                    sensorError = event.reason
                )
            }
            RepetitionDetectionEvent.Repetition -> sessionController.recordAutomaticRepetition()
        }
    }

    private fun stopMotionTracking()
    {
        if (!isMotionTracking)
        {
            return
        }
        motionSensorSource.stop()
        repetitionDetector = null
        isMotionTracking = false
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
        const val ACTION_UNDO_REP = "com.stretchify.session.UNDO_REP"
        const val ACTION_FINISH = "com.stretchify.session.FINISH"
        const val ACTION_STOP = "com.stretchify.session.STOP"

        private const val NOTIFICATION_CHANNEL_ID = "active_session"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN_APP = 100
        private const val WAKE_LOCK_TAG = "Stretchify:ActiveSession"
        private const val COMPLETION_SERVICE_DELAY_MILLIS = 1500L
    }
}
