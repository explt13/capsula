package com.capsula.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `countdown under an hour shows minutes and seconds`() {
        assertEquals("01:30", TimeFormatter.countdown(90_000))
    }

    @Test
    fun `countdown rounds seconds up so a sealed capsule never shows zero`() {
        assertEquals("00:01", TimeFormatter.countdown(1))
    }

    @Test
    fun `countdown with hours`() {
        assertEquals("01:01:01", TimeFormatter.countdown(3_661_000))
    }

    @Test
    fun `countdown with days`() {
        assertEquals("1d 01h 01m 01s", TimeFormatter.countdown(Durations.DAY + 3_661_000))
    }

    @Test
    fun `human duration picks the largest unit`() {
        assertEquals("1 day", TimeFormatter.humanDuration(Durations.DAY))
        assertEquals("3 days", TimeFormatter.humanDuration(3 * Durations.DAY))
        assertEquals("2 months", TimeFormatter.humanDuration(65 * Durations.DAY))
        assertEquals("1 year", TimeFormatter.humanDuration(400 * Durations.DAY))
        assertEquals("less than a minute", TimeFormatter.humanDuration(10_000))
    }
}
