package com.telvique.phone

import com.telvique.phone.data.CallLogItem
import com.telvique.phone.data.CallLogType
import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.PhoneNumberEntry
import com.telvique.phone.data.SlideToAnswerUtils
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
    fun formatDisplayNumber_formatsIndianNumbersCorrectly() {
        assertEquals("97469 85190", PhoneFormatUtils.formatDisplayNumber("9746985190", "IN"))
        assertEquals("+91 97469 85190", PhoneFormatUtils.formatDisplayNumber("+919746985190", "IN"))
        assertEquals("097469 85190", PhoneFormatUtils.formatDisplayNumber("09746985190", "IN"))
        assertEquals("*121#", PhoneFormatUtils.formatDisplayNumber("*121#", "IN"))
        assertEquals("Private Number", PhoneFormatUtils.formatDisplayNumber("Private Number", "IN"))
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
    }

    @Test
    fun normalizeDialInput_preservesUssdCodes() {
        assertEquals("*#06#", PhoneFormatUtils.normalizeDialInput("USSD: *#06#"))
        assertEquals("+919876543210", PhoneFormatUtils.normalizeDialInput("+91 98765-43210"))
    }

    @Test
    fun callLogItem_displayTitle_namePriority() {
        // 1. Saved contact name takes priority over operator CNAP name
        val withContactAndCnap = CallLogItem(
            id = 1L,
            number = "+919876543210",
            cachedName = "Amma",
            type = CallLogType.INCOMING,
            timestamp = 1000L,
            durationSeconds = 10L,
            simLabel = null,
            photoUri = null,
            cnapName = "AIRTEL USER"
        )
        assertEquals("Amma", withContactAndCnap.displayTitle)

        // 2. Unknown number uses CNAP operator name when available
        val unknownWithCnap = CallLogItem(
            id = 2L,
            number = "+919876543210",
            cachedName = null,
            type = CallLogType.INCOMING,
            timestamp = 1000L,
            durationSeconds = 10L,
            simLabel = null,
            photoUri = null,
            cnapName = "RAMESH KUMAR"
        )
        assertEquals("RAMESH KUMAR", unknownWithCnap.displayTitle)

        // 3. Fallback to phone number when neither contact nor CNAP name exists
        val unknownWithoutCnap = CallLogItem(
            id = 3L,
            number = "+919876543210",
            cachedName = null,
            type = CallLogType.INCOMING,
            timestamp = 1000L,
            durationSeconds = 0L,
            simLabel = null,
            photoUri = null,
            cnapName = null
        )
        assertEquals("+919876543210", unknownWithoutCnap.displayTitle)

        // 4. Fallback to Private Number if number is blank
        val privateNumber = CallLogItem(
            id = 4L,
            number = "",
            cachedName = null,
            type = CallLogType.MISSED,
            timestamp = 1000L,
            durationSeconds = 0L,
            simLabel = null,
            photoUri = null,
            cnapName = null
        )
        assertEquals("Private Number", privateNumber.displayTitle)
    }

    @Test
    fun slideToAnswer_thresholdAndAlphaCalculations() {
        val maxDragPx = 600f
        val threshold = maxDragPx * 0.72f

        // Releasing below threshold must not complete
        assertFalse(400f >= threshold)

        // Releasing at or above threshold must complete
        assertTrue(threshold >= threshold)
        assertTrue(450f >= threshold)
        assertTrue(600f >= threshold)

        // Offset clamping
        val dragNegative = (-50f).coerceIn(0f, maxDragPx)
        assertEquals(0f, dragNegative, 0.001f)

        val dragOver = (750f).coerceIn(0f, maxDragPx)
        assertEquals(maxDragPx, dragOver, 0.001f)

        // Text alpha fades smoothly before thumb reaches completion
        fun textAlpha(offset: Float): Float = (1f - (offset / maxDragPx) * 1.5f).coerceIn(0f, 1f)

        assertEquals(1.0f, textAlpha(0f), 0.001f)
        assertEquals(0.25f, textAlpha(300f), 0.001f)
        assertEquals(0.0f, textAlpha(450f), 0.001f)
        assertEquals(0.0f, textAlpha(600f), 0.001f)
    }

    @Test
    fun longPressToDecline_durationAndProgressCalculations() {
        assertEquals(1000L, SlideToAnswerUtils.DEFAULT_HOLD_DURATION_MS)

        // Progress calculations
        assertEquals(0.0f, SlideToAnswerUtils.calculateProgress(0L), 0.001f)
        assertEquals(0.25f, SlideToAnswerUtils.calculateProgress(250L), 0.001f)
        assertEquals(0.5f, SlideToAnswerUtils.calculateProgress(500L), 0.001f)
        assertEquals(1.0f, SlideToAnswerUtils.calculateProgress(1000L), 0.001f)
        assertEquals(1.0f, SlideToAnswerUtils.calculateProgress(1500L), 0.001f) // Clamped at 1.0f

        // Hold completion detection
        assertFalse(SlideToAnswerUtils.isHoldCompleted(0L))
        assertFalse(SlideToAnswerUtils.isHoldCompleted(999L))
        assertTrue(SlideToAnswerUtils.isHoldCompleted(1000L))
        assertTrue(SlideToAnswerUtils.isHoldCompleted(1200L))

        // Sweep angle calculations for the circular progress arc
        assertEquals(0f, SlideToAnswerUtils.calculateSweepAngle(0.0f), 0.001f)
        assertEquals(90f, SlideToAnswerUtils.calculateSweepAngle(0.25f), 0.001f)
        assertEquals(180f, SlideToAnswerUtils.calculateSweepAngle(0.5f), 0.001f)
        assertEquals(270f, SlideToAnswerUtils.calculateSweepAngle(0.75f), 0.001f)
        assertEquals(360f, SlideToAnswerUtils.calculateSweepAngle(1.0f), 0.001f)
        assertEquals(360f, SlideToAnswerUtils.calculateSweepAngle(1.2f), 0.001f) // Clamped
    }

    @Test
    fun longPressToDecline_touchSlopAndCancellation() {
        val touchSlopPx = 16f

        // Movement within touch slop does NOT cancel hold
        assertFalse(SlideToAnswerUtils.shouldCancelHoldForMovement(0f, touchSlopPx))
        assertFalse(SlideToAnswerUtils.shouldCancelHoldForMovement(8f, touchSlopPx))
        assertFalse(SlideToAnswerUtils.shouldCancelHoldForMovement(16f, touchSlopPx))

        // Movement exceeding touch slop cancels hold immediately
        assertTrue(SlideToAnswerUtils.shouldCancelHoldForMovement(16.1f, touchSlopPx))
        assertTrue(SlideToAnswerUtils.shouldCancelHoldForMovement(30f, touchSlopPx))
    }

    @Test
    fun slideToAnswerUtils_thresholdAndAlphaCalculations() {
        val maxDragPx = 600f

        // Slide answer threshold
        assertFalse(SlideToAnswerUtils.isSlideAnswerThresholdReached(400f, maxDragPx))
        assertTrue(SlideToAnswerUtils.isSlideAnswerThresholdReached(maxDragPx * 0.72f, maxDragPx))
        assertTrue(SlideToAnswerUtils.isSlideAnswerThresholdReached(500f, maxDragPx))

        // Text alpha calculations
        assertEquals(1.0f, SlideToAnswerUtils.calculateTextAlpha(0f, maxDragPx), 0.001f)
        assertEquals(0.25f, SlideToAnswerUtils.calculateTextAlpha(300f, maxDragPx), 0.001f)
        assertEquals(0.0f, SlideToAnswerUtils.calculateTextAlpha(450f, maxDragPx), 0.001f)
        assertEquals(0.0f, SlideToAnswerUtils.calculateTextAlpha(600f, maxDragPx), 0.001f)
    }
}
