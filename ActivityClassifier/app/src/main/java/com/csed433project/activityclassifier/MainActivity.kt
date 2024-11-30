package com.csed433project.activityclassifier

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.Runnable
import kotlin.math.sqrt

import libsvm.*
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccel: Sensor
    private lateinit var mGravity: Sensor
    private lateinit var mGyro: Sensor
    private lateinit var mSensorEventListener: SensorEventListener
    private lateinit var sensorThread: HandlerThread
    private lateinit var sensorWorker: Handler
    private var sensorTsOriginTime: Long = 0L
    private lateinit var svmThread: HandlerThread
    private lateinit var svmWorker: Handler

    private var working: Boolean = false

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

    private var cirqGyro: Array<Array<Array<Float>>> = Array(3) {Array(100) {Array(3) {0.0F} } }
    private var cirqGyroPushCnt: Int = 0
    private var cirqGrav: Array<Array<Array<Float>>> = Array(3) {Array(100) {Array(3) {0.0F} } }
    private var cirqGravPushCnt: Int = 0
    private var cirqAccl: Array<Array<Array<Float>>> = Array(3) {Array(100) {Array(3) {0.0F} } }
    private var cirqAcclPushCnt: Int = 0
    private var cirqSlotClockHand: Int = 0

    private lateinit var svmModel: svm_model
    private lateinit var svmCoeff: Array<Array<Float>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) as Sensor
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) as Sensor
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) as Sensor

        svmModel = getSvmModel("model_file")
        svmCoeff = getSvmNormalizeCoeff("normalization_scale")


        sensorThread = HandlerThread("Sensor Retrieve Thread")
        sensorThread.start()
        sensorWorker = Handler(sensorThread.looper)

        svmThread = HandlerThread("SVM Classifier Thread")
        svmThread.start()
        svmWorker = Handler(svmThread.looper)


        val btnStartStop = findViewById<Button>(R.id.start_stop_btn)

        btnStartStop.setOnClickListener() {
            working = !working
            if (working)
            {
                startSensing()
                startPredict()
                btnStartStop.text = "STOP"
                btnStartStop.setBackgroundColor(resources.getColor(R.color.stop))
                btnStartStop.setTextColor(resources.getColor(R.color.white))
            }
            else
            {
                stopSensing()
                btnStartStop.text = "START"
                btnStartStop.setBackgroundColor(resources.getColor(R.color.start))
                btnStartStop.setTextColor(resources.getColor(R.color.black))
            }

        }


    }

    fun startSensing() {
        sensorTsOriginTime = System.nanoTime()
        mSensorEventListener = object: SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val ts = event?.timestamp as Long
                val storeSlot = ((ts - sensorTsOriginTime) / 1_000_000_000).toInt() % 3

                if (cirqSlotClockHand != storeSlot) {
                    cirqSlotClockHand = storeSlot
                    cirqAcclPushCnt = 0
                    cirqGravPushCnt = 0
                    cirqGyroPushCnt = 0
                }

                if (event.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    cirqAccl[storeSlot][cirqAcclPushCnt % 100][0] = event.values[0]
                    cirqAccl[storeSlot][cirqAcclPushCnt % 100][1] = event.values[1]
                    cirqAccl[storeSlot][cirqAcclPushCnt % 100][2] = event.values[2]
                    cirqAcclPushCnt += 1
                }
                if (event.sensor?.type == Sensor.TYPE_GRAVITY) {
                    cirqGrav[storeSlot][cirqGravPushCnt % 100][0] = event.values[0]
                    cirqGrav[storeSlot][cirqGravPushCnt % 100][1] = event.values[1]
                    cirqGrav[storeSlot][cirqGravPushCnt % 100][2] = event.values[2]
                    cirqGravPushCnt += 1
                }
                if (event.sensor?.type == Sensor.TYPE_GYROSCOPE) {
                    cirqGyro[storeSlot][cirqGyroPushCnt % 100][0] = event.values[0]
                    cirqGyro[storeSlot][cirqGyroPushCnt % 100][1] = event.values[1]
                    cirqGyro[storeSlot][cirqGyroPushCnt % 100][2] = event.values[2]
                    cirqGyroPushCnt += 1
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        mSensorManager.registerListener(mSensorEventListener, mAccel, 10000, sensorWorker)
        mSensorManager.registerListener(mSensorEventListener, mGravity, 10000, sensorWorker)
        mSensorManager.registerListener(mSensorEventListener, mGyro, 10000, sensorWorker)
    }

    fun stopSensing() {
        mSensorManager.unregisterListener(mSensorEventListener, mAccel)
        mSensorManager.unregisterListener(mSensorEventListener, mGyro)
        mSensorManager.unregisterListener(mSensorEventListener, mGravity)
    }

    fun startPredict() {
        var windowAccl: Array<Array<Float>> = Array(3) {Array(200) {0.0F} }
        var windowGrav: Array<Array<Float>> = Array(3) {Array(200) {0.0F} }
        var windowGyro: Array<Array<Float>> = Array(3) {Array(200) {0.0F} }

        var svmFeatures: Array<Float> = Array<Float>(36) {0.0F}

        svmWorker.postDelayed(object: Runnable {
            override fun run() {
                val cirqSlotFront = (cirqSlotClockHand + 1) % 3
                val cirqSlotBack = (cirqSlotClockHand + 2) % 3

                for (fidx in 0..99) {
                    windowAccl[0][fidx] = cirqAccl[cirqSlotFront][fidx][0]
                    windowAccl[1][fidx] = cirqAccl[cirqSlotFront][fidx][1]
                    windowAccl[2][fidx] = cirqAccl[cirqSlotFront][fidx][2]

                    windowGrav[0][fidx] = cirqGrav[cirqSlotFront][fidx][0]
                    windowGrav[1][fidx] = cirqGrav[cirqSlotFront][fidx][1]
                    windowGrav[2][fidx] = cirqGrav[cirqSlotFront][fidx][2]

                    windowGyro[0][fidx] = cirqGyro[cirqSlotFront][fidx][0]
                    windowGyro[1][fidx] = cirqGyro[cirqSlotFront][fidx][1]
                    windowGyro[2][fidx] = cirqGyro[cirqSlotFront][fidx][2]
                }
                for (bidx in 100..199) {
                    windowAccl[0][bidx] = cirqAccl[cirqSlotBack][bidx - 100][0]
                    windowAccl[1][bidx] = cirqAccl[cirqSlotBack][bidx - 100][1]
                    windowAccl[2][bidx] = cirqAccl[cirqSlotBack][bidx - 100][2]

                    windowGrav[0][bidx] = cirqGrav[cirqSlotBack][bidx - 100][0]
                    windowGrav[1][bidx] = cirqGrav[cirqSlotBack][bidx - 100][1]
                    windowGrav[2][bidx] = cirqGrav[cirqSlotBack][bidx - 100][2]

                    windowGyro[0][bidx] = cirqGyro[cirqSlotBack][bidx - 100][0]
                    windowGyro[1][bidx] = cirqGyro[cirqSlotBack][bidx - 100][1]
                    windowGyro[2][bidx] = cirqGyro[cirqSlotBack][bidx - 100][2]
                }

                /*
                    Grav
                    Accl
                    Gyro
                 */
                svmFeatures[0] = arrayMean(windowGrav[0])
                svmFeatures[1] = arrayMean(windowGrav[1])
                svmFeatures[2] = arrayMean(windowGrav[2])
                svmFeatures[3] = arrayVariance(windowGrav[0])
                svmFeatures[4] = arrayVariance(windowGrav[1])
                svmFeatures[5] = arrayVariance(windowGrav[2])
                svmFeatures[6] = arrayTEFS(windowGrav[0])
                svmFeatures[7] = arrayTEFS(windowGrav[1])
                svmFeatures[8] = arrayTEFS(windowGrav[2])
                svmFeatures[9] = arrayCorrelation(windowGrav[0], windowGrav[1])
                svmFeatures[10] = arrayCorrelation(windowGrav[0], windowGrav[2])
                svmFeatures[11] = arrayCorrelation(windowGrav[1], windowGrav[2])

                svmFeatures[12] = arrayMean(windowAccl[0])
                svmFeatures[13] = arrayMean(windowAccl[1])
                svmFeatures[14] = arrayMean(windowAccl[2])
                svmFeatures[15] = arrayVariance(windowAccl[0])
                svmFeatures[16] = arrayVariance(windowAccl[1])
                svmFeatures[17] = arrayVariance(windowAccl[2])
                svmFeatures[18] = arrayTEFS(windowAccl[0])
                svmFeatures[19] = arrayTEFS(windowAccl[1])
                svmFeatures[20] = arrayTEFS(windowAccl[2])
                svmFeatures[21] = arrayCorrelation(windowAccl[0], windowAccl[1])
                svmFeatures[22] = arrayCorrelation(windowAccl[0], windowAccl[2])
                svmFeatures[23] = arrayCorrelation(windowAccl[1], windowAccl[2])

                svmFeatures[24] = arrayMean(windowGyro[0])
                svmFeatures[25] = arrayMean(windowGyro[1])
                svmFeatures[26] = arrayMean(windowGyro[2])
                svmFeatures[27] = arrayVariance(windowGyro[0])
                svmFeatures[28] = arrayVariance(windowGyro[1])
                svmFeatures[29] = arrayVariance(windowGyro[2])
                svmFeatures[30] = arrayTEFS(windowGyro[0])
                svmFeatures[31] = arrayTEFS(windowGyro[1])
                svmFeatures[32] = arrayTEFS(windowGyro[2])
                svmFeatures[33] = arrayCorrelation(windowGyro[0], windowGyro[1])
                svmFeatures[34] = arrayCorrelation(windowGyro[0], windowGyro[2])
                svmFeatures[35] = arrayCorrelation(windowGyro[1], windowGyro[2])

                val svmNode: Array<svm_node> = Array<svm_node>(36) {svm_node()}

                for (idx in 0 .. 35) {
                    /*
                        [MIN - MAX] -> [-0.5, +0.5]
                     */
                    svmNode[idx].index = idx + 1
                    svmNode[idx].value = ((svmFeatures[idx] - svmCoeff[idx][0]) / (svmCoeff[idx][1] - svmCoeff[idx][0]) * 2 - 1).toDouble()
                    svmFeatures[idx] = svmNode[idx].value.toFloat()
                }

                Log.d("Feat", "Value = %s".format(svmFeatures.joinToString(prefix = "[", separator = ",", postfix = "]")))

                val prediction = svm.svm_predict(svmModel, svmNode).toInt()
                runOnUiThread(object: Runnable{
                    override fun run() {
                        findViewById<TextView>(R.id.prediction_label_text).text = resources.getStringArray(R.array.activityclass_name)[prediction]
                    }
                })
                svmWorker.postDelayed(this, 1000)
            }
        }, 1000)
    }

    fun arrayMean(v: Array<Float>): Float {
        return v.sum() / v.size.toFloat()
    }

    fun arrayVariance(v: Array<Float>): Float {
        val mu = arrayMean(v)
        val diff = v.map { x -> (x - mu) * (x - mu) }
        return diff.sum() / diff.size.toFloat()
    }
    
    fun arrayTEFS(v: Array<Float>): Float {
        val sq = v.map {x -> x * x}
        return sq.sum() / sq.size.toFloat()
    }

    fun arrayCorrelation(v1: Array<Float>, v2: Array<Float>): Float{
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

    fun getSvmModel(path: String): svm_model {
        val assetInputStream = assets.open(path)
        val assetReader = BufferedReader(InputStreamReader(assetInputStream))
        return svm.svm_load_model(assetReader)
    }

    fun getSvmNormalizeCoeff(path: String): Array<Array<Float>> {
        val assetInputStream = assets.open(path)
        val assetReader = BufferedReader(InputStreamReader(assetInputStream))
        var coeffArray: Array<Array<Float>> = emptyArray()

        var minmaxString: String?
        while (true)
        {
            minmaxString = assetReader.readLine()
            if (minmaxString == null){
                break
            }
            else{
                val minValue = minmaxString.split(" ")[0].trim().toFloat()
                val maxValue = minmaxString.split(" ")[1].trim().toFloat()
                coeffArray += floatArrayOf(minValue, maxValue).toTypedArray()
            }
        }
        return coeffArray
    }
}