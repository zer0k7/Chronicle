# Chronicle v1.3.0

## What's New

### Dedicated Network Data Telemetry Suite (4th Tab)
* **Dedicated Data Tab:** Integrated into the floating pill bottom navigation bar and two-pane navigation rail (`Timeline`, `Analytics`, `Data`, `Settings`).
* **Real-Time Bandwidth Hero Card:** Live overview of total network data consumed across Day, Week, Month, and Billing Cycle periods.
* **Hardware-Level Segmentation:** Direct separation of **Wi-Fi** vs **Mobile SIM** data with proportional distribution ratio bars.
* **Per-Application Network Consumption:** Ranks installed and removed applications by total data usage, complete with percentage badges and explicit **Download (Rx)** vs **Upload (Tx)** telemetry.

### Mobile Hotspot & Tethering Monitor
* **Kernel Tethering Isolation:** Tracks and isolates network traffic transmitted to connected laptops, tablets, and secondary devices via kernel `UID_TETHERING`.
* **Hotspot Summary Card:** Dedicated card showing exact hotspot data consumption for the active period.

### Carrier Data Budgets & Billing Cycle Engine
* **Daily Mobile Data Budget:** Configure target daily carrier limits (256 MB to 5 GB) with dynamic progress bars and percentage threshold warnings.
* **Monthly Quota & Billing Cycle:** Set total monthly data allowances (5 GB to 200 GB) aligned to your carrier's billing cycle reset day (1st to 31st).
* **High Usage Alerts:** Configurable notification warnings when approaching 90% of daily data quota.

---

## Improvements & Fixes

* **Database Engine Upgrade (v3):** Added `daily_data_summaries` and `app_data_usage` tables in Room with automated migration.
* **Background Network Synchronization:** Integrated network stats sync into `DailyUsageSyncWorker` for continuous local history updates.
* **Permission Resilience:** Non-intrusive Material 3 permission banner guiding users to system Usage Access settings when required.
* **Zero Emojis:** Verified complete absence of emojis across all UI screens, logs, resources, and release notes.

---

## Verification & Integrity

* **Unit Testing:** Full automated test coverage for data size conversions, network aggregation, and filter pipelines in `NetworkDataUsageTest`.
* **Clean Architecture:** Strict separation between Android `NetworkStatsManager`, Room DAOs, domain use cases, ViewModels, and Jetpack Compose composables.
* **100% Offline & Private:** Zero third-party network SDKs, zero telemetry tracking, strictly on-device processing.

---

## Installation

* **In-App Update:** Automatically detected on launch via the built-in OTA update manager.
* **Manual Install:** Download `chronicle-v1.3.0.apk` from the Assets below and install on Android 8.0+ (API 26–35, arm64-v8a).
