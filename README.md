# Chronicle

Chronicle is a privacy-first, local-first Android application designed to track and visualize device screen time and application usage patterns. Designed around an always-on wallpaper aesthetic, Chronicle aggregates daily, weekly, monthly, and yearly activity into high-density visual grids and paginated reports.

## Features

- **Usage Timeline:** Wallpaper-inspired visual matrix representing application utilization across Day, Week, Month, and Year periods.
- **In-App Analytics:** Comprehensive daily breakdowns, category aggregations, launch counts, and total active durations.
- **Removed App Preservation:** Maintains historical records for uninstalled applications under a dedicated Removed Apps category without altering aggregate totals.
- **Paginated PDF Generation:** On-device vector PDF report generation with customizable date ranges.
- **High-Resolution Image Export:** Render and share snapshot cards directly to the system gallery or messaging applications.
- **Indian Standard Time (IST) Support:** All calculations, interval boundaries, and background synchronizations are aligned to Indian Standard Time (UTC+5:30).
- **Material 3 Design System:** Full support for Light, Dark, AMOLED Black, and Material You dynamic color schemes, accompanied by a curated accent color selector.
- **Automated Summaries:** Scheduled daily notifications and launcher badge indicators.

## Architecture

Chronicle is built according to Modern Android Architecture guidelines, utilizing Clean Architecture and MVVM design patterns:

- **Presentation Layer:** Jetpack Compose, Material 3, Navigation Compose, Custom UI Overlays.
- **Domain Layer:** Business logic, Use Cases, Models, Repository interfaces.
- **Data Layer:** Room Database for persistent analytics, DataStore Preferences for user configuration, Android `UsageStatsManager` synchronization on background coroutine dispatchers.
- **Dependency Injection:** Hilt.
- **Background Tasks:** WorkManager and AlarmManager for reliable notification scheduling.

## Permissions

| Permission | Purpose |
|---|---|
| `android.permission.PACKAGE_USAGE_STATS` | Required to query application screen time and launch metrics. |
| `android.permission.POST_NOTIFICATIONS` | Required on Android 13+ to dispatch daily summary notifications. |
| `android.permission.QUERY_ALL_PACKAGES` | Required to map package identifiers to application labels and icons. |
| `android.permission.SCHEDULE_EXACT_ALARM` | Required for deterministic notification delivery at the configured time. |
| `android.permission.READ_MEDIA_IMAGES` / `WRITE_EXTERNAL_STORAGE` | Required for saving exported image cards to public storage on supported API levels. |

## Build Configuration

### Prerequisites
- JDK 17
- Android SDK Platform 35
- Gradle 8.8+

### Building from Source

To compile the release APK:

```bash
./gradlew assembleRelease
```

To run the local unit test suite:

```bash
./gradlew testDebugUnitTest
```

To run lint checks:

```bash
./gradlew lint
```

## Security & Privacy

Chronicle operates entirely on-device. No telemetry, analytics, or application usage data is transmitted over the network. Application backups are explicitly disabled (`android:allowBackup="false"`), and release builds are minified and obfuscated via R8.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
