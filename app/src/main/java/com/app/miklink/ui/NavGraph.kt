package com.app.miklink.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.miklink.ui.client.ClientEditScreen
import com.app.miklink.ui.client.ClientListScreen
import com.app.miklink.ui.dashboard.DashboardScreen
import com.app.miklink.ui.history.HistoryScreen
import com.app.miklink.ui.history.ReportDetailScreen
// import com.app.miklink.ui.probe.ProbeListScreen // DEPRECATO: sonda unica
import com.app.miklink.ui.probe.ProbeEditScreen
import com.app.miklink.ui.profile.TestProfileEditScreen
import com.app.miklink.ui.profile.TestProfileListScreen
import com.app.miklink.ui.settings.SettingsScreen
import com.app.miklink.ui.test.TestExecutionScreen
import com.app.miklink.ui.splash.SplashScreen
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        // Splash Screen
        composable("splash") { SplashScreen(navController) }

        // Main Screen
        composable("dashboard") { DashboardScreen(navController) }

        // Test Execution - use path-segments for robustness
        composable(
            route = "test_execution/{clientId}/{probeId}/{profileId}/{socketName}",
            arguments = listOf(
                navArgument("clientId") { type = NavType.LongType },
                navArgument("probeId") { type = NavType.LongType },
                navArgument("profileId") { type = NavType.LongType },
                navArgument("socketName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Scope ViewModel to this NavBackStackEntry to avoid stale shared state
            val vm: com.app.miklink.ui.test.TestViewModel = hiltViewModel(backStackEntry)
            TestExecutionScreen(navController, vm)
        }

        // History and Reports
        composable("history") { HistoryScreen(navController) }
        composable(
            route = "report_detail/{reportId}",
            arguments = listOf(navArgument("reportId") { type = NavType.LongType })
        ) { ReportDetailScreen(navController) }

        // Settings and Management Screens
        composable("settings") { SettingsScreen(navController) }
        
        // Probe Routes (DEPRECATO: multi-probe, mantenuto per compatibility)
        // composable("probe_list") { ProbeListScreen(navController) }
        composable("probe_add") { ProbeEditScreen(navController) } // CRASH FIX
        composable(
            route = "probe_edit/{probeId}",
            arguments = listOf(navArgument("probeId") { type = NavType.LongType; defaultValue = -1L })
        ) { ProbeEditScreen(navController) }

        // Profile Routes
        composable("profile_list") { TestProfileListScreen(navController) }
        composable("profile_add") { TestProfileEditScreen(navController) }
        composable(
            route = "profile_edit/{profileId}",
            arguments = listOf(navArgument("profileId") { type = NavType.LongType; defaultValue = -1L })
        ) { TestProfileEditScreen(navController) }

        // Client Routes
        composable("client_list") { ClientListScreen(navController) }
        composable("client_add") { ClientEditScreen(navController) }
        composable(
            route = "client_edit/{clientId}",
            arguments = listOf(navArgument("clientId") { type = NavType.LongType; defaultValue = -1L })
        ) { ClientEditScreen(navController) }
    }
}