# Chronicle v1.6.0

## What's New

### Notification Influx & Attention Fragmentation Radar
* **Privacy-Hardened Influx Monitoring:** On-device `NotificationListenerService` logging arrival timestamps, source packages, and interruption categories without accessing or storing notification titles, body text, or sender information.
* **Attention Fragmentation Index (0–100):** Real-time fragmentation metric classifying user distraction into four distinct tiers: Minimal Disruption, Moderate Fragmentation, Elevated Restlessness, and Severe Influx Chaos.
* **24-Hour Hourly Ping Density Canvas:** Interactive visual breakdown of notification frequency across all 24 hours in Indian Standard Time (IST), featuring tap-to-inspect hourly details.
* **Bait-and-Switch Detection:** Correlates notification timestamps with immediate foreground application launches within 45 seconds to expose disruptive apps that lure users into doomscroll loops in other apps.
* **Top Disruptor Ranking:** Identifies the apps creating the largest concentration of disruptive notifications throughout the day.
* **Notification Radar Card:** Material 3 presentation component on the Report screen with interactive hourly inspection and one-tap permission onboarding.

### Quick Settings Tiles (TileService) Suite
* **Screen Time Tile (`ScreenTimeTileService`):** Live screen time telemetry directly in the Android notification shade with dynamic budget percentage and warning states; single tap deep-links to the 24-hour timeline.
* **Zen Focus Tile (`ZenFocusTileService`):** One-tap toggle to immediately initiate or cancel a 25-minute deep focus block with remaining time countdown.
* **Data Burn Tile (`DataBurnTileService`):** Displays today's cellular mobile SIM data consumption and carrier quota pacing status (`Safe` vs `High Burn`); single tap launches the Network Data dashboard.
* **Background Tile Synchronization:** Automated `TileService.requestListeningState` triggers integrated into periodic background sync routines to keep tile metrics accurate without opening the app.

### Multi-Touchpoint Smart Notification Engine
* **Midday Reality Check (14:00 IST):** Proactive midday check-in detailing accrued screen time, current top application, and progress against the user's daily budget.
* **Distraction Surge Warnings:** Instant alerts triggered when incoming notifications exceed 30 pings per hour.
* **Budget Milestone Alerts:** Proactive warnings delivered upon reaching 80% and 100% of the daily screen time allowance.
* **Reliability Overhaul:** Platform-hardened alarm scheduling handling exact alarm capabilities and `SecurityException` fallbacks on Android 12 through 15, paired with a periodic WorkManager safety net.

---

## Improvements & Fixes

* **Data Burn Velocity Card Aspect-Ratio Fix:** Re-engineered the header layout in `DataDepletionForecastCard` with flexible text bounds, ellipsis truncation, and a compact non-wrapping status pill badge to prevent layout distortion across all device aspect ratios, resolutions, and font scaling settings.
* **Deep Linking Navigation Routing:** Added intent route handling in `MainActivity` and `ChronicleNavGraph` for seamless single-tap navigation from system shade Quick Settings tiles.
* **AAPT String Resource Compilation:** Added explicit formatting attributes to prevent percentage signs in settings descriptions from triggering non-positional format errors during asset processing.
* **Platform Backward Compatibility:** Annotated tile service launch fallbacks with `@SuppressLint("StartActivityAndCollapseDeprecated")` and explanatory comments to ensure clean lint validation while preserving compatibility on Android versions below API 34.
* **Room Schema v5 Migration:** Added indexed entities and queries for persistent notification telemetry and historical analytics.
* **Zero Emojis Enforced:** Maintained complete compliance with Chronicle's zero-emoji mandate across all resources, code, logs, and release documentation.

---

## Verification & Integrity

* **CI/CD Validation:** Verified linting, unit test suites, and release compilation in GitHub Actions.
* **100% Offline & Private:** Zero network analytics and zero external server dependencies; all notification metadata, usage stats, and limits operate entirely on-device.
* **Timezone Consistency:** All hourly bucketing, scheduled pacing, and notifications strictly adhere to Indian Standard Time (IST, UTC+5:30).
* **Target SDK 35 Compatibility:** Built with Kotlin 100%, Material 3, Clean Architecture, and Android 8.0 through 15 compatibility.

---

## Installation

* **In-App Update:** Automatically detected on launch via Chronicle's built-in update manager.
* **Manual Install:** Download `chronicle-v1.6.0.apk` from the Releases Assets and install on Android 8.0+ (API 26–35, arm64-v8a).
