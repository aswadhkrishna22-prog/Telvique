package com.telvique.phone.telecom

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.telvique.phone.MainActivity
import com.telvique.phone.R
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.ui.call.CallActivity

object CallNotificationHelper {

    const val CHANNEL_INCOMING = "telvique_incoming_call"
    const val CHANNEL_ONGOING = "telvique_ongoing_call"
    const val CHANNEL_MISSED = "telvique_missed_call"
    const val EXTRA_OPEN_RECENTS = "extra_open_recents"
    private const val NOTIFICATION_ID = 4011
    private const val MISSED_NOTIFICATION_ID = 4012

    fun showOrUpdate(context: Context, call: CallUiItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val fullScreenIntent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            100,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callerPerson = Person.Builder()
            .setName(call.displayName)
            .setImportant(true)
            .build()

        val manager = NotificationManagerCompat.from(context)

        val notification = if (call.phase == CallPhase.RINGING) {
            val answerIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_ANSWER
                putExtra(CallActionReceiver.EXTRA_CALL_ID, call.id)
            }
            val answerPending = PendingIntent.getBroadcast(
                context,
                101,
                answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_DECLINE
                putExtra(CallActionReceiver.EXTRA_CALL_ID, call.id)
            }
            val declinePending = PendingIntent.getBroadcast(
                context,
                102,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_INCOMING)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(call.displayName)
                .setContentText(
                    call.number.takeIf { it.isNotBlank() }?.let {
                        PhoneFormatUtils.formatDisplayNumber(it)
                    } ?: "Incoming call"
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(contentPendingIntent)
                .setFullScreenIntent(contentPendingIntent, true)
                .setStyle(
                    NotificationCompat.CallStyle.forIncomingCall(
                        callerPerson,
                        declinePending,
                        answerPending
                    )
                )

            if (!call.networkName.isNullOrBlank()) {
                builder.setSubText(context.getString(R.string.cnap_operator_records_label))
            }
            builder.build()
        } else {
            val hangupIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_HANGUP
                putExtra(CallActionReceiver.EXTRA_CALL_ID, call.id)
            }
            val hangupPending = PendingIntent.getBroadcast(
                context,
                103,
                hangupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val subtitle = when (call.phase) {
                CallPhase.DIALING -> "Dialing..."
                CallPhase.HOLDING -> "On hold"
                CallPhase.ACTIVE -> "Ongoing call"
                CallPhase.DISCONNECTING -> "Ending call..."
                else -> "Call"
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(call.displayName)
                .setContentText(subtitle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setUsesChronometer(call.phase == CallPhase.ACTIVE && call.connectTimeMillis > 0L)
                .apply {
                    if (call.phase == CallPhase.ACTIVE && call.connectTimeMillis > 0L) {
                        setWhen(call.connectTimeMillis)
                    }
                }
                .setContentIntent(contentPendingIntent)
                .setStyle(
                    NotificationCompat.CallStyle.forOngoingCall(
                        callerPerson,
                        hangupPending
                    )
                )

            if (!call.networkName.isNullOrBlank()) {
                builder.setSubText(context.getString(R.string.cnap_operator_records_label))
            }
            builder.build()
        }

        runCatching {
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    fun showMissedCall(context: Context, displayName: String, number: String, isOperatorName: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openRecentsIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_RECENTS, true)
        }
        val openPending = PendingIntent.getActivity(
            context,
            200,
            openRecentsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Missed call")
            .setContentText(
                if (displayName.isNotBlank() && displayName != number) {
                    "$displayName (${PhoneFormatUtils.formatDisplayNumber(number)})"
                } else {
                    PhoneFormatUtils.formatDisplayNumber(number).ifBlank { "Unknown Caller" }
                }
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(openPending)

        if (isOperatorName) {
            builder.setSubText(context.getString(R.string.cnap_operator_records_label))
        }

        if (number.isNotBlank()) {
            val callBackIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_CALL_BACK
                putExtra(CallActionReceiver.EXTRA_NUMBER, number)
            }
            val callBackPending = PendingIntent.getBroadcast(
                context,
                201,
                callBackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Call Back", callBackPending)

            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val smsPending = PendingIntent.getActivity(
                context,
                202,
                smsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Message", smsPending)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(MISSED_NOTIFICATION_ID, builder.build())
        }
    }

    fun cancelMissedCalls(context: Context) {
        NotificationManagerCompat.from(context).cancel(MISSED_NOTIFICATION_ID)
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
