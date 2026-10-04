package com.xeg911.appcontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavArgumentBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xeg911.appcontrol.ui.auth.AuthScreen
import com.xeg911.appcontrol.ui.devicelist.DeviceListScreen
import com.xeg911.appcontrol.ui.feature.composer.NotificationComposerScreen
import com.xeg911.appcontrol.ui.feature.defaults.DefaultConfigScreen
import com.xeg911.appcontrol.ui.feature.defaults.RequiredPermissionsScreen
import com.xeg911.appcontrol.ui.feature.devicehub.DeviceHubScreen
import com.xeg911.appcontrol.ui.feature.devicehub.DeviceHubViewModel
import com.xeg911.appcontrol.ui.feature.files.FilesScreen
import com.xeg911.appcontrol.ui.feature.filters.NotificationFilterScreen
import com.xeg911.appcontrol.ui.feature.history.TransferHistoryScreen
import com.xeg911.appcontrol.ui.feature.rules.RulesScreen
import com.xeg911.appcontrol.ui.feature.settings.SettingsScreen
import com.xeg911.appcontrol.ui.feature.templates.TemplatesScreen
import com.xeg911.appcontrol.ui.home.HomeActions
import com.xeg911.appcontrol.ui.home.HomeScreen
import com.xeg911.appcontrol.ui.home.Tool
import com.xeg911.appcontrol.ui.tools.AllToolsScreen
import com.xeg911.appcontrol.ui.util.NavTransitions

@Composable
fun AppNavGraph(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    fun openTool(tool: Tool) {
        val route = when (tool) {
            Tool.COMPOSER -> Screen.composerRoute("")
            Tool.FILTERS -> Screen.NOTIFICATION_FILTERS
            Tool.TEMPLATES -> Screen.TEMPLATES
            Tool.RULES -> Screen.RULES
            Tool.FILES -> Screen.filesRoute()
            Tool.TRANSFER_HISTORY -> Screen.TRANSFER_HISTORY
            Tool.DEFAULT_CONFIG -> Screen.DEFAULT_CONFIG
            Tool.REQUIRED_PERMISSIONS -> Screen.REQUIRED_PERMISSIONS
            Tool.SETTINGS -> Screen.SETTINGS
        }
        navController.navigate(route) { launchSingleTop = true }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = NavTransitions.enter,
        exitTransition = NavTransitions.exit,
        popEnterTransition = NavTransitions.popEnter,
        popExitTransition = NavTransitions.popExit,
        predictivePopEnterTransition = { NavTransitions.popEnter(this) },
        predictivePopExitTransition = { NavTransitions.popExit(this) },
    ) {
        composable(
            route = Screen.AUTH,
            enterTransition = NavTransitions.fadeEnter,
            exitTransition = NavTransitions.fadeExit,
            popEnterTransition = NavTransitions.fadeEnter,
            popExitTransition = NavTransitions.fadeExit,
        ) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.HOME) {
                        popUpTo(Screen.AUTH) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.HOME) {
            HomeScreen(
                actions = HomeActions(
                    onOpenTool = ::openTool,
                    onOpenAllTools = { navController.navigate(Screen.ALL_TOOLS) },
                    onOpenDevice = { navController.navigate(Screen.deviceHubRoute(it)) },
                    onOpenAllDevices = { navController.navigate(Screen.DEVICE_LIST) },
                    onLogout = {
                        navController.navigate(Screen.AUTH) {
                            popUpTo(Screen.HOME) {
                                inclusive = true
                            }
                        }
                    },
                )
            )
        }

        composable(Screen.DEVICE_LIST) {
            DeviceListScreen(
                onDeviceClick = { navController.navigate(Screen.deviceHubRoute(it)) },
                onNavigateBack = navController::popBackStack,
            )
        }

        composable(Screen.ALL_TOOLS) {
            AllToolsScreen(onOpenTool = ::openTool, onNavigateBack = navController::popBackStack)
        }

        composable(Screen.SETTINGS) { SettingsScreen(onNavigateBack = navController::popBackStack) }

        composable(Screen.DEFAULT_CONFIG) { DefaultConfigScreen(onNavigateBack = navController::popBackStack) }

        composable(Screen.REQUIRED_PERMISSIONS) {
            RequiredPermissionsScreen(onNavigateBack = navController::popBackStack)
        }

        composable(Screen.TRANSFER_HISTORY) {
            TransferHistoryScreen(
                onNavigateBack = navController::popBackStack,
                onOpenDevice = { navController.navigate(Screen.deviceHubRoute(it)) },
            )
        }

        composable(Screen.TEMPLATES) {
            TemplatesScreen(
                onEditTemplate = {
                    navController.navigate(
                        Screen.composerRoute(
                            "",
                            templateId = it
                        )
                    )
                },
                onCreateTemplate = { navController.navigate(Screen.composerRoute("")) },
                onNavigateBack = navController::popBackStack,
            )
        }

        composable(Screen.NOTIFICATION_FILTERS) {
            NotificationFilterScreen(onNavigateBack = navController::popBackStack)
        }

        composable(Screen.RULES) {
            RulesScreen(onNavigateBack = navController::popBackStack)
        }

        composable(
            route = Screen.FILES_ROUTE,
            arguments = listOf(navArgument(Screen.ARG_DEVICE_ID) {
                type = NavType.StringType; defaultValue = ""
            }),
        ) { backStackEntry ->
            FilesScreen(
                initialDeviceId = backStackEntry.arguments?.getString(Screen.ARG_DEVICE_ID)
                    .orEmpty(),
                onNavigateBack = navController::popBackStack,
                onOpenHistory = { navController.navigate(Screen.TRANSFER_HISTORY) },
            )
        }

        composable(
            route = Screen.DEVICE_HUB_ROUTE,
            arguments = listOf(navArgument(Screen.ARG_DEVICE_ID) {
                type = NavType.StringType; defaultValue = ""
            }),
        ) { backStackEntry ->
            val deviceId: String = remember(backStackEntry) {
                backStackEntry.arguments?.getString(Screen.ARG_DEVICE_ID) ?: ""
            }
            DeviceHubScreen(
                deviceId = deviceId,
                deviceHubViewModel = hiltViewModel<DeviceHubViewModel>(),
                onOpenComposer = { event ->
                    navController.navigate(
                        Screen.composerRoute(
                            deviceId = event.deviceId,
                            notificationId = event.notificationId,
                            packageName = event.packageName,
                            appName = event.appName,
                            permission = event.permission,
                        )
                    )
                },
                onOpenFiles = { navController.navigate(Screen.filesRoute(deviceId)) },
                onNavigateBack = navController::popBackStack,
            )
        }

        composable(
            route = Screen.COMPOSER_ROUTE,
            arguments = listOf(
                navArgument(Screen.ARG_DEVICE_ID) { optionalString() },
                navArgument(Screen.ARG_NOTIFICATION_ID) { optionalString() },
                navArgument(Screen.ARG_PACKAGE_NAME) { optionalString() },
                navArgument(Screen.ARG_APP_NAME) { optionalString() },
                navArgument(Screen.ARG_PERMISSION) { optionalString() },
                navArgument(Screen.ARG_TEMPLATE_ID) { optionalString() },
            ),
        ) {
            NotificationComposerScreen(onNavigateBack = navController::popBackStack)
        }
    }
}

private fun NavArgumentBuilder.optionalString() {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}
