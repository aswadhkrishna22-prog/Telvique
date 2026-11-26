package com.telvique.phone.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class PhoneRepository(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver
    private val telecomManager: TelecomManager? = context.getSystemService(TelecomManager::class.java)

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun observeContactsChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        if (hasPermission(Manifest.permission.READ_CONTACTS)) {
            resolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer
            )
        }
        trySend(Unit)
        awaitClose {
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }

    fun observeCallLogChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        if (hasPermission(Manifest.permission.READ_CALL_LOG)) {
            resolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                observer
            )
        }
        trySend(Unit)
        awaitClose {
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }

    suspend fun loadContacts(): List<ContactItem> = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return@withContext emptyList()

        val contactMap = linkedMapOf<Long, MutableContactBuilder>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )

        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val lookupIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
            val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
            val starredIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val lookup = cursor.getString(lookupIdx).orEmpty()
                val name = cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } ?: continue
                val number = cursor.getString(numIdx)?.trim()?.takeIf { it.isNotBlank() } ?: continue
                val type = cursor.getInt(typeIdx)
                val customLabel = cursor.getString(labelIdx)
                val typeLabel = ContactsContract.CommonDataKinds.Phone
                    .getTypeLabel(context.resources, type, customLabel)
                    .toString()
                val photoUri = cursor.getString(photoIdx)
                val isStarred = cursor.getInt(starredIdx) == 1

                val builder = contactMap.getOrPut(id) {
                    MutableContactBuilder(id, lookup, name, photoUri, isStarred)
                }
                val normalized = PhoneFormatUtils.normalizeDigits(number)
                if (builder.numbers.none { PhoneFormatUtils.normalizeDigits(it.number) == normalized }) {
                    builder.numbers.add(PhoneNumberEntry(number, typeLabel))
                }
            }
        }

        contactMap.values.map { it.build() }
    }

    suspend fun loadCallLogs(limit: Int = 250): List<CallLogItem> = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.READ_CALL_LOG)) return@withContext emptyList()

        val result = mutableListOf<CallLogItem>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )

        val simAccounts = getSimAccounts().associate { it.id to it.label }

        resolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val photoIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)
            val accountIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)

            while (cursor.moveToNext() && result.size < limit) {
                val id = cursor.getLong(idIdx)
                val number = cursor.getString(numIdx).orEmpty()
                val name = cursor.getString(nameIdx)
                val rawType = cursor.getInt(typeIdx)
                val date = cursor.getLong(dateIdx)
                val duration = cursor.getLong(durIdx)
                val photoUri = cursor.getString(photoIdx)
                val accountId = cursor.getString(accountIdx)

                val mappedType = when (rawType) {
                    CallLog.Calls.INCOMING_TYPE -> CallLogType.INCOMING
                    CallLog.Calls.OUTGOING_TYPE -> CallLogType.OUTGOING
                    CallLog.Calls.MISSED_TYPE -> CallLogType.MISSED
                    CallLog.Calls.REJECTED_TYPE -> CallLogType.REJECTED
                    CallLog.Calls.BLOCKED_TYPE -> CallLogType.BLOCKED
                    CallLog.Calls.VOICEMAIL_TYPE -> CallLogType.VOICEMAIL
                    else -> CallLogType.UNKNOWN
                }

                result.add(
                    CallLogItem(
                        id = id,
                        number = number,
                        cachedName = name,
                        type = mappedType,
                        timestamp = date,
                        durationSeconds = duration,
                        simLabel = accountId?.let { simAccounts[it] },
                        photoUri = photoUri
                    )
                )
            }
        }
        result
    }

    suspend fun deleteCallLogEntry(id: Long): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.WRITE_CALL_LOG)) return@withContext false
        runCatching {
            val deleted = resolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls._ID} = ?",
                arrayOf(id.toString())
            )
            deleted > 0
        }.getOrDefault(false)
    }

    suspend fun clearAllCallLogs(): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.WRITE_CALL_LOG)) return@withContext false
        runCatching {
            resolver.delete(CallLog.Calls.CONTENT_URI, null, null) >= 0
        }.getOrDefault(false)
    }

    suspend fun setContactStarred(contactId: Long, starred: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.WRITE_CONTACTS)) return@withContext false
        runCatching {
            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
            }
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            resolver.update(uri, values, null, null) > 0
        }.getOrDefault(false)
    }

    suspend fun lookupCallerInfo(phoneNumber: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        if (phoneNumber.isBlank() || !hasPermission(Manifest.permission.READ_CONTACTS)) {
            return@withContext null to null
        }
        runCatching {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI
            )
            resolver.query(lookupUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    val photo = cursor.getString(1)
                    return@withContext name to photo
                }
            }
            null to null
        }.getOrDefault(null to null)
    }

    @SuppressLint("MissingPermission")
    fun getSimAccounts(): List<SimAccountInfo> {
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) return emptyList()
        val manager = telecomManager ?: return emptyList()
        return runCatching {
            manager.callCapablePhoneAccounts.mapIndexedNotNull { idx, handle ->
                val account = manager.getPhoneAccount(handle)
                val label = account?.label?.toString()?.takeIf { it.isNotBlank() }
                    ?: "SIM ${idx + 1}"
                SimAccountInfo(
                    id = handle.id,
                    label = label,
                    handle = handle
                )
            }
        }.getOrDefault(emptyList())
    }

    @SuppressLint("MissingPermission")
    fun placeCall(number: String, accountHandle: PhoneAccountHandle? = null): Boolean {
        val cleaned = number.trim()
        if (cleaned.isEmpty() || !hasPermission(Manifest.permission.CALL_PHONE)) return false
        val manager = telecomManager ?: return false

        val uri = Uri.fromParts("tel", cleaned, null)
        val extras = Bundle().apply {
            if (accountHandle != null) {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle)
            }
        }
        return runCatching {
            manager.placeCall(uri, extras)
            true
        }.getOrDefault(false)
    }

    private class MutableContactBuilder(
        val id: Long,
        val lookupKey: String,
        val name: String,
        val photoUri: String?,
        val isStarred: Boolean,
        val numbers: MutableList<PhoneNumberEntry> = mutableListOf()
    ) {
        fun build() = ContactItem(
            id = id,
            lookupKey = lookupKey,
            name = name,
            numbers = numbers.toList(),
            photoUri = photoUri,
            isStarred = isStarred
        )
    }
}
