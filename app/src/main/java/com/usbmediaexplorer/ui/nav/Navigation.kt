package com.usbmediaexplorer.ui.nav

import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

object Routes {
    const val HOME = "home"
    const val BROWSE = "browse"
    const val PLAYER = "player"
    const val IMAGE = "image"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val RECENT = "recent"
    const val SETTINGS = "settings"
    const val TRANSFERS = "transfers"

    const val ARG_URI = "uri"
    const val ARG_FOLDER = "folder"

    /** Set when the browser should open a folder with every item already selected (spec §8). */
    const val ARG_SELECT = "select"

    fun browse(uri: Uri, selectAll: Boolean = false): String = buildString {
        append("$BROWSE?$ARG_URI=${Uri.encode(uri.toString())}")
        if (selectAll) append("&$ARG_SELECT=true")
    }

    fun player(uri: Uri, folderUri: Uri?): String = buildString {
        append("$PLAYER?$ARG_URI=${Uri.encode(uri.toString())}")
        if (folderUri != null) append("&$ARG_FOLDER=${Uri.encode(folderUri.toString())}")
    }

    fun image(uri: Uri, folderUri: Uri?): String = buildString {
        append("$IMAGE?$ARG_URI=${Uri.encode(uri.toString())}")
        if (folderUri != null) append("&$ARG_FOLDER=${Uri.encode(folderUri.toString())}")
    }

    fun search(rootUri: Uri): String = "$SEARCH?$ARG_URI=${Uri.encode(rootUri.toString())}"

    fun browseRoute(): String = "$BROWSE?$ARG_URI={$ARG_URI}&$ARG_SELECT={$ARG_SELECT}"
    fun playerRoute(): String = "$PLAYER?$ARG_URI={$ARG_URI}&$ARG_FOLDER={$ARG_FOLDER}"
    fun imageRoute(): String = "$IMAGE?$ARG_URI={$ARG_URI}&$ARG_FOLDER={$ARG_FOLDER}"
    fun searchRoute(): String = "$SEARCH?$ARG_URI={$ARG_URI}"
}

/** Thin, type-safe wrapper over [NavHostController] shared by every screen. */
class AppNavigator(private val navController: NavHostController) {

    /** Back to the storage dashboard: pops when home is on the back stack, navigates otherwise. */
    fun home() {
        if (!navController.popBackStack(Routes.HOME, inclusive = false)) {
            navController.navigate(Routes.HOME)
        }
    }

    fun openVolume(uri: Uri) = navigateFolder(Routes.browse(uri))

    fun openFolder(uri: Uri) = navigateFolder(Routes.browse(uri))

    /** Return to an existing breadcrumb destination instead of adding a duplicate entry. */
    fun openBreadcrumb(uri: Uri) {
        val route = Routes.browse(uri)
        if (!navController.popBackStack(route, inclusive = false)) navigateFolder(route)
    }

    /** Opens a folder and selects its content — the "select content" context action. */
    fun openFolderSelecting(uri: Uri) = navigateFolder(Routes.browse(uri, selectAll = true))

    fun playVideo(uri: Uri, folderUri: Uri? = null) = navigateSingle(Routes.player(uri, folderUri))

    fun viewImage(uri: Uri, folderUri: Uri? = null) = navigateSingle(Routes.image(uri, folderUri))

    fun search(rootUri: Uri) = navigateSingle(Routes.search(rootUri))

    fun favorites() = navigateTopLevel(Routes.FAVORITES)

    fun recent() = navigateTopLevel(Routes.RECENT)

    fun settings() = navigateTopLevel(Routes.SETTINGS)

    fun transfers() = navigateTopLevel(Routes.TRANSFERS)

    private fun navigateFolder(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
        }
    }

    private fun navigateSingle(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    private fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(Routes.HOME) { saveState = true }
        }
    }

    fun back(): Boolean = runCatching { navController.popBackStack() }.getOrDefault(false)

    fun root(): Boolean = runCatching {
        navController.popBackStack(Routes.HOME, inclusive = false)
    }.getOrDefault(false)
}

val LocalNavigator = staticCompositionLocalOf<AppNavigator> {
    error("AppNavigator not provided")
}
