package com.stretchify.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import kotlin.math.sqrt

class AndroidMotionSensorSource(context: Context)
{
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private var sensorThread: HandlerThread? = null
    private var listener: SensorEventListener? = null
    private var gravity = floatArrayOf(0f, 0f, SensorManager.GRAVITY_EARTH)
    private var latestPressureAltitude: Float? = null
    private var latestRotationRate: Float? = null

    val isAvailable: Boolean
        get() = accelerometer != null

    fun start(onSample: (MotionSample) -> Unit): Boolean
    {
        if (accelerometer == null || listener != null)
        {
            return accelerometer != null
        }

        val thread = HandlerThread(THREAD_NAME).apply { start() }
        sensorThread = thread
        val handler = Handler(thread.looper)
        val eventListener = object : SensorEventListener
        {
            override fun onSensorChanged(event: SensorEvent)
            {
                when (event.sensor.type)
                {
                    Sensor.TYPE_GRAVITY -> gravity = event.values.copyOf(3)
                    Sensor.TYPE_PRESSURE -> latestPressureAltitude = SensorManager.getAltitude(
                        SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
                        event.values[0]
                    )
                    Sensor.TYPE_GYROSCOPE -> latestRotationRate = vectorMagnitude(event.values)
                    Sensor.TYPE_LINEAR_ACCELERATION -> onSample(
                        MotionSample(
                            event.timestamp,
                            projectOntoGravity(event.values),
                            latestPressureAltitude,
                            latestRotationRate
                        )
                    )
                    Sensor.TYPE_ACCELEROMETER ->
                    {
                        if (gravitySensor == null)
                        {
                            updateGravityFromAccelerometer(event.values)
                        }
                        if (linearAcceleration == null)
                        {
                            val adjustedAcceleration = floatArrayOf(
                                event.values[0] - gravity[0],
                                event.values[1] - gravity[1],
                                event.values[2] - gravity[2]
                            )
                            onSample(
                                MotionSample(
                                    event.timestamp,
                                    projectOntoGravity(adjustedAcceleration),
                                    latestPressureAltitude,
                                    latestRotationRate
                                )
                            )
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = eventListener
        gravitySensor?.let { sensorManager.registerListener(eventListener, it, SENSOR_PERIOD_MICROS, handler) }
        gyroscope?.let { sensorManager.registerListener(eventListener, it, SENSOR_PERIOD_MICROS, handler) }
        pressureSensor?.let { sensorManager.registerListener(eventListener, it, SENSOR_PERIOD_MICROS, handler) }
        if (gravitySensor == null && linearAcceleration != null)
        {
            sensorManager.registerListener(eventListener, accelerometer, SENSOR_PERIOD_MICROS, handler)
        }
        val motionSensor = linearAcceleration ?: accelerometer
        return sensorManager.registerListener(eventListener, motionSensor, SENSOR_PERIOD_MICROS, handler)
    }

    fun stop()
    {
        listener?.let(sensorManager::unregisterListener)
        listener = null
        sensorThread?.quitSafely()
        sensorThread = null
        latestPressureAltitude = null
        latestRotationRate = null
    }

    private fun projectOntoGravity(values: FloatArray): Float
    {
        val magnitude = vectorMagnitude(gravity)
        if (magnitude <= 0.01f)
        {
            return values.getOrElse(2) { 0f }
        }
        return (values[0] * gravity[0] + values[1] * gravity[1] + values[2] * gravity[2]) / magnitude
    }

    private fun vectorMagnitude(values: FloatArray): Float
    {
        return sqrt(values.sumOf { value -> (value * value).toDouble() }).toFloat()
    }

    private fun updateGravityFromAccelerometer(values: FloatArray)
    {
        for (index in gravity.indices)
        {
            gravity[index] = GRAVITY_FILTER_ALPHA * gravity[index] +
                (1f - GRAVITY_FILTER_ALPHA) * values[index]
        }
    }

    companion object
    {
        private const val THREAD_NAME = "StretchifyMotionSensors"
        private const val SENSOR_PERIOD_MICROS = 20_000
        private const val GRAVITY_FILTER_ALPHA = 0.98f
    }
}
