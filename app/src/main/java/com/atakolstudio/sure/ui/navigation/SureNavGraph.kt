package com.atakolstudio.sure.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.atakolstudio.sure.ui.screens.brand.BrandSelectionScreen
import com.atakolstudio.sure.ui.screens.connectiontype.ConnectionTypeSelectionScreen
import com.atakolstudio.sure.ui.screens.devices.DevicesScreen
import com.atakolstudio.sure.ui.screens.devicetype.DeviceTypeSelectionScreen
import com.atakolstudio.sure.ui.screens.manualsearch.ManualSearchScreen
import com.atakolstudio.sure.ui.screens.remote.RemoteScreen

@Composable
fun SureNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = SureDestination.Devices.route) {

        composable(SureDestination.Devices.route) {
            DevicesScreen(
                onAddDeviceClick = { navController.navigate(SureDestination.DeviceTypeSelection.route) },
                onDeviceClick = { deviceId ->
                    navController.navigate(SureDestination.Remote.createRouteForSavedDevice(deviceId))
                }
            )
        }

        composable(SureDestination.DeviceTypeSelection.route) {
            DeviceTypeSelectionScreen(
                onTypeSelected = { type ->
                    navController.navigate(SureDestination.ConnectionTypeSelection.createRoute(type.name))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = SureDestination.ConnectionTypeSelection.route,
            arguments = listOf(navArgument("deviceType") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceType = backStackEntry.arguments?.getString("deviceType") ?: "TV"
            ConnectionTypeSelectionScreen(
                onTypeSelected = { connectionType ->
                    navController.navigate(
                        SureDestination.BrandSelection.createRoute(deviceType, connectionType.name)
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = SureDestination.BrandSelection.route,
            arguments = listOf(
                navArgument("deviceType") { type = NavType.StringType },
                navArgument("connectionType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deviceType = backStackEntry.arguments?.getString("deviceType") ?: "TV"
            val connectionType = backStackEntry.arguments?.getString("connectionType") ?: "TRADITIONAL_IR"
            BrandSelectionScreen(
                onBrandSelected = { brand ->
                    navController.navigate(
                        SureDestination.Remote.createRouteForSetup(brand.brandKey, deviceType, connectionType)
                    ) {
                        // Kurulum akışı ekranlarını geri yığınından temizle
                        popUpTo(SureDestination.Devices.route)
                    }
                },
                onManualSearchClick = {
                    navController.navigate(SureDestination.ManualSearch.createRoute(deviceType, connectionType))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = SureDestination.ManualSearch.route,
            arguments = listOf(
                navArgument("deviceType") { type = NavType.StringType },
                navArgument("connectionType") { type = NavType.StringType }
            )
        ) {
            ManualSearchScreen(
                onBack = { navController.popBackStack() },
                onDeviceSaved = { savedDeviceId ->
                    navController.navigate(
                        SureDestination.Remote.createRouteForSavedDevice(savedDeviceId)
                    ) {
                        // Kurulum akışı ekranlarını (marka/tür/bağlantı seçimi) geri yığınından temizle
                        popUpTo(SureDestination.Devices.route)
                    }
                }
            )
        }

        composable(
            route = SureDestination.Remote.route,
            arguments = listOf(
                navArgument("savedDeviceId") { type = NavType.StringType; defaultValue = "-1" },
                navArgument("brandKey") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("deviceType") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("connectionType") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            RemoteScreen(
                onBack = {
                    navController.popBackStack(SureDestination.Devices.route, inclusive = false)
                }
            )
        }
    }
}
