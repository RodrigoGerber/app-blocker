package com.example.appblocker.accessibility

import android.util.Log
import com.example.appblocker.blocking.BlockingDecision
import com.example.appblocker.blocking.BlockingPolicy
import com.example.appblocker.blocking.HomeRedirector
import com.example.appblocker.config.AppBlockerConfig

/**
 * Reacts to a monitored app coming to the foreground: ask the policy, and if it
 * says block, redirect Home. This keeps the decision-to-action wiring out of the
 * Android service class (spec §9.2).
 *
 * Behaviour is driven by two flags (defaults come from [AppBlockerConfig], but
 * they are constructor parameters so this class stays unit-testable):
 *  - [spikeRedirectOnDetect]: Phase 3 spike — bypass the limit and redirect on
 *    every detection;
 *  - [dryRun]: log the intended action but never actually redirect.
 */
class ForegroundAppHandler(
    private val blockingPolicy: BlockingPolicy,
    private val homeRedirector: HomeRedirector,
    private val spikeRedirectOnDetect: Boolean = AppBlockerConfig.SPIKE_REDIRECT_ON_DETECT,
    private val dryRun: Boolean = AppBlockerConfig.DRY_RUN,
) {
    suspend fun onAppOpened(packageName: String) {
        if (spikeRedirectOnDetect) {
            Log.w(
                TAG,
                "SPIKE: $packageName detected — redirecting Home (limit ignored), " +
                    "dryRun=$dryRun",
            )
            redirectUnlessDryRun()
            return
        }

        when (val decision = blockingPolicy.evaluate(packageName)) {
            BlockingDecision.Allow -> {
                Log.d(TAG, "Allow $packageName")
            }

            is BlockingDecision.Block -> {
                Log.i(
                    TAG,
                    "Block ${decision.packageName}: used=${decision.usedMillis}ms " +
                        "limit=${decision.limitMillis}ms dryRun=$dryRun",
                )
                redirectUnlessDryRun()
            }
        }
    }

    private fun redirectUnlessDryRun() {
        if (!dryRun) {
            homeRedirector.redirectToHome()
        }
    }

    private companion object {
        const val TAG = "ForegroundAppHandler"
    }
}
