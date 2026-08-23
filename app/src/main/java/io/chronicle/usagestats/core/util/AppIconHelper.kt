package io.chronicle.usagestats.core.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import io.chronicle.usagestats.R
import io.chronicle.usagestats.domain.model.AppCategory
import java.util.concurrent.ConcurrentHashMap

class AppIconHelper(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val labelCache = ConcurrentHashMap<String, String>()
    private val iconCache = ConcurrentHashMap<String, Drawable>()

    fun getAppLabel(packageName: String, fallbackLabel: String? = null): String {
        labelCache[packageName]?.let { return it }

        val label = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            fallbackLabel ?: formatPackageNameToLabel(packageName)
        }

        labelCache[packageName] = label
        return label
    }

    fun getAppIcon(packageName: String): Drawable? {
        iconCache[packageName]?.let { return it }

        val icon = try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            ContextCompat.getDrawable(context, R.drawable.ic_chronicle_logo)
        }

        if (icon != null) {
            iconCache[packageName] = icon
        }
        return icon
    }

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getAppCategory(packageName: String): AppCategory {
        if (!isAppInstalled(packageName)) {
            return AppCategory.REMOVED
        }

        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
                ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> AppCategory.ENTERTAINMENT
                ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_ACCESSIBILITY, ApplicationInfo.CATEGORY_MAPS, ApplicationInfo.CATEGORY_NEWS -> AppCategory.UTILITIES
                else -> {
                    if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                        AppCategory.SYSTEM
                    } else {
                        AppCategory.OTHER
                    }
                }
            }
        } catch (_: Exception) {
            AppCategory.OTHER
        }
    }

    private fun formatPackageNameToLabel(packageName: String): String {
        val parts = packageName.split(".")
        return if (parts.isNotEmpty()) {
            parts.last().replaceFirstChar { it.uppercase() }
        } else {
            packageName
        }
    }
}
