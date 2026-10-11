package com.nichita.swearingtaskapp.domain.roast

import com.nichita.swearingtaskapp.domain.model.RoastLevel.HARD
import com.nichita.swearingtaskapp.domain.model.RoastLevel.MEDIUM
import com.nichita.swearingtaskapp.domain.model.RoastLevel.SOFT
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class EscalationTest {

    private fun hours(h: Long) = Duration.ofHours(h)
    private val justUnder3h = hours(3).minusSeconds(1)
    private val justUnder6h = hours(6).minusSeconds(1)

    @Test
    fun `no escalation below 3 hours`() {
        assertEquals(SOFT, effectiveRoastLevel(SOFT, Duration.ZERO))
        assertEquals(SOFT, effectiveRoastLevel(SOFT, justUnder3h))
        assertEquals(MEDIUM, effectiveRoastLevel(MEDIUM, justUnder3h))
        assertEquals(HARD, effectiveRoastLevel(HARD, Duration.ZERO))
    }

    @Test
    fun `one step from 3 hours`() {
        assertEquals(MEDIUM, effectiveRoastLevel(SOFT, hours(3)))
        assertEquals(MEDIUM, effectiveRoastLevel(SOFT, justUnder6h))
        assertEquals(HARD, effectiveRoastLevel(MEDIUM, hours(3)))
    }

    @Test
    fun `two steps from 6 hours`() {
        assertEquals(HARD, effectiveRoastLevel(SOFT, hours(6)))
        assertEquals(HARD, effectiveRoastLevel(SOFT, hours(12)))
    }

    @Test
    fun `never goes above HARD`() {
        assertEquals(HARD, effectiveRoastLevel(MEDIUM, hours(6)))
        assertEquals(HARD, effectiveRoastLevel(HARD, hours(3)))
        assertEquals(HARD, effectiveRoastLevel(HARD, hours(24)))
    }

    @Test
    fun `escalate ignores negative steps`() {
        assertEquals(MEDIUM, MEDIUM.escalate(-1))
    }
}
