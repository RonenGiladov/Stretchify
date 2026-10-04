package com.stretchify.session

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.stretchify.R
import com.stretchify.model.AlertMode

class SessionAlertController(private val context: Context)
{
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var audioFocusRequest: AudioFocusRequest? = null
    private var mediaPlayer: MediaPlayer? = null

    fun play(sessionAlert: SessionAlert, alertMode: AlertMode)
    {
        if (alertMode.isVibrationEnabled)
        {
            vibrate(sessionAlert)
        }
        if (alertMode.isSoundEnabled)
        {
            playSound(sessionAlert)
        }
    }

    fun release()
    {
        releaseAudio()
        try
        {
            vibrator().cancel()
        }
        catch (exception: RuntimeException)
        {
            Log.e(TAG, "Unable to cancel session vibration", exception)
        }
    }

    private fun releaseAudio()
    {
        mediaPlayer?.release()
        mediaPlayer = null
        abandonAudioFocus()
    }

    private fun playSound(sessionAlert: SessionAlert)
    {
        releaseAudio()
        try
        {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
                    {
                        release()
                    }
                }
                .build()
            if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            {
                return
            }
            audioFocusRequest = focusRequest

            val soundResource = when (sessionAlert)
            {
                SessionAlert.CountdownComplete -> R.raw.rest_complete
                SessionAlert.StretchComplete -> R.raw.stretch_complete
                SessionAlert.RestComplete -> R.raw.rest_complete
                SessionAlert.RoutineComplete -> R.raw.routine_complete
            }
            val player = MediaPlayer.create(context, soundResource, audioAttributes, 0)
            if (player == null)
            {
                abandonAudioFocus()
                return
            }
            mediaPlayer = player
            player.setOnCompletionListener { completedPlayer -> finishPlayback(completedPlayer) }
            player.setOnErrorListener { failedPlayer, _, _ ->
                finishPlayback(failedPlayer)
                true
            }
            player.start()
        }
        catch (exception: RuntimeException)
        {
            Log.e(TAG, "Unable to play session alert", exception)
            releaseAudio()
        }
    }

    private fun finishPlayback(completedPlayer: MediaPlayer)
    {
        if (mediaPlayer === completedPlayer)
        {
            completedPlayer.release()
            mediaPlayer = null
            abandonAudioFocus()
        }
    }

    private fun abandonAudioFocus()
    {
        audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
        audioFocusRequest = null
    }

    private fun vibrate(sessionAlert: SessionAlert)
    {
        val timings = when (sessionAlert)
        {
            SessionAlert.CountdownComplete -> longArrayOf(0, 120)
            SessionAlert.StretchComplete -> longArrayOf(0, 120)
            SessionAlert.RestComplete -> longArrayOf(0, 60, 80, 60)
            SessionAlert.RoutineComplete -> longArrayOf(0, 150, 100, 220)
        }
        try
        {
            val vibrator = vibrator()
            if (vibrator.hasVibrator())
            {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            }
        }
        catch (exception: RuntimeException)
        {
            Log.e(TAG, "Unable to vibrate for session alert", exception)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrator(): Vibrator
    {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        }
        else
        {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    companion object
    {
        private const val TAG = "SessionAlertController"
    }
}
