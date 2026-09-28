package com.stretchify.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertModeTest
{
    @Test
    fun alertModesEnableExpectedOutputs()
    {
        assertTrue(AlertMode.SoundAndVibration.isSoundEnabled)
        assertTrue(AlertMode.SoundAndVibration.isVibrationEnabled)
        assertTrue(AlertMode.SoundOnly.isSoundEnabled)
        assertFalse(AlertMode.SoundOnly.isVibrationEnabled)
        assertFalse(AlertMode.VibrationOnly.isSoundEnabled)
        assertTrue(AlertMode.VibrationOnly.isVibrationEnabled)
        assertFalse(AlertMode.Off.isSoundEnabled)
        assertFalse(AlertMode.Off.isVibrationEnabled)
    }
}
