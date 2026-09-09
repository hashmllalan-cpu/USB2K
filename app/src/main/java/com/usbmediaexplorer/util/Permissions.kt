package com.usbmediaexplorer.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Runtime-permission helpers.
 *
 * The app uses Scoped Storage with media permissions for internal storage and
 * Storage Access Framework (SAF) tree grants for external/removable USB volumes.
 * The broad `MANAGE_EXTERNAL_STORAGE` permission is deliberately not requested.
 */
object Permissions {

    /** Permissions needed to read the *internal* storage by path. */
    fun mediaPermissions(): Array<String> {
        val list = ArrayList<String>(4)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14 "Select photos and videos" (partial access).
            list += Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list += Manifest.permission.READ_MEDIA_VIDEO
            list += Manifest.permission.READ_MEDIA_IMAGES
            list += Manifest.permission.READ_MEDIA_AUDIO
        } else {
            list += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return list.toTypedArray()
    }

    /**
     * Everything the app asks for at runtime in one call: media access, plus notifications while
     * they are still missing (the transfer notification is useless without them on Android 13+).
     */
    fun runtimePermissions(context: Context): Array<String> {
        val list = mediaPermissions().toMutableList()
        // Same dialog, but explicit: copy/move/delete onto shared storage needs the write half
        // on API <= 29, and relying on group-grant behaviour alone is needlessly fragile.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            list += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        if (needsNotificationPermission(context)) list += Manifest.permission.POST_NOTIFICATIONS
        return list.toTypedArray()
    }

    /**
     * Combined gate: "can the app read the drives" — via media/storage permissions.
     * Removable drives obtain access through explicit SAF tree grants.
     */
    fun hasStorageAccess(context: Context): Boolean = hasMediaAccess(context)

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * True when the app can really read media from the internal storage.
     *
     * Deliberately *not* "any of the permissions is granted": on Android 13+ an audio-only grant
     * says nothing about videos and photos, and on Android 14+ the user may pick partial access,
     * which is reported through READ_MEDIA_VISUAL_USER_SELECTED alone.
     */
    fun hasMediaAccess(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            granted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ||
                (
                    granted(context, Manifest.permission.READ_MEDIA_VIDEO) &&
                        granted(context, Manifest.permission.READ_MEDIA_IMAGES)
                    )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            // AND, not OR: an images-only or video-only grant is partial access — treating it as
            // full access hid the permission UI while half the library stayed unreadable.
            granted(context, Manifest.permission.READ_MEDIA_VIDEO) &&
                granted(context, Manifest.permission.READ_MEDIA_IMAGES)

        else -> granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /**
     * True when a media permission is denied *and* Android will no longer show a dialog for it
     * (the user picked "Don't ask again"). Only meaningful right after a request result — before
     * any request the rationale flag is false as well.
     */
    fun permanentlyDenied(activity: Activity): Boolean =
        missingMediaPermissions(activity).any { permission ->
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }

    /** This app's page in the system settings, the only way out of a permanent denial. */
    fun appSettingsIntent(packageName: String): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )

    fun missingMediaPermissions(context: Context): List<String> =
        mediaPermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    /** Android 13+ hides the ongoing-transfer notification until this is granted. */
    fun needsNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
}
