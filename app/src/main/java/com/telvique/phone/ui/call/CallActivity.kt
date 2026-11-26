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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.AddIcCall
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
import com.telvique.phone.ui.components.posterColorsForName
import com.telvique.phone.ui.glass.backdropLiquidGlass
import com.telvique.phone.ui.glass.isBackdropGlassCapable
import com.telvique.phone.ui.glass.liquidBubbleHighlight
import com.telvique.phone.ui.icons.ApplePhoneIcons
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
            TelviqueTheme(darkTheme = true, forceDarkSystemBars = true) {
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
                    onPlayDtmf = { digit, id -> CallManager.playDtmf(digit, id) },
                    onStopDtmf = { id -> CallManager.stopDtmf(id) }
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

    val glassCapable = isBackdropGlassCapable()
    val posterBackdrop = if (glassCapable) {
        rememberLayerBackdrop {
            drawRect(Color(0xFF0A0B0E))
            drawContent()
        }
    } else null

    val posterName = primary?.displayName ?: "Telvique"
    val (posterTop, posterBottom) = posterColorsForName(posterName)

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Full-bleed Apple Contact Poster atmospheric background (captured by Kyant Backdrop!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (posterBackdrop != null) Modifier.layerBackdrop(posterBackdrop) else Modifier)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            posterTop.copy(alpha = 0.72f),
                            posterBottom.copy(alpha = 0.56f),
                            Color(0xFF0D1017),
                            Color(0xFF050608),
                        )
                    )
                )
        ) {
            if (!primary?.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = primary?.photoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.48f)
                        .blur(48.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.78f),
                                )
                            )
                        )
                )
            }
        }

        // Layer 2: Interactive Liquid Glass Controls & Caller Poster UI
        if (primary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Call Ended",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (secondary != null) {
                    SecondaryCallGlassCard(
                        call = secondary,
                        backdrop = posterBackdrop,
                        onSwap = onSwapCalls,
                        onDisconnect = { onDisconnect(secondary.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // SIM line badge
                if (!primary.simLabel.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .backdropLiquidGlass(
                                backdrop = posterBackdrop,
                                shape = CircleShape,
                                isDark = true,
                                interactive = false,
                                scrimAlpha = 0.25f,
                                fallbackTint = Color.White.copy(alpha = 0.14f)
                            )
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = primary.simLabel,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Status / Live tabular timer above Caller Name (iOS Contact Poster style)
                val statusText = when (primary.phase) {
                    CallPhase.NEW, CallPhase.DIALING -> "calling..."
                    CallPhase.RINGING -> "Incoming Call"
                    CallPhase.HOLDING -> "On Hold"
                    CallPhase.ACTIVE -> PhoneFormatUtils.formatTimer(elapsedSeconds)
                    CallPhase.DISCONNECTING -> "ending..."
                    CallPhase.DISCONNECTED -> primary.disconnectCauseLabel ?: "Call Ended"
                }

                Text(
                    text = statusText,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum"
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = primary.displayName,
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (primary.number.isNotBlank() && primary.number != primary.displayName) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = primary.number,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                if (!showKeypad) {
                    ContactAvatar(
                        name = primary.displayName,
                        photoUri = primary.photoUri,
                        size = 104.dp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (primary.phase == CallPhase.RINGING) {
                    // Quick SMS pill
                    Row(
                        modifier = Modifier
                            .backdropLiquidGlass(
                                backdrop = posterBackdrop,
                                shape = CircleShape,
                                isDark = true,
                                interactive = true,
                                pressedScale = 1.05f,
                                scrimAlpha = 0.24f,
                                fallbackTint = Color.White.copy(alpha = 0.16f)
                            )
                            .clip(CircleShape)
                            .clickable { showSmsSheet = true }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ApplePhoneIcons.MessageFill,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Message",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(34.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 26.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppleSolidCallCircle(
                            icon = ApplePhoneIcons.PhoneDownFill,
                            label = "Decline",
                            containerColor = TelviqueColors.CallRed,
                            onClick = { onDecline(primary.id) }
                        )
                        AppleSolidCallCircle(
                            icon = ApplePhoneIcons.PhoneFill,
                            label = "Accept",
                            containerColor = TelviqueColors.CallGreen,
                            onClick = { onAnswer(primary.id) }
                        )
                    }
                } else {
                    AnimatedVisibility(
                        visible = showKeypad,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        AppleInCallDtmfKeypad(
                            digitsEntered = dtmfDigits,
                            backdrop = posterBackdrop,
                            onDigitClick = { digit ->
                                dtmfDigits += digit
                                onPlayDtmf(digit, primary.id)
                            },
                            onDigitReleased = { onStopDtmf(primary.id) },
                            onHide = { showKeypad = false }
                        )
                    }

                    if (!showKeypad) {
                        AppleInCallControlsGrid(
                            primary = primary,
                            hasSecondaryCall = secondary != null,
                            audioState = telecomState.audioState,
                            showKeypad = showKeypad,
                            backdrop = posterBackdrop,
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

                    Spacer(modifier = Modifier.height(30.dp))

                    AppleSolidCallCircle(
                        icon = ApplePhoneIcons.PhoneDownFill,
                        label = "End",
                        containerColor = TelviqueColors.CallRed,
                        onClick = { onDisconnect(primary.id) }
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }

    if (showSmsSheet && primary != null) {
        ModalBottomSheet(
            onDismissRequest = { showSmsSheet = false },
            containerColor = Color(0xFF1C1C1E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Decline with Message",
                    color = Color.White,
                    fontSize = 18.sp,
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
                        color = Color(0xFF2C2C2E)
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 16.sp,
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
        ModalBottomSheet(
            onDismissRequest = { showAudioRouteSheet = false },
            containerColor = Color(0xFF1C1C1E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Audio",
                    color = Color.White,
                    fontSize = 18.sp,
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
                    label = "iPhone / Earpiece",
                    selected = audio.route == CallAudioState.ROUTE_EARPIECE,
                    onClick = {
                        onSelectAudioRoute(CallAudioState.ROUTE_EARPIECE)
                        showAudioRouteSheet = false
                    }
                )
                AudioRouteRow(
                    icon = ApplePhoneIcons.SpeakerWave3Fill,
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
private fun SecondaryCallGlassCard(
    call: CallUiItem,
    backdrop: Backdrop?,
    onSwap: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .backdropLiquidGlass(
                backdrop = backdrop,
                shape = RoundedCornerShape(18.dp),
                isDark = true,
                interactive = false,
                scrimAlpha = 0.28f,
                fallbackTint = Color.White.copy(alpha = 0.14f)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.displayName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (call.phase == CallPhase.HOLDING) "On Hold" else "Other Call",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp
            )
        }
        TextButton(onClick = onSwap) {
            Text("Swap", color = TelviqueColors.AppleBlueDark, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onDisconnect) {
            Text("End", color = TelviqueColors.CallRed, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AppleInCallControlsGrid(
    primary: CallUiItem,
    hasSecondaryCall: Boolean,
    audioState: CallAudioUiState,
    showKeypad: Boolean,
    backdrop: Backdrop?,
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
        else -> ApplePhoneIcons.SpeakerWave3Fill
    }
    val audioLabel = when (audioState.route) {
        CallAudioState.ROUTE_BLUETOOTH -> "bluetooth"
        CallAudioState.ROUTE_WIRED_HEADSET -> "headset"
        else -> "speaker"
    }
    val audioActive = audioState.route != CallAudioState.ROUTE_EARPIECE

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LiquidCallControlTile(
                icon = audioIcon,
                label = audioLabel,
                active = audioActive,
                backdrop = backdrop,
                onClick = onAudioClick
            )
            LiquidCallControlTile(
                icon = if (primary.phase == CallPhase.HOLDING) ApplePhoneIcons.PlayFill else ApplePhoneIcons.PauseFill,
                label = if (primary.phase == CallPhase.HOLDING) "resume" else "hold",
                active = primary.phase == CallPhase.HOLDING,
                enabled = primary.canHold,
                backdrop = backdrop,
                onClick = onToggleHold
            )
            LiquidCallControlTile(
                icon = if (audioState.isMuted) ApplePhoneIcons.MicSlashFill else ApplePhoneIcons.MicFill,
                label = "mute",
                active = audioState.isMuted,
                backdrop = backdrop,
                onClick = onToggleMute
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (hasSecondaryCall && primary.canMerge) {
                LiquidCallControlTile(
                    icon = Icons.Rounded.CallMerge,
                    label = "merge",
                    active = false,
                    backdrop = backdrop,
                    onClick = onMergeCalls
                )
            } else {
                LiquidCallControlTile(
                    icon = Icons.Rounded.AddIcCall,
                    label = "add call",
                    active = false,
                    backdrop = backdrop,
                    onClick = onAddCall
                )
            }
            LiquidCallControlTile(
                icon = ApplePhoneIcons.KeypadDots,
                label = "keypad",
                active = showKeypad,
                backdrop = backdrop,
                onClick = onToggleKeypad
            )
            if (hasSecondaryCall) {
                LiquidCallControlTile(
                    icon = Icons.Rounded.SwapCalls,
                    label = "swap",
                    active = false,
                    backdrop = backdrop,
                    onClick = onSwapCalls
                )
            } else {
                Spacer(modifier = Modifier.width(84.dp))
            }
        }
    }
}

@Composable
private fun LiquidCallControlTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    backdrop: Backdrop?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(84.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val circleModifier = when {
            !enabled -> Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
            active -> Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White, CircleShape)
                .liquidBubbleHighlight(isDark = false, cornerFraction = 0.5f)
            else -> Modifier
                .size(72.dp)
                .backdropLiquidGlass(
                    backdrop = backdrop,
                    shape = CircleShape,
                    isDark = true,
                    interactive = true,
                    pressedScale = 1.07f,
                    scrimAlpha = 0.22f,
                    fallbackTint = Color.White.copy(alpha = 0.16f)
                )
                .clip(CircleShape)
        }

        Box(
            modifier = circleModifier.clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    !enabled -> Color.White.copy(alpha = 0.3f)
                    active -> Color.Black
                    else -> Color.White
                },
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun AppleSolidCallCircle(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(containerColor, CircleShape)
                .liquidBubbleHighlight(isDark = false, cornerFraction = 0.5f) { 0.75f }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.88f)
        )
    }
}

@Composable
private fun AppleInCallDtmfKeypad(
    digitsEntered: String,
    backdrop: Backdrop?,
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
                style = TextStyle(
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .backdropLiquidGlass(
                                backdrop = backdrop,
                                shape = CircleShape,
                                isDark = true,
                                interactive = true,
                                pressedScale = 1.07f,
                                scrimAlpha = 0.22f,
                                fallbackTint = Color.White.copy(alpha = 0.16f)
                            )
                            .clip(CircleShape)
                            .clickable {
                                onDigitClick(digit)
                                onDigitReleased()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit.toString(),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Normal,
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        TextButton(onClick = onHide) {
            Text("Hide", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
        color = if (selected) TelviqueColors.AppleBlueDark.copy(alpha = 0.25f) else Color(0xFF2C2C2E)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) TelviqueColors.AppleBlueDark else Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = ApplePhoneIcons.Checkmark,
                    contentDescription = "Selected",
                    tint = TelviqueColors.AppleBlueDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
