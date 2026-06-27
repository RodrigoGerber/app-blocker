package com.example.appblocker.config

/**
 * Central place for the few app-wide constants and debug flags.
 */
object AppBlockerConfig {
    /** Seed package for migrating the old single-rule data into the rule list. */
    const val INSTAGRAM_PACKAGE = "com.instagram.android"

    /** Default daily limit applied to a newly added rule. */
    const val DEFAULT_DAILY_LIMIT_MINUTES = 30

    /**
     * Detection spike (spec §18–§19, Phase 3). When true, the daily limit is
     * ignored and the service redirects Home on EVERY Instagram detection. Use
     * this to confirm on a real device that detection + GLOBAL_ACTION_HOME work
     * before trusting the usage math. Leave false for normal operation.
     */
    const val SPIKE_REDIRECT_ON_DETECT = false

    /**
     * Safety valve. When true, the chosen action (spike or policy) is logged but
     * the Home redirect is suppressed, so you can never accidentally lock
     * yourself out of Instagram while developing. Combine with
     * [SPIKE_REDIRECT_ON_DETECT] to dry-run the spike: it logs every detection
     * but never redirects.
     */
    const val DRY_RUN = false

    /**
     * When true, logs the package name of every window-state change, not just
     * Instagram. Handy during the spike to learn empirically which events
     * Instagram emits across launch paths (icon, notification, deep link,
     * recents). Never logs window content (spec §16). Verbose — keep off
     * outside the spike.
     */
    const val VERBOSE_WINDOW_LOGGING = false
}
