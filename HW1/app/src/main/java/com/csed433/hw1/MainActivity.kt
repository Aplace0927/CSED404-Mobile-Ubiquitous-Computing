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
    private lateinit var mSensorEventListener: SensorEventListener
    private lateinit var mWorkerThread: HandlerThread
    private lateinit var mHandlerWorker: Handler


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) as Sensor
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
                        findViewById<TextView>(R.id.time_t_value_text).text = "%d".format(ts)
                        updateTextViewValue(R.id.accel_x_value_text, values[0])
                        updateTextViewValue(R.id.accel_y_value_text, values[1])
                        updateTextViewValue(R.id.accel_z_value_text, values[2])
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        mSensorManager.registerListener(mSensorEventListener, mAccel, 10000, mHandlerWorker)
    }

    fun stopSensing() {
        mSensorManager.unregisterListener(mSensorEventListener, mAccel)
    }
}