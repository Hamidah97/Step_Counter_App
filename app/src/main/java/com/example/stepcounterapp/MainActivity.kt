package com.stepcounterapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private lateinit var db: FirebaseFirestore

    private var permissionGranted by mutableStateOf(false)
    private var totalSteps = 0f
    private var previousSteps = 0f

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionGranted = isGranted
        if (isGranted) {
            Log.d("Permission", "ACTIVITY_RECOGNITION permission granted")
            registerStepSensor()
        } else {
            Log.d("Permission", "ACTIVITY_RECOGNITION permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        db = FirebaseFirestore.getInstance()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        checkActivityRecognitionPermission()

        setContent {
            var steps by remember { mutableStateOf(getSavedSteps()) }
            var lastSynced by remember { mutableStateOf(System.currentTimeMillis()) }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StepContent(
                        permissionGranted = permissionGranted,
                        stepCounterSensor = stepCounterSensor,
                        steps = steps,
                        lastSynced = lastSynced,
                        onGrantPermission = { requestPermission() }
                    )
                }

                // Live sensor listener effect
                if (permissionGranted && stepCounterSensor != null) {
                    DisposableEffect(Unit) {
                        val listener = object : SensorEventListener {
                            override fun onSensorChanged(event: SensorEvent?) {
                                if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                                    totalSteps = event.values[0]
                                    val currentSteps = (totalSteps - previousSteps).toInt()
                                    if (currentSteps > 0) {
                                        steps += currentSteps
                                        previousSteps = totalSteps
                                        uploadStepCount(steps)
                                        saveStepsLocally(steps)
                                        lastSynced = System.currentTimeMillis()
                                        Log.d("SensorLiveUpdate", "Steps updated: $steps")
                                    }
                                }
                            }

                            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                        }

                        sensorManager.registerListener(listener, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)

                        onDispose {
                            sensorManager.unregisterListener(listener)
                        }
                    }
                }
            }
        }
    }

    private fun checkActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            if (permissionGranted) {
                registerStepSensor()
            } else {
                requestPermission()
            }
        } else {
            permissionGranted = true
            registerStepSensor()
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    private fun registerStepSensor() {
        stepCounterSensor?.also { sensor ->
            sensorManager.registerListener(object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                        totalSteps = event.values[0]
                        Log.d("SensorInit", "Initial totalSteps: $totalSteps")
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun uploadStepCount(stepCount: Int) {
        val userId = "user123"

        val stepData = hashMapOf(
            "userId" to userId,
            "steps" to stepCount,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("stepCounts")
            .add(stepData)
            .addOnSuccessListener {
                Log.d("Firestore", "Step data uploaded: $stepCount")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to upload step data", e)
            }
    }

    private fun saveStepsLocally(stepCount: Int) {
        val sharedPref = getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putInt("last_step", stepCount).apply()
    }

    private fun getSavedSteps(): Int {
        val sharedPref = getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
        return sharedPref.getInt("last_step", 0)
    }
}

@Composable
fun StepCounterUI(stepCount: Int, lastSynced: Long) {
    val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSynced))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Step Counter App", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Steps taken: $stepCount", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Last synced: $formattedTime", fontSize = 14.sp)
    }
}

@Composable
fun StepContent(
    permissionGranted: Boolean,
    stepCounterSensor: Sensor?,
    steps: Int,
    lastSynced: Long,
    onGrantPermission: () -> Unit
) {
    if (permissionGranted && stepCounterSensor != null) {
        StepCounterUI(stepCount = steps, lastSynced = lastSynced)
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (stepCounterSensor == null) {
                Text(
                    text = "Step Counter Sensor not available on this device.",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Text(
                    text = "Please grant ACTIVITY_RECOGNITION permission",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = onGrantPermission) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
