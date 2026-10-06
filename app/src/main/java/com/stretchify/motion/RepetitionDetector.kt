package com.stretchify.motion

import com.stretchify.model.RepetitionDetectorType

data class MotionSample(
    val timestampNanos: Long,
    val verticalAccelerationMetersPerSecondSquared: Float,
    val pressureAltitudeMeters: Float? = null,
    val rotationRateRadiansPerSecond: Float? = null
)

data class PullUpCalibrationProfile(
    val version: Int,
    val topExcursionMeters: Float,
    val bottomExcursionMeters: Float,
    val medianRepDurationMillis: Long,
    val createdAtMillis: Long
)

sealed interface RepetitionDetectionEvent
{
    data object Ready : RepetitionDetectionEvent

    data class CalibrationProgress(val completedRepetitions: Int) : RepetitionDetectionEvent

    data class CalibrationComplete(val profile: PullUpCalibrationProfile) : RepetitionDetectionEvent

    data class CalibrationFailed(val reason: String) : RepetitionDetectionEvent

    data object Repetition : RepetitionDetectionEvent
}

interface RepetitionDetector
{
    fun process(sample: MotionSample): List<RepetitionDetectionEvent>

    fun reset()
}

object RepetitionDetectorFactory
{
    fun create(
        detectorType: RepetitionDetectorType,
        calibrationProfile: PullUpCalibrationProfile?
    ): RepetitionDetector
    {
        return when (detectorType)
        {
            RepetitionDetectorType.PullUp -> PullUpRepetitionDetector(calibrationProfile)
        }
    }
}
