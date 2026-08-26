# Chronicle v1.5.0

## What's New

### Iron Discipline App Limits & Focus Guard Suite
* **Foreground Focus Guard Service:** Persistent low-overhead monitor service checking active application packages every 2 seconds and enforcing daily time allocations.
* **Full-Screen Hardware-Resistant Blocker:** Full-screen edge-to-edge Compose blocking activity that intercepts system back navigation and redirects users to the home screen.
* **Iron Discipline (No Bypass Mode):** Completely locks restricted applications until the 12:00 AM IST daily reset without override options.
* **Cognitive Typing Challenge:** Grants 5 minutes of emergency access upon typing a 200-character mindful passage with 100% case and punctuation accuracy; resets immediately on any typo.
* **Strict Math Streak Challenge:** Requires solving 5 consecutive multi-digit multiplication problems in sequence; any incorrect answer resets the streak back to Problem 1.
* **App Limits Management Hub:** Searchable applications directory in Settings showing live usage gauges, active limit badges, and modal configuration sheets (5m to 8h slider).

---

## Improvements & Fixes

* **3-Button Navigation Bar Inset Fixes:** Applied `WindowInsets.navigationBars` to Onboarding action buttons and Floating Navigation Bar to eliminate overlap on non-gesture Android devices.
* **Responsive Layout Optimization for Small 16:9 Screens:** Compacted blocker cards, adaptive icon proportions, and enabled vertical scrolling for small-factor displays (e.g. 5.0" 16:9 devices like Lenovo K6 Power).
* **Website Mobile Viewport & App-like Fit:** Configured viewport metadata with `user-scalable=no, viewport-fit=cover` and added `touch-action: pan-y manipulation` to prevent accidental zooming and horizontal panning.
* **Redesigned Floating Download Pill:** Replaced legacy notification toast with a floating frosted glass pill featuring animated spinner state and clean auto-hiding transitions.
* **Zero Emojis Enforced:** Verified 100% zero-emoji compliance across UI components, logs, string resources, and release notes.

---

## Verification & Integrity

* **CI/CD Validation:** Verified linting, unit test suites, and release compilation.
* **Architecture Compliance:** Strict MVVM + Clean Architecture with Room database version 4 and Hilt dependency injection.
* **100% Offline & Private:** Zero network analytics, zero third-party telemetry, all limit enforcement and statistics calculated locally.

---

## Installation

* **In-App Update:** Automatically detected on launch via the built-in update manager.
* **Manual Install:** Download `chronicle-v1.5.0.apk` from the Assets below and install on Android 8.0+ (API 26–35, arm64-v8a).
