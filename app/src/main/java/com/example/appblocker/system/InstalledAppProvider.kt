package com.example.appblocker.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wrapper over [PackageManager] for app metadata and the launchable-app list
 * used by the picker (spec §21.3).
 */
class InstalledAppProvider(
    private val context: Context,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    /** Human-readable label for [packageName], or null if it is not installed. */
    fun getAppLabel(packageName: String): String? {
        val pm = context.packageManager
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun isInstalled(packageName: String): Boolean = getAppLabel(packageName) != null

    /**
     * All launchable apps (one entry per package), sorted A–Z by label, with
     * icons rasterized. Excludes our own app and the current home launcher
     * (blocking either would be miserable). Runs off the main thread because
     * label/icon loading and rasterization are not free.
     */
    suspend fun getLaunchableApps(): List<InstalledApp> = withContext(defaultDispatcher) {
        val pm = context.packageManager
        val excluded = buildSet {
            add(context.packageName)
            currentHomePackage(pm)?.let { add(it) }
        }

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        @Suppress("DEPRECATION")
        pm.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .filterNot { it.activityInfo.packageName in excluded }
            .distinctBy { it.activityInfo.packageName }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val label = runCatching { resolveInfo.loadLabel(pm).toString() }
                    .getOrDefault(pkg)
                val icon = runCatching {
                    resolveInfo.loadIcon(pm).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
                }.getOrNull()
                InstalledApp(packageName = pkg, label = label, icon = icon)
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun currentHomePackage(pm: PackageManager): String? {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        val resolved = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName
    }

    private companion object {
        // 48dp at ~xxhdpi; large enough for the picker rows without being huge.
        const val ICON_PX = 144
    }
}
