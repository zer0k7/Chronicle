## What's New in Chronicle v1.0.8

### 1. Conscious Waking Life Calculator
* **16-Hour Waking Life Metric:** Translates screen time into the true percentage of your conscious awake life spent looking at screens (e.g., `28.1% of conscious waking life`).
* **Annual Days Lost Projection:** Computes and displays projected full 24-hour days lost across the year (e.g., `Projected 68 full days lost in 2026`).

### 2. Ghost Reflex Opens (Micro-Habit Detector)
* **Sub-30s Session Analysis:** Automatically isolates and flags micro-sessions under 30 seconds opened out of reflex or boredom.
* **Reflex Breakdown:** Displays total daily ghost opens and highlights your primary offending habit app (e.g., `Ghost Opens: 24 micro-checks • Top: Instagram (18 opens)`).

### 3. Morning Bed Doomscroll Tracker
* **Dopamine Baseline Metric:** Measures total screen time spent on Social, Entertainment, and Gaming applications within the first 45 minutes of waking up.
* **Bedtime Routine Contrast:** Shows exact duration and top morning distraction app directly alongside night bedtime usage.

### 4. Dynamic Reality-Check Notifications
* **Context-Aware Notifications:** Replaces static reminders with dynamic, targeted reality checks based on actual usage:
  * Heavy Gaming reality checks (>1.5h in games)
  * Doomscroll alerts (>1.5h in social media)
  * Daily Focus alerts (>4.5h screen time with low productivity score)
  * High Discipline celebrations (>65% productive focus)
  * Sleep recovery reminders (late-night bedtime usage)

### 5. App vs App Side-by-Side Comparison
* Compare any two applications head-to-head on the Analytics tab (Screen Time, Launches, Average Session Duration).

---

## Improvements & Bug Fixes

* **In-App Auto-Updater Hardening:**
  * Added multi-hop HTTP 302 cross-domain redirect resolution from GitHub Releases to AWS S3 storage.
  * Implemented atomic temporary file downloads (`.tmp`) with strict 1 MB+ binary sanity validation.
  * Resolved the package parsing error (`There was a problem while parsing the package`) caused by interrupted downloads or stale caches.
  * Added `<external-files-path>` to `file_paths.xml` for full Android OEM compatibility.
* **Analytics Tab Overhaul:** Renamed the 2nd navigation tab from "Reports" to "Analytics" and integrated the full suite of habit cards, category distribution, and app comparison tools.
* **Strict Privacy & Zero Emojis:** 100% on-device processing, zero network telemetry, and full Material 3 dynamic theming.

---

## Installation

* **In-App Update:** Open Chronicle -> Settings -> Check for Updates.
* **Manual Install:** Download `chronicle-v1.0.8.apk` below and open to install over existing builds.
