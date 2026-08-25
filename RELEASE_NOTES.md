# Chronicle v1.0.10

## What's New

### Unbounded Multi-Year Historical Retention
* **Permanent Local Archival:** Extended Room database engine to permanently retain usage logs across months and years without temporal truncation.
* **Autonomous Historical Backfill:** Navigating or jumping to past dates automatically synchronizes and persists historical telemetry directly from Android UsageStatsManager.

### Multi-Scope PDF Dossier & Infographic Export
* **Custom Material 3 Export Scope Dialog:**
  * **Today (Single Day):** Comprehensive single-day breakdown with 24-column timeline and top apps.
  * **Last 7 Days (Weekly Dossier):** Executive summary, daily consumption averages, and cumulative application matrix.
  * **Last 30 Days (Monthly Dossier):** Long-term category distribution, aggregate screen time trends, and rank lists.
  * **Custom Date Range:** Integrated date pickers allowing arbitrary multi-day vector PDF dossier generation.
* **Multi-Page Executive PDF Generator:** Generates formatted vector PDF reports with summary metrics, category distributions, and paginated application tables.

### Advanced Psychological & Behavioural Analytics Engine
* **Life Clock Projection:** Mathematically projects cumulative full years of conscious waking life lost to screen consumption by age 75 based on actual daily consumption.
* **Dopamine Debt & Screen Fasting:** Measures excess screen time against the 2.5-hour daily cognitive baseline and prescribes exact digital fasting recovery periods.
* **Phantom Reflex Unlocks:** Detects compulsive micro-checks where the device is unlocked and locked again within 10 seconds without launching any application.

### Landing Page Silent Direct Downloads
* **In-Place APK Downloading:** Updated website download handler with silent background iframe execution and bottom toast notifications to eliminate page redirects and tab opening.

---

## Improvements & Fixes

* **Telemetry Accuracy:** Every psychological insight and timeline aggregate is computed strictly from real hardware sensor timestamps and system usage events (zero mock or dummy entries).
* **Database Optimization:** Added specialized Room index queries for earliest recorded timestamp, total days tracked, and date range summaries.
* **Strict IST Calculations:** Pinned all multi-day date boundaries, historical backfills, and daily averages to Indian Standard Time (UTC+5:30).
* **UI Polish:** Material 3 styled insight cards and dialogs with zero emojis across all layouts, strings, and notifications.

---

## Verification & Integrity

* **Deterministic Testing:** Automated unit tests in `UsageInsightsTest` verify mathematical calculations for Life Clock projections, Dopamine Debt formulas, Phantom Unlock detections, and conscious life percentages.
* **Architecture Integrity:** Clean Architecture + MVVM boundary separation maintained across domain models, Room DAO queries, and Jetpack Compose presentation layers.
* **Security & Privacy:** 100% offline, zero network tracking, zero external dependencies, signed with official production keystore.

---

## Installation

* **In-App Update:** Automatically detected on launch via the built-in SHA-256 verified OTA updater.
* **Manual Install:** Download `chronicle-v1.0.10.apk` from the Assets below and install on Android 8.0+ (API 26–35, arm64-v8a).
