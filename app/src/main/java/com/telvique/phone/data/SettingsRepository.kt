package com.telvique.phone.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.telviqueDataStore by preferencesDataStore(name = "telvique_settings")

data class TelviqueSettings(
    val dialpadTonesEnabled: Boolean = true,
    val dialpadVibrateEnabled: Boolean = true,
    val vibrateOnConnectEnabled: Boolean = true,
    val blockedNumbers: Set<String> = emptySet(),
    val quickResponses: List<String> = DEFAULT_QUICK_RESPONSES,
    val preferredSimId: String? = null,
    val layoutMode: PhoneLayoutMode = PhoneLayoutMode.UNIFIED,
    val speedDial: Map<String, String> = emptyMap(),
    val voicemailNumber: String = "*86",
    val hideInRecents: Boolean = true,
    val appLockEnabled: Boolean = false,
    val lockGraceSeconds: Int = 30,
    val maskNumbers: Boolean = false
) {
    companion object {
        val DEFAULT_QUICK_RESPONSES = listOf(
            "Can't talk right now. What's up?",
            "I'll call you right back.",
            "In a meeting, text me please.",
            "On my way, call you soon."
        )
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DIALPAD_TONES = booleanPreferencesKey("dialpad_tones")
        val DIALPAD_VIBRATE = booleanPreferencesKey("dialpad_vibrate")
        val VIBRATE_ON_CONNECT = booleanPreferencesKey("vibrate_on_connect")
        val BLOCKED_NUMBERS = stringSetPreferencesKey("blocked_numbers")
        val QUICK_RESPONSES = stringPreferencesKey("quick_responses_joined")
        val PREFERRED_SIM_ID = stringPreferencesKey("preferred_sim_id")
        val LAYOUT_MODE = stringPreferencesKey("phone_layout_mode")
        val VOICEMAIL_NUMBER = stringPreferencesKey("voicemail_number")
        val HIDE_IN_RECENTS = booleanPreferencesKey("hide_in_recents")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val LOCK_GRACE_SECONDS = intPreferencesKey("lock_grace_seconds")
        val MASK_NUMBERS = booleanPreferencesKey("mask_numbers")
        fun speedDialKey(digit: String) = stringPreferencesKey("speed_dial_$digit")
    }

    val settingsFlow: Flow<TelviqueSettings> = context.telviqueDataStore.data.map { prefs ->
        val rawResponses = prefs[Keys.QUICK_RESPONSES]
        val responses = if (rawResponses.isNullOrBlank()) {
            TelviqueSettings.DEFAULT_QUICK_RESPONSES
        } else {
            rawResponses.split("|||").filter { it.isNotBlank() }
        }
        val mode = prefs[Keys.LAYOUT_MODE]
            ?.let { raw -> PhoneLayoutMode.entries.firstOrNull { it.name == raw } }
            ?: PhoneLayoutMode.UNIFIED
        val speedDialMap = buildMap {
            for (d in '2'..'9') {
                val digit = d.toString()
                val num = prefs[Keys.speedDialKey(digit)]?.takeIf { it.isNotBlank() }
                if (num != null) put(digit, num)
            }
        }
        TelviqueSettings(
            dialpadTonesEnabled = prefs[Keys.DIALPAD_TONES] ?: true,
            dialpadVibrateEnabled = prefs[Keys.DIALPAD_VIBRATE] ?: true,
            vibrateOnConnectEnabled = prefs[Keys.VIBRATE_ON_CONNECT] ?: true,
            blockedNumbers = prefs[Keys.BLOCKED_NUMBERS] ?: emptySet(),
            quickResponses = responses,
            preferredSimId = prefs[Keys.PREFERRED_SIM_ID]?.takeIf { it.isNotBlank() },
            layoutMode = mode,
            speedDial = speedDialMap,
            voicemailNumber = prefs[Keys.VOICEMAIL_NUMBER]?.takeIf { it.isNotBlank() } ?: "*86",
            hideInRecents = prefs[Keys.HIDE_IN_RECENTS] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            lockGraceSeconds = prefs[Keys.LOCK_GRACE_SECONDS] ?: 30,
            maskNumbers = prefs[Keys.MASK_NUMBERS] ?: false
        )
    }

    suspend fun setDialpadTones(enabled: Boolean) {
        context.telviqueDataStore.edit { it[Keys.DIALPAD_TONES] = enabled }
    }

    suspend fun setDialpadVibrate(enabled: Boolean) {
        context.telviqueDataStore.edit { it[Keys.DIALPAD_VIBRATE] = enabled }
    }

    suspend fun setVibrateOnConnect(enabled: Boolean) {
        context.telviqueDataStore.edit { it[Keys.VIBRATE_ON_CONNECT] = enabled }
    }

    suspend fun setPreferredSimId(simId: String?) {
        context.telviqueDataStore.edit {
            if (simId.isNullOrBlank()) {
                it.remove(Keys.PREFERRED_SIM_ID)
            } else {
                it[Keys.PREFERRED_SIM_ID] = simId
            }
        }
    }

    suspend fun setLayoutMode(mode: PhoneLayoutMode) {
        context.telviqueDataStore.edit { it[Keys.LAYOUT_MODE] = mode.name }
    }

    suspend fun setSpeedDial(digit: String, number: String?) {
        if (digit.length != 1 || digit[0] !in '2'..'9') return
        context.telviqueDataStore.edit {
            val cleaned = number?.trim().orEmpty()
            if (cleaned.isEmpty()) it.remove(Keys.speedDialKey(digit))
            else it[Keys.speedDialKey(digit)] = cleaned
        }
    }

    suspend fun setVoicemailNumber(number: String) {
        val cleaned = number.trim()
        if (cleaned.isEmpty()) return
        context.telviqueDataStore.edit { it[Keys.VOICEMAIL_NUMBER] = cleaned }
    }

    suspend fun setHideInRecents(enabled: Boolean) {
        context.telviqueDataStore.edit { it[Keys.HIDE_IN_RECENTS] = enabled }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.telviqueDataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setLockGraceSeconds(seconds: Int) {
        context.telviqueDataStore.edit { it[Keys.LOCK_GRACE_SECONDS] = seconds.coerceAtLeast(0) }
    }

    suspend fun setMaskNumbers(enabled: Boolean) {
        context.telviqueDataStore.edit { it[Keys.MASK_NUMBERS] = enabled }
    }

    suspend fun blockNumber(number: String) {
        val normalized = PhoneFormatUtils.normalizeDigits(number)
        if (normalized.isBlank()) return
        context.telviqueDataStore.edit { prefs ->
            val updated = (prefs[Keys.BLOCKED_NUMBERS] ?: emptySet()) + normalized
            prefs[Keys.BLOCKED_NUMBERS] = updated
        }
    }

    suspend fun unblockNumber(number: String) {
        context.telviqueDataStore.edit { prefs ->
            val current = prefs[Keys.BLOCKED_NUMBERS] ?: emptySet()
            prefs[Keys.BLOCKED_NUMBERS] = current.filterNot {
                it == number || PhoneFormatUtils.numbersMatch(it, number)
            }.toSet()
        }
    }

    suspend fun isBlocked(number: String): Boolean {
        if (number.isBlank()) return false
        val blocked = settingsFlow.first().blockedNumbers
        return blocked.any { it == number || PhoneFormatUtils.numbersMatch(it, number) }
    }

    suspend fun updateQuickResponse(index: Int, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        context.telviqueDataStore.edit { prefs ->
            val current = settingsFlow.first().quickResponses.toMutableList()
            if (index in current.indices) {
                current[index] = trimmed
                prefs[Keys.QUICK_RESPONSES] = current.joinToString("|||")
            }
        }
    }
}
