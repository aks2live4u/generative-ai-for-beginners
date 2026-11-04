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
        assertEquals(FontChoice.MAJOR_MONO, FontChoice.parse("MONO", FontChoice.MANROPE))
        assertEquals(FontChoice.CURSIVE, FontChoice.parse("CURSIVE", FontChoice.MANROPE))
        assertEquals(FontChoice.CASUAL, FontChoice.parse("CASUAL", FontChoice.MANROPE))
        assertEquals(FontChoice.OUTFIT, FontChoice.parse("SORA", FontChoice.MANROPE))
        assertEquals(FontChoice.MAJOR_MONO, FontChoice.parse("JETBRAINS_MONO", FontChoice.MANROPE))
        assertEquals(FontChoice.FRAUNCES, FontChoice.parse("FRAUNCES", FontChoice.MANROPE))
        assertEquals(FontChoice.MANROPE, FontChoice.parse("nonsense", FontChoice.MANROPE))
        assertEquals(300, FontChoice.COMFORTAA.clamp(100))
    }

    @Test fun reasonsFitTheApp() {
        val browser = reasonsFor(AppKind.BROWSER).map { it.label }
        assertFalse(browser.any { "Reply" in it })
        assertTrue(reasonsFor(AppKind.MESSAGING).any { it.label == "Reply to someone" })
        assertTrue(reasonsFor(AppKind.SHOPPING).first().ask != null)
        AppKind.entries.forEach { kind ->
            val r = reasonsFor(kind)
            assertTrue("$kind needs an honest option", r.any { it.idle })
            assertTrue("$kind needs a purposeful option", r.any { !it.idle })
        }
        assertFalse(AppKind.MESSAGING.isDistracting)
    }

    @Test fun worldClockHelpers() {
        val here = java.time.ZonedDateTime.of(2026, 11, 4, 23, 30, 0, 0, java.time.ZoneId.of("Asia/Kolkata"))
        val london = here.withZoneSameInstant(java.time.ZoneId.of("Europe/London"))
        val sydney = here.withZoneSameInstant(java.time.ZoneId.of("Australia/Sydney"))
        assertEquals("\u22125h 30m", relativeOffset(london, here))
        assertEquals("+5h 30m", relativeOffset(sydney, here))
        assertEquals("same time", relativeOffset(here, here))
        assertEquals("tomorrow", dayShift(sydney.toLocalDate(), here.toLocalDate()))
        assertEquals(null, dayShift(london.toLocalDate(), here.toLocalDate()))
        assertEquals("Buenos Aires", cityName("America/Argentina/Buenos_Aires"))
        assertEquals("UTC+5:30", utcLabel("Asia/Kolkata"))

        val s = LauncherSettings(ApplicationProvider.getApplicationContext())
        s.worldClocks = listOf("Europe/London", "Asia/Tokyo", "Europe/London", "Asia/Dubai", "Asia/Singapore", "Pacific/Auckland")
        assertEquals(listOf("Europe/London", "Asia/Tokyo", "Asia/Dubai", "Asia/Singapore"), s.worldClocks)
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
