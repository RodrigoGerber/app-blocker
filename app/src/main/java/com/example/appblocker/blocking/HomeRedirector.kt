package com.example.appblocker.blocking

import android.accessibilityservice.AccessibilityService
import android.util.Log

/**
 * Abstraction over the "go to Home" action. It exists so the decision logic
 * never has to hold an [AccessibilityService] reference directly.
 */
interface HomeRedirector {
    /** Returns true if the Home action was dispatched successfully. */
    fun redirectToHome(): Boolean
}

/**
 * Default implementation: performGlobalAction belongs to the accessibility
 * service, so the service supplies itself here (spec §9.6).
 */
class AccessibilityHomeRedirector(
    private val service: AccessibilityService,
) : HomeRedirector {

    override fun redirectToHome(): Boolean {
        val dispatched = service.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_HOME,
        )
        if (!dispatched) {
            // Don't retry in a loop; a later event can try again (spec §15.4).
            Log.w(TAG, "GLOBAL_ACTION_HOME returned false.")
        }
        return dispatched
    }

    private companion object {
        const val TAG = "HomeRedirector"
    }
}
