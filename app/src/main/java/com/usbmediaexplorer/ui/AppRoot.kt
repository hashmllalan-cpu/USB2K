package com.usbmediaexplorer.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.usbmediaexplorer.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.usbmediaexplorer.data.ops.JobState
import com.usbmediaexplorer.ui.common.LocalAppContainer
import com.usbmediaexplorer.ui.nav.AppNavHost
import com.usbmediaexplorer.ui.nav.AppNavigator
import com.usbmediaexplorer.ui.nav.LocalNavigator
import com.usbmediaexplorer.ui.nav.Routes
import com.usbmediaexplorer.ui.ops.TransferBar
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Application shell: navigation host, snackbar host and the global transfer bar.
 *
 * The [Scaffold] is *always* present — swapping between two different trees when the player
 * opens would destroy the NavHost and lose the whole back stack, so only its contents change.
 */
@Composable
fun AppRoot(pendingRoute: MutableStateFlow<String?>) {
    val navController = rememberNavController()
    val navigator = remember(navController) { AppNavigator(navController) }
    val snackbarHostState = remember { SnackbarHostState() }
    val container = LocalAppContainer.current

    val activeJob by container.fileOpsManager.activeJob.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val pending by pendingRoute.collectAsState()

    LaunchedEffect(pending) {
        val route = pending
        if (!route.isNullOrEmpty()) {
            navController.navigate(route)
            pendingRoute.value = null
        }
    }

    val immersive = currentRoute?.startsWith(Routes.PLAYER) == true

    CompositionLocalProvider(LocalNavigator provides navigator) {
        Scaffold(
            snackbarHost = { if (!immersive) SnackbarHost(snackbarHostState) },
            contentWindowInsets = if (immersive) {
                WindowInsets(0, 0, 0, 0)
            } else {
                androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
            },
            bottomBar = {
                if (!immersive) {
                    val job = activeJob
                    val topLevel = currentRoute?.let { route ->
                        route == Routes.HOME || route == Routes.FAVORITES ||
                            route == Routes.RECENT || route == Routes.TRANSFERS ||
                            route == Routes.SETTINGS
                    } == true
                    if (job != null && job.state != JobState.DONE) {
                        TransferBar(
                            job = job,
                            onClick = { navigator.transfers() },
                            onPause = { container.fileOpsManager.pause(job.jobId) },
                            onResume = { container.fileOpsManager.resume(job.jobId) },
                            onCancel = { container.fileOpsManager.cancel(job.jobId) },
                        )
                    }
                    if (topLevel) AppNavigationBar(currentRoute, navigator)
                }
            },
        ) { padding ->
            val contentPadding = if (immersive) PaddingValues(0.dp) else padding
            AppNavHost(
                navController = navController,
                snackbarHostState = snackbarHostState,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Composable
private fun AppNavigationBar(currentRoute: String?, navigator: AppNavigator) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = navigator::home,
            icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.FAVORITES,
            onClick = navigator::favorites,
            icon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_favorites)) },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.RECENT,
            onClick = navigator::recent,
            icon = { Icon(Icons.Outlined.History, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_recent)) },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.TRANSFERS,
            onClick = navigator::transfers,
            icon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_transfers)) },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.SETTINGS,
            onClick = navigator::settings,
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
        )
    }
}
