# Chronicle v1.7.0

> [!IMPORTANT]
> **100% Offline · Zero Telemetry · Clean Architecture · Target SDK 35**

---

### Release Snapshot

| Component | Status | Key Upgrade |
| :--- | :--- | :--- |
| **Iron Focus Blocker** | `ACTIVE` | Real-time `UsageEvents` stream + Dual-Vector Full-Screen Intent lockout |
| **Privacy Architecture** | `HARDENED` | Redundant media permissions purged; Scoped Storage enforcement |
| **Smart Notifications** | `ACTIVE` | Midday checks, surge alarms, and instant single-tap channel dismissals |
| **Permission Engine** | `OPTIMIZED` | In-app visual 2-step setup & Android 13+ restricted setting helper |

---

## What's New

* **Event-Driven App Limits:** Overhauled background monitoring from periodic polling to microsecond `UsageEvents` stream tracking. Exceeded app limits now trigger **instant hardware-level lockouts** over target applications across Android 10 through 15 via dual-vector full-screen intents (`CATEGORY_ALARM`).
* **Granular Dismissal Controls:** Toggling any notification channel in Settings now immediately purges active notifications from the Android status shade and cancels registered alarms.
* **Streamlined Onboarding Flow:** Integrated visual step-by-step guidance cards, trust shield banners, and a 1-tap shortcut to App Details to bypass Android 13+ restricted setting barriers.

---

## Improvements & Fixes

* **Permission Cleanup:** Removed unused `READ_MEDIA_IMAGES` declaration; all infographic exports use zero-permission Scoped Storage.
* **Active Session Live Tracking:** App limits now dynamically calculate ongoing in-app duration to trigger mid-session the moment limits expire.
* **Zero Emojis Verified:** Strict compliance across all UI, system logs, resources, and documentation.

<details>
<summary><b>View Detailed Engineering Diff</b></summary>

```diff
+ UsageEvents.Event.ACTIVITY_RESUMED real-time stream
+ Full-Screen Intent (CATEGORY_ALARM, PRIORITY_MAX) dual-vector dispatch
+ NotificationManager instant active-tray dismissal logic
+ In-app Android 13+ Restricted Settings guidance dialogs
- uses-permission android.permission.READ_MEDIA_IMAGES
```
</details>

---

## Verification & Integrity

### Cryptographic Binary Verification
Verify the build integrity and authenticity of the release APK:

```bash
# Verify release binary checksum
sha256sum chronicle-v1.7.0.apk
```

```yaml
Build Artifact  : chronicle-v1.7.0.apk
Target SDK      : 35 (Android 15)
Min SDK         : 26 (Android 8.0)
Architecture    : arm64-v8a (R8 Optimized)
Timezone Engine : Indian Standard Time (IST, UTC+5:30)
Storage Policy  : Local SQLite Room v5 (No Cloud Sync)
```

---

## Installation

* **In-App Update:** Automatically detected on launch via Chronicle's built-in updater.
* **Manual Download:** Fetch `chronicle-v1.7.0.apk` from the Assets below.
