# Chronicle v1.2.0

## What's New

### Application Detail Drill-Down Dossier
* **Interactive App Insights:** Tap any application on the Timeline or Analytics tab to open an in-depth **Application Dossier Sheet**.
* **24-Hour Single-App Activity Profile:** Dedicated 24-column timeline canvas rendering the exact hours and minutes spent in a specific application throughout the day.
* **7-Day Trajectory Bar Chart:** Visualizes multi-day usage history and trends for individual applications.
* **Micro-Check Loop Detector:** Identifies unconscious "ghost" micro-opens where an app was launched and exited in under 30 seconds.

### Custom App Overrides & Categorization Suite
* **Category Reclassification:** Reassign any application to custom categories (e.g. mark YouTube or Web Browsers as Productivity).
* **Per-App Usage Budgets:** Configure custom daily minute time limits on a per-application basis with interactive slider controls.
* **Distraction Flagging:** Tag habit-forming applications as distractions to prioritize them for reality-check alerts and focus enforcement.

### Mindful Discipline Streaks Engine
* **Goal Compliance Streak:** Tracks consecutive days spent within your configured daily screen time budget.
* **Morning Shield Streak:** Tracks consecutive days maintaining digital discipline without opening social, gaming, or entertainment feeds in the first 45 minutes of waking.
* **Sleep Sanctuary Streak:** Tracks consecutive days protecting the hour before bedtime from late-night screen intrusion.
* **All-Time Record:** Celebrates your longest recorded discipline streak on the Analytics dashboard.

### Direct CSV Data Exporter
* **Comprehensive Data Export:** Export complete historical usage records to CSV format with dates, package names, labels, categories, active durations, launch counts, and IST timestamps.
* **System Share Integration:** Exports seamlessly via Android FileProvider for instant sharing or saving to local device storage.

### Home Screen Android Widgets
* **2x2 Screen Time & Goal Widget:** Compact launcher widget showing real-time screen time, daily goal progress, and remaining conscious budget.
* **4x2 Timeline Summary Widget:** Wide widget displaying daily active time, active application count, and your most-used app with duration.
* **Background Sync Refresh:** Automatically synchronizes and updates widget displays whenever background sync completes.

---

## Improvements & Fixes

* **Database Engine Upgrade (v2):** Added `app_custom_overrides` table in Room with automated migration to persist category overrides and per-app limits.
* **Strict IST Timezone Alignment:** Pinned all streak calculations, 24-hour single-app profiles, and morning shield windows to Indian Standard Time (UTC+5:30).
* **Zero Emojis:** Verified complete absence of emojis across all UI layouts, strings, logs, comments, and release documentation.
* **Custom Material 3 Polish:** Custom BottomSheet and Card dialogs with no default Android AlertDialog or Toast components.

---

## Verification & Integrity

* **Unit Testing:** Automated test coverage for `DisciplineStreaks` structures, `CustomAppOverride` configurations, and `UsageInsightsTest`.
* **Clean Architecture:** Maintained strict decoupling across data DAOs, domain use cases, ViewModels, and Jetpack Compose screens.
* **100% Offline & Private:** Zero network analytics, zero third-party telemetry, fully local-first processing.

---

## Installation

* **In-App Update:** Automatically detected on launch via the built-in OTA update manager.
* **Manual Install:** Download `chronicle-v1.2.0.apk` from the Assets below and install on Android 8.0+ (API 26–35, arm64-v8a).
