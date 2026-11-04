package com.focus.launcher

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogicTest {

    @Test fun wordClockRoundsToFiveMinutes() {
        assertEquals("nine o'clock", timeInWords(9, 1))
        assertEquals("quarter past nine", timeInWords(9, 14))
        assertEquals("twenty past nine", timeInWords(9, 21))
        assertEquals("half past nine", timeInWords(9, 31))
        assertEquals("quarter to ten", timeInWords(9, 44))
        assertEquals("five to ten", timeInWords(9, 56))
        assertEquals("ten o'clock", timeInWords(9, 58))
        assertEquals("twelve o'clock", timeInWords(23, 59))
    }

    @Test fun legacyFontNamesMigrate() {
        assertEquals(FontChoice.OUTFIT, FontChoice.parse("LIGHT", FontChoice.MANROPE))
        assertEquals(FontChoice.JETBRAINS_MONO, FontChoice.parse("MONO", FontChoice.MANROPE))
        assertEquals(FontChoice.CAVEAT, FontChoice.parse("CURSIVE", FontChoice.MANROPE))
        assertEquals(FontChoice.FRAUNCES, FontChoice.parse("FRAUNCES", FontChoice.MANROPE))
        assertEquals(FontChoice.MANROPE, FontChoice.parse("nonsense", FontChoice.MANROPE))
        assertEquals(300, FontChoice.SPACE_GROTESK.clamp(100))
    }

    @Test fun movingEssentialsByKey() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), list.moved("b", -1))
        assertEquals(listOf("a", "c", "b"), list.moved("b", 1))
        assertEquals(list, list.moved("a", -1))
        assertEquals(list, list.moved("missing", 1))
    }

    @Test fun dailyLimitAndCounters() {
        val s = LauncherSettings(ApplicationProvider.getApplicationContext())
        assertFalse(s.limitReached("x"))
        s.dailyOpenLimit = 2
        s.recordOpen("x")
        assertFalse(s.limitReached("x"))
        s.recordOpen("x")
        assertTrue(s.limitReached("x"))
        assertEquals(2, s.opensToday("x"))
        s.recordResisted()
        assertEquals(1, s.resistedToday)
        s.dailyOpenLimit = 0
        assertFalse(s.limitReached("x"))
    }
}
