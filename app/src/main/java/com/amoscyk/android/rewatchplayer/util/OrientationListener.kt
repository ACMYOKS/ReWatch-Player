package com.amoscyk.android.rewatchplayer.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log


class OrientationListener(context: Context) : SensorEventListener {
    private val mContext: Context = context
    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mMagnetic: Sensor? = null
    private var mOnOrientationListener: OnOrientationListener? = null
    private var mAccelerometerValues = FloatArray(3)
    private var mMagneticValues = FloatArray(3)

    interface OnOrientationListener {
        fun onOrientationChanged(
            azimuth: Float,
            pitch: Float,
            roll: Float
        )
    }

    init {
        mSensorManager = mContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagnetic = mSensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    fun setOnOrientationListener(onOrientationListener: OnOrientationListener?) {
        mOnOrientationListener = onOrientationListener
    }

    fun registerListener() {
        if (mSensorManager != null && mAccelerometer != null && mMagnetic != null) {
            mSensorManager!!.registerListener(
                this,
                mAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            mSensorManager!!.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregisterListener() {
        mSensorManager!!.unregisterListener(this)
    }

    private fun calculateOrientation() {
        val values = FloatArray(3)
        val matrix = FloatArray(9)
        SensorManager.getRotationMatrix(matrix, null, mAccelerometerValues, mMagneticValues)
        SensorManager.getOrientation(matrix, values)
        var azimuth = Math.toDegrees(values[0].toDouble()).toFloat()
        if (azimuth < 0) {
            azimuth += 360f
        }
        azimuth = azimuth / 5 * 5
        val pitch = Math.toDegrees(values[1].toDouble()).toFloat()
        val roll = Math.toDegrees(values[2].toDouble()).toFloat()
        if (mOnOrientationListener != null) {
            Log.d(TAG, "azimuth：$azimuth\tpitch：$pitch\troll：$roll")
            mOnOrientationListener!!.onOrientationChanged(azimuth, pitch, roll)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometerValues = event.values
        }
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            mMagneticValues = event.values
        }
        calculateOrientation()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    companion object {
        const val TAG = "OrientationListener"
    }
}