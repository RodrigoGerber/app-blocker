package com.example.appblocker.system

import androidx.compose.ui.graphics.ImageBitmap

/**
 * A launchable installed app, ready for display in the picker. [icon] is
 * pre-rasterized off the main thread; null if it couldn't be loaded.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)
