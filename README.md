<div align="center">

<img src="assets/logo.svg" alt="Chronicle Logo" width="120" height="120" />

# Chronicle

### Privacy-First Screen Time & Psychological Habit Analytics for Android

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI Toolkit](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-FF6F00?style=flat-square)](https://developer.android.com/topic/architecture)
[![CI Pipeline](https://img.shields.io/github/actions/workflow/status/zer0k7/Chronicle/ci.yml?branch=main&label=CI%20Build&style=flat-square)](https://github.com/zer0k7/Chronicle/actions)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Release ABI](https://img.shields.io/badge/ABI-arm64--v8a%20(Production)-00E5FF?style=flat-square)](https://github.com/zer0k7/Chronicle/releases)

---

<p align="center">
  <b>Chronicle</b> is a high-density, local-first Android application designed to track, analyze, and psychologically reconstruct your digital consumption patterns. Built with an always-on wallpaper aesthetic, Chronicle transforms raw phone usage into deep behavioural metrics: conscious life calculations, ghost reflex opens, morning dopamine baseline tracking, and 24-hour interactive session inspection.
</p>

</div>

---

## Architectural Comparison: Chronicle vs Stock Wellbeing

| Capability | Default System Wellbeing | Chronicle Pro Engine |
|---|:---:|:---:|
| **Hourly 24-Hour Timeline** | Single static total | **Interactive 24-column tap-to-filter strip** |
| **Conscious Life Calculator** | Not available | **Calculates % of 16h waking day & annual days lost** |
| **Ghost Reflex Loop Detection** | Not available | **Isolates micro-checks under 30s opened out of habit** |
| **Morning Bed Doomscroll** | Not available | **Tracks consumption within 45m of waking up** |
| **Dynamic Tough-Love Notifications** | Static generic counter | **Context-aware reality checks on gaming & feeds** |
| **App vs App Comparison** | Not available | **Head-to-head metrics, launches, and session length** |
| **Longest Continuous Session** | Not available | **Identifies single longest binge session with times** |
| **Removed App Preservation** | Wiped upon uninstall | **100% historical usage retained and categorized** |
| **Data Export** | Restricted | **Paginated vector PDF & High-Res image export** |
| **Privacy & Telemetry** | Cloud account binding | **100% on-device local Room storage, zero network tracking** |

---

## Key Features

### 1. Conscious Waking Life Calculator
Raw numbers like *4 hours 30 mins* fail to trigger behavioural urgency. Chronicle translates your total foreground usage into the exact proportion of your **16-hour conscious waking life** spent staring at screens, projecting the total full 24-hour days lost across the calendar year.

### 2. Ghost Reflex Opens (Micro-Habit Detector)
Detects and isolates unconscious muscle-memory loops where applications are opened for **under 30 seconds** out of reflex or boredom. Identifies your primary offending habit app to break dopamine feedback loops.

### 3. Morning Bed Doomscroll Metric
Measures screen time on Social Media, Streaming, and Gaming applications during the critical **45-minute window immediately following your first morning unlock**. Helps safeguard your morning dopamine baseline.

### 4. Interactive 24-Hour Timeline Bar Chart
A high-density 24-column canvas rendering usage across all 24 hours of the day (00:00 to 23:59). Tapping any individual hour highlights that time slot and dynamically filters the application list to show exclusively what ran during that window.

### 5. Dynamic Reality-Check Notifications
Replaces uninspired daily counter notifications with context-aware, targeted alerts:
* **Heavy Gaming Reality Check:** Triggers when gaming exceeds 90 minutes.
* **Doomscroll Alert:** Triggers when social media feeds exceed 90 minutes.
* **Focus & Low Productivity Alert:** Alerts on high screen time with low productivity score.
* **High Discipline Celebration:** Acknowledges focused work sessions above 65% productivity.
* **Sleep Recovery Notice:** Highlights screen time immediately prior to bedtime.

### 6. App vs App Side-by-Side Comparison
Compare any two installed or removed applications head-to-head on the Analytics tab, evaluating total screen time, launch frequency, and true average session duration.

### 7. Historical Removed Apps Retention
When an application is uninstalled from your device, Chronicle preserves its historical records under a dedicated **Removed Apps** category, ensuring cumulative daily, weekly, monthly, and yearly totals remain strictly accurate.

### 8. Paginated Vector PDF & Infographic Export
Generate polished, multi-page vector PDF reports or render high-resolution infographic summary cards directly on-device for sharing or archival.

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
                  |   Room DB  |  DataStore  |  UsageStats Engine   |
                  +-------------------------------------------------+
                                           |
                                           v
                  +-------------------------------------------------+
                  |              Background Workers                 |
                  |       WorkManager  |  Exact AlarmManager        |
                  +-------------------------------------------------+
```

* **Presentation Layer:** Declarative UI with Jetpack Compose, dynamic Material You color extraction, custom-themed dialogs, and navigation rail / floating navigation bars.
* **Domain Layer:** Business use cases including PDF generation, image rendering, category aggregations, and trend mathematics.
* **Data Layer:** Room database for persistent local analytics, DataStore for user settings and budgets, and `UsageStatsRepositoryImpl` leveraging the **Activity-Aware State Machine** for multi-activity games and apps.
* **Background Tasks:** Deterministic AlarmManager execution for Indian Standard Time (IST, UTC+5:30) notification dispatch.

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
| **Database** | SQLite via AndroidX Room |
| **Concurrency** | Kotlin Coroutines & Reactive StateFlow |

---

## Permissions Overview

| Permission | Scope & Purpose |
|---|---|
| `PACKAGE_USAGE_STATS` | System permission granting read access to foreground session timestamps and launch counts. |
| `POST_NOTIFICATIONS` | Required on Android 13+ to deliver scheduled daily summaries and reality-check alerts. |
| `QUERY_ALL_PACKAGES` | Allows mapping package identifiers to installed application labels and high-resolution icons. |
| `SCHEDULE_EXACT_ALARM` | Guarantees deterministic notification execution at configured user times. |
| `READ_MEDIA_IMAGES` | Allows saving generated report infographics directly to the system photo gallery. |

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
