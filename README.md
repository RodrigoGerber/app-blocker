# App Blocker

Personal-use Android app that limits daily Instagram use. Once the day's usage
reaches your configured limit, any new attempt to open Instagram bounces you
back to the Home screen. The goal is friction, not an unbreakable lock — see
[`app-blocker-spec.md`](../../app-blocker-spec.md) for the full specification.

## Status

Scaffold of the MVP described in the spec. All layers are wired and compile-ready;
the detection/blocking behaviour still needs to be validated on a real device
(spec §18 "spike técnico").

## Tech stack

| Concern        | Choice                                    |
| -------------- | ----------------------------------------- |
| Language       | Kotlin                                     |
| UI             | Jetpack Compose + Material 3               |
| Persistence    | Preferences DataStore                      |
| Detection      | `AccessibilityService`                     |
| Usage metering | `UsageStatsManager`                        |
| Block action   | `performGlobalAction(GLOBAL_ACTION_HOME)`  |
| DI             | Hand-rolled `AppContainer` (no Hilt)       |
| Min / target   | `minSdk 26` / `targetSdk 34`               |

These defaults (minSdk 26, package `com.example.appblocker`) were chosen during
scaffolding and are easy to change — adjust `app/build.gradle.kts` and the
`namespace`/`applicationId` if you want something else.

## Project layout

```
app/src/main/java/com/example/appblocker/
├── AppBlockerApplication.kt   # owns the DI container
├── MainActivity.kt            # Compose host, lifecycle-driven refresh
├── config/                    # constants (Instagram package, default limit, DRY_RUN)
├── di/                        # AppContainer — manual dependency wiring
├── ui/                        # state, ViewModel, screen, components, theme
├── accessibility/             # AccessibilityService, ForegroundAppHandler, checker
├── blocking/                  # BlockingPolicy, BlockingDecision, HomeRedirector
├── usage/                     # DailyUsageProvider, UsageEventParser (+ Android impl)
├── rules/                     # BlockingRule, RuleRepository (DataStore-backed)
└── system/                    # SettingsNavigator, InstalledAppProvider, ClockProvider
```

The decision logic (`BlockingPolicy`) and the usage math (`UsageEventParser`)
are deliberately framework-free so they can be unit tested on the JVM. Tests
live in `app/src/test/`.

## Building

The Gradle wrapper (8.7) is committed, so you can build straight away.

**In Android Studio:** just open the folder and Sync. It uses its bundled JBR
automatically — nothing else needed.

**From the terminal:** Gradle 8.7 does not support JDK 25, so point `JAVA_HOME`
at a JDK 17–21. Android Studio's bundled JBR (21) works well:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew assembleDebug      # build the debug APK
./gradlew testDebugUnitTest  # run unit tests (BlockingPolicy, UsageEventParser)
./gradlew installDebug       # install on a connected device/emulator
```

The SDK path is read from `local.properties` (git-ignored, machine-specific).

## First-run setup on device

App Blocker needs two permissions, both granted manually in system Settings
(there are no runtime dialogs for these):

1. **Usage access** — so it can measure how long Instagram has been used.
2. **Accessibility service** — so it can detect Instagram opening and send you
   Home.

The app surfaces both states on its single screen with buttons that deep-link to
the right Settings page.

## Developing the detection spike

Set `AppBlockerConfig.DRY_RUN = true` (in `config/AppBlockerConfig.kt`) while
working on detection. The service will log every detection but won't actually
redirect you Home, so you don't lock yourself out of Instagram mid-development
(spec §19, Phase 3).

## What's intentionally out of scope (MVP)

Multiple apps, per-weekday rules, time-window blocks, overlays, temporary
unlocks, history/charts, accounts, cloud sync, Play Store distribution. See
spec §3.2 and §21 for the deferred list.
```
