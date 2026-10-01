package com.example.mediaflow.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.mediaflow.di.AppContainer
import com.example.mediaflow.presentation.analyzer.AnalyzerScreen
import com.example.mediaflow.presentation.analyzer.AnalyzerViewModel
import com.example.mediaflow.presentation.downloads.DownloadsScreen
import com.example.mediaflow.presentation.downloads.DownloadsViewModel
import com.example.mediaflow.presentation.history.HistoryScreen
import com.example.mediaflow.presentation.history.HistoryViewModel
import com.example.mediaflow.presentation.home.HomeScreen
import com.example.mediaflow.presentation.home.HomeViewModel
import com.example.mediaflow.presentation.library.LibraryScreen
import com.example.mediaflow.presentation.library.LibraryViewModel
import com.example.mediaflow.presentation.settings.SettingsScreen
import com.example.mediaflow.presentation.settings.SettingsViewModel

@Composable
fun MediaFlowNavGraph(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val bottomBarScreens = listOf(
        Screen.Home,
        Screen.Downloads,
        Screen.Library,
        Screen.History,
        Screen.Settings
    )

    val showBottomBar = bottomBarScreens.any { it.route == currentDestination }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("main_bottom_nav_bar")
                ) {
                    bottomBarScreens.forEach { screen ->
                        val selected = currentDestination == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                if (currentDestination != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_${screen.route}")
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
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.provideFactory(
                        downloadRepository = container.downloadRepository,
                        settingsRepository = container.settingsRepository,
                        validateUrlUseCase = container.validateUrlUseCase
                    )
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAnalyzer = { url ->
                        navController.navigate(Screen.createAnalyzerRoute(url))
                    },
                    onNavigateToDownloads = {
                        navController.navigate(Screen.Downloads.route)
                    }
                )
            }

            composable(
                route = Screen.ROUTE_ANALYZER,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val decodedUrl = java.net.URLDecoder.decode(encodedUrl, "UTF-8")

                val analyzerViewModel: AnalyzerViewModel = viewModel(
                    factory = AnalyzerViewModel.provideFactory(
                        analyzeMediaUseCase = container.analyzeMediaUseCase,
                        downloadUseCases = container.downloadUseCases
                    )
                )
                AnalyzerScreen(
                    url = decodedUrl,
                    viewModel = analyzerViewModel,
                    onBack = { navController.popBackStack() },
                    onDownloadStarted = {
                        navController.navigate(Screen.Downloads.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(Screen.Downloads.route) {
                val downloadsViewModel: DownloadsViewModel = viewModel(
                    factory = DownloadsViewModel.provideFactory(
                        downloadUseCases = container.downloadUseCases,
                        historyUseCase = container.getHistoryUseCase
                    )
                )
                DownloadsScreen(viewModel = downloadsViewModel)
            }

            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.provideFactory(
                        libraryUseCases = container.libraryUseCases
                    )
                )
                LibraryScreen(viewModel = libraryViewModel)
            }

            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.provideFactory(
                        historyUseCase = container.getHistoryUseCase,
                        downloadUseCases = container.downloadUseCases
                    )
                )
                HistoryScreen(viewModel = historyViewModel)
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(
                        settingsRepository = container.settingsRepository,
                        storageRepository = container.storageRepository,
                        historyRepository = container.historyRepository
                    )
                )
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
