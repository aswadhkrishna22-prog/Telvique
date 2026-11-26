package com.telvique.phone.ui.call

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.CallAudioState
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddCall
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.telvique.phone.MainActivity
import com.telvique.phone.TelviqueApp
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.TelviqueSettings
import com.telvique.phone.telecom.CallAudioUiState
import com.telvique.phone.telecom.CallManager
import com.telvique.phone.telecom.CallPhase
import com.telvique.phone.telecom.CallUiItem
import com.telvique.phone.telecom.TelecomUiState
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.theme.TelviqueColors
import com.telvique.phone.ui.theme.TelviqueTheme
import kotlinx.coroutines.delay

class CallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val settingsFlow = (application as TelviqueApp).settingsRepository.settingsFlow

        setContent {
            TelviqueTheme {
                val telecomState by CallManager.uiState.collectAsStateWithLifecycle()
                val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = TelviqueSettings())

                LaunchedEffect(telecomState.calls) {
                    if (telecomState.calls.isEmpty()) {
                        delay(350)
                        if (CallManager.uiState.value.calls.isEmpty()) {
                            finish()
                        }
                    }
                }

                CallScreen(
                    telecomState = telecomState,
                    quickResponses = settings.quickResponses,
                    onAnswer = { CallManager.answer(it) },
                    onDecline = { CallManager.decline(it) },
                    onRejectWithSms = { msg, id -> CallManager.rejectWithMessage(msg, id) },
                    onDisconnect = { CallManager.disconnect(it) },
                    onToggleMute = { CallManager.toggleMute() },
                    onToggleSpeaker = { CallManager.toggleSpeaker() },
                    onSelectAudioRoute = { CallManager.setAudioRoute(it) },
                    onToggleHold = { CallManager.toggleHold(it) },
                    onSwapCalls = { CallManager.swapCalls() },
                    onMergeCalls = { CallManager.mergeCalls() },
                    onAddCall = {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(intent)
                    },
                    onPlayDtmf = { digit, id ->
                        CallManager.playDtmf(digit, id)
                    },
                    onStopDtmf = { id ->
                        CallManager.stopDtmf(id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallScreen(
    telecomState: TelecomUiState,
    quickResponses: List<String>,
    onAnswer: (String) -> Unit,
    onDecline: (String) -> Unit,
    onRejectWithSms: (String, String) -> Unit,
    onDisconnect: (String) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSelectAudioRoute: (Int) -> Unit,
    onToggleHold: (String) -> Unit,
    onSwapCalls: () -> Unit,
    onMergeCalls: () -> Unit,
    onAddCall: () -> Unit,
    onPlayDtmf: (Char, String) -> Unit,
    onStopDtmf: (String) -> Unit
) {
    val primary = telecomState.primaryCall
    val secondary = telecomState.secondaryCall
    var showKeypad by remember { mutableStateOf(false) }
    var dtmfDigits by remember { mutableStateOf("") }
    var showSmsSheet by remember { mutableStateOf(false) }
    var showAudioRouteSheet by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(primary?.id, primary?.phase, primary?.connectTimeMillis) {
        if (primary != null && primary.phase == CallPhase.ACTIVE && primary.connectTimeMillis > 0L) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - primary.connectTimeMillis) / 1000L)
                    .coerceAtLeast(0L)
                delay(1000L)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (primary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Call ended",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (secondary != null) {
                SecondaryCallCard(
                    call = secondary,
                    onSwap = onSwapCalls,
                    onDisconnect = { onDisconnect(secondary.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!primary.simLabel.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = primary.simLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Spacer(modifier = Modifier.height(22.dp))
            }

            ContactAvatar(
                name = primary.displayName,
                photoUri = primary.photoUri,
                size = 96.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = primary.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (primary.number.isNotBlank() && primary.number != primary.displayName) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = primary.number,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val statusText = when (primary.phase) {
                CallPhase.NEW, CallPhase.DIALING -> "Dialing..."
                CallPhase.RINGING -> "Incoming call"
                CallPhase.HOLDING -> "On hold"
                CallPhase.ACTIVE -> PhoneFormatUtils.formatTimer(elapsedSeconds)
                CallPhase.DISCONNECTING -> "Ending call..."
                CallPhase.DISCONNECTED -> primary.disconnectCauseLabel ?: "Call ended"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = if (primary.phase == CallPhase.ACTIVE) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (primary.phase == CallPhase.RINGING) {
                OutlinedButton(onClick = { showSmsSheet = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Message,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Message")
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundCallButton(
                        icon = Icons.Rounded.CallEnd,
                        label = "Decline",
                        containerColor = TelviqueColors.CallRed,
                        onClick = { onDecline(primary.id) }
                    )
                    RoundCallButton(
                        icon = Icons.Rounded.Call,
                        label = "Answer",
                        containerColor = TelviqueColors.CallGreen,
                        onClick = { onAnswer(primary.id) }
                    )
                }
            } else {
                AnimatedVisibility(visible = showKeypad) {
                    InCallDtmfKeypad(
                        digitsEntered = dtmfDigits,
                        onDigitClick = { digit ->
                            dtmfDigits += digit
                            onPlayDtmf(digit, primary.id)
                        },
                        onDigitReleased = { onStopDtmf(primary.id) },
                        onHide = { showKeypad = false }
                    )
                }

                if (!showKeypad) {
                    InCallControlsGrid(
                        primary = primary,
                        hasSecondaryCall = secondary != null,
                        audioState = telecomState.audioState,
                        showKeypad = showKeypad,
                        onToggleMute = onToggleMute,
                        onToggleKeypad = { showKeypad = !showKeypad },
                        onAudioClick = {
                            val hasExtraRoutes =
                                (telecomState.audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0 ||
                                    (telecomState.audioState.supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET) != 0
                            if (hasExtraRoutes) {
                                showAudioRouteSheet = true
                            } else {
                                onToggleSpeaker()
                            }
                        },
                        onToggleHold = { onToggleHold(primary.id) },
                        onAddCall = onAddCall,
                        onSwapCalls = onSwapCalls,
                        onMergeCalls = onMergeCalls
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                RoundCallButton(
                    icon = Icons.Rounded.CallEnd,
                    label = "End",
                    containerColor = TelviqueColors.CallRed,
                    onClick = { onDisconnect(primary.id) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSmsSheet && primary != null) {
        ModalBottomSheet(onDismissRequest = { showSmsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Decline with message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                quickResponses.forEach { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showSmsSheet = false
                                onRejectWithSms(msg, primary.id)
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showAudioRouteSheet) {
        val audio = telecomState.audioState
        ModalBottomSheet(onDismissRequest = { showAudioRouteSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Audio output",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if ((audio.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0) {
                    AudioRouteRow(
                        icon = Icons.Rounded.BluetoothAudio,
                        label = audio.bluetoothDeviceName ?: "Bluetooth",
                        selected = audio.route == CallAudioState.ROUTE_BLUETOOTH,
                        onClick = {
                            onSelectAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
                            showAudioRouteSheet = false
                        }
                    )
                }
                if ((audio.supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
                    AudioRouteRow(
                        icon = Icons.Rounded.Headset,
                        label = "Headset",
                        selected = audio.route == CallAudioState.ROUTE_WIRED_HEADSET,
                        onClick = {
                            onSelectAudioRoute(CallAudioState.ROUTE_WIRED_HEADSET)
                            showAudioRouteSheet = false
                        }
                    )
                }
                AudioRouteRow(
                    icon = Icons.Rounded.PhoneInTalk,
                    label = "Phone Earpiece",
                    selected = audio.route == CallAudioState.ROUTE_EARPIECE,
                    onClick = {
                        onSelectAudioRoute(CallAudioState.ROUTE_EARPIECE)
                        showAudioRouteSheet = false
                    }
                )
                AudioRouteRow(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    label = "Speaker",
                    selected = audio.route == CallAudioState.ROUTE_SPEAKER,
                    onClick = {
                        onSelectAudioRoute(CallAudioState.ROUTE_SPEAKER)
                        showAudioRouteSheet = false
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SecondaryCallCard(
    call: CallUiItem,
    onSwap: () -> Unit,
    onDisconnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (call.phase == CallPhase.HOLDING) "On hold" else "Other call",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onSwap) {
                Text("Swap")
            }
            TextButton(onClick = onDisconnect) {
                Text("End", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun InCallControlsGrid(
    primary: CallUiItem,
    hasSecondaryCall: Boolean,
    audioState: CallAudioUiState,
    showKeypad: Boolean,
    onToggleMute: () -> Unit,
    onToggleKeypad: () -> Unit,
    onAudioClick: () -> Unit,
    onToggleHold: () -> Unit,
    onAddCall: () -> Unit,
    onSwapCalls: () -> Unit,
    onMergeCalls: () -> Unit
) {
    val audioIcon = when (audioState.route) {
        CallAudioState.ROUTE_BLUETOOTH -> Icons.Rounded.BluetoothAudio
        CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Rounded.Headset
        else -> Icons.AutoMirrored.Rounded.VolumeUp
    }
    val audioLabel = when (audioState.route) {
        CallAudioState.ROUTE_BLUETOOTH -> "Bluetooth"
        CallAudioState.ROUTE_WIRED_HEADSET -> "Headset"
        else -> "Speaker"
    }
    val audioActive = audioState.route != CallAudioState.ROUTE_EARPIECE

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CallControlTile(
                icon = if (audioState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                label = if (audioState.isMuted) "Unmute" else "Mute",
                active = audioState.isMuted,
                onClick = onToggleMute
            )
            CallControlTile(
                icon = Icons.Rounded.Dialpad,
                label = "Keypad",
                active = showKeypad,
                onClick = onToggleKeypad
            )
            CallControlTile(
                icon = audioIcon,
                label = audioLabel,
                active = audioActive,
                onClick = onAudioClick
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CallControlTile(
                icon = if (primary.phase == CallPhase.HOLDING) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                label = if (primary.phase == CallPhase.HOLDING) "Resume" else "Hold",
                active = primary.phase == CallPhase.HOLDING,
                enabled = primary.canHold,
                onClick = onToggleHold
            )
            if (hasSecondaryCall && primary.canMerge) {
                CallControlTile(
                    icon = Icons.Rounded.CallMerge,
                    label = "Merge",
                    active = false,
                    onClick = onMergeCalls
                )
            } else {
                CallControlTile(
                    icon = Icons.Rounded.AddCall,
                    label = "Add call",
                    active = false,
                    onClick = onAddCall
                )
            }
            if (hasSecondaryCall) {
                CallControlTile(
                    icon = Icons.Rounded.SwapCalls,
                    label = "Swap",
                    active = false,
                    onClick = onSwapCalls
                )
            } else {
                Spacer(modifier = Modifier.size(76.dp))
            }
        }
    }
}

@Composable
private fun CallControlTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(84.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
            shape = CircleShape,
            color = when {
                !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                active -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        active -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            }
        )
    }
}

@Composable
private fun RoundCallButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = containerColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InCallDtmfKeypad(
    digitsEntered: String,
    onDigitClick: (Char) -> Unit,
    onDigitReleased: () -> Unit,
    onHide: () -> Unit
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#')
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (digitsEntered.isNotEmpty()) {
            Text(
                text = digitsEntered,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    Surface(
                        modifier = Modifier
                            .size(width = 80.dp, height = 54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .clickable {
                                onDigitClick(digit)
                                onDigitReleased()
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = digit.toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        TextButton(onClick = onHide) {
            Text("Hide keypad")
        }
    }
}

@Composable
private fun AudioRouteRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
