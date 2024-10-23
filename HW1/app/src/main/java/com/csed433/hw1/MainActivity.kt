package com.csed433.hw1

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    private lateinit var wGyroFile: File
    private lateinit var wGyroOutputStream: FileOutputStream

    private lateinit var wGravFile: File
    private lateinit var wGravOutputStream: FileOutputStream

    private lateinit var wAcclFile: File
    private lateinit var wAcclOutputStream: FileOutputStream

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

        val spinnerActionSelect = findViewById<Spinner>(R.id.action_category_spinner)
        spinnerActionSelect.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.action_category))
        spinnerActionSelect.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                actionCategoryID = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                actionCategoryID = 0
            }
        }

        val btnStartStop = findViewById<Button>(R.id.start_stop_btn)
        val btnPauseResume = findViewById<Button>(R.id.pause_resume_btn)

        btnStartStop.setOnClickListener() {
            mSensing = !mSensing
            if (mSensing) {

                val wTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))

                wAcclFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/" + wTimeString + "_accel.csv")
                wGravFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/" + wTimeString + "_grav.csv")
                wGyroFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/" + wTimeString + "_gyro.csv")

                if (!wAcclFile.exists()) {
                    wAcclFile.createNewFile()
                }

                if (!wGravFile.exists()) {
                    wGravFile.createNewFile()
                }

                if (!wGyroFile.exists()){
                    wGyroFile.createNewFile()
                }

                wAcclOutputStream = FileOutputStream(wAcclFile)
                wGravOutputStream = FileOutputStream(wGravFile)
                wGyroOutputStream = FileOutputStream(wGyroFile)

                startSensing()
                mLogging = true

                btnStartStop.setBackgroundColor(0xFF_FF4081.toInt())
                btnStartStop.setTextColor(0xFF_FFFFFF.toInt())
                btnStartStop.setText("STOP")

                btnPauseResume.setText("PAUSE")
            }
            else {
                mLogging = false
                stopSensing()

                wAcclOutputStream.close()
                wGravOutputStream.close()
                wGyroOutputStream.close()

                btnStartStop.setBackgroundColor(0xFF_69F0AE.toInt())
                btnStartStop.setTextColor(0xFF_000000.toInt())
                btnStartStop.setText("START")

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
                val getTextViewValue: (Int) -> String = {id -> findViewById<TextView>(id).text.toString()}
                val updateTextViewValue: (Int, Float) -> Unit = {id, value -> findViewById<TextView>(id).text = "%.9e".format(value)}
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    timestamp = (event.timestamp / 1000).toLong()    // Nanosecond -> Microsecond
                    xAccl = event.values[0]
                    yAccl = event.values[1]
                    zAccl = event.values[2]
                    runOnUiThread {
                        findViewById<TextView>(R.id.time_t_value_text).text = "%d".format(timestamp)    // Nanosecond -> Microsecond
                        updateTextViewValue(R.id.accel_x_value_text, xAccl)
                        updateTextViewValue(R.id.accel_y_value_text, yAccl)
                        updateTextViewValue(R.id.accel_z_value_text, zAccl)
                    }
                    if (mLogging) {
                        wAcclOutputStream.write("%d, %d, %.9e, %.9e, %.9e\n".format(timestamp, actionCategoryID, xAccl, yAccl, zAccl).toByteArray())
                    }
                }
                if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
                    timestamp = (event.timestamp / 1000).toLong()    // Nanosecond -> Microsecond
                    xGrav = event.values[0]
                    yGrav = event.values[1]
                    zGrav = event.values[2]
                    runOnUiThread {
                        findViewById<TextView>(R.id.time_t_value_text).text = "%d".format(timestamp)    // Nanosecond -> Microsecond
                        updateTextViewValue(R.id.gravity_x_value_text, xGrav)
                        updateTextViewValue(R.id.gravity_y_value_text, yGrav)
                        updateTextViewValue(R.id.gravity_z_value_text, zGrav)
                    }
                    if (mLogging) {
                        wGravOutputStream.write("%d, %d, %.9e, %.9e, %.9e\n".format(timestamp, actionCategoryID, xGrav, yGrav, zGrav).toByteArray())
                    }
                }
                if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
                    timestamp = (event.timestamp / 1000).toLong()    // Nanosecond -> Microsecond
                    xGyro = event.values[0]
                    yGyro = event.values[1]
                    zGyro = event.values[2]
                    runOnUiThread {
                        findViewById<TextView>(R.id.time_t_value_text).text = "%d".format(timestamp)    // Nanosecond -> Microsecond
                        updateTextViewValue(R.id.gyro_x_value_text, xGyro)
                        updateTextViewValue(R.id.gyro_y_value_text, yGyro)
                        updateTextViewValue(R.id.gyro_z_value_text, zGyro)
                    }
                    if (mLogging) {
                        wGyroOutputStream.write("%d, %d, %.9e, %.9e, %.9e\n".format(timestamp, actionCategoryID, xGyro, yGyro, zGyro).toByteArray())
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