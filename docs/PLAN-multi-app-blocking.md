# Plan — Multi-app blocking

Status: **proposal for review** (no code written yet).

## Goal

Today App Blocker monitors exactly one hard-coded app (Instagram) with a single
daily limit. This phase generalises that to **any number of apps, each with its
own independent daily usage limit**, chosen from a list of installed apps.

Decisions locked in with you:
- A rule = **an app + a daily usage limit (minutes)** (no time-of-day schedules
  in this phase).
- Limits are **independent per app** (no shared pool).

Consequence: at most **one rule per app**, so the app's package name is the
rule's natural identity (adding an app that already has a rule just edits it).

## What changes vs. today

```
            BEFORE                               AFTER
  one rule (Instagram, fixed)        list of rules, one per chosen app
  single-rule DataStore              list persisted in DataStore
  service filters == INSTAGRAM       service filters against a live set of
                                       monitored packages
  one screen                         3 screens: rules list · app picker · editor
  UiState = one app's numbers        UiState = list of per-app rows
```

The core engine already generalises cleanly: `DailyUsageProvider`,
`UsageEventParser`, `BlockingPolicy`, and `ForegroundAppHandler` are all already
parameterised by `packageName`. The real work is data (a list instead of one),
the service's package filter, and UI.

## Proposed technical choices (my recommendations — confirm or override)

| Area         | Recommendation | Why / alternative |
| ------------ | -------------- | ----------------- |
| Persistence  | **Serialized list (kotlinx.serialization JSON) in the existing Preferences DataStore** | Smallest new infra; respects the spec's "defer Room" stance (§4.3). List is small (tens of rules). Alternatives: Proto DataStore (more typed) or Room (best when history/queries arrive in a later phase). |
| Navigation   | **Navigation Compose** (`navigation-compose`) | Standard, handles the back stack across 3 screens (spec §4.2 allows it once >1 screen). Lightweight alt: a `when(screen)` state in the ViewModel, no dep. |
| App icons    | **Manual `Drawable → ImageBitmap`** in `InstalledAppProvider`, loaded off the main thread | Avoids adding an image-loading dependency (e.g. Coil) for the one place we need icons. |
| DI           | Keep the **manual `AppContainer`** (no Hilt) | Still small enough; consistent with spec §4.6. |

New dependencies if you accept the above: `kotlinx-serialization-json` (+ the
Kotlin serialization plugin) and `androidx.navigation:navigation-compose`.

## Data model

```kotlin
// BlockingRule already exists; packageName becomes the primary key.
data class BlockingRule(
    val packageName: String,
    val dailyLimitMinutes: Int,
    val enabled: Boolean,
)
```

`RuleRepository` goes from single-rule to collection:

```kotlin
interface RuleRepository {
    val rules: Flow<List<BlockingRule>>
    suspend fun getRules(): List<BlockingRule>
    suspend fun getRule(packageName: String): BlockingRule?   // null = not monitored
    suspend fun upsertRule(rule: BlockingRule)
    suspend fun deleteRule(packageName: String)
    suspend fun setEnabled(packageName: String, enabled: Boolean)
    suspend fun setDailyLimitMinutes(packageName: String, minutes: Int)
}
```

**Migration:** on first launch after the update, if the old single-rule keys
(`daily_limit_minutes`, `blocking_enabled`) exist, seed them as one rule for
`com.instagram.android`, then clear the old keys. (Personal app, so a fallback
of "just start empty" is also acceptable if you prefer.)

## Blocking logic

- `BlockingPolicy.evaluate(packageName)` → look up the rule for that package;
  `null` or disabled ⇒ `Allow`; otherwise compare today's usage to *that rule's*
  limit. Minimal change (it already takes a package name).
- `AppBlockerAccessibilityService`:
  - On connect, **collect `rules`** into a `@Volatile Set<String>` of monitored
    package names (updated whenever rules change).
  - `onAccessibilityEvent` filters with an O(1) set lookup instead of the
    hard-coded Instagram check. Everything else (debounce, mutex, hand-off)
    stays.
  - Refinement: make debounce **per-package** (a `Map<pkg, lastEvalTime>`) so
    quickly switching between two monitored apps isn't collapsed into one.
  - Our own package never has a rule, so we never block ourselves.

## UI / screens (Navigation Compose)

1. **Rules list (home)** — one card per rule: app icon + name, "X of Y min used
   today", an enable toggle, tap to edit. A `+` FAB adds a rule. Empty state when
   no rules. Missing-permission prompts (already conditional) live here as a
   banner.
2. **App picker** — searchable list of launchable installed apps (icon + label),
   excluding our own app and apps that already have a rule. Selecting one creates
   a rule with the default limit and opens the editor.
3. **Rule editor** — reuse `DailyLimitSelector`, an enable switch, and a Delete
   action. (Could be a bottom sheet instead of a full screen — minor.)

`InstalledAppProvider` gains `getLaunchableApps(): List<InstalledApp>` (package,
label, icon) via `PackageManager.queryIntentActivities(MAIN/LAUNCHER)`. The
`<queries>` entry for Android 11+ visibility is already in the manifest.

## State & ViewModel

- `AppBlockerUiState` becomes a list of row models plus permission flags:

```kotlin
data class RuleRow(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val limitMinutes: Int,
    val usedMinutesToday: Long,
    val enabled: Boolean,
)
```

- ViewModel loads rules, queries usage **per rule** (in parallel), maps to rows,
  and exposes add/edit/delete/toggle actions plus app-picker state.
- Optional optimisation: one `UsageStatsManager` query covering all packages
  instead of N queries. Not needed initially.

## Testing

- Update `BlockingPolicyTest` for rule-lookup-by-package (incl. "no rule ⇒
  Allow").
- New `RuleRepository` tests: add / edit / delete / persistence / migration.
- `UsageEventParserTest` and `ForegroundAppHandlerTest` are unaffected (still
  per-package).
- Consider an instrumented test that the monitored-set updates when rules change.

## Risks

- The service now sees **every** window change (not just Instagram), so the
  cheap set-lookup-first filter matters for battery/CPU.
- Icon loading in a long picker list — load lazily, off the main thread.
- Per-manufacturer service-killing still applies (spec §22), unchanged.

## Suggested implementation order

1. Data layer: list-based `RuleRepository` + serialization + migration + tests.
2. Blocking: per-package policy + service monitored-set + per-package debounce.
3. `InstalledAppProvider.getLaunchableApps()` + icon loading.
4. Navigation + rules-list screen + ViewModel rework.
5. App picker + rule editor.
6. Polish: empty states, delete UX, optional single-query usage.

Each step builds and keeps tests green before the next.
