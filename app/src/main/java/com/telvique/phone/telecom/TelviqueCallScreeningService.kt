package com.telvique.phone.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import com.telvique.phone.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TelviqueCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        scope.launch {
            val blocked = runCatching {
                SettingsRepository(applicationContext).isBlocked(number)
            }.getOrDefault(false)

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
}
