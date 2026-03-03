package co.adityarajput.fileflow.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

enum class Permission {
    UNRESTRICTED_BACKGROUND_USAGE,
    MANAGE_EXTERNAL_STORAGE,
}

fun Context.isGranted(permission: Permission) = when (permission) {
    Permission.UNRESTRICTED_BACKGROUND_USAGE ->
        (getSystemService(POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(packageName)

    Permission.MANAGE_EXTERNAL_STORAGE ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) false
        else Environment.isExternalStorageManager()
}

fun Context.isGranted(permissions: Iterable<Permission>) =
    permissions.associateWith(::isGranted).withDefault { false }

@SuppressLint("BatteryLife")
fun Context.request(permission: Permission, remove: Boolean = false) = try {
    when (permission) {
        Permission.UNRESTRICTED_BACKGROUND_USAGE ->
            startActivity(
                if (remove)
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                else
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        "package:${packageName}".toUri(),
                    ),
            )

        Permission.MANAGE_EXTERNAL_STORAGE ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) Unit
            else startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }
} catch (e: Exception) {
    Logger.e("Permissions", "Error while requesting $permission", e)
}

fun Context.requestPersistableFolderPermission(uri: Uri) =
    contentResolver.takePersistableUriPermission(
        uri,
        FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION,
    )
