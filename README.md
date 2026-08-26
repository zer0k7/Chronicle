<div align="center">

<img src="assets/logo.svg" alt="Chronicle Logo" width="120" height="120" />

# Chronicle

### Privacy-First Screen Time, Network Telemetry & Behavioral Analytics for Android

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI Toolkit](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-FF6F00?style=flat-square)](https://developer.android.com/topic/architecture)
[![CI Pipeline](https://img.shields.io/github/actions/workflow/status/zer0k7/Chronicle/ci.yml?branch=main&label=CI%20Build&style=flat-square)](https://github.com/zer0k7/Chronicle/actions)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Release ABI](https://img.shields.io/badge/ABI-arm64--v8a%20(Production)-00E5FF?style=flat-square)](https://github.com/zer0k7/Chronicle/releases)

---

<p align="center">
  <b>Chronicle</b> is a high-density, local-first Android application designed to track, analyze, and psychologically reconstruct your digital consumption patterns. Built with an always-on wallpaper aesthetic, Chronicle transforms raw device telemetry into actionable intelligence: conscious life calculations, ghost reflex loop detection, predictive burnout forecasts, continuous doomscroll radar, per-app Wi-Fi/Mobile SIM/Hotspot data monitoring, interactive 24-hour network canvases, and dynamic Material You home screen widgets.
</p>

</div>

---

## Architectural Comparison: Chronicle Pro Suite vs Stock Wellbeing

| Capability | Default System Wellbeing | Chronicle Pro Engine |
|---|:---:|:---:|
| **Hourly 24-Hour Timeline** | Single static total | **Interactive 24-column tap-to-filter canvas** |
| **Network & Data Telemetry** | Basic system carrier total | **Per-app Wi-Fi, Mobile SIM, Rx/Tx & Hotspot tracking** |
| **Hourly Network Traffic Canvas** | Not available | **24-hour dual-stream Wi-Fi vs Mobile SIM spike canvas** |
| **Multi-Day Bandwidth Charts** | Not available | **7-day to 30-day historical progression curves** |
| **Overnight Sleep Drain Detector** | Not available | **Detects silent background cellular/battery drain (00:00 - 07:00 IST)** |
| **Wi-Fi Preferred App Warnings** | Not available | **Tags bandwidth-heavy apps with amber cellular warnings** |
| **Live Network Speed Meter** | Not available | **Optional real-time transfer rate foreground service** |
| **Screen Time Forecast & Burnout** | Not available | **End-of-day projection and 4-tier cognitive fatigue risk score** |
| **Continuous Doomscroll Radar** | Not available | **Prolonged session detector with 20-20-20 optic rest timer** |
| **Distraction Cascade Analyzer** | Not available | **Reveals sequential rabbit-hole triggers (e.g. WhatsApp to Instagram)** |
| **Carrier Quota Depletion Radar** | Not available | **Live data burn rate (MB/hr) and projected exhaustion time** |
| **Weekly Executive Briefing** | Generic recap | **Executive scorecard with conscious hours reclaimed & efficiency score** |
| **Digital Zen Shield** | Hard stock block | **Mindful reflection prompt with 15-second conscious friction timer** |
| **Dynamic Material You Widgets** | Generic / None | **5 bespoke widgets extracting live wallpaper colors (Android 12+)** |
| **App Detail Drill-Down Dossier** | Basic total minutes | **24h activity strip, 7d trajectory, ghost opens & budget slider** |
| **Removed App Preservation** | Wiped upon uninstall | **100% historical usage retained and categorized** |
| **Data Export** | Restricted | **Paginated vector PDF, high-res PNG & Full CSV export** |
| **Privacy & Telemetry** | Cloud account binding | **100% on-device local Room storage, zero network tracking** |

---

## Core Feature Suite

### 1. Network Data, Wi-Fi & Cellular Telemetry Suite
Monitor your daily and historical bandwidth consumption with precise hardware-level segmentation:
* **Wi-Fi vs Mobile SIM Split:** Mathematical separation without double-counting; standalone Mobile Hotspot & Tethering tracking via kernel `UID_TETHERING`.
* **24-Hour Hourly Network Canvas:** Interactive 24-column bar chart for Day view displaying hourly Wi-Fi vs Mobile data spikes with tap-to-filter app breakdowns.
* **Multi-Day Historical Bandwidth Chart:** 7-day to 30-day bandwidth trend graphs for Week and Month views.
* **Category Bandwidth Distribution Bar:** Proportional visualization across Streaming, Social, Productivity, Gaming, Communication, and System categories.
* **Overnight Sleep Drain Detector:** Identifies and isolates silent background cellular and Wi-Fi data drains occurring during sleep hours (00:00 to 07:00 IST).
* **Wi-Fi Preferred App Flags:** Mark heavy apps as Wi-Fi preferred with amber warning badges when consuming mobile data.
* **Live Network Speed Meter:** Optional lightweight foreground service delivering real-time transfer rates (KB/s, MB/s) in the notification shade.
* **Carrier Data Quota & Billing Cycle:** Daily mobile data limits, billing cycle anchor days, and custom warning thresholds.
* **Dedicated Data Export:** Export custom date ranges as paginated vector PDFs or high-resolution PNG summary images.

### 2. Dynamic Material You Home Screen Widgets (5 Bespoke Widgets)
Built with Android 12+ (API 31+) dynamic wallpaper color extraction (`@android:color/system_*` tokens) and smooth dark/light theme adaptation:
1. **Daily Command Center (4x2 / 4x3):** Big bold screen time, daily goal progress ring, `% of goal reached`, and top 3 most-used apps with duration indicators.
2. **Live Network Telemetry (3x2 / 4x2):** Grand total bandwidth, dedicated Mobile SIM vs Wi-Fi telemetry cards, and daily quota progress gauge.
3. **Digital Zen & Focus (2x2):** Focused gauge with screen time, goal progress bar, and mindful state indicator (*Disciplined Flow*, *Nearing Budget*, *Over Budget*).
4. **Habits & Ghost Reflex Monitor (4x2):** Total device unlocks, pickup frequency rate (*Every 15m*), and ghost reflex micro-checks (<30s).
5. **At-a-Glance Telemetry Pill (2x1 / 3x1):** Sleek horizontal pill displaying Screen Time on the left and Total Data Bandwidth on the right.

### 3. Cognitive & Telemetry Intelligence Engines
* **Screen Time Forecast & Cognitive Burnout Index:** Real-time predictive pacing calculating projected end-of-day screen time and evaluating cognitive fatigue into a 4-tier Burnout Risk score (*Low*, *Moderate*, *Elevated*, *High*).
* **Continuous Doomscroll Radar:** Prolonged session detector that flags un-interrupted app usage (>30m) and launches an interactive **20-20-20 Eye Rest Reset** timer.
* **Distraction Cascade & Habit Loop Analyzer:** Discovers sequential app-launch rabbit holes (*Trigger App -> Destination App*, e.g., WhatsApp immediately triggering Instagram).
* **Carrier Quota Depletion Radar:** Computes live cellular burn rate (MB/hr) and projects the exact minute your mobile quota will run out.
* **Weekly Executive Briefing Card:** Executive scorecard in Week view reviewing conscious hours reclaimed, longest focus streak, top distraction trigger, and digital efficiency rating.
* **Digital Zen Shield:** Mindful reflection pause with deep breathing prompts and a 15-second friction extension timer for app limits.

### 4. Application Detail Drill-Down Dossier
Tap any application on the Timeline or Analytics screen to open a modal dossier sheet:
* **24-Hour Single-App Profile:** Hourly bar strip displaying the exact hours an app was used today.
* **7-Day Trajectory Chart:** Multi-day historical trend analysis for individual applications.
* **Micro-Check Loop Detector:** Identifies and counts unconscious reflex opens lasting under 30 seconds.
* **Custom App Overrides:** Reclassify categories, set individual daily minute budgets, toggle distraction flags, and mark Wi-Fi preferred status.

### 5. Mindful Discipline Streaks Engine
* **Goal Compliance Streak:** Consecutive days staying within your daily screen time budget.
* **Morning Shield Streak:** Consecutive days protecting your morning baseline from social and gaming feeds within 45 minutes of waking.
* **Sleep Sanctuary Streak:** Consecutive days protecting the hour before bedtime from late-night screen intrusion.
* **All-Time Record:** Tracks and preserves your longest recorded discipline streak.

### 6. Full CSV, PDF & Infographic Exporter
* **CSV Data Exporter:** One-tap export of historical usage records with dates, packages, categories, durations, and timestamps via Android `FileProvider`.
* **Paginated Vector PDF:** Multi-page reports with thematic color palettes, category distribution charts, and data breakdowns.
* **Shareable Infographics:** High-resolution PNG summary cards rendered directly on-device.

### 7. Iron Discipline App Limits & Blocker Suite
* **Hardcore App Restrictions:** Set daily minute budgets per application with live foreground polling via `AppLimitMonitorService`.
* **Iron Discipline Mode (No Bypass):** Complete app lockout with zero bypass mechanisms until 12:00 AM IST.
* **Cognitive Typing Challenge:** Type a 200-character mindful passage with zero mistakes and disabled auto-correct to unlock 5 minutes of emergency access.
* **Strict Math Challenge:** Solve 5 consecutive multi-digit multiplication problems in sequence with streak reset penalties.
* **Smart App Limits Sub-Screen:** Searchable installed apps categorized into active limits vs unconstrained apps with dynamic progress bars.

---

## System Architecture

Chronicle is built according to Modern Android Architecture standards, enforcing strict layer separation:

```
                  +-------------------------------------------------+
                  |               Presentation Layer                |
                  |     Jetpack Compose + Material 3 + ViewModels   |
                  +-------------------------------------------------+
                                           |
                                           v
                  +-------------------------------------------------+
                  |                  Domain Layer                   |
                  |   Use Cases, Intelligence Engines, Contracts    |
                  +-------------------------------------------------+
                                           |
                                           v
                  +-------------------------------------------------+
                  |                   Data Layer                    |
                  |   Room DB  |  DataStore  | NetworkStats Engine  |
                  +-------------------------------------------------+
                                           |
                                           v
                  +-------------------------------------------------+
                  |              Background Services                |
                  |   WorkManager | LiveSpeedService | AlarmManager |
                  +-------------------------------------------------+
```

* **Presentation Layer:** Declarative UI with Jetpack Compose, Material You dynamic color extraction, custom-themed dialogs, and navigation rail / floating navigation bars.
* **Domain Layer:** Clean business use cases for timeline aggregation, streak evaluation, data usage filtering, predictive telemetry forecasting, and report generation.
* **Data Layer:** Room database for persistent usage snapshots and custom overrides, DataStore for preferences, and `NetworkStatsDataSource` querying platform `NetworkStatsManager`.
* **Background Tasks:** WorkManager synchronization, foreground speed services, and deterministic AlarmManager execution for Indian Standard Time (IST, UTC+5:30) notification schedules.

---

## Security & Privacy Framework

* **Zero Telemetry:** No analytics SDKs, trackers, or network logging libraries are bundled.
* **Local-First Processing:** All usage statistics calculations occur strictly on-device on background IO coroutine dispatchers.
* **Hardened Manifest:** Application backup is explicitly disabled (`android:allowBackup="false"`), and release builds enforce `android:debuggable="false"`.
* **Scoped Storage & FileProvider:** Exported reports and temporary update files are accessed exclusively through strict `FileProvider` content URIs.
* **R8 Minification:** Production releases are minified, resource-shrunk, and obfuscated using optimized ProGuard/R8 configurations.

---

## Technical Specifications

| Parameter | Specification |
|---|---|
| **Language** | Kotlin 100% |
| **Minimum SDK** | Android 8.0 (API Level 26) |
| **Target / Compile SDK** | Android 15 (API Level 35) |
| **Primary ABI** | arm64-v8a (64-bit production release) |
| **Timezone Reference** | Indian Standard Time — IST (UTC+5:30) |
| **Dependency Injection** | Dagger Hilt |
| **Database** | SQLite via AndroidX Room (Schema v3) |
| **Concurrency** | Kotlin Coroutines & Reactive StateFlow |

---

## Permissions Overview

| Permission | Scope & Purpose |
|---|---|
| `PACKAGE_USAGE_STATS` | System permission granting read access to foreground session timestamps, launches, and NetworkStats telemetry. |
| `POST_NOTIFICATIONS` | Required on Android 13+ to deliver scheduled daily summaries, live speed meter, and reality-check alerts. |
| `QUERY_ALL_PACKAGES` | Allows mapping package identifiers to installed application labels and high-resolution icons. |
| `SCHEDULE_EXACT_ALARM` | Guarantees deterministic notification execution at configured user times. |
| `READ_MEDIA_IMAGES` | Allows saving generated report infographics directly to the system photo gallery. |
| `ACCESS_NETWORK_STATE` | Allows querying active network interface states for Wi-Fi and Mobile data monitoring. |
| `FOREGROUND_SERVICE_DATA_SYNC` | Allows running the optional real-time network speed meter in the notification shade. |

---

## Building from Source

### Prerequisites
* JDK 17 (Eclipse Temurin or OpenJDK)
* Android SDK Platform 35
* Gradle 8.8+

### Compilation Commands

To build the signed production release APK:

```bash
./gradlew assembleRelease
```

To run the local unit test suite:

```bash
./gradlew testDebugUnitTest
```

To run Android lint analysis:

```bash
./gradlew lint
```

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for complete details.
