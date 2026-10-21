package com.medcounter.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medcounter.MedCounterApplication
import com.medcounter.data.AppContainer
import com.medcounter.ui.screens.about.AboutScreen
import com.medcounter.ui.screens.addedit.AddEditMedicationScreen
import com.medcounter.ui.screens.addedit.AddEditMedicationViewModel
import com.medcounter.ui.screens.dashboard.DashboardScreen
import com.medcounter.ui.screens.dashboard.DashboardViewModel
import com.medcounter.ui.screens.detail.MedicationDetailScreen
import com.medcounter.ui.screens.detail.MedicationDetailViewModel
import com.medcounter.ui.screens.onboarding.OnboardingScreen
import com.medcounter.ui.screens.profiles.ProfilesScreen
import com.medcounter.ui.screens.profiles.ProfilesViewModel
import com.medcounter.ui.screens.settings.SettingsScreen
import com.medcounter.ui.screens.settings.SettingsViewModel

@Composable
fun AppNavigation(container: AppContainer) {
    val nav = rememberNavController()
    val start = if (container.preferences.hasCompletedOnboarding) "dashboard" else "onboarding"
    val app = LocalContext.current.applicationContext as MedCounterApplication
    NavHost(navController = nav, startDestination = start) {
        composable("onboarding") {
            OnboardingScreen(
                preferences = container.preferences,
                onFinished = {
                    nav.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
            )
        }
        composable("dashboard") {
            val vm: DashboardViewModel = viewModel(factory = simpleFactory { DashboardViewModel(container) })
            DashboardScreen(
                viewModel = vm,
                onAddMedication = { profileId ->
                    nav.navigate("medication/-1/$profileId")
                },
                onOpenMedication = { id -> nav.navigate("detail/$id") },
                onOpenProfiles = { nav.navigate("profiles") },
                onOpenSettings = { nav.navigate("settings") },
            )
        }
        composable(
            "detail/{medicationId}",
            arguments = listOf(navArgument("medicationId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("medicationId") ?: return@composable
            val vm: MedicationDetailViewModel = viewModel(
                factory = simpleFactory { MedicationDetailViewModel(container, id) },
            )
            MedicationDetailScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onEdit = { medId, profileId -> nav.navigate("medication/$medId/$profileId") },
            )
        }
        composable(
            "medication/{medicationId}/{profileId}",
            arguments = listOf(
                navArgument("medicationId") { type = NavType.LongType },
                navArgument("profileId") { type = NavType.LongType },
            ),
        ) { entry ->
            val medicationId = entry.arguments?.getLong("medicationId") ?: -1L
            val profileId = entry.arguments?.getLong("profileId") ?: -1L
            val vm: AddEditMedicationViewModel = viewModel(
                factory = simpleFactory { AddEditMedicationViewModel(container, medicationId, profileId) },
            )
            AddEditMedicationScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
            )
        }
        composable("settings") {
            val vm: SettingsViewModel = viewModel(factory = simpleFactory { SettingsViewModel(app) })
            SettingsScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onOpenAbout = { nav.navigate("about") },
            )
        }
        composable("about") {
            AboutScreen(onNavigateBack = { nav.popBackStack() })
        }
        composable("profiles") {
            val vm: ProfilesViewModel = viewModel(factory = simpleFactory { ProfilesViewModel(container) })
            ProfilesScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onProfileSelected = { nav.popBackStack() },
            )
        }
    }
}

private fun <T : ViewModel> simpleFactory(create: () -> T): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
    }
}
