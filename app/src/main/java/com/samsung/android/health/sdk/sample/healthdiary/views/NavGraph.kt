package com.samsung.android.health.sdk.sample.healthdiary.views

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.samsung.android.health.sdk.sample.healthdiary.repository.DocumentRepository
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Logs : Screen("logs")
    object TxAgent : Screen("txagent")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val documentRepository = DocumentRepository(context)

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
                onUploadPdf = { pdfLauncher.launch("application/pdf") }
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
    }
}
