package com.example.appblocker.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.appblocker.AppBlockerApplication
import com.example.appblocker.BuildConfig
import com.example.appblocker.blocking.AccessibilityHomeRedirector
import com.example.appblocker.config.AppBlockerConfig
import com.example.appblocker.rules.RuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Entry point that listens for window changes, filters down to the set of
 * monitored apps, and hands off to [ForegroundAppHandler]. It deliberately
 * holds no business logic itself (spec §9.1).
 *
 * Multi-app: it watches every window-state change and checks the package
 * against a live [monitoredPackages] set derived from the enabled rules (empty
 * while paused), so the hot path stays an O(1) lookup.
 *
 * Event-storm protection (spec §14):
 *  - filter by event type, then by the monitored-package set, immediately;
 *  - debounce repeated detections of the *same* package within [DEBOUNCE_MILLIS];
 *  - a [Mutex] prevents concurrent evaluations.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val evaluationMutex = Mutex()

    /** Updated from the rules/pause flows; read on the main (event) thread. */
    @Volatile
    private var monitoredPackages: Set<String> = emptySet()

    /** Last evaluation time per package, for per-package debounce. Main thread only. */
    private val lastEvaluationAt = HashMap<String, Long>()

    private lateinit var foregroundAppHandler: ForegroundAppHandler

    override fun onServiceConnected() {
        super.onServiceConnected()
        val container = (application as AppBlockerApplication).container
        foregroundAppHandler = ForegroundAppHandler(
            blockingPolicy = container.blockingPolicy,
            homeRedirector = AccessibilityHomeRedirector(this),
        )
        observeMonitoredPackages(container.ruleRepository)
        Log.i(TAG, "Accessibility service connected.")
    }

    private fun observeMonitoredPackages(ruleRepository: RuleRepository) {
        serviceScope.launch {
            combine(ruleRepository.rules, ruleRepository.paused) { rules, paused ->
                MonitoredPackages.from(rules, paused)
            }.collect { packages ->
                monitoredPackages = packages
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Monitoring ${packages.size} app(s): $packages")
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // Spike aid: log every foreground app so we can learn empirically which
        // events apps emit across launch paths (spec §18, §22). No window
        // content is read — only the package name.
        if (AppBlockerConfig.VERBOSE_WINDOW_LOGGING) {
            Log.v(TAG, "Window state changed: $packageName / ${event.className}")
        }

        if (packageName !in monitoredPackages) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        val last = lastEvaluationAt[packageName] ?: 0L
        if (now - last < DEBOUNCE_MILLIS) {
            return
        }
        lastEvaluationAt[packageName] = now

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Detected foreground: $packageName")
        }

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
