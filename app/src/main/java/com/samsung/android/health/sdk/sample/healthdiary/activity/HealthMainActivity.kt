/*
 * Copyright (C) 2024 Samsung Electronics Co., Ltd. All rights reserved
 */
package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.view.LayoutInflater
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.databinding.HealthMainBinding
import com.samsung.android.health.sdk.sample.healthdiary.databinding.ItemHealthMetricCardBinding
import com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricCardUiState
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.resolveException
import com.samsung.android.health.sdk.sample.healthdiary.utils.showErrorToast
import com.samsung.android.health.sdk.sample.healthdiary.utils.showToast
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthMainViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthViewModelFactory
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.SyncViewModel
import com.samsung.android.health.sdk.sample.healthdiary.views.NavGraph
import com.samsung.android.sdk.health.data.helper.SdkVersion
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes
import kotlinx.coroutines.launch
import com.samsung.android.health.sdk.sample.healthdiary.utils.TelemetryLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.gson.Gson
import java.io.File

class HealthMainActivity : AppCompatActivity() {

    private lateinit var healthMainViewModel: HealthMainViewModel
    private lateinit var syncViewModel: SyncViewModel
    // private lateinit var binding: HealthMainBinding // Removed DataBinding
    private val debugGson = Gson()

    // #region agent log
    private fun debugLog(hypothesisId: String, message: String, data: Map<String, Any?> = emptyMap()) {
        try {
            val payload = mapOf(
                "sessionId" to "debug-session",
                "runId" to "pre-fix",
                "hypothesisId" to hypothesisId,
                "location" to "HealthMainActivity.kt",
                "message" to message,
                "data" to data,
                "timestamp" to System.currentTimeMillis()
            )
            File("/Users/teoechavarria/Documents/hh/.cursor/debug.log")
                .appendText(debugGson.toJson(payload) + "\n")
        } catch (_: Exception) {
            // best-effort logging only
        }
    }
    // #endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar autenticación antes de continuar
        // #region agent log
        debugLog("H1", "HealthMainActivity.onCreate entry")
        // #endregion
        TokenManager.initialize(this)
        // #region agent log
        debugLog("H1", "TokenManager initialized")
        // #endregion
        if (!TokenManager.hasToken()) {
            // #region agent log
            debugLog("H1", "Token missing, redirecting to LoginActivity")
            // #endregion
            // No hay token, navegar a LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        try {
            val factory = HealthViewModelFactory(this)
            healthMainViewModel = ViewModelProvider(this, factory)[HealthMainViewModel::class.java]
            syncViewModel = ViewModelProvider(this, factory)[SyncViewModel::class.java]
            // #region agent log
            debugLog("H2", "ViewModels initialized")
            // #endregion
        } catch (e: Exception) {
            // #region agent log
            debugLog("H2", "ViewModel init failed", mapOf("error" to e.javaClass.simpleName))
            // #endregion
            throw e
        }

        // Trigger flush of offline sensor data
        try {
            com.samsung.android.health.sdk.sample.healthdiary.data.repository.SensorRepository(applicationContext).scheduleUpload()
            // #region agent log
            debugLog("H3", "SensorRepository.scheduleUpload completed")
            // #endregion
        } catch (e: Exception) {
            // #region agent log
            debugLog("H3", "SensorRepository.scheduleUpload failed", mapOf("error" to e.javaClass.simpleName))
            // #endregion
            throw e
        }
        
        // Initialize upload resilience systems
        try {
            com.samsung.android.health.sdk.sample.healthdiary.worker.UploadHealthMonitor.initialize(applicationContext)
            com.samsung.android.health.sdk.sample.healthdiary.worker.UploadScheduler.ensurePeriodicSafetyNet(applicationContext)
            com.samsung.android.health.sdk.sample.healthdiary.worker.UploadWatchdog.start(applicationContext)
            Log.d("HealthMainActivity", "Upload resilience systems initialized")
            // #region agent log
            debugLog("H3", "Upload resilience systems initialized")
            // #endregion
        } catch (e: Exception) {
            // #region agent log
            debugLog("H3", "Upload resilience systems init failed", mapOf("error" to e.javaClass.simpleName))
            // #endregion
            throw e
        }
        
        // Initialize training reminder scheduler
        try {
            com.samsung.android.health.sdk.sample.healthdiary.training.TrainingReminderScheduler(applicationContext).scheduleAllReminders()
            Log.d("HealthMainActivity", "Training reminders scheduled")
            // #region agent log
            debugLog("H4", "Training reminders scheduled")
            // #endregion
        } catch (e: Exception) {
            // Do not crash the app if scheduling fails (e.g., missing exact alarm permission).
            debugLog("H4", "Training reminders scheduling failed", mapOf("error" to e.javaClass.simpleName))
            showToast(this, getString(R.string.training_reminder_permission_required))
        }

        // Start PhoneWearableService for persistent watch communication
        try {
            val serviceIntent = Intent(this, com.samsung.android.health.sdk.sample.healthdiary.wearable.PhoneWearableService::class.java)
            startForegroundService(serviceIntent)
            Log.d("HealthMainActivity", "PhoneWearableService started from MainActivity")
            // #region agent log
            debugLog("H4", "PhoneWearableService started")
            // #endregion
        } catch (e: Exception) {
            // #region agent log
            debugLog("H4", "PhoneWearableService start failed", mapOf("error" to e.javaClass.simpleName))
            // #endregion
            throw e
        }
        
        // Log app startup to TelemetryLogger
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date())
        TelemetryLogger.log(
            "PHONE",
            "App Started",
            "[$timestamp] Health Diary app initialized. Waiting for watch data. Upload interval: 3 minutes."
        )

        // Set Compose Content
        try {
            setContent {
                com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White
                    ) {
                        NavGraph()
                    }
                }
            }
            // #region agent log
            debugLog("H5", "setContent completed")
            // #endregion
        } catch (e: Exception) {
            // #region agent log
            debugLog("H5", "setContent failed", mapOf("error" to e.javaClass.simpleName))
            // #endregion
            throw e
        }

        /*
        // Old UI Setup
        binding = DataBindingUtil
            .setContentView(this, R.layout.health_main)
        binding.cvNutrition.setOnClickListener(this)
        binding.cvStep.setOnClickListener(this)
        binding.cvHeartRate.setOnClickListener(this)
        binding.cvSleepTotal.setOnClickListener(this)
        binding.versionValue.text = SdkVersion.getVersionName()
        */

        /** Show toast on exception occurrence **/

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                healthMainViewModel.exceptionResponse.collect { exception ->
                    if(exception.message != "Default"){
                        showErrorToast(this@HealthMainActivity, exception)
                        resolveException(exception, this@HealthMainActivity)
                    }
                }
            }
        }

        collectResponse()
        // observeMetricCards() // Disabled for now
        // observeTodaySteps()
        // observeTodaySleep()
        observeSyncState()
    }

    private fun checkBatteryOptimizations() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        healthMainViewModel.refreshMetricCards()
    }

    /*
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.connect_samsung_health, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.permission -> {
                showToast(this, "Conectando con Samsung Health...\nSolicitando permisos de acceso a datos de salud")
                healthMainViewModel.connectToSamsungHealth(this)
                true
            }
            
            R.id.sync -> {
                showSyncDialog()
                true
            }

            else -> false
        }
    */

    override fun onStop() {
        super.onStop()
        healthMainViewModel.setDefaultValueToExceptionResponse()
    }

    /*
    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.cv_nutrition -> {
                val permSet = mutableSetOf(
                    Permission.of(DataTypes.NUTRITION, AccessType.READ)
                )
                healthMainViewModel.checkForPermission(
                    this,
                    permSet,
                    AppConstants.NUTRITION_ACTIVITY
                )
            }
            // ... other cases
        }
    }
    */

    private fun launchRespectiveActivity(activityId: Int) {
        val intent = when (activityId) {
            AppConstants.NUTRITION_ACTIVITY -> Intent(this, NutritionActivity::class.java)
            AppConstants.STEP_ACTIVITY -> Intent(this, StepActivity::class.java)
            AppConstants.HEART_RATE_ACTIVITY -> Intent(this, HeartRateActivity::class.java)
            AppConstants.SLEEP_ACTIVITY -> Intent(this, SleepActivity::class.java)
            AppConstants.EXERCISE_ACTIVITY -> Intent(this, ExerciseActivity::class.java)
            AppConstants.EXERCISE_LOCATION_ACTIVITY -> Intent(this, ExerciseLocationActivity::class.java)
            AppConstants.SKIN_TEMPERATURE_ACTIVITY -> Intent(this, SkinTemperatureActivity::class.java)
            AppConstants.BLOOD_OXYGEN_ACTIVITY -> Intent(this, BloodOxygenActivity::class.java)
            AppConstants.ACTIVITY_SUMMARY_ACTIVITY -> Intent(this, ActivitySummaryActivity::class.java)
            AppConstants.FLOORS_CLIMBED_ACTIVITY -> Intent(this, FloorsClimbedActivity::class.java)
            AppConstants.BLOOD_GLUCOSE_ACTIVITY -> Intent(this, BloodGlucoseActivity::class.java)
            AppConstants.BLOOD_PRESSURE_ACTIVITY -> Intent(this, BloodPressureActivity::class.java)
            AppConstants.BODY_COMPOSITION_ACTIVITY -> Intent(this, BodyCompositionActivity::class.java)
            AppConstants.SLEEP_GOAL_ACTIVITY -> Intent(this, SleepGoalActivity::class.java)
            AppConstants.STEPS_GOAL_ACTIVITY -> Intent(this, StepsGoalActivity::class.java)
            AppConstants.ACTIVE_CALORIES_GOAL_ACTIVITY -> Intent(this, ActiveCaloriesGoalActivity::class.java)
            AppConstants.ACTIVE_TIME_GOAL_ACTIVITY -> Intent(this, ActiveTimeGoalActivity::class.java)
            AppConstants.WATER_INTAKE_ACTIVITY -> Intent(this, WaterIntakeActivity::class.java)
            AppConstants.WATER_INTAKE_GOAL_ACTIVITY -> Intent(this, WaterIntakeGoalActivity::class.java)
            AppConstants.NUTRITION_GOAL_ACTIVITY -> Intent(this, NutritionGoalActivity::class.java)
            AppConstants.ENERGY_SCORE_ACTIVITY -> Intent(this, EnergyScoreActivity::class.java)
            AppConstants.USER_PROFILE_ACTIVITY -> Intent(this, UserProfileActivity::class.java)
            AppConstants.BODY_TEMPERATURE_ACTIVITY -> Intent(this, BodyTemperatureActivity::class.java)
            else -> null
        }
        if (intent != null) {
            startActivity(intent)
        }
    }
    
    private fun showSyncDialog() {
        val options = arrayOf(
            getString(R.string.sync_all),
            getString(R.string.sync_health),
            getString(R.string.sync_food)
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.sync_data))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> syncViewModel.syncAll()
                    1 -> syncViewModel.syncHealth()
                    2 -> syncViewModel.syncFood()
                }
            }
            .setNegativeButton(getString(R.string.ok), null)
            .show()
    }
    
    private fun observeSyncState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncViewModel.syncState.collect { state ->
                    when (state) {
                        is SyncViewModel.SyncState.Idle -> {
                            // Do nothing
                        }
                        is SyncViewModel.SyncState.Loading -> {
                            showToast(this@HealthMainActivity, getString(R.string.sync_in_progress))
                        }
                        is SyncViewModel.SyncState.Success -> {
                            val response = state.response
                            val message = buildString {
                                append(getString(R.string.sync_success))
                                append("\n")
                                append(getString(R.string.sync_items_health, response.syncedItems.health))
                                append("\n")
                                append(getString(R.string.sync_items_food, response.syncedItems.food))
                            }
                            showToast(this@HealthMainActivity, message)
                            syncViewModel.resetState()
                        }
                        is SyncViewModel.SyncState.Error -> {
                            val errorMsg = state.message
                            showToast(this@HealthMainActivity, "${getString(R.string.sync_error)}: $errorMsg")
                            syncViewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun collectResponse() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                /**  Handle response of permission request */
                healthMainViewModel.permissionResponse.collect { result ->
                    if (result.first == AppConstants.SUCCESS) {
                        launchRespectiveActivity(result.second)
                    } else if (result.first != AppConstants.WAITING) {
                        showToast(this@HealthMainActivity, result.first)
                    }
                    healthMainViewModel.resetPermissionResponse()
                }
            }
        }
    }
}
