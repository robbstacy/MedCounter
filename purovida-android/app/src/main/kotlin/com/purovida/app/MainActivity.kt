package com.purovida.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.purovida.app.ui.PuroColors
import com.purovida.app.ui.PuroVidaTheme
import com.purovida.app.ui.screens.AiAssistantScreen
import com.purovida.app.ui.screens.DiscoverScreen
import com.purovida.app.ui.screens.HomeScreen
import com.purovida.app.ui.screens.HumidorAddScreen
import com.purovida.app.ui.screens.HumidorScanScreen
import com.purovida.app.ui.screens.HumidorScreen
import com.purovida.app.ui.screens.JournalAddScreen
import com.purovida.app.ui.screens.JournalDetailScreen
import com.purovida.app.ui.screens.JournalScreen
import com.purovida.app.ui.screens.ProfileEditScreen
import com.purovida.app.ui.screens.ProfileScreen

val LocalApp = staticCompositionLocalOf<PuroVidaApplication> {
    error("PuroVidaApplication not provided")
}

object Routes {
    const val SHELL = "shell"
    const val JOURNAL_ADD = "journal_add"
    const val JOURNAL_DETAIL = "journal_detail/{entryId}"
    const val HUMIDOR_ADD = "humidor_add"
    const val HUMIDOR_SCAN = "humidor_scan"
    const val PROFILE_EDIT = "profile_edit"
    const val ASSISTANT = "assistant"

    fun journalDetail(entryId: String) = "journal_detail/$entryId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PuroVidaApplication
        setContent {
            CompositionLocalProvider(LocalApp provides app) {
                PuroVidaTheme {
                    PuroVidaNavHost()
                }
            }
        }
    }
}

@Composable
fun PuroVidaNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SHELL) {
        composable(Routes.SHELL) { MainShell(navController) }
        composable(Routes.JOURNAL_ADD) { JournalAddScreen(navController) }
        composable(Routes.JOURNAL_DETAIL) { backStackEntry ->
            JournalDetailScreen(
                navController,
                entryId = backStackEntry.arguments?.getString("entryId") ?: "",
            )
        }
        composable(Routes.HUMIDOR_ADD) { HumidorAddScreen(navController) }
        composable(Routes.HUMIDOR_SCAN) { HumidorScanScreen(navController) }
        composable(Routes.PROFILE_EDIT) { ProfileEditScreen(navController) }
        composable(Routes.ASSISTANT) { AiAssistantScreen(navController) }
    }
}

private data class TabSpec(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
fun MainShell(navController: NavHostController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = listOf(
        TabSpec("Home", Icons.Outlined.Home, Icons.Filled.Home),
        TabSpec("Journal", Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
        TabSpec("Humidor", Icons.Outlined.Inventory2, Icons.Filled.Inventory2),
        TabSpec("Discover", Icons.Outlined.Explore, Icons.Filled.Explore),
        TabSpec("Profile", Icons.Outlined.Person, Icons.Filled.Person),
    )

    Scaffold(
        containerColor = PuroColors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = PuroColors.tabBar) {
                tabs.forEachIndexed { i, spec ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            Icon(
                                if (tab == i) spec.selectedIcon else spec.icon,
                                contentDescription = spec.label,
                            )
                        },
                        label = { Text(spec.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PuroColors.primary,
                            selectedTextColor = PuroColors.primary,
                            unselectedIconColor = PuroColors.mutedForeground,
                            unselectedTextColor = PuroColors.mutedForeground,
                            indicatorColor = PuroColors.primary.copy(alpha = 0.16f),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (tab) {
            0 -> HomeScreen(navController, contentModifier)
            1 -> JournalScreen(navController, contentModifier)
            2 -> HumidorScreen(navController, contentModifier)
            3 -> DiscoverScreen(navController, contentModifier)
            4 -> ProfileScreen(navController, contentModifier)
        }
    }
}
