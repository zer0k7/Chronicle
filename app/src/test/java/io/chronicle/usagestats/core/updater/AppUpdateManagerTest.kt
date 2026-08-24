package io.chronicle.usagestats.core.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) return num1.compareTo(num2)
        }
        return 0
    }

    @Test
    fun testVersionComparison_NewerVersionAvailable() {
        assertTrue(compareVersions("1.0.2", "1.0.1") > 0)
        assertTrue(compareVersions("1.1.0", "1.0.9") > 0)
        assertTrue(compareVersions("2.0.0", "1.9.9") > 0)
        assertTrue(compareVersions("1.0.10", "1.0.9") > 0)
    }

    @Test
    fun testVersionComparison_SameVersion() {
        assertEquals(0, compareVersions("1.0.1", "1.0.1"))
        assertEquals(0, compareVersions("1.0.0", "1.0"))
    }

    @Test
    fun testVersionComparison_OlderVersion() {
        assertTrue(compareVersions("1.0.0", "1.0.1") < 0)
        assertTrue(compareVersions("0.9.9", "1.0.0") < 0)
    }
}
