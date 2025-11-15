/*
 * Copyright (C) 2024 Samsung Electronics Co., Ltd. All rights reserved
 */
package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
import com.samsung.android.health.sdk.sample.healthdiary.utils.resolveException
import com.samsung.android.health.sdk.sample.healthdiary.utils.showErrorToast
import com.samsung.android.health.sdk.sample.healthdiary.utils.showToast
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthMainViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthViewModelFactory
import com.samsung.android.sdk.health.data.helper.SdkVersion
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes
import kotlinx.coroutines.launch

class HealthMainActivity : AppCompatActivity(),
    View.OnClickListener {

    private lateinit var healthMainViewModel: HealthMainViewModel
    private lateinit var binding: HealthMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthMainViewModel = ViewModelProvider(
            this, HealthViewModelFactory(this)
        )[HealthMainViewModel::class.java]

        /**  Initialize OnClickListener on Heart Rate, Sleep, Nutrition and Step buttons and set sdk version */
        binding = DataBindingUtil
            .setContentView(this, R.layout.health_main)
        binding.cvNutrition.setOnClickListener(this)
        binding.cvStep.setOnClickListener(this)
        binding.cvHeartRate.setOnClickListener(this)
        binding.cvSleepTotal.setOnClickListener(this)
        binding.versionValue.text = SdkVersion.getVersionName()

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
        observeMetricCards()
        observeTodaySteps()
        observeTodaySleep()
    }

    override fun onResume() {
        super.onResume()
        healthMainViewModel.refreshMetricCards()
    }

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

            else -> false
        }

    override fun onStop() {
        super.onStop()
        healthMainViewModel.setDefaultValueToExceptionResponse()
    }

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

            R.id.cv_step -> {
                val permSet = mutableSetOf(
                    Permission.of(DataTypes.STEPS, AccessType.READ)
                )
                healthMainViewModel.checkForPermission(this, permSet, AppConstants.STEP_ACTIVITY)
            }

            R.id.cv_heart_rate -> {
                val permSet = mutableSetOf(
                    Permission.of(DataTypes.HEART_RATE, AccessType.READ)
                )
                healthMainViewModel.checkForPermission(
                    this,
                    permSet,
                    AppConstants.HEART_RATE_ACTIVITY
                )
            }

            R.id.cv_sleep_total -> {
                val permSet = mutableSetOf(
                    Permission.of(DataTypes.SLEEP, AccessType.READ),
                    Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ),
                    Permission.of(DataTypes.SKIN_TEMPERATURE, AccessType.READ)
                )
                healthMainViewModel.checkForPermission(this, permSet, AppConstants.SLEEP_ACTIVITY)
            }
        }
    }

    private fun onCardClicked(uiState: HealthMetricCardUiState) {
        val permSet = uiState.definition.permissions.toMutableSet()
        if (permSet.isEmpty()) {
            launchRespectiveActivity(uiState.definition.activityId)
        } else {
            healthMainViewModel.checkForPermission(
                this,
                permSet,
                uiState.definition.activityId
            )
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

    private fun observeMetricCards() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                healthMainViewModel.metricCards.collect { cards ->
                    updateMetricCards(cards)
                }
            }
        }
    }

    private fun observeTodaySteps() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                healthMainViewModel.todayStepsTotal.collect { totalSteps ->
                    binding.stepsValue.text = totalSteps
                }
            }
        }
    }

    private fun observeTodaySleep() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                healthMainViewModel.todaySleepTotal.collect { totalSleep ->
                    binding.sleepValue.text = totalSleep
                }
            }
        }
    }

    private fun updateMetricCards(cards: List<HealthMetricCardUiState>) {
        binding.additionalMetricsContainer.removeAllViews()
        
        cards.forEach { uiState ->
            val cardBinding = ItemHealthMetricCardBinding.inflate(
                LayoutInflater.from(this),
                binding.additionalMetricsContainer,
                false
            )
            
            cardBinding.metricTitle.setText(uiState.definition.titleRes)
            cardBinding.metricIcon.setImageResource(uiState.definition.iconRes)
            cardBinding.metricValue.text = if (uiState.isLoading) {
                getString(R.string.loading_value)
            } else {
                uiState.latestValue
            }
            
            cardBinding.root.setOnClickListener {
                onCardClicked(uiState)
            }
            
            binding.additionalMetricsContainer.addView(cardBinding.root)
        }
    }

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
}
