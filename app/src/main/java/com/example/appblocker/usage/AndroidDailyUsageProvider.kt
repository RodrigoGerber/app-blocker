package com.example.appblocker.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate

/**
 * [DailyUsageProvider] backed by [UsageStatsManager]. It queries the raw usage
 * events since the start of the local day, maps them into [ForegroundEvent]s,
 * and delegates the duration math to [UsageEventParser].
 */
class AndroidDailyUsageProvider(
    private val usageStatsManager: UsageStatsManager,
    private val clock: Clock,
    private val eventParser: UsageEventParser,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DailyUsageProvider {

    override suspend fun getUsageToday(packageName: String): DailyUsage =
        withContext(ioDispatcher) {
            val nowMillis = clock.millis()
            val startOfDayMillis = LocalDate.now(clock)
                .atStartOfDay(clock.zone)
                .toInstant()
                .toEpochMilli()

            val rawEvents = usageStatsManager.queryEvents(startOfDayMillis, nowMillis)
            val foregroundEvents = mapToForegroundEvents(rawEvents, packageName)

            val usedMillis = eventParser.calculateForegroundTime(
                events = foregroundEvents,
                packageName = packageName,
                intervalEndMillis = nowMillis,
            )

            DailyUsage(packageName = packageName, usedMillis = usedMillis)
        }

    private fun mapToForegroundEvents(
        rawEvents: UsageEvents,
        packageName: String,
    ): List<ForegroundEvent> {
        val result = mutableListOf<ForegroundEvent>()
        val event = UsageEvents.Event()

        while (rawEvents.hasNextEvent()) {
            rawEvents.getNextEvent(event)
            if (event.packageName != packageName) continue

            // MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND are the long-standing
            // constants (API 21+); on API 29+ they share the same integer
            // values as ACTIVITY_RESUMED / ACTIVITY_PAUSED, so matching on them
            // covers both naming generations without duplicate when-branches.
            @Suppress("DEPRECATION")
            val transition = when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> ForegroundTransition.ENTER
                UsageEvents.Event.MOVE_TO_BACKGROUND -> ForegroundTransition.EXIT
                else -> null
            } ?: continue

            result += ForegroundEvent(
                packageName = event.packageName,
                timestampMillis = event.timeStamp,
                transition = transition,
            )
        }

        return result
    }
}
