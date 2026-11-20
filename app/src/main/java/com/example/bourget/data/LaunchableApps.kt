package com.example.bourget.data

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class LaunchableApp(val packageName: String, val label: String, val icon: ImageBitmap)

/**
 * Apps with a launcher icon, excluding Bourget itself. Visible on Android 11+ through the
 * MAIN/LAUNCHER <queries> entry in the manifest. Call off the main thread.
 */
fun loadLaunchableApps(context: Context, iconSizePx: Int): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { it.activityInfo.applicationInfo }
        .distinctBy { it.packageName }
        .filter { it.packageName != context.packageName }
        .map { info ->
            LaunchableApp(
                packageName = info.packageName,
                label = info.loadLabel(pm).toString(),
                icon = info.loadIcon(pm).toBitmap(iconSizePx, iconSizePx).asImageBitmap(),
            )
        }
        .sortedBy { it.label.lowercase() }
}
