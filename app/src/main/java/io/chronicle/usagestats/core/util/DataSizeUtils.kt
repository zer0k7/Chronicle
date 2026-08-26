package io.chronicle.usagestats.core.util

import java.util.Locale

object DataSizeUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        val tb = gb / 1024.0

        return when {
            tb >= 1.0 -> String.format(Locale.ENGLISH, "%.2f TB", tb)
            gb >= 1.0 -> String.format(Locale.ENGLISH, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.ENGLISH, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.ENGLISH, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatBytesDetailed(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val mb = bytes / (1024.0 * 1024.0)
        val gb = mb / 1024.0

        return if (gb >= 1.0) {
            String.format(Locale.ENGLISH, "%.2f GB (%,d bytes)", gb, bytes)
        } else {
            String.format(Locale.ENGLISH, "%.1f MB (%,d bytes)", mb, bytes)
        }
    }

    fun bytesToMegabytes(bytes: Long): Double {
        return bytes / (1024.0 * 1024.0)
    }

    fun bytesToGigabytes(bytes: Long): Double {
        return bytes / (1024.0 * 1024.0 * 1024.0)
    }
}
