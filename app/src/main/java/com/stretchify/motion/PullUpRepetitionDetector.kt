package com.stretchify.motion

import kotlin.math.abs
import kotlin.math.max

class PullUpRepetitionDetector(
    private val calibrationProfile: PullUpCalibrationProfile?
) : RepetitionDetector
{
    private var phase = Phase.Readiness
    private var lastTimestampNanos = 0L
    private var filteredAcceleration = 0f
    private var velocity = 0f
    private var displacement = 0f
    private var pressureBaselineMeters: Float? = null
    private var stableSinceNanos = 0L
    private var ascentStartedAtNanos = 0L
    private var peakExcursion = 0f
    private var hasStartedDescending = false
    private val calibrationExcursions = mutableListOf<Float>()
    private val calibrationDurationsMillis = mutableListOf<Long>()

    override fun process(sample: MotionSample): List<RepetitionDetectionEvent>
    {
        if ((sample.rotationRateRadiansPerSecond ?: 0f) > MAX_ROTATION_RATE)
        {
            reset()
            pressureBaselineMeters = sample.pressureAltitudeMeters
            return emptyList()
        }
        if (lastTimestampNanos == 0L)
        {
            lastTimestampNanos = sample.timestampNanos
            pressureBaselineMeters = sample.pressureAltitudeMeters
            return emptyList()
        }

        val deltaSeconds = ((sample.timestampNanos - lastTimestampNanos) / NANOS_PER_SECOND)
            .coerceIn(MIN_SAMPLE_SECONDS, MAX_SAMPLE_SECONDS)
        lastTimestampNanos = sample.timestampNanos
        filteredAcceleration = FILTER_ALPHA * filteredAcceleration +
            (1f - FILTER_ALPHA) * sample.verticalAccelerationMetersPerSecondSquared
        velocity = (velocity + filteredAcceleration * deltaSeconds) * VELOCITY_DAMPING
        displacement = (displacement + velocity * deltaSeconds).coerceIn(MIN_DISPLACEMENT, MAX_DISPLACEMENT)

        val pressureExcursion = pressureBaselineMeters?.let { baseline ->
            sample.pressureAltitudeMeters?.minus(baseline)
        } ?: 0f
        val excursion = max(displacement, pressureExcursion)
        val isStable = abs(filteredAcceleration) <= STABLE_ACCELERATION && abs(velocity) <= STABLE_VELOCITY
        updateStablePeriod(sample.timestampNanos, isStable)

        return when (phase)
        {
            Phase.Readiness -> processReadiness(sample.timestampNanos)
            Phase.Bottom -> processBottom(sample.timestampNanos, excursion)
            Phase.Ascending -> processAscent(sample.timestampNanos, excursion)
            Phase.Top -> processTop(sample.timestampNanos, excursion, isStable)
        }
    }

    override fun reset()
    {
        phase = Phase.Readiness
        lastTimestampNanos = 0L
        filteredAcceleration = 0f
        velocity = 0f
        displacement = 0f
        pressureBaselineMeters = null
        stableSinceNanos = 0L
        ascentStartedAtNanos = 0L
        peakExcursion = 0f
        hasStartedDescending = false
    }

    private fun processReadiness(timestampNanos: Long): List<RepetitionDetectionEvent>
    {
        if (stableSinceNanos == 0L || timestampNanos - stableSinceNanos < READINESS_NANOS)
        {
            return emptyList()
        }

        resetMotion()
        phase = Phase.Bottom
        return if (calibrationProfile == null) emptyList() else listOf(RepetitionDetectionEvent.Ready)
    }

    private fun processBottom(timestampNanos: Long, excursion: Float): List<RepetitionDetectionEvent>
    {
        if (filteredAcceleration >= ASCENT_ACCELERATION && velocity >= ASCENT_VELOCITY)
        {
            phase = Phase.Ascending
            ascentStartedAtNanos = timestampNanos
            peakExcursion = excursion
        }
        return emptyList()
    }

    private fun processAscent(timestampNanos: Long, excursion: Float): List<RepetitionDetectionEvent>
    {
        peakExcursion = max(peakExcursion, excursion)
        val topThreshold = calibrationProfile?.topExcursionMeters ?: CALIBRATION_TOP_EXCURSION
        if (excursion < topThreshold)
        {
            if (timestampNanos - ascentStartedAtNanos > MAX_REP_NANOS)
            {
                resetToBottom()
            }
            return emptyList()
        }

        phase = Phase.Top
        return if (calibrationProfile == null)
        {
            emptyList()
        }
        else
        {
            listOf(RepetitionDetectionEvent.Repetition)
        }
    }

    private fun processTop(
        timestampNanos: Long,
        excursion: Float,
        isStable: Boolean
    ): List<RepetitionDetectionEvent>
    {
        peakExcursion = max(peakExcursion, excursion)
        if (velocity < -DESCENT_VELOCITY || excursion <= peakExcursion * DESCENT_EXCURSION_RATIO)
        {
            hasStartedDescending = true
        }
        val bottomThreshold = calibrationProfile?.bottomExcursionMeters ?:
            max(MIN_BOTTOM_EXCURSION, peakExcursion * BOTTOM_EXCURSION_RATIO)
        val repDurationNanos = timestampNanos - ascentStartedAtNanos
        val hasReturnedToBottom = hasStartedDescending &&
            (excursion <= bottomThreshold || isStable && repDurationNanos >= MIN_REP_NANOS)
        if (!hasReturnedToBottom)
        {
            if (repDurationNanos > MAX_REP_NANOS)
            {
                resetToBottom()
            }
            return emptyList()
        }

        if (calibrationProfile != null)
        {
            resetToBottom()
            return emptyList()
        }

        if (repDurationNanos !in MIN_REP_NANOS..MAX_REP_NANOS || peakExcursion < CALIBRATION_TOP_EXCURSION)
        {
            resetToBottom()
            return emptyList()
        }

        calibrationExcursions += peakExcursion
        calibrationDurationsMillis += repDurationNanos / NANOS_PER_MILLISECOND
        val completedRepetitions = calibrationExcursions.size
        resetToBottom()
        if (completedRepetitions < REQUIRED_CALIBRATION_REPETITIONS)
        {
            return listOf(RepetitionDetectionEvent.CalibrationProgress(completedRepetitions))
        }

        val medianExcursion = calibrationExcursions.sorted()[calibrationExcursions.size / 2]
        val medianDuration = calibrationDurationsMillis.sorted()[calibrationDurationsMillis.size / 2]
        val excursionSpread = calibrationExcursions.maxOrNull()!! - calibrationExcursions.minOrNull()!!
        val durationSpread = calibrationDurationsMillis.maxOrNull()!! - calibrationDurationsMillis.minOrNull()!!
        if (excursionSpread > medianExcursion * MAX_CALIBRATION_EXCURSION_SPREAD ||
            durationSpread > medianDuration * MAX_CALIBRATION_DURATION_SPREAD)
        {
            calibrationExcursions.clear()
            calibrationDurationsMillis.clear()
            return listOf(
                RepetitionDetectionEvent.CalibrationFailed(
                    "The practice reps were too different. Try three smooth reps with the same full range."
                )
            )
        }
        val profile = PullUpCalibrationProfile(
            version = PROFILE_VERSION,
            topExcursionMeters = (medianExcursion * TOP_EXCURSION_RATIO)
                .coerceIn(MIN_TOP_EXCURSION, MAX_TOP_EXCURSION),
            bottomExcursionMeters = (medianExcursion * BOTTOM_EXCURSION_RATIO)
                .coerceAtLeast(MIN_BOTTOM_EXCURSION),
            medianRepDurationMillis = medianDuration,
            createdAtMillis = System.currentTimeMillis()
        )
        return listOf(
            RepetitionDetectionEvent.CalibrationProgress(completedRepetitions),
            RepetitionDetectionEvent.CalibrationComplete(profile)
        )
    }

    private fun updateStablePeriod(timestampNanos: Long, isStable: Boolean)
    {
        if (isStable && stableSinceNanos == 0L)
        {
            stableSinceNanos = timestampNanos
        }
        else if (!isStable)
        {
            stableSinceNanos = 0L
        }
    }

    private fun resetToBottom()
    {
        resetMotion()
        phase = Phase.Bottom
    }

    private fun resetMotion()
    {
        filteredAcceleration = 0f
        velocity = 0f
        displacement = 0f
        stableSinceNanos = 0L
        ascentStartedAtNanos = 0L
        peakExcursion = 0f
        hasStartedDescending = false
    }

    private enum class Phase
    {
        Readiness,
        Bottom,
        Ascending,
        Top
    }

    companion object
    {
        const val REQUIRED_CALIBRATION_REPETITIONS = 3
        const val PROFILE_VERSION = 1

        private const val NANOS_PER_SECOND = 1_000_000_000f
        private const val NANOS_PER_MILLISECOND = 1_000_000L
        private const val MIN_SAMPLE_SECONDS = 0.005f
        private const val MAX_SAMPLE_SECONDS = 0.1f
        private const val FILTER_ALPHA = 0.72f
        private const val VELOCITY_DAMPING = 0.985f
        private const val MIN_DISPLACEMENT = -0.12f
        private const val MAX_DISPLACEMENT = 1.2f
        private const val STABLE_ACCELERATION = 0.32f
        private const val STABLE_VELOCITY = 0.1f
        private const val ASCENT_ACCELERATION = 0.45f
        private const val ASCENT_VELOCITY = 0.04f
        private const val DESCENT_VELOCITY = 0.08f
        private const val CALIBRATION_TOP_EXCURSION = 0.18f
        private const val MIN_TOP_EXCURSION = 0.16f
        private const val MAX_TOP_EXCURSION = 0.65f
        private const val MIN_BOTTOM_EXCURSION = 0.04f
        private const val TOP_EXCURSION_RATIO = 0.9f
        private const val BOTTOM_EXCURSION_RATIO = 0.25f
        private const val DESCENT_EXCURSION_RATIO = 0.65f
        private const val MAX_CALIBRATION_EXCURSION_SPREAD = 0.45f
        private const val MAX_CALIBRATION_DURATION_SPREAD = 0.75f
        private const val MAX_ROTATION_RATE = 4f
        private const val READINESS_NANOS = 2_000_000_000L
        private const val MIN_REP_NANOS = 600_000_000L
        private const val MAX_REP_NANOS = 8_000_000_000L
    }
}
