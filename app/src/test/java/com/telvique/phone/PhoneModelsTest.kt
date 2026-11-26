package com.telvique.phone

import com.telvique.phone.data.CallLogItem
import com.telvique.phone.data.CallLogType
import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.PhoneNumberEntry
import com.telvique.phone.data.T9Matcher
import com.telvique.phone.data.groupConsecutiveCallLogs
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

    @Test
    fun isSuspectedTelemarketer_detectsIndian140And160Series() {
        assertTrue(PhoneFormatUtils.isSuspectedTelemarketer("+91 140 812 3456"))
        assertTrue(PhoneFormatUtils.isSuspectedTelemarketer("1600123456"))
        assertFalse(PhoneFormatUtils.isSuspectedTelemarketer("+91 98765 43210"))
    }

    @Test
    fun groupConsecutiveCallLogs_mergesMatchingAdjacentCalls() {
        val now = 1_795_700_000_000L
        val logs = listOf(
            CallLogItem(1L, "+919876543210", "Amma", CallLogType.MISSED, now, 0L, null, null),
            CallLogItem(2L, "9876543210", "Amma", CallLogType.MISSED, now - 60_000L, 0L, null, null),
            CallLogItem(3L, "9876543210", "Amma", CallLogType.OUTGOING, now - 120_000L, 45L, null, null),
        )
        val grouped = groupConsecutiveCallLogs(logs)
        assertEquals(2, grouped.size)
        assertEquals(2, grouped[0].count)
        assertEquals(listOf(1L, 2L), grouped[0].entryIds)
        assertEquals(1, grouped[1].count)
    @Test
    fun normalizeDialInput_preservesUssdCodes() {
        assertEquals("*#06#", PhoneFormatUtils.normalizeDialInput("USSD: *#06#"))
        assertEquals("+919876543210", PhoneFormatUtils.normalizeDialInput("+91 98765-43210"))
    }
}
