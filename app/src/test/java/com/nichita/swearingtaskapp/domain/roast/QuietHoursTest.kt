package com.nichita.swearingtaskapp.domain.roast

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class QuietHoursTest {

    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)

    @Test
    fun `window crossing midnight`() {
        val start = t(23)
        val end = t(8)
        assertTrue(isQuietTime(t(23), start, end))
        assertTrue(isQuietTime(t(2), start, end))
        assertTrue(isQuietTime(t(7, 59), start, end))
        assertFalse(isQuietTime(t(8), start, end))
        assertFalse(isQuietTime(t(14), start, end))
        assertFalse(isQuietTime(t(22, 59), start, end))
    }

    @Test
    fun `window within one day`() {
        val start = t(13)
        val end = t(15)
        assertTrue(isQuietTime(t(13), start, end))
        assertTrue(isQuietTime(t(14, 30), start, end))
        assertFalse(isQuietTime(t(15), start, end))
        assertFalse(isQuietTime(t(9), start, end))
    }

    @Test
    fun `equal bounds disable quiet hours`() {
        assertFalse(isQuietTime(t(0), t(10), t(10)))
        assertFalse(isQuietTime(t(10), t(10), t(10)))
    }
}
