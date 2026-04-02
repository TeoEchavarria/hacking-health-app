package com.samsung.android.health.sdk.sample.healthdiary.views

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.remember
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.samsung.android.health.sdk.sample.healthdiary.activity.LoginActivity
import com.samsung.android.health.sdk.sample.healthdiary.repository.DocumentRepository
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Profile : Screen("profile")
    object LegacyHome : Screen("legacy_home")
    object PreviousVersion : Screen("previous_version")
    object Logs : Screen("logs")
    object TxAgent : Screen("txagent")
    object Settings : Screen("settings")
    object Training : Screen("training")
    object Habits : Screen("habits")
    object HeartRateHistory : Screen("heart_rate_history")
    object StepsHistory : Screen("steps_history")
    object SleepHistory : Screen("sleep_history")
    object OpenWearables : Screen("open_wearables")
    data class WorkoutPlayer(val routineId: String? = null, val sessionId: String? = null) : Screen("workout_player?routineId=$routineId&sessionId=$sessionId")
    data class RoutineEditor(val routineId: String? = null) : Screen("routine_editor/${routineId ?: "new"}")
    data class HabitEditor(val habitId: String? = null) : Screen("habit_editor/${habitId ?: "new"}")
    object SandboxGallery : Screen("sandbox_gallery")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val documentRepository = DocumentRepository(context)
    
    // Check for deep-link intent (e.g., from notification)
    val activity = remember { 
        (context as? androidx.activity.ComponentActivity)
    }
    LaunchedEffect(Unit) {
        activity?.intent?.let { intent ->
            if (intent.getBooleanExtra("open_training", false)) {
                // If a specific routine ID is provided, navigate directly to player
                val routineId = intent.getStringExtra("open_routine_id")
                if (!routineId.isNullOrBlank()) {
                    navController.navigate("workout_player?routineId=$routineId")
                } else {
                    navController.navigate(Screen.Training.route)
                }
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                documentRepository.saveDocument(it)
                // Optionally navigate to logs or show snackbar
                navController.navigate(Screen.Logs.route)
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            MainScaffold(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToTraining = { navController.navigate(Screen.Training.route) },
                onNavigateToHabits = { navController.navigate(Screen.Habits.route) },
                onNavigateToHeartRateHistory = { navController.navigate(Screen.HeartRateHistory.route) },
                onNavigateToStepsHistory = { navController.navigate(Screen.StepsHistory.route) },
                onNavigateToSleepHistory = { navController.navigate(Screen.SleepHistory.route) },
                onLogout = {
                    // Navigate to LoginActivity and clear task stack
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    // Finish current activity
                    activity?.finish()
                }
            )
        }
        
        // Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToNotifications = { 
                    // TODO: Navigate to notifications screen when implemented
                },
                onNavigateToSecurity = { 
                    // TODO: Navigate to security screen when implemented
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Previous Version - Old DeviceHomeScreen for Settings access
        composable(Screen.PreviousVersion.route) {
            PreviousVersionScreen(
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                onNavigateToTxAgent = { navController.navigate(Screen.TxAgent.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToTraining = { navController.navigate(Screen.Training.route) },
                onNavigateToHabits = { navController.navigate(Screen.Habits.route) },
                onUploadPdf = { pdfLauncher.launch("application/pdf") },
                onLogout = {
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    activity?.finish()
                },
                onReturnToNewVersion = { navController.popBackStack() },
                onNavigateToSandboxGallery = if (com.samsung.android.health.sdk.sample.healthdiary.BuildConfig.DEBUG) {
                    { navController.navigate(Screen.SandboxGallery.route) }
                } else null
            )
        }
        
        composable(Screen.LegacyHome.route) {
            LegacyHomeScreen(
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                onNavigateToTxAgent = { navController.navigate(Screen.TxAgent.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToTraining = { navController.navigate(Screen.Training.route) },
                onNavigateToHabits = { navController.navigate(Screen.Habits.route) },
                onUploadPdf = { pdfLauncher.launch("application/pdf") },
                onReturnToNewVersion = { navController.popBackStack() },
                onLogout = {
                    // Navigate to LoginActivity and clear task stack
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    // Finish current activity
                    activity?.finish()
                },
                onNavigateToSandboxGallery = if (com.samsung.android.health.sdk.sample.healthdiary.BuildConfig.DEBUG) {
                    { navController.navigate(Screen.SandboxGallery.route) }
                } else null
            )
        }
        
        composable(Screen.Logs.route) {
            LogsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.TxAgent.route) {
            TxAgentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLegacyHome = { navController.navigate(Screen.PreviousVersion.route) },
                onNavigateToOpenWearables = { navController.navigate(Screen.OpenWearables.route) }
            )
        }
        
        // OpenWearables Health Connect Screen
        composable(Screen.OpenWearables.route) {
            OpenWearablesScreen()
        }
        
        // Health Metric History Screens
        composable(Screen.HeartRateHistory.route) {
            HeartRateHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.StepsHistory.route) {
            StepsHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.SleepHistory.route) {
            SleepHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Training.route) {
            TrainingSessionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { sessionId ->
                    navController.navigate("workout_player?sessionId=$sessionId")
                }
            )
        }
        composable(Screen.Habits.route) {
            HabitListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { habitId ->
                    navController.navigate(Screen.HabitEditor(habitId).route)
                }
            )
        }
        composable("habit_editor/{habitId}") { backStackEntry ->
            val hid = backStackEntry.arguments?.getString("habitId")
            HabitEditorScreen(
                habitId = if (hid == "new") null else hid,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "workout_player?routineId={routineId}&sessionId={sessionId}",
            arguments = listOf(
                navArgument("routineId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("sessionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val rid = backStackEntry.arguments?.getString("routineId")
            val sid = backStackEntry.arguments?.getString("sessionId")
            val context = LocalContext.current
            WorkoutPlayerScreen(
                routineId = rid,
                sessionId = sid,
                onNavigateBack = { navController.popBackStack() },
                onFinishSession = {
                    com.samsung.android.health.sdk.sample.healthdiary.training.TrainingStateManager(context).apply {
                        setActiveBlock(null)
                        setActiveWorkoutSession(null)
                    }
                }
            )
        }
        composable("routine_editor/{routineId}") { backStackEntry ->
            val rid = backStackEntry.arguments?.getString("routineId")
            RoutineEditorScreen(
                routineId = if (rid == "new") null else rid,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Sandbox Gallery - Debug only (route always registered for navigation stability)
        composable(Screen.SandboxGallery.route) {
            if (com.samsung.android.health.sdk.sample.healthdiary.BuildConfig.DEBUG) {
                SandboxGalleryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                // In release builds, navigate back if someone somehow reaches this route
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}
