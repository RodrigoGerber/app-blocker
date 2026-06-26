package com.example.appblocker.system

import java.time.Clock

/**
 * Supplies the [Clock] used for "today" calculations. Centralised so usage math
 * always uses the device's local time zone (spec §15.5) and so tests can inject
 * a fixed clock.
 */
object ClockProvider {
    fun systemClock(): Clock = Clock.systemDefaultZone()
}
