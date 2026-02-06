package co.adityarajput.fileflow.utils

import android.content.Context
import android.content.pm.ApplicationInfo

fun Context.isDebugBuild() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
