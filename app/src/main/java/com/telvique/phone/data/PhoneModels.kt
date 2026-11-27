package com.telvique.phone.data

import android.content.Context
import android.telecom.PhoneAccountHandle
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import com.telvique.phone.TelviqueApp
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
    val isStarred: Boolean,
    val photoFullUri: String? = null
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
    val photoUri: String?,
    val photoFullUri: String? = null,
    val cnapName: String? = null
) {
    val displayTitle: String
        get() = cachedName?.takeIf { it.isNotBlank() }
            ?: cnapName?.takeIf { it.isNotBlank() }
            ?: number.ifBlank { "Private Number" }
}

data class GroupedCallLogItem(
    val primary: CallLogItem,
    val count: Int,
    val entryIds: List<Long>,
    val entries: List<CallLogItem> = listOf(primary)
)

fun groupConsecutiveCallLogs(logs: List<CallLogItem>): List<GroupedCallLogItem> {
    if (logs.isEmpty()) return emptyList()
    val result = mutableListOf<GroupedCallLogItem>()
    var currentHead = logs.first()
    val currentEntries = mutableListOf(currentHead)

    fun sameBucket(a: CallLogItem, b: CallLogItem): Boolean {
        val aMissed = a.type == CallLogType.MISSED || a.type == CallLogType.REJECTED
        val bMissed = b.type == CallLogType.MISSED || b.type == CallLogType.REJECTED
        if (aMissed != bMissed) return false
        val sameNumber = if (a.number.isNotBlank() && b.number.isNotBlank()) {
            a.number == b.number || PhoneFormatUtils.numbersMatch(a.number, b.number)
        } else {
            a.displayTitle == b.displayTitle
        }
        if (!sameNumber) return false
        val calA = Calendar.getInstance().apply { timeInMillis = a.timestamp }
        val calB = Calendar.getInstance().apply { timeInMillis = b.timestamp }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    for (i in 1 until logs.size) {
        val item = logs[i]
        if (sameBucket(currentHead, item)) {
            currentEntries.add(item)
        } else {
            result.add(
                GroupedCallLogItem(
                    primary = currentHead,
                    count = currentEntries.size,
                    entryIds = currentEntries.map { it.id },
                    entries = currentEntries.toList()
                )
            )
            currentHead = item
            currentEntries.clear()
            currentEntries.add(item)
        }
    }
    result.add(
        GroupedCallLogItem(
            primary = currentHead,
            count = currentEntries.size,
            entryIds = currentEntries.map { it.id },
            entries = currentEntries.toList()
        )
    )
    return result
}

enum class PhoneLayoutMode(val label: String) {
    UNIFIED("Unified"),
    CLASSIC("Classic")
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

    fun normalizeDialInput(input: String): String =
        input.filter { it.isDigit() || it == '+' || it == '*' || it == '#' || it == ',' || it == ';' }

    fun getDefaultCountryIso(context: Context? = null): String {
        val ctx = context ?: runCatching { TelviqueApp.instance }.getOrNull()
        if (ctx != null) {
            val tm = ctx.getSystemService(TelephonyManager::class.java)
            if (tm != null) {
                val simCountry = runCatching { tm.simCountryIso }.getOrNull()?.trim()
                if (!simCountry.isNullOrBlank()) {
                    return simCountry.uppercase(Locale.ROOT)
                }
                val networkCountry = runCatching { tm.networkCountryIso }.getOrNull()?.trim()
                if (!networkCountry.isNullOrBlank()) {
                    return networkCountry.uppercase(Locale.ROOT)
                }
            }
        }
        val localeCountry = Locale.getDefault().country.trim()
        return if (localeCountry.isNotBlank() && localeCountry.length == 2 && !localeCountry.equals("US", ignoreCase = true)) {
            localeCountry.uppercase(Locale.ROOT)
        } else {
            "IN"
        }
    }

    fun formatDisplayNumber(number: String, countryIso: String? = null, context: Context? = null): String {
        val trimmed = number.trim()
        if (trimmed.isEmpty() || trimmed.contains('*') || trimmed.contains('#') || !trimmed.any { it.isDigit() }) {
            return trimmed
        }

        val iso = countryIso?.takeIf { it.isNotBlank() } ?: getDefaultCountryIso(context)
        val upperIso = iso.uppercase(Locale.ROOT)

        val formatted = runCatching {
            PhoneNumberUtils.formatNumber(trimmed, upperIso)
        }.getOrNull()?.takeIf { it.isNotBlank() }

        if (formatted != null && !formatted.startsWith("(") && (upperIso != "IN" || !formatted.contains('('))) {
            return formatted
        }

        // Graceful Indian format fallback if PhoneNumberUtils produced US style or failed
        if (upperIso == "IN") {
            val digitsOnly = trimmed.filter { it.isDigit() }
            when {
                trimmed.startsWith("+91") && digitsOnly.startsWith("91") && digitsOnly.length == 12 -> {
                    val mobile = digitsOnly.substring(2)
                    return "+91 ${mobile.substring(0, 5)} ${mobile.substring(5)}"
                }
                trimmed.startsWith("0") && digitsOnly.length == 11 && (digitsOnly[1] in '6'..'9') -> {
                    val mobile = digitsOnly.substring(1)
                    return "0${mobile.substring(0, 5)} ${mobile.substring(5)}"
                }
                digitsOnly.length == 10 && (digitsOnly[0] in '6'..'9') -> {
                    return "${digitsOnly.substring(0, 5)} ${digitsOnly.substring(5)}"
                }
            }
        }

        return formatted ?: trimmed
    }

    fun comparableTail(input: String): String {
        val digits = input.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    fun numbersMatch(a: String, b: String): Boolean {
        val tailA = comparableTail(a)
        val tailB = comparableTail(b)
        return tailA.isNotEmpty() && tailA == tailB
    }

    fun isSuspectedTelemarketer(number: String): Boolean {
        val digits = number.filter { it.isDigit() }
        if (digits.length < 10) return false
        val local = if (digits.startsWith("91") && digits.length == 12) digits.drop(2) else digits
        return local.length == 10 && (local.startsWith("140") || local.startsWith("160"))
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

    fun formatAppleRecentTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val sameYear = target.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        val dayDiff = now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR)

        return when {
            sameYear && dayDiff == 0 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestampMillis))
            sameYear && dayDiff == 1 -> "Yesterday"
            sameYear && dayDiff in 2..6 -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestampMillis))
            else -> SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(timestampMillis))
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

object SlideToAnswerUtils {
    const val DEFAULT_HOLD_DURATION_MS = 1000L
    const val COMPLETION_THRESHOLD_RATIO = 0.72f

    fun calculateProgress(elapsedMs: Long, totalDurationMs: Long = DEFAULT_HOLD_DURATION_MS): Float {
        if (totalDurationMs <= 0L) return 1f
        return (elapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    }

    fun isHoldCompleted(elapsedMs: Long, totalDurationMs: Long = DEFAULT_HOLD_DURATION_MS): Boolean {
        return elapsedMs >= totalDurationMs
    }

    fun calculateSweepAngle(progress: Float): Float {
        return (progress.coerceIn(0f, 1f) * 360f)
    }

    fun shouldCancelHoldForMovement(distancePx: Float, touchSlopPx: Float): Boolean {
        return distancePx > touchSlopPx
    }

    fun isSlideAnswerThresholdReached(offsetPx: Float, maxDragPx: Float, thresholdRatio: Float = COMPLETION_THRESHOLD_RATIO): Boolean {
        val threshold = maxDragPx * thresholdRatio
        return offsetPx >= threshold
    }

    fun calculateTextAlpha(offsetPx: Float, maxDragPx: Float): Float {
        if (maxDragPx <= 0f) return 0f
        return (1f - (offsetPx / maxDragPx) * 1.5f).coerceIn(0f, 1f)
    }
}

