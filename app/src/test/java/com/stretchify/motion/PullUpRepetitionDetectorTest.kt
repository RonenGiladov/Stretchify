package com.stretchify.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PullUpRepetitionDetectorTest
{
    @Test
    fun countsAtTopAndRequiresReturnToBottom()
    {
        val detector = PullUpRepetitionDetector(profile())
        val samples = SampleBuilder()
        val events = mutableListOf<RepetitionDetectionEvent>()

        events += samples.still(detector)
        events += samples.ascend(detector, 0.28f)
        events += samples.hold(detector, 0.28f)
        events += samples.ascend(detector, 0.32f)

        assertEquals(1, events.count { it == RepetitionDetectionEvent.Repetition })

        events += samples.descend(detector, 0.28f)
        events += samples.still(detector)
        events += samples.ascend(detector, 0.28f)

        assertEquals(2, events.count { it == RepetitionDetectionEvent.Repetition })
    }

    @Test
    fun partialMovementDoesNotCount()
    {
        val detector = PullUpRepetitionDetector(profile())
        val samples = SampleBuilder()
        val events = mutableListOf<RepetitionDetectionEvent>()

        events += samples.still(detector)
        events += samples.partialAscent(detector)
        events += samples.partialDescent(detector)
        events += samples.still(detector)

        assertTrue(events.none { it == RepetitionDetectionEvent.Repetition })
    }

    @Test
    fun threeCompletePracticeRepetitionsCreateCalibration()
    {
        val detector = PullUpRepetitionDetector(null)
        val samples = SampleBuilder()
        val events = mutableListOf<RepetitionDetectionEvent>()

        events += samples.still(detector)
        repeat(3)
        {
            events += samples.ascend(detector, 0.3f)
            events += samples.descend(detector, 0.3f)
            events += samples.still(detector)
        }

        assertEquals(listOf(1, 2, 3), events.filterIsInstance<RepetitionDetectionEvent.CalibrationProgress>()
            .map { it.completedRepetitions })
        assertEquals(1, events.count { it is RepetitionDetectionEvent.CalibrationComplete })
    }

    @Test
    fun moderateHeightPracticeRepetitionsCreateReachableCalibration()
    {
        val detector = PullUpRepetitionDetector(null)
        val samples = SampleBuilder()
        val events = mutableListOf<RepetitionDetectionEvent>()

        events += samples.still(detector)
        repeat(3)
        {
            events += samples.ascend(detector, 0.14f, 0.35f)
            events += samples.descend(detector, 0.14f, -0.35f)
            events += samples.still(detector)
        }

        val profile = events.filterIsInstance<RepetitionDetectionEvent.CalibrationComplete>()
            .single()
            .profile
        assertEquals(listOf(1, 2, 3), events.filterIsInstance<RepetitionDetectionEvent.CalibrationProgress>()
            .map { it.completedRepetitions })
        assertTrue(profile.topExcursionMeters < 0.14f)
    }

    private fun profile(): PullUpCalibrationProfile
    {
        return PullUpCalibrationProfile(
            version = PullUpRepetitionDetector.PROFILE_VERSION,
            topExcursionMeters = 0.2f,
            bottomExcursionMeters = 0.05f,
            medianRepDurationMillis = 1500L,
            createdAtMillis = 1L
        )
    }

    private class SampleBuilder
    {
        private var timestampNanos = 0L

        fun still(detector: RepetitionDetector): List<RepetitionDetectionEvent>
        {
            return feed(detector, 120, 0f) { 0f }
        }

        fun ascend(
            detector: RepetitionDetector,
            topMeters: Float,
            acceleration: Float = 1.1f
        ): List<RepetitionDetectionEvent>
        {
            return feed(detector, 40, acceleration) { index -> topMeters * (index + 1) / 40f }
        }

        fun hold(detector: RepetitionDetector, heightMeters: Float): List<RepetitionDetectionEvent>
        {
            return feed(detector, 30, 0f) { heightMeters }
        }

        fun partialAscent(detector: RepetitionDetector): List<RepetitionDetectionEvent>
        {
            return feed(detector, 15, 0.8f) { index -> 0.1f * (index + 1) / 15f }
        }

        fun partialDescent(detector: RepetitionDetector): List<RepetitionDetectionEvent>
        {
            return feed(detector, 20, -0.8f) { index -> 0.1f * (1f - (index + 1) / 20f) }
        }

        fun descend(
            detector: RepetitionDetector,
            topMeters: Float,
            acceleration: Float = -1.1f
        ): List<RepetitionDetectionEvent>
        {
            return feed(detector, 50, acceleration) { index -> topMeters * (1f - (index + 1) / 50f) }
        }

        private fun feed(
            detector: RepetitionDetector,
            count: Int,
            acceleration: Float,
            height: (Int) -> Float
        ): List<RepetitionDetectionEvent>
        {
            return buildList {
                repeat(count) { index ->
                    timestampNanos += 20_000_000L
                    addAll(detector.process(MotionSample(timestampNanos, acceleration, height(index))))
                }
            }
        }
    }
}
