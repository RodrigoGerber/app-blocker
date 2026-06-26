package com.example.appblocker.usage

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

/**
 * Usage Access is not a runtime permission: it is granted by the user in a
 * dedicated Settings screen and read back via [AppOpsManager]. This checker
 * reports the current state so the UI can reflect it (spec §13.1, §15.1).
 */
class UsageAccessPermissionChecker(
    private val context: Context,
) {
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
