package com.csed433.hw1

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
import androidx.compose.ui.graphics.Color
import java.io.BufferedWriter
import java.io.File
import java.nio.Buffer

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
    private lateinit var wFile: File
    private lateinit var wBuffer: BufferedWriter


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

        val btnStartStop = findViewById<Button>(R.id.start_stop_btn)
        val btnPauseResume = findViewById<Button>(R.id.pause_resume_btn)

        btnStartStop.setOnClickListener() {
            mSensing = !mSensing
            if (mSensing) {
                // Initialize buffer
                startSensing()
                btnStartStop.setBackgroundColor(0xFF_FF4081.toInt())
                btnStartStop.setTextColor(0xFF_FFFFFF.toInt())
                btnStartStop.setText("STOP")
            }
            else {
                // Open file
                // Write buffer data into file
                // Close file
                // Clear buffer
                stopSensing()
                btnStartStop.setBackgroundColor(0xFF_69F0AE.toInt())
                btnStartStop.setTextColor(0xFF_000000.toInt())
                btnStartStop.setText("START")
                mLogging = false
                btnPauseResume.setText("RESUME")
            }
        }

        btnPauseResume.setOnClickListener() {
            if (mSensing) {
                mLogging = !mLogging
                if (mLogging) {
                    btnPauseResume.setText("PAUSE")
                }
                else {
                    btnPauseResume.setText("RESUME")
                }

            }
        }
    }

    fun startSensing() {
        mSensorEventListener = object: SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val updateTextViewValue: (Int, Float) -> Unit = {id, value -> findViewById<TextView>(id).text = "%.4f".format(value)}
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val ts = event.timestamp
                    val values = event.values.clone()
                    runOnUiThread {
                        findViewById<TextView>(R.id.time_t_value_text).text = "%d".format(ts / 1000)    // Nanosecond -> Microsecond
                        updateTextViewValue(R.id.accel_x_value_text, values[0])
                        updateTextViewValue(R.id.accel_y_value_text, values[1])
                        updateTextViewValue(R.id.accel_z_value_text, values[2])
                    }
                }
                if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
                    val ts = event.timestamp
                    val values = event.values.clone()
                    runOnUiThread {
                        findViewById<TextView>(R.id.time_t_value_text).text = "%d".format(ts / 1000)    // Nanosecond -> Microsecond
                        updateTextViewValue(R.id.gravity_x_value_text, values[0])
                        updateTextViewValue(R.id.gravity_y_value_text, values[1])
                        updateTextViewValue(R.id.gravity_z_value_text, values[2])
                    }
                }
                if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
                    val ts = event.timestamp
                    val values = event.values.clone()
                    runOnUiThread {
                        findViewById<TextView>(R.id.time_t_value_text).text = "%d".format(ts / 1000)    // Nanosecond -> Microsecond
                        updateTextViewValue(R.id.gyro_x_value_text, values[0])
                        updateTextViewValue(R.id.gyro_y_value_text, values[1])
                        updateTextViewValue(R.id.gyro_z_value_text, values[2])
                    }
                }
                if (mLogging) {
                    // Append sensor data to buffer
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
}