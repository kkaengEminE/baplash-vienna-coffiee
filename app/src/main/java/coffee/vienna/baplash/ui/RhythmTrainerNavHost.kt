package coffee.vienna.baplash.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coffee.vienna.baplash.ui.calibration.CalibrationScreen
import coffee.vienna.baplash.ui.history.HistoryScreen
import coffee.vienna.baplash.ui.home.HomeScreen
import coffee.vienna.baplash.ui.metronome.MetronomeScreen
import coffee.vienna.baplash.ui.practice.PracticeScreen
import coffee.vienna.baplash.ui.score.RhythmPatternEditorScreen
import coffee.vienna.baplash.ui.score.ScoreListScreen
import coffee.vienna.baplash.ui.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "홈", Icons.Default.Home)
    data object Scores : Screen("scores", "악보", Icons.Default.LibraryMusic)
    data object History : Screen("history", "기록", Icons.Default.BarChart)
    data object Settings : Screen("settings", "설정", Icons.Default.Settings)
    data object Practice : Screen("practice", "연습")
    data object Metronome : Screen("metronome", "메트로놈")
    data object PatternEditor : Screen("pattern_editor", "패턴 만들기")
    data object Calibration : Screen("calibration", "캘리브레이션")
}

private val bottomNavItems = listOf(Screen.Home, Screen.Scores, Screen.History, Screen.Settings)

@Composable
fun RhythmTrainerNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToPractice = { navController.navigate(Screen.Practice.route) },
                    onNavigateToMetronome = { navController.navigate(Screen.Metronome.route) },
                    onNavigateToScores = { navController.navigate(Screen.Scores.route) },
                    onNavigateToCalibration = { navController.navigate(Screen.Calibration.route) }
                )
            }
            composable(Screen.Scores.route) {
                ScoreListScreen(
                    onNavigateToEditor = { navController.navigate(Screen.PatternEditor.route) },
                    onNavigateToPractice = { navController.navigate(Screen.Practice.route) }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToCalibration = { navController.navigate(Screen.Calibration.route) }
                )
            }
            composable(Screen.Practice.route) {
                PracticeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Metronome.route) {
                MetronomeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.PatternEditor.route) {
                RhythmPatternEditorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Calibration.route) {
                CalibrationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
