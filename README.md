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
  <b>Chronicle</b> is a high-density, local-first Android application designed to track, analyze, and psychologically reconstruct your digital consumption patterns. Built with an always-on wallpaper aesthetic, Chronicle transforms raw device telemetry into actionable insights: conscious life calculations, ghost reflex loop detection, morning dopamine baseline tracking, per-app Wi-Fi/Mobile/Hotspot data monitoring, and 24-hour interactive session inspection.
</p>

</div>

---

## Architectural Comparison: Chronicle vs Stock Wellbeing

| Capability | Default System Wellbeing | Chronicle Pro Engine |
|---|:---:|:---:|
| **Hourly 24-Hour Timeline** | Single static total | **Interactive 24-column tap-to-filter canvas** |
| **Network & Data Telemetry** | Basic system carrier total | **Per-app Wi-Fi, Mobile SIM, Rx/Tx & Hotspot tracking** |
| **App Detail Drill-Down Dossier** | Basic total minutes | **24h activity strip, 7d trajectory, ghost opens & budget slider** |
| **Mindful Discipline Streaks** | Not available | **Goal compliance, Morning Shield, and Sleep Sanctuary streaks** |
| **Conscious Life Calculator** | Not available | **Calculates % of 16h waking day & annual days lost** |
| **Ghost Reflex Loop Detection** | Not available | **Isolates micro-checks under 30s opened out of habit** |
| **Morning Bed Doomscroll** | Not available | **Tracks consumption within 45m of waking up** |
| **Dynamic Tough-Love Notifications** | Static generic counter | **Context-aware reality checks on gaming & feeds** |
| **App vs App Comparison** | Not available | **Head-to-head metrics, launches, and session length** |
| **Removed App Preservation** | Wiped upon uninstall | **100% historical usage retained and categorized** |
| **Home Screen Widgets** | Generic / None | **2x2 Goal Ring and 4x2 Mini-Timeline Widgets** |
| **Data Export** | Restricted | **Paginated vector PDF, high-res PNG & Full CSV export** |
| **Privacy & Telemetry** | Cloud account binding | **100% on-device local Room storage, zero network tracking** |

---

## Core Feature Suite

### 1. Network Data, Wi-Fi & Hotspot Telemetry Suite
Monitor your daily and historical bandwidth consumption with precise hardware-level segmentation:
* **Wi-Fi vs Mobile SIM Split:** Displays real-time device totals and visual proportional ratio bars.
* **Per-App Data Telemetry:** Breakdown of Download (Rx) vs Upload (Tx) bytes for every application.
* **Mobile Hotspot & Tethering Monitor:** Isolates and tracks data shared with connected laptops and secondary devices via kernel `UID_TETHERING`.
* **Carrier Data Budgets:** Configure daily mobile data limits (e.g. 2.0 GB) and billing cycle reset days with warning thresholds.

### 2. Application Detail Drill-Down Dossier
Tap any application on the Timeline or Analytics screen to open a modal dossier sheet:
* **24-Hour Single-App Profile:** Hourly bar strip displaying the exact hours an app was used today.
* **7-Day Trajectory Chart:** Multi-day historical trend analysis for individual applications.
* **Micro-Check Loop Detector:** Identifies and counts unconscious reflex opens lasting under 30 seconds.
* **Custom App Overrides:** Reclassify categories, set individual daily minute budgets, and toggle distraction flags.

### 3. Mindful Discipline Streaks Engine
Build sustainable digital habits through non-gamified discipline metrics:
* **Goal Compliance Streak:** Consecutive days staying within your daily screen time budget.
* **Morning Shield Streak:** Consecutive days protecting your morning baseline from social and gaming feeds within 45 minutes of waking.
* **Sleep Sanctuary Streak:** Consecutive days protecting the hour before bedtime from late-night screen intrusion.
* **All-Time Record:** Celebrates your longest recorded discipline streak.

### 4. Conscious Waking Life Calculator
Raw numbers like *4 hours 30 mins* fail to trigger behavioural urgency. Chronicle translates your total foreground usage into the exact proportion of your **16-hour conscious waking life** spent staring at screens, projecting the total full 24-hour days lost across the calendar year.

### 5. Interactive 24-Hour Timeline Bar Chart
A high-density 24-column canvas rendering usage across all 24 hours of the day (00:00 to 23:59). Tapping any individual hour highlights that time slot and dynamically filters the application list to show exclusively what ran during that window.

### 6. Dynamic Reality-Check Notifications
Replaces uninspired daily counter notifications with context-aware, targeted alerts:
* **Heavy Gaming Reality Check:** Triggers when gaming exceeds 90 minutes.
* **Doomscroll Alert:** Triggers when social media feeds exceed 90 minutes.
* **Focus & Low Productivity Alert:** Alerts on high screen time with low productivity score.
* **High Discipline Celebration:** Acknowledges focused work sessions above 65% productivity.
* **Sleep Recovery Notice:** Highlights screen time immediately prior to bedtime.

### 7. Full CSV, PDF & Infographic Exporter
* **CSV Data Exporter:** One-tap export of historical usage records with dates, packages, categories, durations, and timestamps via Android `FileProvider`.
* **Paginated Vector PDF:** Multi-page reports with thematic color palettes and category distribution charts.
* **Shareable Infographics:** High-resolution PNG summary cards rendered directly on-device.

### 8. Home Screen Android Widgets
* **2x2 Goal Ring Widget:** Real-time screen time, daily goal progress, and remaining conscious budget.
* **4x2 Timeline Summary Widget:** Overview of daily active time, active application count, and your most-used application.
* **Background Sync Integration:** Automatically updates widget displays upon background data synchronization.

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
                  |        Use Cases, Domain Models, Contracts      |
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
                  |              Background Workers                 |
                  |       WorkManager  |  Exact AlarmManager        |
                  +-------------------------------------------------+
```

* **Presentation Layer:** Declarative UI with Jetpack Compose, dynamic Material You color extraction, custom-themed dialogs, and navigation rail / floating navigation bars.
* **Domain Layer:** Clean business use cases for timeline aggregation, streak evaluation, data usage filtering, and report generation.
* **Data Layer:** Room database (v3) for persistent usage snapshots and custom overrides, DataStore for preferences, and `NetworkStatsDataSource` querying platform `NetworkStatsManager`.
* **Background Tasks:** WorkManager synchronization and deterministic AlarmManager execution for Indian Standard Time (IST, UTC+5:30) notification schedules.

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
| `POST_NOTIFICATIONS` | Required on Android 13+ to deliver scheduled daily summaries and reality-check alerts. |
| `QUERY_ALL_PACKAGES` | Allows mapping package identifiers to installed application labels and high-resolution icons. |
| `SCHEDULE_EXACT_ALARM` | Guarantees deterministic notification execution at configured user times. |
| `READ_MEDIA_IMAGES` | Allows saving generated report infographics directly to the system photo gallery. |
| `ACCESS_NETWORK_STATE` | Allows querying active network interface states for Wi-Fi and Mobile data monitoring. |

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
