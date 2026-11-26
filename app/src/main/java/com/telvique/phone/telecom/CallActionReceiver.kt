package com.telvique.phone.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID)
        when (intent.action) {
            ACTION_ANSWER -> CallManager.answer(callId)
            ACTION_DECLINE -> CallManager.decline(callId)
            ACTION_HANGUP -> CallManager.disconnect(callId)
        }
    }

    companion object {
        const val ACTION_ANSWER = "com.telvique.phone.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE = "com.telvique.phone.ACTION_DECLINE_CALL"
        const val ACTION_HANGUP = "com.telvique.phone.ACTION_HANGUP_CALL"
        const val EXTRA_CALL_ID = "extra_call_id"
    }
}
