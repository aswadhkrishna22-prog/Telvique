package com.telvique.phone.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class DeletedCallRecord(
    val cacheId: String,
    val number: String,
    val cachedName: String?,
    val type: CallLogType,
    val timestamp: Long,
    val durationSeconds: Long,
    val deletedAtMillis: Long
) {
    val displayTitle: String
        get() = cachedName?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Private Number" }
}

/**
 * Local-only 30-day cache for deleted call history entries.
 * Stored inside internal app files (excluded from cloud/device backups).
 */
class DeletedCallCache(private val context: Context) {

    private val cacheFile: File
        get() = File(context.filesDir, CACHE_FILE_NAME)

    suspend fun loadAll(nowMillis: Long = System.currentTimeMillis()): List<DeletedCallRecord> =
        withContext(Dispatchers.IO) {
            val raw = readInternal(nowMillis)
            val pruned = pruneExpired(raw, nowMillis)
            if (pruned.size != raw.size) {
                writeInternal(pruned)
            }
            pruned
        }

    suspend fun addDeletedCalls(
        items: List<CallLogItem>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<DeletedCallRecord> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext emptyList()
        val existing = pruneExpired(readInternal(nowMillis), nowMillis)
        val added = items.map { item ->
            DeletedCallRecord(
                cacheId = UUID.randomUUID().toString(),
                number = item.number,
                cachedName = item.cachedName,
                type = item.type,
                timestamp = item.timestamp,
                durationSeconds = item.durationSeconds,
                deletedAtMillis = nowMillis
            )
        }
        val merged = (added + existing)
            .distinctBy { "${it.number}|${it.timestamp}|${it.type.name}|${it.durationSeconds}" }
            .take(MAX_CACHED_ENTRIES)
        writeInternal(merged)
        added
    }

    suspend fun removeRecords(cacheIds: Set<String>, nowMillis: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            if (cacheIds.isEmpty()) return@withContext
            val existing = pruneExpired(readInternal(nowMillis), nowMillis)
            val remaining = existing.filterNot { it.cacheId in cacheIds }
            writeInternal(remaining)
        }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        runCatching {
            if (cacheFile.exists()) cacheFile.delete()
        }
        Unit
    }

    private fun pruneExpired(
        records: List<DeletedCallRecord>,
        nowMillis: Long
    ): List<DeletedCallRecord> = records
        .filter { record -> (nowMillis - record.deletedAtMillis) in 0..RETENTION_MILLIS }
        .sortedByDescending { it.deletedAtMillis }

    private fun readInternal(nowMillis: Long): List<DeletedCallRecord> {
        val file = cacheFile
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val typeName = obj.optString("type", CallLogType.UNKNOWN.name)
                    val mappedType = CallLogType.entries.firstOrNull { it.name == typeName }
                        ?: CallLogType.UNKNOWN
                    add(
                        DeletedCallRecord(
                            cacheId = obj.optString("cacheId").ifBlank { UUID.randomUUID().toString() },
                            number = obj.optString("number", ""),
                            cachedName = obj.optString("cachedName", "").takeIf { it.isNotBlank() },
                            type = mappedType,
                            timestamp = obj.optLong("timestamp", nowMillis),
                            durationSeconds = obj.optLong("durationSeconds", 0L),
                            deletedAtMillis = obj.optLong("deletedAtMillis", nowMillis)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeInternal(records: List<DeletedCallRecord>) {
        runCatching {
            if (records.isEmpty()) {
                if (cacheFile.exists()) cacheFile.delete()
                return
            }
            val array = JSONArray()
            records.forEach { r ->
                array.put(
                    JSONObject().apply {
                        put("cacheId", r.cacheId)
                        put("number", r.number)
                        put("cachedName", r.cachedName.orEmpty())
                        put("type", r.type.name)
                        put("timestamp", r.timestamp)
                        put("durationSeconds", r.durationSeconds)
                        put("deletedAtMillis", r.deletedAtMillis)
                    }
                )
            }
            cacheFile.writeText(array.toString())
        }
    }

    companion object {
        const val RETENTION_DAYS = 30L
        const val RETENTION_MILLIS = RETENTION_DAYS * 24L * 60L * 60L * 1000L
        private const val MAX_CACHED_ENTRIES = 1500
        private const val CACHE_FILE_NAME = "deleted_calls_cache_v1.json"
    }
}
