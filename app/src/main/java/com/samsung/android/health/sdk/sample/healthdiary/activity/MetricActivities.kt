package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.api.models.UserProfileData
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.UserProfileHelper
import com.samsung.android.health.sdk.sample.healthdiary.utils.showToast
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.ActiveCaloriesGoalViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.ActiveTimeGoalViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.ActivitySummaryViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.AuthViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.BloodGlucoseViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.BloodOxygenDetailViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.BloodPressureDetailViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.BodyCompositionViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.BodyTemperatureViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.EnergyScoreViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.ExerciseLocationViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.ExerciseViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.FloorsClimbedViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthViewModelFactory
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.NutritionGoalViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.SkinTemperatureDetailViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.SleepGoalViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.StepsGoalViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.UserProfileViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.WaterIntakeGoalViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.WaterIntakeViewModel
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.data.UserDataPoint
import com.samsung.android.sdk.health.data.request.DataTypes
import kotlinx.coroutines.launch

class ExerciseActivity : BaseHealthDataActivity<ExerciseViewModel>() {
    override val viewModelClass = ExerciseViewModel::class.java
    override val titleRes: Int = R.string.exercise
    override val iconRes: Int = R.drawable.ic_health_metric
}

class ExerciseLocationActivity : BaseHealthDataActivity<ExerciseLocationViewModel>() {
    override val viewModelClass = ExerciseLocationViewModel::class.java
    override val titleRes: Int = R.string.exercise_location
    override val iconRes: Int = R.drawable.ic_health_metric
}

class SkinTemperatureActivity : BaseHealthDataActivity<SkinTemperatureDetailViewModel>() {
    override val viewModelClass = SkinTemperatureDetailViewModel::class.java
    override val titleRes: Int = R.string.skin_temperature
    override val iconRes: Int = R.drawable.ic_health_metric
}

class BloodOxygenActivity : BaseHealthDataActivity<BloodOxygenDetailViewModel>() {
    override val viewModelClass = BloodOxygenDetailViewModel::class.java
    override val titleRes: Int = R.string.blood_oxygen
    override val iconRes: Int = R.drawable.ic_health_metric
}

class ActivitySummaryActivity : BaseHealthDataActivity<ActivitySummaryViewModel>() {
    override val viewModelClass = ActivitySummaryViewModel::class.java
    override val titleRes: Int = R.string.activity_summary
    override val iconRes: Int = R.drawable.ic_health_metric
}

class FloorsClimbedActivity : BaseHealthDataActivity<FloorsClimbedViewModel>() {
    override val viewModelClass = FloorsClimbedViewModel::class.java
    override val titleRes: Int = R.string.floors_climbed
    override val iconRes: Int = R.drawable.ic_health_metric
}

class BloodGlucoseActivity : BaseHealthDataActivity<BloodGlucoseViewModel>() {
    override val viewModelClass = BloodGlucoseViewModel::class.java
    override val titleRes: Int = R.string.blood_glucose
    override val iconRes: Int = R.drawable.ic_health_metric
}

class BloodPressureActivity : BaseHealthDataActivity<BloodPressureDetailViewModel>() {
    override val viewModelClass = BloodPressureDetailViewModel::class.java
    override val titleRes: Int = R.string.blood_pressure
    override val iconRes: Int = R.drawable.ic_health_metric
}

class BodyCompositionActivity : BaseHealthDataActivity<BodyCompositionViewModel>() {
    override val viewModelClass = BodyCompositionViewModel::class.java
    override val titleRes: Int = R.string.body_composition
    override val iconRes: Int = R.drawable.ic_health_metric
}

class SleepGoalActivity : BaseHealthDataActivity<SleepGoalViewModel>() {
    override val viewModelClass = SleepGoalViewModel::class.java
    override val titleRes: Int = R.string.sleep_goal
    override val iconRes: Int = R.drawable.ic_health_metric
}

class StepsGoalActivity : BaseHealthDataActivity<StepsGoalViewModel>() {
    override val viewModelClass = StepsGoalViewModel::class.java
    override val titleRes: Int = R.string.steps_goal
    override val iconRes: Int = R.drawable.ic_health_metric
}

class ActiveCaloriesGoalActivity : BaseHealthDataActivity<ActiveCaloriesGoalViewModel>() {
    override val viewModelClass = ActiveCaloriesGoalViewModel::class.java
    override val titleRes: Int = R.string.active_calories_goal
    override val iconRes: Int = R.drawable.ic_health_metric
}

class ActiveTimeGoalActivity : BaseHealthDataActivity<ActiveTimeGoalViewModel>() {
    override val viewModelClass = ActiveTimeGoalViewModel::class.java
    override val titleRes: Int = R.string.active_time_goal
    override val iconRes: Int = R.drawable.ic_health_metric
}

class WaterIntakeActivity : BaseHealthDataActivity<WaterIntakeViewModel>() {
    override val viewModelClass = WaterIntakeViewModel::class.java
    override val titleRes: Int = R.string.water_intake
    override val iconRes: Int = R.drawable.ic_health_metric
}

class WaterIntakeGoalActivity : BaseHealthDataActivity<WaterIntakeGoalViewModel>() {
    override val viewModelClass = WaterIntakeGoalViewModel::class.java
    override val titleRes: Int = R.string.water_intake_goal
    override val iconRes: Int = R.drawable.ic_health_metric
}

class NutritionGoalActivity : BaseHealthDataActivity<NutritionGoalViewModel>() {
    override val viewModelClass = NutritionGoalViewModel::class.java
    override val titleRes: Int = R.string.nutrition_goal
    override val iconRes: Int = R.drawable.ic_health_metric
}

class EnergyScoreActivity : BaseHealthDataActivity<EnergyScoreViewModel>() {
    override val viewModelClass = EnergyScoreViewModel::class.java
    override val titleRes: Int = R.string.energy_score
    override val iconRes: Int = R.drawable.ic_health_metric
}

class UserProfileActivity : BaseHealthDataActivity<UserProfileViewModel>() {
    override val viewModelClass = UserProfileViewModel::class.java
    override val titleRes: Int = R.string.user_profile
    override val iconRes: Int = R.drawable.ic_health_metric
    
    private lateinit var authViewModel: AuthViewModel
    private var hasAttemptedAutoLogin = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val factory = HealthViewModelFactory(this)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]
        
        TokenManager.initialize(this)
        observeAuthState()
    }
    
    override fun observeViewModel() {
        super.observeViewModel()
        
        // Observar los records del perfil para detectar cuando hay datos
        viewModel.records.observe(this) { records ->
            if (records.isNotEmpty() && !hasAttemptedAutoLogin && !TokenManager.hasToken()) {
                hasAttemptedAutoLogin = true
                attemptAutoLogin(records)
            }
        }
    }
    
    private fun observeAuthState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    when (state) {
                        is AuthViewModel.AuthState.Idle -> {
                            // Do nothing
                        }
                        is AuthViewModel.AuthState.Loading -> {
                            // Puedes mostrar un indicador de carga si lo deseas
                        }
                        is AuthViewModel.AuthState.Success -> {
                            showToast(this@UserProfileActivity, getString(R.string.login_success))
                            authViewModel.resetState()
                        }
                        is AuthViewModel.AuthState.Error -> {
                            // Mostrar error solo si no es un error silencioso
                            if (!state.message.contains("Network", ignoreCase = true)) {
                                // No mostrar error para evitar spam, el usuario puede intentar manualmente
                            }
                            authViewModel.resetState()
                        }
                    }
                }
            }
        }
    }
    
    private fun attemptAutoLogin(records: List<com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricRecord>) {
        lifecycleScope.launch {
            try {
                // Leer el UserDataPoint directamente desde el HealthDataStore
                // USER_PROFILE uses a simple builder (UserDataPoint doesn't support date filters)
                @Suppress("UNCHECKED_CAST")
                val readRequest = DataTypes.USER_PROFILE.readDataRequestBuilder
                    .build() as com.samsung.android.sdk.health.data.request.ReadDataRequest<com.samsung.android.sdk.health.data.data.UserDataPoint>
                
                val healthDataStore = HealthDataService.getStore(this@UserProfileActivity)
                val dataList = healthDataStore.readData(readRequest).dataList
                val userDataPoint = dataList.firstOrNull() as? UserDataPoint
                
                if (userDataPoint != null) {
                    val profileData = UserProfileHelper.extractProfileData(userDataPoint)
                    if (profileData != null && UserProfileHelper.hasMinimumData(profileData)) {
                        // Intentar login automático
                        authViewModel.loginWithProfile(profileData)
                    } else {
                        showToast(this@UserProfileActivity, getString(R.string.profile_incomplete))
                    }
                }
            } catch (e: Exception) {
                // Silenciar errores de lectura, el usuario puede hacer login manualmente
            }
        }
    }
}

class BodyTemperatureActivity : BaseHealthDataActivity<BodyTemperatureViewModel>() {
    override val viewModelClass = BodyTemperatureViewModel::class.java
    override val titleRes: Int = R.string.body_temperature
    override val iconRes: Int = R.drawable.ic_health_metric
}



