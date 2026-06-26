package com.example.appblocker.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.appblocker.AppBlockerApplication
import com.example.appblocker.blocking.AccessibilityHomeRedirector
import com.example.appblocker.config.AppBlockerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Entry point that listens for window changes, filters down to the monitored
 * app, and hands off to [ForegroundAppHandler]. It deliberately holds no
 * business logic itself (spec §9.1).
 *
 * Event-storm protection (spec §14):
 *  - filter by event type and package name immediately;
 *  - debounce repeated detections within [DEBOUNCE_MILLIS];
 *  - a [Mutex] prevents concurrent evaluations.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val evaluationMutex = Mutex()

    @Volatile
    private var lastEvaluationAt = 0L

    private lateinit var foregroundAppHandler: ForegroundAppHandler

    override fun onServiceConnected() {
        super.onServiceConnected()
        val container = (application as AppBlockerApplication).container
        foregroundAppHandler = ForegroundAppHandler(
            blockingPolicy = container.blockingPolicy,
            homeRedirector = AccessibilityHomeRedirector(this),
        )
        Log.i(TAG, "Accessibility service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // Spike aid: log every foreground app so we can learn empirically which
        // events Instagram emits across launch paths (spec §18, §22). No window
        // content is read — only the package name.
        if (AppBlockerConfig.VERBOSE_WINDOW_LOGGING) {
            Log.v(TAG, "Window state changed: $packageName / ${event.className}")
        }

        if (packageName != AppBlockerConfig.INSTAGRAM_PACKAGE) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastEvaluationAt < DEBOUNCE_MILLIS) {
            return
        }
        lastEvaluationAt = now

        Log.d(TAG, "Detected foreground: $packageName")

        serviceScope.launch {
            // Skip if another evaluation is already running rather than queueing.
            if (!evaluationMutex.tryLock()) return@launch
            try {
                foregroundAppHandler.onAppOpened(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Evaluation failed for $packageName", e)
            } finally {
                evaluationMutex.unlock()
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        Log.i(TAG, "Accessibility service destroyed.")
        super.onDestroy()
    }

    private companion object {
        const val TAG = "AppBlockerService"
        const val DEBOUNCE_MILLIS = 500L
    }
}
