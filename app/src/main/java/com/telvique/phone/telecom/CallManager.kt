package com.telvique.phone.telecom

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.OutcomeReceiver
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.VideoProfile
import androidx.annotation.RequiresApi
import com.telvique.phone.data.PhoneRepository
import com.telvique.phone.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CallPhase {
    NEW,
    DIALING,
    RINGING,
    HOLDING,
    ACTIVE,
    DISCONNECTING,
    DISCONNECTED
}

data class CallUiItem(
    val id: String,
    val number: String,
    val displayName: String,
    val photoUri: String?,
    val simLabel: String?,
    val phase: CallPhase,
    val connectTimeMillis: Long,
    val isConference: Boolean,
    val canHold: Boolean,
    val canMerge: Boolean,
    val canSwap: Boolean,
    val disconnectCauseLabel: String?
)

data class CallAudioUiState(
    val isMuted: Boolean = false,
    val route: Int = CallAudioState.ROUTE_EARPIECE,
    val supportedRouteMask: Int = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER,
    val bluetoothDeviceName: String? = null
)

data class TelecomUiState(
    val calls: List<CallUiItem> = emptyList(),
    val audioState: CallAudioUiState = CallAudioUiState()
) {
    val primaryCall: CallUiItem?
        get() = calls.firstOrNull { it.phase == CallPhase.RINGING }
            ?: calls.firstOrNull { it.phase == CallPhase.ACTIVE }
            ?: calls.firstOrNull { it.phase == CallPhase.DIALING }
            ?: calls.firstOrNull()

    val secondaryCall: CallUiItem?
        get() {
            val primary = primaryCall ?: return null
            return calls.firstOrNull { it.id != primary.id }
        }
}

object CallManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var serviceRef: TelviqueInCallService? = null
    private val activeCalls = linkedMapOf<String, Call>()
    private val callIdsByInstance = mutableMapOf<Call, String>()
    private val resolvedContacts = mutableMapOf<String, Pair<String?, String?>>()
    private val callbacksByCall = mutableMapOf<Call, Call.Callback>()
    private var nextCallId = 1
    private val connectedVibratedCallIds = mutableSetOf<String>()
    private var availableEndpoints: List<CallEndpoint> = emptyList()

    private val _uiState = MutableStateFlow(TelecomUiState())
    val uiState: StateFlow<TelecomUiState> = _uiState.asStateFlow()

    fun attachService(service: TelviqueInCallService) {
        serviceRef = service
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            service.callAudioState?.let { updateAudioState(it) }
        }
    }

    fun detachService(service: TelviqueInCallService) {
        if (serviceRef === service) {
            serviceRef = null
            availableEndpoints = emptyList()
        }
    }

    fun onCallAdded(context: Context, call: Call) {
        val id = "call_${nextCallId++}"
        activeCalls[id] = call
        callIdsByInstance[call] = id

        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                if (state == Call.STATE_ACTIVE && connectedVibratedCallIds.add(id)) {
                    vibrateIfEnabled(context)
                }
                refreshState(context)
                serviceRef?.onCallsUpdated()
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                refreshState(context)
                serviceRef?.onCallsUpdated()
            }

            override fun onConferenceableCallsChanged(
                call: Call,
                conferenceableCalls: MutableList<Call>
            ) {
                refreshState(context)
            }
        }
        callbacksByCall[call] = callback
        call.registerCallback(callback)

        val number = extractNumber(call)
        if (number.isNotBlank() && !resolvedContacts.containsKey(number)) {
            scope.launch {
                val repo = PhoneRepository(context.applicationContext)
                val resolved = repo.lookupCallerInfo(number)
                resolvedContacts[number] = resolved
                refreshState(context)
                serviceRef?.onCallsUpdated()
            }
        }

        refreshState(context)
        serviceRef?.onCallsUpdated()
    }

    fun onCallRemoved(context: Context, call: Call) {
        val id = callIdsByInstance.remove(call)
        callbacksByCall.remove(call)?.let { call.unregisterCallback(it) }
        if (id != null) {
            activeCalls.remove(id)
            connectedVibratedCallIds.remove(id)
        }
        refreshState(context)
        serviceRef?.onCallsUpdated()
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun updateAudioState(audioState: CallAudioState) {
        val btName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            audioState.activeBluetoothDevice?.name
                ?: audioState.supportedBluetoothDevices.firstOrNull()?.name
        } else {
            null
        }
        _uiState.update { current ->
            current.copy(
                audioState = CallAudioUiState(
                    isMuted = audioState.isMuted,
                    route = audioState.route,
                    supportedRouteMask = audioState.supportedRouteMask,
                    bluetoothDeviceName = btName
                )
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun updateCallEndpoint(endpoint: CallEndpoint) {
        val route = mapEndpointTypeToRoute(endpoint.endpointType)
        val btName = if (endpoint.endpointType == CallEndpoint.TYPE_BLUETOOTH) {
            endpoint.endpointName.toString().takeIf { it.isNotBlank() }
        } else {
            _uiState.value.audioState.bluetoothDeviceName
        }
        _uiState.update { current ->
            current.copy(
                audioState = current.audioState.copy(
                    route = route,
                    bluetoothDeviceName = btName
                )
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun updateAvailableCallEndpoints(endpoints: List<CallEndpoint>) {
        availableEndpoints = endpoints
        var mask = 0
        var btName: String? = null
        for (ep in endpoints) {
            mask = mask or mapEndpointTypeToRoute(ep.endpointType)
            if (ep.endpointType == CallEndpoint.TYPE_BLUETOOTH && btName == null) {
                btName = ep.endpointName.toString().takeIf { it.isNotBlank() }
            }
        }
        if (mask == 0) {
            mask = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER
        }
        _uiState.update { current ->
            current.copy(
                audioState = current.audioState.copy(
                    supportedRouteMask = mask,
                    bluetoothDeviceName = btName ?: current.audioState.bluetoothDeviceName
                )
            )
        }
    }

    fun updateMuteState(isMuted: Boolean) {
        _uiState.update { current ->
            current.copy(audioState = current.audioState.copy(isMuted = isMuted))
        }
    }

    fun answer(callId: String? = null) {
        val call = findCall(callId) ?: findCallByPhase(CallPhase.RINGING) ?: return
        call.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun decline(callId: String? = null) {
        val call = findCall(callId) ?: findCallByPhase(CallPhase.RINGING) ?: return
        call.reject(false, null)
    }

    fun rejectWithMessage(message: String, callId: String? = null) {
        val call = findCall(callId) ?: findCallByPhase(CallPhase.RINGING) ?: return
        call.reject(true, message)
    }

    fun disconnect(callId: String? = null) {
        val call = findCall(callId) ?: _uiState.value.primaryCall?.id?.let { activeCalls[it] } ?: return
        if (getCallState(call) == Call.STATE_RINGING) {
            call.reject(false, null)
        } else {
            call.disconnect()
        }
    }

    fun toggleHold(callId: String? = null) {
        val call = findCall(callId) ?: _uiState.value.primaryCall?.id?.let { activeCalls[it] } ?: return
        if (getCallState(call) == Call.STATE_HOLDING) {
            call.unhold()
        } else {
            call.hold()
        }
    }

    fun swapCalls() {
        val calls = activeCalls.values.toList()
        if (calls.size < 2) return
        val holding = calls.firstOrNull { getCallState(it) == Call.STATE_HOLDING }
        val active = calls.firstOrNull { getCallState(it) == Call.STATE_ACTIVE }
        when {
            holding != null -> holding.unhold()
            active != null && active.details.can(Call.Details.CAPABILITY_SWAP_CONFERENCE) -> {
                active.swapConference()
            }
        }
    }

    fun mergeCalls() {
        val calls = activeCalls.values.toList()
        val primary = calls.firstOrNull { getCallState(it) == Call.STATE_ACTIVE } ?: calls.firstOrNull() ?: return
        val conferenceTarget = primary.conferenceableCalls.firstOrNull()
            ?: calls.firstOrNull { it !== primary }
            ?: return
        if (primary.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE)) {
            primary.mergeConference()
        } else {
            primary.conference(conferenceTarget)
        }
    }

    fun toggleMute() {
        val service = serviceRef ?: return
        val currentMuted = _uiState.value.audioState.isMuted
        service.setMuted(!currentMuted)
    }

    fun setAudioRoute(route: Int) {
        val service = serviceRef ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            requestEndpointChange(service, route)
        ) {
            return
        }
        @Suppress("DEPRECATION")
        service.setAudioRoute(route)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun requestEndpointChange(service: TelviqueInCallService, route: Int): Boolean {
        val targetType = when (route) {
            CallAudioState.ROUTE_BLUETOOTH -> CallEndpoint.TYPE_BLUETOOTH
            CallAudioState.ROUTE_WIRED_HEADSET -> CallEndpoint.TYPE_WIRED_HEADSET
            CallAudioState.ROUTE_SPEAKER -> CallEndpoint.TYPE_SPEAKER
            else -> CallEndpoint.TYPE_EARPIECE
        }
        val endpoint = availableEndpoints.firstOrNull { it.endpointType == targetType } ?: return false
        service.requestCallEndpointChange(
            endpoint,
            service.mainExecutor,
            object : OutcomeReceiver<Void, CallEndpointException> {
                override fun onResult(result: Void?) = Unit
                override fun onError(error: CallEndpointException) = Unit
            }
        )
        return true
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun mapEndpointTypeToRoute(endpointType: Int): Int = when (endpointType) {
        CallEndpoint.TYPE_BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
        CallEndpoint.TYPE_WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
        CallEndpoint.TYPE_SPEAKER -> CallAudioState.ROUTE_SPEAKER
        else -> CallAudioState.ROUTE_EARPIECE
    }

    fun toggleSpeaker() {
        val currentRoute = _uiState.value.audioState.route
        val nextRoute = if (currentRoute == CallAudioState.ROUTE_SPEAKER) {
            if ((_uiState.value.audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0) {
                CallAudioState.ROUTE_BLUETOOTH
            } else if ((_uiState.value.audioState.supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
                CallAudioState.ROUTE_WIRED_HEADSET
            } else {
                CallAudioState.ROUTE_EARPIECE
            }
        } else {
            CallAudioState.ROUTE_SPEAKER
        }
        setAudioRoute(nextRoute)
    }

    fun playDtmf(digit: Char, callId: String? = null) {
        val call = findCall(callId) ?: _uiState.value.primaryCall?.id?.let { activeCalls[it] } ?: return
        call.playDtmfTone(digit)
    }

    fun stopDtmf(callId: String? = null) {
        val call = findCall(callId) ?: _uiState.value.primaryCall?.id?.let { activeCalls[it] } ?: return
        call.stopDtmfTone()
    }

    private fun findCall(callId: String?): Call? =
        if (callId != null) activeCalls[callId] else null

    private fun findCallByPhase(phase: CallPhase): Call? {
        val matchId = _uiState.value.calls.firstOrNull { it.phase == phase }?.id ?: return null
        return activeCalls[matchId]
    }

    private fun refreshState(context: Context) {
        val repo = PhoneRepository(context.applicationContext)
        val simLabels = repo.getSimAccounts().associate { it.handle.id to it.label }

        val items = activeCalls.entries.map { (id, call) ->
            val details = call.details
            val number = extractNumber(call)
            val resolved = resolvedContacts[number]
            val isConference = details.hasProperty(Call.Details.PROPERTY_CONFERENCE)
            val resolvedName = resolved?.first?.takeIf { it.isNotBlank() }
            val contactName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                details.contactDisplayName?.takeIf { it.isNotBlank() }
            } else {
                null
            }
            val callerName = details.callerDisplayName?.takeIf { it.isNotBlank() }
            val displayName = when {
                isConference -> "Conference Call"
                resolvedName != null -> resolvedName
                contactName != null -> contactName
                callerName != null -> callerName
                number.isNotBlank() -> number
                else -> "Unknown Caller"
            }

            val simLabel = details.accountHandle?.id?.let { simLabels[it] }
            val state = getCallState(call)
            val phase = mapPhase(state)

            val canHold = details.can(Call.Details.CAPABILITY_SUPPORT_HOLD) ||
                details.can(Call.Details.CAPABILITY_HOLD)
            val canMerge = call.conferenceableCalls.isNotEmpty() ||
                details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE)
            val canSwap = details.can(Call.Details.CAPABILITY_SWAP_CONFERENCE) ||
                activeCalls.size >= 2

            CallUiItem(
                id = id,
                number = number,
                displayName = displayName,
                photoUri = resolved?.second,
                simLabel = simLabel,
                phase = phase,
                connectTimeMillis = details.connectTimeMillis,
                isConference = isConference,
                canHold = canHold,
                canMerge = canMerge,
                canSwap = canSwap,
                disconnectCauseLabel = details.disconnectCause?.label?.toString()
            )
        }

        _uiState.update { it.copy(calls = items) }
    }

    private fun extractNumber(call: Call): String =
        call.details.handle?.schemeSpecificPart.orEmpty()

    private fun getCallState(call: Call): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
    }

    private fun mapPhase(state: Int): CallPhase = when (state) {
        Call.STATE_NEW -> CallPhase.NEW
        Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_SELECT_PHONE_ACCOUNT -> CallPhase.DIALING
        Call.STATE_RINGING -> CallPhase.RINGING
        Call.STATE_HOLDING -> CallPhase.HOLDING
        Call.STATE_ACTIVE -> CallPhase.ACTIVE
        Call.STATE_DISCONNECTING -> CallPhase.DISCONNECTING
        Call.STATE_DISCONNECTED -> CallPhase.DISCONNECTED
        else -> CallPhase.NEW
    }

    private fun vibrateIfEnabled(context: Context) {
        scope.launch {
            val enabled = runCatching {
                SettingsRepository(context.applicationContext).settingsFlow.first().vibrateOnConnectEnabled
            }.getOrDefault(true)
            if (!enabled) return@launch

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            } ?: return@launch

            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}
