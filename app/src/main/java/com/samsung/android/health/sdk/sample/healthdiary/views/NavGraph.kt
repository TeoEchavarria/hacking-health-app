package com.samsung.android.health.sdk.sample.healthdiary.views

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
import com.samsung.android.health.sdk.sample.healthdiary.repository.DocumentRepository
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Logs : Screen("logs")
    object TxAgent : Screen("txagent")
    object Settings : Screen("settings")
    object Training : Screen("training")
    object Habits : Screen("habits")
    data class WorkoutPlayer(val routineId: String? = null) : Screen("workout_player?routineId={routineId}")
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
                navController.navigate(Screen.Training.route)
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
            HomeScreen(
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                onNavigateToTxAgent = { navController.navigate(Screen.TxAgent.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToTraining = { navController.navigate(Screen.Training.route) },
                onNavigateToHabits = { navController.navigate(Screen.Habits.route) },
                onUploadPdf = { pdfLauncher.launch("application/pdf") },
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
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Training.route) {
            RoutineListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { routineId ->
                    navController.navigate(Screen.RoutineEditor(routineId).route)
                },
                onNavigateToPlayer = { routineId ->
                    navController.navigate("workout_player?routineId=$routineId")
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
            route = "workout_player?routineId={routineId}",
            arguments = listOf(navArgument("routineId") { 
                type = NavType.StringType
                nullable = true 
            })
        ) { backStackEntry ->
            val rid = backStackEntry.arguments?.getString("routineId")
            WorkoutPlayerScreen(
                routineId = rid,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("routine_editor/{routineId}") { backStackEntry ->
            val rid = backStackEntry.arguments?.getString("routineId")
            RoutineEditorScreen(
                routineId = if (rid == "new") null else rid,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Sandbox Gallery - Debug only
        if (com.samsung.android.health.sdk.sample.healthdiary.BuildConfig.DEBUG) {
            composable(Screen.SandboxGallery.route) {
                SandboxGalleryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
