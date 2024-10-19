package com.csed433.hw1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.os.Handler
import android.os.HandlerThread
import android.widget.Button
import androidx.annotation.WorkerThread
import com.csed433.hw1.ui.theme.CSED433HW1SensorMonitorTheme

class MainActivity : ComponentActivity() {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccel: Sensor
    private var mSensing: Boolean = false
    private lateinit var mSensorEventListener: SensorEventListener
    private lateinit var mWorkerThread: HandlerThread
    private lateinit var mHandlerWorker: Handler


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mWorkerThread = HandlerThread("Worker Thread")
        mWorkerThread.start();
        mHandlerWorker = Handler(mWorkerThread.looper)

        findViewById<Button>(R.id.buttonStartStop).setOnclickListener {

        }
    }
}