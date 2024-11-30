package com.csed433project.activityclassifier

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.os.Handler
import android.os.HandlerThread
import android.widget.Button
import android.widget.TextView
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {

    private lateinit var mSensorManager: SensorManager
    private var mSensing: Boolean = false
    private var mLogging: Boolean = false
    private lateinit var mAccel: Sensor
    private lateinit var mGravity: Sensor
    private lateinit var mGyro: Sensor
    private lateinit var mSensorEventListener: SensorEventListener
    private lateinit var mWorkerThread: HandlerThread
    private lateinit var mHandlerWorker: Handler

    private var actionCategoryID = 0
    private var timestamp = 0L
    private var xGyro = 0.0f
    private var yGyro = 0.0f
    private var zGyro = 0.0f
    private var xGrav = 0.0f
    private var yGrav = 0.0f
    private var zGrav = 0.0f
    private var xAccl = 0.0f
    private var yAccl = 0.0f
    private var zAccl = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) as Sensor
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) as Sensor
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) as Sensor

        mWorkerThread = HandlerThread("Worker Thread")
        mWorkerThread.start()
        mHandlerWorker = Handler(mWorkerThread.looper)

        val btnStart = findViewById<Button>(R.id.start_btn)
        val btnStop = findViewById<Button>(R.id.stop_btn)

        btnStart.setOnClickListener() {
            startSensing()
        }

        btnStop.setOnClickListener() {
            stopSensing()
        }

    }

    fun startSensing() {
        mSensorEventListener = object: SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val getTextViewValue: (Int) -> String = {id -> findViewById<TextView>(id).text.toString()}
                val updateTextViewValue: (Int, Float) -> Unit = {id, value -> findViewById<TextView>(id).text = "%.9e".format(value)}
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    timestamp = (event.timestamp / 1000).toLong()    // Nanosecond -> Microsecond
                    xAccl = event.values[0]
                    yAccl = event.values[1]
                    zAccl = event.values[2]
                }
                if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
                    timestamp = (event.timestamp / 1000).toLong()    // Nanosecond -> Microsecond
                    xGrav = event.values[0]
                    yGrav = event.values[1]
                    zGrav = event.values[2]
                }
                if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
                    timestamp = (event.timestamp / 1000).toLong()    // Nanosecond -> Microsecond
                    xGyro = event.values[0]
                    yGyro = event.values[1]
                    zGyro = event.values[2]
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        mSensorManager.registerListener(mSensorEventListener, mAccel, 10000, mHandlerWorker)
        mSensorManager.registerListener(mSensorEventListener, mGravity, 10000, mHandlerWorker)
        mSensorManager.registerListener(mSensorEventListener, mGyro, 10000, mHandlerWorker)
    }

    fun stopSensing() {
        mSensorManager.unregisterListener(mSensorEventListener, mAccel)
        mSensorManager.unregisterListener(mSensorEventListener, mGyro)
        mSensorManager.unregisterListener(mSensorEventListener, mGravity)
    }

    fun arrayMean(v: FloatArray): Float {
        return v.sum() / v.size.toFloat()
    }

    fun arrayVariance(v: FloatArray): Float {
        val mu = arrayMean(v)
        val diff = v.map { x -> (x - mu) * (x - mu) }
        return diff.sum() / diff.size.toFloat()
    }
    
    fun arrayTEFS(v: FloatArray): Float {
        val sq = v.map {x -> x * x}
        return sq.sum() / sq.size.toFloat()
    }

    fun arrayCorrelation(v1: FloatArray, v2: FloatArray): Float{
        if (v1.size != v2.size || v1.isEmpty()){
            Exception("Cannot calculate correlation on array with length %d and %d".format(v1.size, v2.size))
        }

        val muV1 = arrayMean(v1)
        val muV2 = arrayMean(v2)

        val varV1 = arrayVariance(v1)
        val varV2 = arrayVariance(v2)

        var cov = 0.0F
        for ((i1, i2) in v1.zip(v2) ) {
            cov += ((i1 - muV1) * (i2 - muV2))
        }

        return if (varV1 == 0.0F || varV2 == 0.0F) {
            0.0F
        } else {
            cov / sqrt(varV1.toDouble() * varV2.toDouble()).toFloat()
        }
    }
}