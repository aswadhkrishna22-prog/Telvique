package com.telvique.phone

import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.PhoneNumberEntry
import com.telvique.phone.data.T9Matcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneModelsTest {

    @Test
    fun formatTimer_formatsMinutesAndHours() {
        assertEquals("00:05", PhoneFormatUtils.formatTimer(5))
        assertEquals("02:15", PhoneFormatUtils.formatTimer(135))
        assertEquals("1:01:05", PhoneFormatUtils.formatTimer(3665))
    }

    @Test
    fun numbersMatch_ignoresFormattingAndCountryPrefix() {
        assertTrue(PhoneFormatUtils.numbersMatch("+91 98765 43210", "9876543210"))
        assertFalse(PhoneFormatUtils.numbersMatch("9876543210", "9876500000"))
    }

    @Test
    fun t9Matcher_matchesNameInitialsAndNumber() {
        val alice = ContactItem(
            id = 1L,
            lookupKey = "k1",
            name = "Alice Bob",
            numbers = listOf(PhoneNumberEntry("+1 555-234-5678", "Mobile")),
            photoUri = null,
            isStarred = false
        )

        assertTrue(T9Matcher.matches(alice, "254")) // ALI
        assertTrue(T9Matcher.matches(alice, "22"))  // AB initials
        assertTrue(T9Matcher.matches(alice, "5678"))
        assertFalse(T9Matcher.matches(alice, "999"))
    }
}
