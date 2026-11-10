package com.example.pundarapp.data.stellar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StroopsTest {
    @Test fun formatsSevenDecimals() {
        assertEquals("1.0000000", Stroops.toXlmString(10_000_000L))
        assertEquals("0.0000001", Stroops.toXlmString(1L))
        assertEquals("33.3333334", Stroops.toXlmString(333_333_334L))
    }

    @Test fun parsesAndRoundsDown() {
        assertEquals(15_000_000L, Stroops.fromXlmString("1.5"))
        assertEquals(1L, Stroops.fromXlmString("0.00000019"))
        assertNull(Stroops.fromXlmString("abc"))
    }

    @Test fun roundTrips() {
        listOf(1L, 99L, 10_000_000L, 123_456_789L).forEach {
            assertEquals(it, Stroops.fromXlmString(Stroops.toXlmString(it)))
        }
    }
}
