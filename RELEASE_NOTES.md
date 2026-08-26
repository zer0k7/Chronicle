# Chronicle v1.1.0

## What's New

### Streamlined Timeline & Visual Hierarchy Refactor
* **Clutter-Free Timeline:** Redesigned the primary Timeline view to deliver an immediate, glanceable overview of daily device consumption. Duplicated analytics cards were relocated to the dedicated Analytics hub.
* **Daily Screen Time Target Progress Ring:** Integrated a dynamic screen time progress indicator inside the main summary card that reflects consumption against your configured goal with real-time status color shifts (Green for disciplined usage, Amber for approaching thresholds, Red for goal overages).

### Structured Analytics Hub
* **Organized Categorical Sections:** Restructured the Analytics tab into distinct behavioral domains with dedicated headers:
  * **Usage Breakdown:** High-level category distributions with real-time productivity scores.
  * **Habits and Routine:** Pickups, average session durations, sleep-wake windows, bedtime usage, and longest binge sessions.
  * **Screen Life Impact:** Conscious waking life calculations, annual days lost, and ghost reflex opens under 30 seconds.
  * **Psychological Metrics:** Life Clock projections, Dopamine Debt fasting prescriptions, and phantom reflex unlocks.
  * **App Comparison:** Side-by-side comparative inspection of app sessions, launch frequencies, and active foreground times.
  * **All Apps:** Searchable and category-filtered application rankings.

### Comprehensive Power-User Settings Suite
* **Screen Time Budgets:** Added interactive daily screen time goal configuration (30 minutes to 8 hours) with optional weekend override thresholds.
* **Focus Mode Scheduler:** Configurable weekday focus windows to encourage disciplined digital boundaries during work hours.
* **Enhanced Notifications Control:** Dedicated toggles for motivational reality checks (gaming, social doomscroll, low productivity alerts), positive milestone achievements, and weekend quiet mode.
* **General System Settings:** First day of the week customization (Monday vs. Sunday), configurable daily usage counter reset hour (IST), and uninstalled app visibility controls.
* **Data Management Suite:** One-tap CSV usage history export, custom confirmation dialog for clearing cached statistics, and configurable local data retention windows.
* **Accessibility Enhancements:** Added toggles for dense compact layout and high-contrast text rendering.

---

## Improvements & Fixes

* **Eliminated Redundant Renders:** Resolved duplicate rendering of habit, psychology, and category cards between Timeline and Analytics screens, improving scroll performance.
* **Context-Aware Notifications:** Notification engine now strictly respects reality-check toggles and weekend quiet mode settings before dispatching alarms.
* **Extended Dialog Engine:** Upgraded custom Material 3 dialog components to host interactive sliders, radio groups, and confirmation flows without using stock Android alerts.
* **Strict IST Date Boundaries:** All new schedules, weekend checks, and daily reset operations execute deterministically according to Indian Standard Time (UTC+5:30).
* **Zero Emojis:** Verified complete absence of emojis across all UI strings, logs, comments, and notifications.

---

## Verification & Integrity

* **Clean Architecture Compliance:** Preserved strict boundary separation across DataStore preference repositories, Room local database layers, domain models, and Jetpack Compose ViewModels.
* **Privacy & Local Processing:** All goal checks, focus schedules, and data clear actions execute 100% on-device on background coroutine dispatchers with zero network telemetry.
* **Production Build Validation:** Target SDK 35, Min SDK 26, single ABI release packaging (`arm64-v8a`), and full R8/ProGuard optimization enabled.

---

## Installation

* **In-App Update:** Automatically detected on launch via the built-in OTA update manager.
* **Manual Install:** Download `chronicle-v1.1.0.apk` from the Assets below and install on Android 8.0+ (API 26–35, arm64-v8a).
