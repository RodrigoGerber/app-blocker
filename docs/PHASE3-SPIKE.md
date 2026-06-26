# Phase 3 — Detection spike runbook

Goal (spec §18): prove on a **real device** that App Blocker can (1) reliably
detect Instagram coming to the foreground and (2) send you Home via
`GLOBAL_ACTION_HOME`, *before* trusting the usage-time math. The limit is
intentionally ignored during the spike.

> Use a physical device. The standard emulator images don't have the real
> Instagram app, and behaviour varies by manufacturer (spec §22).

## 1. Turn on spike mode

In `app/src/main/java/com/example/appblocker/config/AppBlockerConfig.kt`:

```kotlin
const val SPIKE_REDIRECT_ON_DETECT = true   // redirect Home on EVERY detection
const val DRY_RUN = false                    // set true to log without redirecting
const val VERBOSE_WINDOW_LOGGING = true      // log every foreground app's package
```

Recommended first pass: `DRY_RUN = true`. You'll see detections in Logcat
without being bounced out of Instagram. Once detection looks solid, flip
`DRY_RUN = false` to confirm the actual redirect.

## 2. Build & install

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd /Users/gerber/Documents/Projects/app-blocker
./gradlew installDebug
```

(Or just Run ▶ from Android Studio with the device selected.)

## 3. Grant the accessibility permission

Open App Blocker → tap **Enable service** → enable "App Blocker" under
Settings ▸ Accessibility. (Usage access is not needed for the spike, since the
limit is ignored, but you can grant it too.)

## 4. Watch Logcat

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB logcat -c                       # clear old logs
$ADB logcat AppBlockerService:* ForegroundAppHandler:* HomeRedirector:* "*:S"
```

- `AppBlockerService` — "Detected foreground", plus every window package when
  `VERBOSE_WINDOW_LOGGING` is on (logged at VERBOSE; add `*:V` if filtered out).
- `ForegroundAppHandler` — "SPIKE: ... redirecting Home (limit ignored)".
- `HomeRedirector` — a warning only if `performGlobalAction` returns false.

## 5. Exercise every launch path (spec §17.2)

Open Instagram each of these ways and confirm a detection + redirect each time:

- [ ] Tap the launcher icon
- [ ] From a notification
- [ ] From an external link / deep link (e.g. an instagram.com URL)
- [ ] From the recent-apps switcher
- [ ] After locking/unlocking the screen
- [ ] Repeatedly in quick succession (debounce should prevent a Home-action loop)

## Success criteria (spec §18)

- Instagram opening is detected **consistently** across the paths above.
- The redirect to Home fires (with `DRY_RUN = false`).
- It keeps working with App Blocker's own UI closed (the service is independent).
- No runaway loop of Home actions.

## 6. Turn the spike back OFF

Revert all three flags to `false` before normal use:

```kotlin
const val SPIKE_REDIRECT_ON_DETECT = false
const val DRY_RUN = false
const val VERBOSE_WINDOW_LOGGING = false
```

## Notes / gotchas

- If detection misses some launch paths, widen the event set in
  `res/xml/app_blocker_accessibility_service.xml` (e.g. add
  `typeWindowsChanged`) and re-test — that's exactly what this spike is for.
- Some manufacturers (Samsung/Xiaomi/etc.) aggressively kill background
  services or disable accessibility services after a while; check battery
  optimization settings if the service stops firing (spec §22).
