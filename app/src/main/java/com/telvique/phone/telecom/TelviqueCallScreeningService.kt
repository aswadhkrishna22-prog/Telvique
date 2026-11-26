package com.telvique.phone.telecom

import android.provider.BlockedNumberContract
import android.telecom.Call
import android.telecom.CallScreeningService
import com.telvique.phone.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class TelviqueCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        scope.launch {
            // Android requires respondToCall() within 5 seconds; guard with a 3s timeout.
            val blocked = withTimeoutOrNull(3_000L) {
                val systemBlocked = runCatching {
                    BlockedNumberContract.canCurrentUserBlockNumbers(applicationContext) &&
                        BlockedNumberContract.isBlocked(applicationContext, number)
                }.getOrDefault(false)
                if (systemBlocked) {
                    true
                } else {
                    runCatching {
                        SettingsRepository(applicationContext).isBlocked(number)
                    }.getOrDefault(false)
                }
            } ?: false

            val response = if (blocked) {
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)
                    .build()
            } else {
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .build()
            }
            respondToCall(callDetails, response)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
