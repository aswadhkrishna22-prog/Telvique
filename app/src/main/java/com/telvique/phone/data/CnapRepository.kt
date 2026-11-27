package com.telvique.phone.data

import android.content.Context
import android.content.SharedPreferences

/**
 * App-private storage for India CNAP (Calling Name Presentation) operator caller names.
 * Ensures caller names received from network during calls are preserved with call log entries
 * in Telvique's internal storage without modifying Android's system call log provider.
 */
class CnapRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "telvique_cnap_records"
        private const val KEY_PREFIX_LOG = "log_"
        private const val KEY_PREFIX_PENDING = "pending_"
        private const val KEY_PREFIX_TAIL = "tail_"

        @Volatile
        private var instance: CnapRepository? = null

        fun getInstance(context: Context): CnapRepository =
            instance ?: synchronized(this) {
                instance ?: CnapRepository(context).also { instance = it }
            }
    }

    /**
     * Stores operator CNAP name directly for a specific call log entry ID in app's own storage.
     */
    fun saveCnapForCallLog(callLogId: Long, cnapName: String) {
        if (callLogId < 0 || cnapName.isBlank()) return
        prefs.edit().putString("${KEY_PREFIX_LOG}$callLogId", cnapName.trim()).apply()
    }

    /**
     * Retrieves operator CNAP name stored for a specific call log entry ID.
     */
    fun getCnapForCallLog(callLogId: Long): String? {
        if (callLogId < 0) return null
        return prefs.getString("${KEY_PREFIX_LOG}$callLogId", null)?.takeIf { it.isNotBlank() }
    }

    /**
     * Records a call session with an operator-provided name while the call is ongoing.
     * Stored in pending pool so it can be associated with the call log entry once Android writes it.
     */
    fun recordCallSession(number: String, cnapName: String, timestamp: Long = System.currentTimeMillis()) {
        val trimmedName = cnapName.trim()
        val tail = PhoneFormatUtils.comparableTail(number)
        if (trimmedName.isBlank() || tail.isEmpty()) return

        prefs.edit()
            .putString("${KEY_PREFIX_PENDING}${tail}_$timestamp", trimmedName)
            .putString("${KEY_PREFIX_TAIL}$tail", trimmedName)
            .apply()
    }

    /**
     * Matches a newly loaded call log entry against pending call sessions within a reasonable time window (e.g. 3 minutes).
     * If matched, persists it permanently under the call log entry ID and removes the pending record.
     */
    fun matchAndSavePending(callLogId: Long, number: String, logTimestamp: Long): String? {
        val tail = PhoneFormatUtils.comparableTail(number)
        if (tail.isEmpty() || callLogId < 0) return null

        val allEntries = prefs.all
        val prefix = "${KEY_PREFIX_PENDING}${tail}_"
        var bestKey: String? = null
        var bestName: String? = null
        var minDiff = Long.MAX_VALUE
        val maxDiffAllowed = 3 * 60 * 1000L // 3 minutes window

        for ((key, value) in allEntries) {
            if (key.startsWith(prefix) && value is String && value.isNotBlank()) {
                val callTimeStr = key.removePrefix(prefix)
                val callTime = callTimeStr.toLongOrNull() ?: continue
                val diff = kotlin.math.abs(logTimestamp - callTime)
                if (diff < maxDiffAllowed && diff < minDiff) {
                    minDiff = diff
                    bestKey = key
                    bestName = value
                }
            }
        }

        if (bestKey != null && bestName != null) {
            prefs.edit()
                .putString("${KEY_PREFIX_LOG}$callLogId", bestName)
                .remove(bestKey)
                .apply()
            return bestName
        }

        return null
    }

    /**
     * Looks up the latest known CNAP name for a number (if available).
     */
    fun getLatestCnapForNumber(number: String): String? {
        val tail = PhoneFormatUtils.comparableTail(number)
        if (tail.isEmpty()) return null
        return prefs.getString("${KEY_PREFIX_TAIL}$tail", null)?.takeIf { it.isNotBlank() }
    }

    /**
     * Deletes CNAP record for a deleted call log entry.
     */
    fun deleteCnapForCallLog(callLogId: Long) {
        if (callLogId < 0) return
        prefs.edit().remove("${KEY_PREFIX_LOG}$callLogId").apply()
    }

    /**
     * Clears all stored CNAP call log mappings when call logs are wiped.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
