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

class MainActivity : ComponentActivity() {

    private lateinit var mSensorManager: SensorManager
    private var mSensing: Boolean = false
    private lateinit var mAccel: Sensor
    private lateinit var mGravity: Sensor
    private lateinit var mGyro: Sensor
    private lateinit var mSensorEventListener: SensorEventListener
    private lateinit var mWorkerThread: HandlerThread
    private lateinit var mHandlerWorker: Handler


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

        btnStartStop.setOnClickListener() {
            mSensing = !mSensing
            if (mSensing) {
                startSensing()
                btnStartStop.setText("STOP")
            }
            else {
                stopSensing()
                btnStartStop.setText("START")
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