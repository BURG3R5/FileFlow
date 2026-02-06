package co.adityarajput.fileflow.utils

import android.content.Context
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.net.Uri
import android.os.PowerManager

fun Context.takePersistablePermission(uri: Uri) =
    contentResolver.takePersistableUriPermission(
        uri,
        FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION,
    )

fun Context.hasUnrestrictedBackgroundUsagePermission(): Boolean {
    return (getSystemService(Context.POWER_SERVICE) as PowerManager)
        .isIgnoringBatteryOptimizations(this.packageName)
}
