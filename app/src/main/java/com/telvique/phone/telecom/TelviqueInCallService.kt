package com.telvique.phone.telecom

import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import com.telvique.phone.ui.call.CallActivity

class TelviqueInCallService : InCallService() {

    private var proximityWakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        CallManager.attachService(this)
    }

    override fun onDestroy() {
        releaseProximityLock()
        CallNotificationHelper.cancelAll(this)
        CallManager.detachService(this)
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.onCallAdded(this, call)

        // Active Default Dialer InCallService holds Android's BAL exemption, so launch CallActivity
        // directly for both incoming ringing calls and outgoing calls (in addition to CallStyle
        // notification / fullScreenIntent) so incoming calls always pop up whether locked or unlocked.
        runCatching { launchCallScreen() }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.onCallRemoved(this, call)
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        launchCallScreen()
    }

    @Deprecated("Deprecated in Android API 34")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        @Suppress("DEPRECATION")
        super.onCallAudioStateChanged(audioState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            CallManager.updateAudioState(audioState)
            updateProximityWakeLock()
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        super.onCallEndpointChanged(callEndpoint)
        CallManager.updateCallEndpoint(callEndpoint)
        updateProximityWakeLock()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onAvailableCallEndpointsChanged(availableEndpoints: List<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(availableEndpoints)
        CallManager.updateAvailableCallEndpoints(availableEndpoints)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onMuteStateChanged(isMuted: Boolean) {
        super.onMuteStateChanged(isMuted)
        CallManager.updateMuteState(isMuted)
    }

    fun onCallsUpdated() {
        val state = CallManager.uiState.value
        val primary = state.primaryCall
        if (primary == null || primary.phase == CallPhase.DISCONNECTED) {
            releaseProximityLock()
            CallNotificationHelper.cancelAll(this)
        } else {
            updateProximityWakeLock()
            CallNotificationHelper.showOrUpdate(this, primary)
        }
    }

    private fun launchCallScreen() {
        val intent = Intent(this, CallActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        }
        startActivity(intent)
    }

    private fun updateProximityWakeLock() {
        val state = CallManager.uiState.value
        val hasLiveVoiceCall = state.calls.any {
            it.phase == CallPhase.ACTIVE || it.phase == CallPhase.DIALING || it.phase == CallPhase.HOLDING
        }
        val usingEarpiece = state.audioState.route == CallAudioState.ROUTE_EARPIECE

        if (hasLiveVoiceCall && usingEarpiece) {
            acquireProximityLock()
        } else {
            releaseProximityLock()
        }
    }

    private fun acquireProximityLock() {
        val powerManager = getSystemService(PowerManager::class.java) ?: return
        if (proximityWakeLock == null) {
            if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                proximityWakeLock = powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "Telvique:ProximityCallLock"
                )
            }
        }
        proximityWakeLock?.let { lock ->
            if (!lock.isHeld) {
                runCatching { lock.acquire(4 * 60 * 60 * 1000L) }
            }
        }
    }

    private fun releaseProximityLock() {
        proximityWakeLock?.let { lock ->
            if (lock.isHeld) {
                runCatching { lock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY) }
            }
        }
    }
}
