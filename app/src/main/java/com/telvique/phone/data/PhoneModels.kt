package com.telvique.phone.data

import android.telecom.PhoneAccountHandle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PhoneNumberEntry(
    val number: String,
    val typeLabel: String
)

data class ContactItem(
    val id: Long,
    val lookupKey: String,
    val name: String,
    val numbers: List<PhoneNumberEntry>,
    val photoUri: String?,
    val isStarred: Boolean
) {
    val primaryNumber: String get() = numbers.firstOrNull()?.number.orEmpty()
}

enum class CallLogType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    BLOCKED,
    VOICEMAIL,
    UNKNOWN
}

data class CallLogItem(
    val id: Long,
    val number: String,
    val cachedName: String?,
    val type: CallLogType,
    val timestamp: Long,
    val durationSeconds: Long,
    val simLabel: String?,
    val photoUri: String?
) {
    val displayTitle: String
        get() = cachedName?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Private Number" }
}

enum class RecentsFilter(val label: String) {
    ALL("All"),
    MISSED("Missed"),
    INCOMING("Incoming"),
    OUTGOING("Outgoing")
}

data class SimAccountInfo(
    val id: String,
    val label: String,
    val handle: PhoneAccountHandle
)

object PhoneFormatUtils {

    fun normalizeDigits(input: String): String =
        input.filter { it.isDigit() || it == '+' }

    fun comparableTail(input: String): String {
        val digits = input.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    fun numbersMatch(a: String, b: String): Boolean {
        val tailA = comparableTail(a)
        val tailB = comparableTail(b)
        return tailA.isNotEmpty() && tailA == tailB
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0L) return "0s"
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> String.format(Locale.getDefault(), "%dh %02dm %02ds", hours, mins, secs)
            mins > 0 -> String.format(Locale.getDefault(), "%dm %02ds", mins, secs)
            else -> String.format(Locale.getDefault(), "%ds", secs)
        }
    }

    fun formatTimer(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0L)
        val hours = safe / 3600
        val mins = (safe % 3600) / 60
        val secs = safe % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }
    }

    fun formatRelativeTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        val sameYear = target.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        val dayDiff = now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR)

        return when {
            sameYear && dayDiff == 0 -> timeFormat.format(Date(timestampMillis))
            sameYear && dayDiff == 1 -> "Yesterday, ${timeFormat.format(Date(timestampMillis))}"
            sameYear -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestampMillis))
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestampMillis))
        }
    }
}

object T9Matcher {
    private val charToDigitMap = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
    )

    fun toT9Digits(text: String): String = buildString(text.length) {
        for (ch in text.lowercase(Locale.getDefault())) {
            when {
                ch.isDigit() -> append(ch)
                ch in charToDigitMap -> append(charToDigitMap[ch])
            }
        }
    }

    fun matches(contact: ContactItem, rawQuery: String): Boolean {
        val query = rawQuery.trim()
        if (query.isEmpty()) return false

        val normalizedDigitsQuery = query.filter { it.isDigit() }
        val lowerQuery = query.lowercase(Locale.getDefault())

        if (contact.name.lowercase(Locale.getDefault()).contains(lowerQuery)) {
            return true
        }

        if (normalizedDigitsQuery.isNotEmpty()) {
            val nameT9 = toT9Digits(contact.name)
            if (nameT9.contains(normalizedDigitsQuery)) return true

            // Match initials (e.g. "John Doe" -> "53")
            val initialsT9 = contact.name
                .split(' ', '-', '_')
                .filter { it.isNotBlank() }
                .mapNotNull { word ->
                    val first = word.first().lowercaseChar()
                    charToDigitMap[first] ?: first.takeIf { it.isDigit() }
                }
                .joinToString("")
            if (initialsT9.contains(normalizedDigitsQuery)) return true
        }

        return contact.numbers.any { entry ->
            val digits = entry.number.filter { it.isDigit() }
            (normalizedDigitsQuery.isNotEmpty() && digits.contains(normalizedDigitsQuery)) ||
                entry.number.contains(query, ignoreCase = true)
        }
    }
}
