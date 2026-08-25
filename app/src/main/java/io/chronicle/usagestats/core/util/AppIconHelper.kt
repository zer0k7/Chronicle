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

    companion object {
        private val CURATED_CATEGORIES = mapOf(
            // Games
            "com.activision.callofduty.shooter" to AppCategory.GAMES,
            "com.dts.freefiremax" to AppCategory.GAMES,
            "com.dts.freefireth" to AppCategory.GAMES,
            "com.pubg.imobile" to AppCategory.GAMES,
            "com.tencent.ig" to AppCategory.GAMES,
            "com.epicgames.fortnite" to AppCategory.GAMES,
            "com.ea.gp.apexlegendsmobilefps" to AppCategory.GAMES,
            "com.supercell.clashofclans" to AppCategory.GAMES,
            "com.supercell.brawlstars" to AppCategory.GAMES,
            "com.supercell.clashroyale" to AppCategory.GAMES,
            "com.king.candycrushsaga" to AppCategory.GAMES,
            "com.roblox.client" to AppCategory.GAMES,
            "com.mojang.minecraftpe" to AppCategory.GAMES,

            // Communication & Social
            "com.whatsapp" to AppCategory.COMMUNICATION,
            "com.whatsapp.w4b" to AppCategory.COMMUNICATION,
            "org.telegram.messenger" to AppCategory.COMMUNICATION,
            "org.telegram.messenger.web" to AppCategory.COMMUNICATION,
            "org.thunderdog.challegram" to AppCategory.COMMUNICATION,
            "com.discord" to AppCategory.COMMUNICATION,
            "com.slack" to AppCategory.COMMUNICATION,
            "us.zoom.videomeetings" to AppCategory.COMMUNICATION,
            "com.google.android.apps.tachyon" to AppCategory.COMMUNICATION,
            "com.google.android.talk" to AppCategory.COMMUNICATION,
            "com.instagram.android" to AppCategory.SOCIAL,
            "com.facebook.katana" to AppCategory.SOCIAL,
            "com.facebook.orca" to AppCategory.COMMUNICATION,
            "com.twitter.android" to AppCategory.SOCIAL,
            "com.snapchat.android" to AppCategory.SOCIAL,
            "com.reddit.frontpage" to AppCategory.SOCIAL,
            "com.linkedin.android" to AppCategory.SOCIAL,
            "com.pinterest" to AppCategory.SOCIAL,
            "com.zhiliaoapp.musically" to AppCategory.SOCIAL,

            // Entertainment
            "com.google.android.youtube" to AppCategory.ENTERTAINMENT,
            "com.google.android.apps.youtube.music" to AppCategory.ENTERTAINMENT,
            "com.spotify.music" to AppCategory.ENTERTAINMENT,
            "com.netflix.mediaclient" to AppCategory.ENTERTAINMENT,
            "com.amazon.avod.thirdpartyclient" to AppCategory.ENTERTAINMENT,
            "in.startv.hotstar" to AppCategory.ENTERTAINMENT,
            "com.jio.media.ondemand" to AppCategory.ENTERTAINMENT,
            "com.graymatrix.did" to AppCategory.ENTERTAINMENT,
            "tv.twitch.android.app" to AppCategory.ENTERTAINMENT,

            // Productivity
            "com.google.android.gm" to AppCategory.PRODUCTIVITY,
            "com.microsoft.office.outlook" to AppCategory.PRODUCTIVITY,
            "com.google.android.apps.docs" to AppCategory.PRODUCTIVITY,
            "com.google.android.apps.docs.editors.sheets" to AppCategory.PRODUCTIVITY,
            "com.google.android.apps.docs.editors.slides" to AppCategory.PRODUCTIVITY,
            "com.microsoft.office.word" to AppCategory.PRODUCTIVITY,
            "com.microsoft.office.excel" to AppCategory.PRODUCTIVITY,
            "notion.id" to AppCategory.PRODUCTIVITY,
            "com.todoist" to AppCategory.PRODUCTIVITY,
            "com.anydo" to AppCategory.PRODUCTIVITY,
            "com.ticktick.task" to AppCategory.PRODUCTIVITY,
            "com.google.android.keep" to AppCategory.PRODUCTIVITY,
            "com.google.android.calendar" to AppCategory.PRODUCTIVITY,

            // Utilities
            "com.brave.browser" to AppCategory.UTILITIES,
            "com.android.chrome" to AppCategory.UTILITIES,
            "org.mozilla.firefox" to AppCategory.UTILITIES,
            "com.microsoft.emmx" to AppCategory.UTILITIES,
            "com.google.android.apps.maps" to AppCategory.UTILITIES,
            "com.google.android.deskclock" to AppCategory.UTILITIES,
            "com.android.deskclock" to AppCategory.UTILITIES,
            "com.google.android.calculator" to AppCategory.UTILITIES,
            "com.sec.android.app.clockpackage" to AppCategory.UTILITIES
        )
    }

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

        // 1. Check curated mapping
        CURATED_CATEGORIES[packageName]?.let { return it }

        // 2. Query system ApplicationInfo category
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
