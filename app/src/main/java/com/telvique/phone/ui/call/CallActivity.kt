package com.telvique.phone.ui.call

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.ContactsContract
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddIcCall
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import coil3.compose.AsyncImage
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.telvique.phone.MainActivity
import com.telvique.phone.R
import com.telvique.phone.TelviqueApp
import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.PhoneRepository
import com.telvique.phone.data.SlideToAnswerUtils
import com.telvique.phone.data.TelviqueSettings
import com.telvique.phone.telecom.CallAudioUiState
import com.telvique.phone.telecom.CallManager
import com.telvique.phone.telecom.CallPhase
import com.telvique.phone.telecom.CallUiItem
import com.telvique.phone.telecom.TelecomUiState
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.contacts.AppleSearchField
import com.telvique.phone.ui.glass.backdropLiquidGlass
import com.telvique.phone.ui.glass.isBackdropGlassCapable
import com.telvique.phone.ui.glass.liquidBubbleHighlight
import com.telvique.phone.ui.icons.ApplePhoneIcons
import com.telvique.phone.ui.theme.TelviqueColors
import com.telvique.phone.ui.theme.TelviqueTheme

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

        val settingsRepo = (application as TelviqueApp).settingsRepository
        val settingsFlow = settingsRepo.settingsFlow

        setContent {
            TelviqueTheme(darkTheme = true, forceDarkSystemBars = true) {
                val telecomState by CallManager.uiState.collectAsStateWithLifecycle()
                val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = TelviqueSettings())
                var isDialingVerificationCall by remember { mutableStateOf(false) }

                LaunchedEffect(telecomState.calls, isDialingVerificationCall) {
                    if (telecomState.calls.isEmpty() && !isDialingVerificationCall) {
                        delay(350)
                        if (CallManager.uiState.value.calls.isEmpty() && !isDialingVerificationCall) {
                            finish()
                        }
                    }
                }

                LaunchedEffect(isDialingVerificationCall) {
                    if (isDialingVerificationCall) {
                        delay(3500L)
                        isDialingVerificationCall = false
                    }
                }

                CallScreen(
                    telecomState = telecomState,
                    quickResponses = settings.quickResponses,
                    onAnswer = { CallManager.answer(it) },
                    onAnswerAndHoldOthers = { CallManager.answerAndHoldOthers(it) },
                    onAnswerAndEndOthers = { CallManager.answerAndEndOthers(it) },
                    onDecline = { CallManager.decline(it) },
                    onSilenceRinger = { silenceIncomingRinger() },
                    onBlockAndDecline = { number, id ->
                        lifecycleScope.launch {
                            if (number.isNotBlank()) settingsRepo.blockNumber(number)
                        }
                        CallManager.decline(id)
                    },
                    onRejectWithSms = { msg, id -> CallManager.rejectWithMessage(msg, id) },
                    onDisconnect = { CallManager.disconnect(it) },
                    onSplitParticipant = { CallManager.splitParticipant(it) },
                    onDisconnectParticipant = { CallManager.disconnectParticipant(it) },
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
                    onOpenContacts = {
                        val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { startActivity(intent) }
                    },
                    onPlayDtmf = { digit, id -> CallManager.playDtmf(digit, id) },
                    onStopDtmf = { id -> CallManager.stopDtmf(id) },
                    onVerifyCaller = { callId, newNumber ->
                        isDialingVerificationCall = true
                        CallManager.disconnectAndCall(this, callId, newNumber)
                    }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun silenceIncomingRinger() {
        runCatching {
            getSystemService(TelecomManager::class.java)?.silenceRinger()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val phase = CallManager.uiState.value.primaryCall?.phase
            if (phase == CallPhase.RINGING) {
                silenceIncomingRinger()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallScreen(
    telecomState: TelecomUiState,
    quickResponses: List<String>,
    onAnswer: (String) -> Unit,
    onAnswerAndHoldOthers: (String) -> Unit,
    onAnswerAndEndOthers: (String) -> Unit,
    onDecline: (String) -> Unit,
    onSilenceRinger: () -> Unit,
    onBlockAndDecline: (String, String) -> Unit,
    onRejectWithSms: (String, String) -> Unit,
    onDisconnect: (String) -> Unit,
    onSplitParticipant: (String) -> Unit,
    onDisconnectParticipant: (String) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSelectAudioRoute: (Int) -> Unit,
    onToggleHold: (String) -> Unit,
    onSwapCalls: () -> Unit,
    onMergeCalls: () -> Unit,
    onAddCall: () -> Unit,
    onOpenContacts: () -> Unit,
    onPlayDtmf: (Char, String) -> Unit,
    onStopDtmf: (String) -> Unit,
    onVerifyCaller: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val primary = telecomState.primaryCall
    val secondary = telecomState.secondaryCall
    var showKeypad by remember { mutableStateOf(false) }
    var dtmfDigits by remember { mutableStateOf("") }
    var showSmsSheet by remember { mutableStateOf(false) }
    var showAudioRouteSheet by remember { mutableStateOf(false) }
    var showParticipantsSheet by remember { mutableStateOf(false) }
    var showVerifyCallerSheet by remember { mutableStateOf(false) }
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
            drawRect(Color(0xFF08090C))
            drawContent()
        }
    } else null

    val isDualSim = remember(context) {
        runCatching {
            PhoneRepository(context).getSimAccounts().size > 1
        }.getOrDefault(false)
    }

    val ringingEntranceAnim = remember { Animatable(0f) }
    LaunchedEffect(primary?.id, primary?.phase) {
        if (primary?.phase == CallPhase.RINGING) {
            ringingEntranceAnim.snapTo(0f)
            ringingEntranceAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing)
            )
        } else {
            ringingEntranceAnim.snapTo(1f)
        }
    }
    val entranceAlpha = ringingEntranceAnim.value
    val entranceTranslateY = with(LocalDensity.current) { ((1f - ringingEntranceAnim.value) * 8.dp.toPx()) }

    val hasPhoto = !primary?.photoUri.isNullOrBlank()

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Edge-to-edge Apple Contact Poster backdrop or solid near-black fallback (captured by Kyant Backdrop!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (posterBackdrop != null) Modifier.layerBackdrop(posterBackdrop) else Modifier)
                .background(Color(0xFF08090C))
        ) {
            if (hasPhoto) {
                // 1. Crisp base photo layer (sharp face and caller name)
                AsyncImage(
                    model = primary?.photoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Progressive glass blur layer (Android 12+ API 31 only)
                // Minimal at top (face/name area sharp), gradually increasing downwards towards buttons
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AsyncImage(
                        model = primary?.photoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(radius = 22.dp)
                            .graphicsLayer {
                                alpha = 0.99f
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.00f to Color.Transparent,
                                        0.30f to Color.Transparent,
                                        0.65f to Color.Black,
                                        1.00f to Color.Black
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    )
                }

                // 3. Cinematic vertical scrim for AA+ text legibility & button contrast (transparent -> black alpha ~0.55)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Black.copy(alpha = 0.65f),
                                0.28f to Color.Black.copy(alpha = 0.35f),
                                0.45f to Color.Transparent,
                                0.60f to Color.Transparent,
                                0.78f to Color.Black.copy(alpha = 0.40f),
                                1.00f to Color.Black.copy(alpha = 0.58f)
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
                    .statusBarsPadding()
                    .navigationBarsPadding(),
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
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .graphicsLayer {
                        if (primary.phase == CallPhase.RINGING) {
                            alpha = entranceAlpha
                            translationY = entranceTranslateY
                        }
                    },
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

                Spacer(modifier = Modifier.height(12.dp))

                // Status / Live tabular timer above Caller Name (iOS Contact Poster style)
                val statusText = when (primary.phase) {
                    CallPhase.NEW, CallPhase.DIALING -> "Calling..."
                    CallPhase.RINGING -> "Incoming call"
                    CallPhase.HOLDING -> "On hold"
                    CallPhase.ACTIVE -> PhoneFormatUtils.formatTimer(elapsedSeconds)
                    CallPhase.DISCONNECTING -> "Ending..."
                    CallPhase.DISCONNECTED -> primary.disconnectCauseLabel ?: "Call ended"
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
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!primary.networkName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.cnap_operator_records_label),
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }

                if (primary.number.isNotBlank() && primary.number != primary.displayName) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = PhoneFormatUtils.formatDisplayNumber(primary.number),
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        if (isDualSim && !primary.simLabel.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            SimChip(label = primary.simLabel)
                        }
                    }
                } else if (isDualSim && !primary.simLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SimChip(label = primary.simLabel)
                }

                if (primary.isConference && primary.participants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .backdropLiquidGlass(
                                backdrop = posterBackdrop,
                                shape = CircleShape,
                                isDark = true,
                                interactive = true,
                                pressedScale = 1.04f,
                                hasLens = false,
                                scrimAlpha = 0.28f,
                                fallbackTint = Color.White.copy(alpha = 0.16f)
                            )
                            .clip(CircleShape)
                            .clickable { showParticipantsSheet = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Manage (${primary.participants.size})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (primary.isPotentialSpam) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(TelviqueColors.CallRed.copy(alpha = 0.85f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Suspected Telemarketer / Spam",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!showKeypad && !hasPhoto) {
                    NoPhotoInitialsAvatar(name = primary.displayName)
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (primary.phase == CallPhase.RINGING) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 340.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IncomingCallGlassPillButton(
                            icon = ApplePhoneIcons.MessageOutline,
                            label = "Message",
                            backdrop = posterBackdrop,
                            onClick = { showSmsSheet = true },
                            modifier = Modifier.weight(1f)
                        )

                        IncomingCallGlassPillButton(
                            icon = ApplePhoneIcons.SpeakerSlashOutline,
                            label = "Silence",
                            backdrop = posterBackdrop,
                            onClick = onSilenceRinger,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (primary.isPotentialSpam && primary.number.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(TelviqueColors.CallRed.copy(alpha = 0.85f), CircleShape)
                                .clickable { onBlockAndDecline(primary.number, primary.id) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Block & Decline",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    if (secondary != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 26.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppleSolidCallCircle(
                                icon = ApplePhoneIcons.PhoneDownFill,
                                label = "End & Accept",
                                containerColor = TelviqueColors.CallRed,
                                onClick = { onAnswerAndEndOthers(primary.id) }
                            )
                            AppleSolidCallCircle(
                                icon = ApplePhoneIcons.PhoneDownFill,
                                label = "Decline",
                                containerColor = Color(0xFF3A3A3C),
                                onClick = { onDecline(primary.id) }
                            )
                            AppleSolidCallCircle(
                                icon = ApplePhoneIcons.PhoneFill,
                                label = "Hold & Accept",
                                containerColor = TelviqueColors.CallGreen,
                                onClick = { onAnswerAndHoldOthers(primary.id) }
                            )
                        }
                    } else {
                        AppleSlideToAnswerPill(
                            backdrop = posterBackdrop,
                            callPhase = primary.phase,
                            callId = primary.id,
                            onAnswer = { onAnswer(primary.id) },
                            onDecline = { onDecline(primary.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 48.dp)
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
                            isMuted = telecomState.audioState.isMuted,
                            isSpeaker = telecomState.audioState.route == CallAudioState.ROUTE_SPEAKER,
                            backdrop = posterBackdrop,
                            onDigitClick = { digit ->
                                dtmfDigits += digit
                                onPlayDtmf(digit, primary.id)
                            },
                            onDigitReleased = { onStopDtmf(primary.id) },
                            onToggleMute = onToggleMute,
                            onToggleSpeaker = onToggleSpeaker,
                            onHide = { showKeypad = false }
                        )
                    }

                    if (!showKeypad) {
                        if (!primary.isSavedContact && !primary.isConference &&
                            (primary.phase == CallPhase.ACTIVE || primary.phase == CallPhase.HOLDING)
                        ) {
                            VerifyCallerPill(
                                backdrop = posterBackdrop,
                                onClick = { showVerifyCallerSheet = true }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

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
                            onOpenContacts = onOpenContacts,
                            onSwapCalls = onSwapCalls,
                            onMergeCalls = onMergeCalls
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val endCallLabel = when (primary.phase) {
                        CallPhase.DIALING, CallPhase.NEW, CallPhase.RINGING -> "Cancel"
                        else -> "End call"
                    }

                    AppleSolidCallCircle(
                        icon = ApplePhoneIcons.PhoneDownFill,
                        label = endCallLabel,
                        containerColor = Color(0xFFE53935),
                        onClick = { onDisconnect(primary.id) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
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

    if (showParticipantsSheet && primary != null && primary.participants.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showParticipantsSheet = false },
            containerColor = Color(0xFF1C1C1E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Conference (${primary.participants.size})",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                primary.participants.forEach { participant ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2C2C2E)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = participant.displayName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (participant.number.isNotBlank() && participant.number != participant.displayName) {
                                    Text(
                                        text = PhoneFormatUtils.formatDisplayNumber(participant.number),
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            if (participant.canSplit) {
                                TextButton(onClick = {
                                    onSplitParticipant(participant.id)
                                    showParticipantsSheet = false
                                }) {
                                    Text("Split", color = TelviqueColors.AppleBlueDark, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (participant.canDisconnect) {
                                TextButton(onClick = {
                                    onDisconnectParticipant(participant.id)
                                }) {
                                    Text("End", color = TelviqueColors.CallRed, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    if (showVerifyCallerSheet && primary != null) {
        VerifyCallerSheet(
            onDismissRequest = { showVerifyCallerSheet = false },
            onSelectNumber = { chosenNumber ->
                showVerifyCallerSheet = false
                onVerifyCaller(primary.id, chosenNumber)
            }
        )
    }
}

@Composable
private fun VerifyCallerPill(
    backdrop: Backdrop?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .backdropLiquidGlass(
                backdrop = backdrop,
                shape = CircleShape,
                isDark = true,
                interactive = true,
                pressedScale = 1.04f,
                hasLens = false,
                scrimAlpha = 0.22f,
                fallbackTint = Color.White.copy(alpha = 0.14f)
            )
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.VerifiedUser,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = stringResource(R.string.verify_caller_action),
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerifyCallerSheet(
    onDismissRequest: () -> Unit,
    onSelectNumber: (String) -> Unit
) {
    val context = LocalContext.current
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
    }

    var contacts by remember { mutableStateOf<List<ContactItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var contactForNumberSelection by remember { mutableStateOf<ContactItem?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val noNumberMessage = stringResource(R.string.verify_caller_no_number)

    LaunchedEffect(hasContactsPermission) {
        if (hasContactsPermission) {
            isLoading = true
            withContext(Dispatchers.IO) {
                val loaded = PhoneRepository(context.applicationContext).loadContacts()
                withContext(Dispatchers.Main) {
                    contacts = loaded
                    isLoading = false
                }
            }
        }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            val q = searchQuery.trim().lowercase()
            contacts.filter { contact ->
                contact.name.lowercase().contains(q) ||
                    contact.numbers.any { it.number.contains(q) }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header: Title + Cancel Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.verify_caller_sheet_title),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = stringResource(R.string.verify_caller_cancel),
                        color = TelviqueColors.AppleBlueDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Body: one short line
            Text(
                text = stringResource(R.string.verify_caller_sheet_body),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasContactsPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C2E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.verify_caller_permission_title),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.verify_caller_permission_message),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                        color = TelviqueColors.AppleBlueDark
                    ) {
                        Text(
                            text = stringResource(R.string.verify_caller_grant_permission),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }
                }
            } else {
                // Search bar
                AppleSearchField(
                    query = searchQuery,
                    placeholder = stringResource(R.string.verify_caller_search_placeholder),
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.verify_caller_loading),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                } else if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.verify_caller_empty_search),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredContacts, key = { it.id }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        when {
                                            contact.numbers.isEmpty() -> {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        noNumberMessage,
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }
                                            contact.numbers.size == 1 -> {
                                                onSelectNumber(contact.numbers[0].number)
                                            }
                                            else -> {
                                                contactForNumberSelection = contact
                                            }
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatar(
                                    name = contact.name,
                                    photoUri = contact.photoUri,
                                    size = 44.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (contact.primaryNumber.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = PhoneFormatUtils.formatDisplayNumber(contact.primaryNumber),
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (contactForNumberSelection != null) {
            val contact = contactForNumberSelection!!
            AlertDialog(
                onDismissRequest = { contactForNumberSelection = null },
                containerColor = Color(0xFF2C2C2E),
                title = {
                    Text(
                        text = stringResource(R.string.verify_caller_choose_number_title),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        contact.numbers.forEach { entry ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val num = entry.number
                                        contactForNumberSelection = null
                                        onSelectNumber(num)
                                    },
                                color = Color(0xFF3A3A3C)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        if (entry.typeLabel.isNotBlank()) {
                                            Text(
                                                text = entry.typeLabel,
                                                color = TelviqueColors.AppleBlueDark,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = PhoneFormatUtils.formatDisplayNumber(entry.number),
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = ApplePhoneIcons.PhoneFill,
                                        contentDescription = null,
                                        tint = TelviqueColors.CallGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { contactForNumberSelection = null }) {
                        Text(
                            text = stringResource(R.string.verify_caller_cancel),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            )
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
private fun NoPhotoInitialsAvatar(
    name: String,
    modifier: Modifier = Modifier
) {
    val words = name.trim().split(' ').filter { it.isNotBlank() }
    val initials = when {
        words.size >= 2 -> "${words.first().first().uppercaseChar()}${words.last().first().uppercaseChar()}"
        words.size == 1 && words.first().first().isLetter() -> words.first().first().uppercaseChar().toString()
        else -> null
    }

    Box(
        modifier = modifier
            .size(104.dp)
            .shadow(elevation = 12.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(Color(0xFF14171F), CircleShape)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (initials != null) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(52.dp)
            )
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
    onOpenContacts: () -> Unit,
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
        // Row 1: Mute | Keypad | Speaker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LiquidCallControlTile(
                icon = if (audioState.isMuted) ApplePhoneIcons.MicSlashFill else ApplePhoneIcons.MicFill,
                label = if (audioState.isMuted) "muted" else "mute",
                active = audioState.isMuted,
                backdrop = backdrop,
                onClick = onToggleMute
            )
            LiquidCallControlTile(
                icon = ApplePhoneIcons.KeypadDots,
                label = "keypad",
                active = showKeypad,
                backdrop = backdrop,
                onClick = onToggleKeypad
            )
            LiquidCallControlTile(
                icon = audioIcon,
                label = audioLabel,
                active = audioActive,
                backdrop = backdrop,
                onClick = onAudioClick
            )
        }

        // Row 2: Add call | Hold | Contacts
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
                icon = if (primary.phase == CallPhase.HOLDING) ApplePhoneIcons.PlayFill else ApplePhoneIcons.PauseFill,
                label = if (primary.phase == CallPhase.HOLDING) "resume" else "hold",
                active = primary.phase == CallPhase.HOLDING,
                enabled = primary.canHold,
                backdrop = backdrop,
                onClick = onToggleHold
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
                LiquidCallControlTile(
                    icon = ApplePhoneIcons.PersonCropCircleFill,
                    label = "contacts",
                    active = false,
                    backdrop = backdrop,
                    onClick = onOpenContacts
                )
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
        modifier = Modifier
            .width(84.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                stateDescription = if (active) "Active" else if (!enabled) "Disabled" else "Inactive"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val specularBorder = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.08f),
                Color.Transparent
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )

        val circleModifier = when {
            !enabled -> Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
            active -> Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.White, CircleShape)
                .liquidBubbleHighlight(isDark = false, cornerFraction = 0.5f)
            else -> Modifier
                .size(70.dp)
                .backdropLiquidGlass(
                    backdrop = backdrop,
                    shape = CircleShape,
                    isDark = true,
                    interactive = true,
                    pressedScale = 1.06f,
                    hasLens = false,
                    blurScale = 0.75f,
                    scrimAlpha = 0.22f,
                    fallbackTint = Color.White.copy(alpha = 0.18f)
                )
                .border(1.dp, specularBorder, CircleShape)
                .clip(CircleShape)
        }

        Box(
            modifier = circleModifier.clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !enabled -> Color.White.copy(alpha = 0.30f)
                    active -> Color(0xFF101217)
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
            color = if (enabled) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.35f),
            textAlign = TextAlign.Center
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
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            role = Role.Button
            contentDescription = label
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(containerColor, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                .liquidBubbleHighlight(isDark = false, cornerFraction = 0.5f) { 0.75f }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.92f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppleInCallDtmfKeypad(
    digitsEntered: String,
    isMuted: Boolean,
    isSpeaker: Boolean,
    backdrop: Backdrop?,
    onDigitClick: (Char) -> Unit,
    onDigitReleased: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onHide: () -> Unit
) {
    val rows = listOf(
        listOf('1' to "", '2' to "ABC", '3' to "DEF"),
        listOf('4' to "GHI", '5' to "JKL", '6' to "MNO"),
        listOf('7' to "PQRS", '8' to "TUV", '9' to "WXYZ"),
        listOf('*' to "", '0' to "+", '#' to "")
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
                row.forEach { (digit, letters) ->
                    Column(
                        modifier = Modifier
                            .size(72.dp)
                            .backdropLiquidGlass(
                                backdrop = backdrop,
                                shape = CircleShape,
                                isDark = true,
                                interactive = true,
                                pressedScale = 1.06f,
                                hasLens = false,
                                blurScale = 0.7f,
                                scrimAlpha = 0.22f,
                                fallbackTint = Color.White.copy(alpha = 0.16f)
                            )
                            .clip(CircleShape)
                            .clickable {
                                onDigitClick(digit)
                                onDigitReleased()
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
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
                        if (letters.isNotEmpty()) {
                            Text(
                                text = letters,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.16f), CircleShape)
                    .clickable(onClick = onToggleMute)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isMuted) ApplePhoneIcons.MicSlashFill else ApplePhoneIcons.MicFill,
                    contentDescription = "Mute",
                    tint = if (isMuted) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Mute",
                    color = if (isMuted) Color.Black else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(onClick = onHide) {
                Text("Hide", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSpeaker) Color.White else Color.White.copy(alpha = 0.16f), CircleShape)
                    .clickable(onClick = onToggleSpeaker)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ApplePhoneIcons.SpeakerWave3Fill,
                    contentDescription = "Speaker",
                    tint = if (isSpeaker) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Speaker",
                    color = if (isSpeaker) Color.Black else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SimChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun IncomingCallGlassPillButton(
    icon: ImageVector,
    label: String,
    backdrop: Backdrop?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "pill_button_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 1.0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "pill_button_alpha"
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .backdropLiquidGlass(
                backdrop = backdrop,
                shape = CircleShape,
                isDark = true,
                interactive = false,
                hasLens = false,
                scrimAlpha = 0.12f,
                fallbackTint = Color.White.copy(alpha = 0.12f)
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppleSlideToAnswerPill(
    backdrop: Backdrop?,
    callPhase: CallPhase,
    callId: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isAnswerTriggered by remember(callId) { mutableStateOf(false) }
    var isDeclineTriggered by remember(callId) { mutableStateOf(false) }
    val offsetX = remember(callId) { Animatable(0f) }
    val holdProgress = remember(callId) { Animatable(0f) }

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    fun performHoldTick() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (vibrator?.hasVibrator() == true) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15L)
            }
        }
    }

    fun performDeclineHaptic() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (vibrator?.hasVibrator() == true) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40L)
            }
        }
    }

    LaunchedEffect(callPhase, callId) {
        if (callPhase != CallPhase.RINGING) {
            isAnswerTriggered = false
            isDeclineTriggered = false
            offsetX.snapTo(0f)
            holdProgress.snapTo(0f)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Slide to Answer Track (height ~66dp, full pill glass)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .height(66.dp)
        ) {
            val thumbSize = 58.dp
            val trackPadding = 4.dp
            val maxDragPx = with(density) {
                (maxWidth - (trackPadding * 2) - thumbSize).toPx().coerceAtLeast(1f)
            }
            val completionThreshold = maxDragPx * SlideToAnswerUtils.COMPLETION_THRESHOLD_RATIO

            // Infinite shimmer transition across the track (period ~2.4s)
            val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
            val shimmerProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmer_progress"
            )

            val trackWidthPx = with(density) { maxWidth.toPx() }
            val shimmerBrush = remember(shimmerProgress, trackWidthPx) {
                val startX = -trackWidthPx + (shimmerProgress * trackWidthPx * 2.2f)
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.32f),
                        Color.White.copy(alpha = 0.88f),
                        Color.White.copy(alpha = 0.32f)
                    ),
                    start = Offset(startX, 0f),
                    end = Offset(startX + (trackWidthPx * 0.40f).coerceAtLeast(100f), 0f)
                )
            }

            fun triggerAnswer() {
                if (!isAnswerTriggered && !isDeclineTriggered && callPhase == CallPhase.RINGING) {
                    isAnswerTriggered = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        holdProgress.snapTo(0f)
                        offsetX.animateTo(
                            targetValue = maxDragPx,
                            animationSpec = spring(
                                dampingRatio = 0.70f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        if (callPhase == CallPhase.RINGING) {
                            onAnswer()
                        }
                    }
                }
            }

            fun triggerDecline() {
                if (!isAnswerTriggered && !isDeclineTriggered && callPhase == CallPhase.RINGING) {
                    isDeclineTriggered = true
                    performDeclineHaptic()
                    scope.launch {
                        holdProgress.snapTo(1f)
                        if (callPhase == CallPhase.RINGING) {
                            onDecline()
                        }
                    }
                }
            }

            val accessibilityDescription = stringResource(R.string.slide_to_answer_talkback_description)
            val answerLabel = stringResource(R.string.action_answer_call)
            val declineLabel = stringResource(R.string.action_decline_call)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .backdropLiquidGlass(
                        backdrop = backdrop,
                        shape = CircleShape,
                        isDark = true,
                        interactive = false,
                        hasLens = false,
                        scrimAlpha = 0.35f,
                        fallbackTint = Color.Black.copy(alpha = 0.35f)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .padding(trackPadding)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = accessibilityDescription
                        stateDescription = when {
                            isAnswerTriggered -> "Answering"
                            isDeclineTriggered -> "Declining"
                            holdProgress.value > 0f -> "Holding to decline"
                            else -> "Ready to slide"
                        }
                        onClick(label = answerLabel) {
                            triggerAnswer()
                            true
                        }
                        onLongClick(label = declineLabel) {
                            triggerDecline()
                            true
                        }
                        customActions = listOf(
                            CustomAccessibilityAction(label = declineLabel) {
                                triggerDecline()
                                true
                            }
                        )
                    }
                    .pointerInput(maxDragPx, isAnswerTriggered, isDeclineTriggered, callPhase) {
                        if (isAnswerTriggered || isDeclineTriggered || callPhase != CallPhase.RINGING) return@pointerInput

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (isAnswerTriggered || isDeclineTriggered || callPhase != CallPhase.RINGING) return@awaitEachGesture

                            val thumbSizePx = with(density) { thumbSize.toPx() }
                            val touchSlop = viewConfiguration.touchSlop
                            val isTouchOnThumb = down.position.x in 0f..(offsetX.value + thumbSizePx + touchSlop)
                            if (!isTouchOnThumb) return@awaitEachGesture

                            val startPosition = down.position
                            val initialKnobOffset = offsetX.value
                            var isSliding = false
                            var holdCompleted = false

                            val holdJob = scope.launch {
                                performHoldTick()
                                val remainingProgress = (1f - holdProgress.value).coerceAtLeast(0f)
                                val duration = (SlideToAnswerUtils.DEFAULT_HOLD_DURATION_MS * remainingProgress).toLong().coerceAtLeast(1L)
                                holdProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = duration.toInt(),
                                        easing = LinearEasing
                                    )
                                )
                                if (!isAnswerTriggered && !isDeclineTriggered && callPhase == CallPhase.RINGING && !isSliding) {
                                    holdCompleted = true
                                    triggerDecline()
                                }
                            }

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                    if (!change.pressed) {
                                        change.consume()
                                        break
                                    }

                                    val deltaX = change.position.x - startPosition.x
                                    val deltaY = change.position.y - startPosition.y
                                    val distance = kotlin.math.hypot(deltaX, deltaY)

                                    if (!isSliding) {
                                        if (SlideToAnswerUtils.shouldCancelHoldForMovement(distance, touchSlop)) {
                                            isSliding = true
                                            holdJob.cancel()
                                            scope.launch {
                                                holdProgress.animateTo(0f, tween(150))
                                            }
                                        }
                                    }

                                    if (isSliding) {
                                        change.consume()
                                        if (!isAnswerTriggered && !isDeclineTriggered && callPhase == CallPhase.RINGING) {
                                            val newOffset = (initialKnobOffset + deltaX).coerceIn(0f, maxDragPx)
                                            scope.launch {
                                                offsetX.snapTo(newOffset)
                                            }
                                            if (newOffset >= maxDragPx) {
                                                triggerAnswer()
                                                break
                                            }
                                        }
                                    }
                                }
                            } finally {
                                holdJob.cancel()

                                if (!holdCompleted && !isDeclineTriggered) {
                                    scope.launch {
                                        holdProgress.animateTo(0f, tween(180))
                                    }
                                }

                                if (isSliding && !isAnswerTriggered && !isDeclineTriggered) {
                                    if (offsetX.value >= completionThreshold) {
                                        triggerAnswer()
                                    } else {
                                        scope.launch {
                                            offsetX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.70f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                val textAlpha = SlideToAnswerUtils.calculateTextAlpha(offsetX.value, maxDragPx)

                Text(
                    text = "slide to answer  ›",
                    style = TextStyle(
                        brush = shimmerBrush,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .alpha(textAlpha)
                )

                val thumbScale = if (holdProgress.value > 0f) {
                    1f - (0.05f * holdProgress.value)
                } else {
                    1f
                }

                // Draggable Green Circular Knob with Long-Press-to-Decline & Inner Highlight
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        .size(thumbSize)
                        .graphicsLayer {
                            scaleX = thumbScale
                            scaleY = thumbScale
                        }
                        .drawBehind {
                            // Soft glow shadow behind knob (green alpha ~0.35, blur ~16dp)
                            val glowRadius = (size.minDimension / 2f) + 14.dp.toPx()
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        TelviqueColors.CallGreen.copy(alpha = 0.35f),
                                        TelviqueColors.CallGreen.copy(alpha = 0.12f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = glowRadius
                                ),
                                radius = glowRadius,
                                center = center
                            )
                        }
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF38E562),
                                    TelviqueColors.CallGreen,
                                    Color(0xFF20B33A)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .drawWithContent {
                            drawContent()
                            // Subtle inner highlight (top-light gradient)
                            drawCircle(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.40f),
                                        Color.White.copy(alpha = 0.10f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.55f
                                ),
                                radius = size.minDimension / 2f,
                                center = center
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ApplePhoneIcons.PhoneFill,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )

                    // Subtle red progress animation around the slider handle while holding
                    if (holdProgress.value > 0f) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.5.dp)
                        ) {
                            val strokeWidth = 3.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeftOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)
                            val arcSize = Size(diameter, diameter)

                            // Subtle guide track behind the progress arc
                            drawArc(
                                color = TelviqueColors.CallRed.copy(alpha = 0.22f * holdProgress.value),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeftOffset,
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )

                            // Active subtle red progress arc filling clockwise from 12 o'clock
                            val sweep = SlideToAnswerUtils.calculateSweepAngle(holdProgress.value)
                            drawArc(
                                color = TelviqueColors.CallRed,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeftOffset,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
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
