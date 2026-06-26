package com.example.appblocker.usage

/**
 * Whether a usage event marks an app entering or leaving the foreground.
 */
enum class ForegroundTransition { ENTER, EXIT }

/**
 * A framework-independent view of a single usage event. The Android-specific
 * [AndroidDailyUsageProvider] maps `android.app.usage.UsageEvents.Event` into
 * this type so the parsing logic below stays a pure function that can be unit
 * tested on the JVM without Robolectric (spec §17.1).
 */
data class ForegroundEvent(
    val packageName: String,
    val timestampMillis: Long,
    val transition: ForegroundTransition,
)

/**
 * Turns an ordered (or unordered) list of foreground transitions into a total
 * foreground duration for a single package.
 *
 * Rules (spec §9.5):
 *  - only events for [packageName] count;
 *  - an ENTER opens a session if none is open;
 *  - an EXIT closes the open session, adding the elapsed time;
 *  - a session still open at the end is closed with [intervalEndMillis];
 *  - negative or zero deltas are ignored;
 *  - duplicate ENTERs / orphan EXITs are tolerated.
 */
class UsageEventParser {

    fun calculateForegroundTime(
        events: List<ForegroundEvent>,
        packageName: String,
        intervalEndMillis: Long,
    ): Long {
        var total = 0L
        var sessionStart: Long? = null

        // Sort defensively: UsageStatsManager usually returns events in order,
        // but the spec calls out out-of-order timestamps as a tested scenario.
        val relevant = events
            .asSequence()
            .filter { it.packageName == packageName }
            .sortedBy { it.timestampMillis }

        for (event in relevant) {
            when (event.transition) {
                ForegroundTransition.ENTER -> {
                    if (sessionStart == null) {
                        sessionStart = event.timestampMillis
                    }
                    // A duplicate ENTER while a session is open is ignored.
                }

                ForegroundTransition.EXIT -> {
                    val start = sessionStart
                    if (start != null) {
                        val delta = event.timestampMillis - start
                        if (delta > 0) total += delta
                        sessionStart = null
                    }
                    // An EXIT with no open session is ignored.
                }
            }
        }

        val start = sessionStart
        if (start != null) {
            val delta = intervalEndMillis - start
            if (delta > 0) total += delta
        }

        return total
    }
}
