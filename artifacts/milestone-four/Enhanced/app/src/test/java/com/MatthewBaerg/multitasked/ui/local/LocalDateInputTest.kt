package com.MatthewBaerg.multitasked.ui.local

import org.junit.Assert.*
import org.junit.Test

class LocalDateInputTest {
    @Test fun emptyMeansNoDeadline() { assertNull(LocalDateInput.parse(" ")) }
    @Test fun dateRoundTrips() { assertEquals("2028-02-29", LocalDateInput.format(LocalDateInput.parse("2028-02-29"))) }
    @Test(expected = IllegalArgumentException::class) fun invalidCalendarDayIsRejected() { LocalDateInput.parse("2026-02-29") }
    @Test(expected = IllegalArgumentException::class) fun trailingTextIsRejected() { LocalDateInput.parse("2026-10-06oops") }
    @Test fun epochUsesUtc() { assertEquals(0L, LocalDateInput.parse("1970-01-01")) }
}
