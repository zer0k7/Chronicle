# Chronicle v1.4.1

## What's New

### Advanced Network Telemetry & Data Intelligence Suite
* **24-Hour Hourly Data Bar Chart:** Interactive 24-column distribution canvas displaying dual Wi-Fi vs Mobile SIM data traffic per hour, with tap-to-inspect app breakdowns.
* **Multi-Day Historical Bandwidth Trends:** 7-day to 30-day historical bandwidth trend graphs for long-term carrier quota tracking.
* **Category Bandwidth Distribution Bar:** Proportional multi-segment telemetry bar ranking bandwidth by application domain (Social, Entertainment, Productivity, Gaming, Communication, Utilities, System).
* **Sleep Data Leak Detector:** Automatic overnight diagnostic scanner identifying silent background data drains between 00:00 and 07:00 IST.
* **Real-Time Data Depletion Forecaster:** Dynamic projection model calculating current burn rate (MB/hour) against carrier allowances to predict exact depletion timestamps.
* **Application Data Detail Modal:** Drilldown sheet featuring Wi-Fi Preferred toggle, warning badges for high-consumption apps, and system app settings shortcuts.
* **Live Network Speed Foreground Service:** Optional notification shade speed meter displaying real-time download and upload transfer rates.
* **Comprehensive Data Export:** Export granular network reports across any date range as high-resolution PNG image cards or paginated vector PDFs.

### 5-Tier Material You Home Screen Widget Suite
* **Daily Command Center Widget (Medium 4x2 / 4x3):** Screen time gauge, goal compliance ring, and real-time top 3 application breakdowns.
* **Live Network Telemetry Widget (Medium 3x2 / 4x2):** Total bandwidth monitor, daily mobile quota progress, and Wi-Fi vs Mobile SIM telemetry.
* **Digital Zen Widget (Small 2x2):** Mindful focus ring with conscious usage metrics and on-track pacing status.
* **Habits & Ghost Reflex Widget (Medium 4x2):** Pickups, unlocks, and micro-check (<30s) compulsive reopen tracker.
* **Compact Telemetry Pill (Small 2x1 / 3x1):** Streamlined at-a-glance pill showing active screen time and network bandwidth.
* **Android 12+ Wallpaper Dynamic Color Extraction:** Full dynamic color integration across Android 12 through Android 15 (`@android:color/system_accent1_*` and `@android:color/system_neutral1_*`).

### Cognitive Intelligence & Behavioral Psychology Engines
* **Screen Time Forecasting & Cognitive Burnout Index:** Real-time end-of-day screen time projections and 4-tier Cognitive Burnout Index (*Low*, *Moderate*, *Elevated*, *High*).
* **Doomscroll Radar & 20-20-20 Eye Rest:** Prolonged continuous session detector (>30m) with 20-20-20 optometric eye rest countdown timers.
* **Distraction Cascade Analyzer:** Sequential app transition pattern recognition identifying habit loops (*Trigger App -> Target App*).
* **Weekly Executive Intelligence Briefing:** Executive scorecard aggregating weekly focus time, conscious hours reclaimed, and distraction indexes.
* **Digital Zen Shield Modal:** Mindful reflection overlay with a 15-second friction timer when approaching application limits.

---

## Improvements & Fixes

* **R8 / ProGuard Shrinker Protection:** Added comprehensive rules preserving Hilt generated components, Widget EntryPoint accessors, Room DAOs, ViewModels, and domain enums (`ThemeMode`, `AccentColorPreset`, `AppCategory`, `BurnoutRisk`) to eliminate release runtime crashes.
* **Resilient Startup Pipeline:** Hardened `MainActivity` and `ChronicleApplication` with fallback preferences and defensive initialization to guarantee crash-free launches across all Android OEM skins (MIUI, HyperOS, One UI, Pixel).
* **RemoteViews Divider Whitelist:** Fully compliant with Android AppWidget RemoteViews specifications.
* **Zero Emojis Enforced:** Zero emojis anywhere in UI, resources, logs, or release documentation.

---

## Verification & Integrity

* **Automated CI Validation:** Passed all `./gradlew lint`, `./gradlew testDebugUnitTest`, and `./gradlew assembleRelease` checks.
* **Unit Testing:** Comprehensive test coverage in `NetworkDataUsageTest` covering burn rate projections, habit loop transitions, and executive briefings.
* **100% Offline & Private:** Zero third-party telemetry, zero cloud tracking, strictly local processing and Room persistence.

---

## Installation

* **In-App Update:** Automatically detected on launch via the built-in update manager.
* **Manual Install:** Download `chronicle-v1.4.1.apk` from the Assets below and install on Android 8.0+ (API 26–35, arm64-v8a).
