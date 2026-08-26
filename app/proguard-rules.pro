# Chronicle R8 / ProGuard Configuration

# Preserve Line Numbers for Crash Reporting & Diagnostics
-keepattributes SourceFile,LineNumberTable,EnclosingMethod,InnerClasses,*Annotation*,Signature

# Android Core Components
-keep public class * extends android.app.Application { *; }
-keep public class * extends android.app.Activity { *; }
-keep public class * extends android.app.Service { *; }
-keep public class * extends android.content.BroadcastReceiver { *; }
-keep public class * extends android.appwidget.AppWidgetProvider { *; }

# Hilt / Dagger Dependency Injection
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.ComponentEntryPoint { *; }
-keep @dagger.hilt.EntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.work.HiltWorker class * { *; }
-keep class * extends androidx.hilt.work.HiltWorkerFactory { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
    @dagger.Provides <methods>;
    @dagger.Binds <methods>;
    @dagger.assisted.AssistedInject <init>(...);
    @dagger.assisted.Assisted <fields>;
}
-dontwarn dagger.hilt.**

# Room Database, DAOs & Entities
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep class io.chronicle.usagestats.data.local.** { *; }
-keep class io.chronicle.usagestats.data.local.entity.** { *; }
-keep class io.chronicle.usagestats.data.local.dao.** { *; }
-dontwarn androidx.room.paging.**

# Domain Models & Enums (Critical for DataStore & Serialization)
-keep enum io.chronicle.usagestats.domain.model.** { *; }
-keep class io.chronicle.usagestats.domain.model.** { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Android ViewModels & Lifecycle
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# WorkManager & Background Sync
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class io.chronicle.usagestats.worker.** { *; }

# App Widgets & EntryPoint Accessors
-keep class io.chronicle.usagestats.ui.widget.** { *; }
-keep interface io.chronicle.usagestats.ui.widget.**$* { *; }

# Foreground Services & App Updater
-keep class io.chronicle.usagestats.service.** { *; }
-keep class io.chronicle.usagestats.core.updater.** { *; }
-keep class io.chronicle.usagestats.core.util.** { *; }

# Kotlin Coroutines & Reflection
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose & DataStore
-dontwarn androidx.compose.**
-keep class androidx.datastore.preferences.protobuf.** { *; }
-keep class androidx.datastore.preferences.core.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }
