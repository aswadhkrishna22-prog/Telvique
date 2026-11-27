package com.telvique.phone.ui.dialer

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telvique.phone.R
import com.telvique.phone.data.CallLogItem
import com.telvique.phone.data.CallLogType
import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.DialerContactMatch
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.SimAccountInfo
import com.telvique.phone.data.T9Matcher
import com.telvique.phone.telecom.CallManager
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.components.openAddContact
import com.telvique.phone.ui.contacts.AppleContactDetailSheet
import com.telvique.phone.ui.glass.LiquidCircularGlassButton
import com.telvique.phone.ui.glass.clearLiquidGlassPill
import com.telvique.phone.ui.glass.isBackdropGlassCapable
import com.telvique.phone.ui.glass.liquidBubbleHighlight
import com.telvique.phone.ui.icons.ApplePhoneIcons
import com.telvique.phone.ui.theme.LocalApplePalette
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource

private data class DialKey(
    val digit: String,
    val letters: String = "",
    val longPressDigit: String? = null
)

private val DialPadRows = listOf(
    listOf(DialKey("1", ""), DialKey("2", "ABC"), DialKey("3", "DEF")),
    listOf(DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO")),
    listOf(DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ")),
    listOf(DialKey("*", ""), DialKey("0", "+", "+"), DialKey("#", ""))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(
    input: String,
    t9Matches: List<DialerContactMatch>,
    recentCalls: List<CallLogItem> = emptyList(),
    blockedNumbers: Set<String> = emptySet(),
    lastDialedNumber: String?,
    dialpadTonesEnabled: Boolean,
    dialpadVibrateEnabled: Boolean,
    speedDial: Map<String, String> = emptyMap(),
    voicemailNumber: String = "*86",
    simAccounts: List<SimAccountInfo> = emptyList(),
    preferredSimId: String? = null,
    showTopSettingsButton: Boolean,
    bottomInsetPadding: Dp,
    onOpenSettings: () -> Unit,
    onSelectPreferredSim: (String?) -> Unit = {},
    onDigitPress: (String) -> Unit,
    onSetNumber: (String) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onCallClick: (String, Boolean) -> Unit,
    onToggleFavorite: (ContactItem) -> Unit = {},
    onToggleBlock: (String, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val palette = LocalApplePalette.current
    val density = LocalDensity.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()
    var keypadPanelHeightPx by remember { mutableIntStateOf(0) }
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    var selectedContact by remember { mutableStateOf<ContactItem?>(null) }

    val keypadPanelHeightDp = with(density) { keypadPanelHeightPx.toDp() }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 65) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { toneGenerator?.release() } }
    }

    LaunchedEffect(input.isEmpty()) {
        listState.scrollToItem(0)
    }

    fun playKeyFeedback(digit: String) {
        if (dialpadVibrateEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (dialpadTonesEnabled && toneGenerator != null) {
            val tone = when (digit) {
                "0" -> ToneGenerator.TONE_DTMF_0
                "1" -> ToneGenerator.TONE_DTMF_1
                "2" -> ToneGenerator.TONE_DTMF_2
                "3" -> ToneGenerator.TONE_DTMF_3
                "4" -> ToneGenerator.TONE_DTMF_4
                "5" -> ToneGenerator.TONE_DTMF_5
                "6" -> ToneGenerator.TONE_DTMF_6
                "7" -> ToneGenerator.TONE_DTMF_7
                "8" -> ToneGenerator.TONE_DTMF_8
                "9" -> ToneGenerator.TONE_DTMF_9
                "*" -> ToneGenerator.TONE_DTMF_S
                "#" -> ToneGenerator.TONE_DTMF_P
                else -> -1
            }
            if (tone >= 0) runCatching { toneGenerator.startTone(tone, 110) }
        }
    }

    val isBlurEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isBackdropGlassCapable()
    val glassPanelTint = if (palette.isDark) {
        Color(0xFF141418).copy(alpha = 0.62f)
    } else {
        Color.White.copy(alpha = 0.68f)
    }
    val fallbackPanelColor = palette.background.copy(alpha = 0.94f)

    val keypadHazeStyle = if (isBlurEnabled) {
        remember(palette.background, glassPanelTint) {
            val tintEffect = HazeColorEffect.tint(glassPanelTint)
            HazeBlurStyle {
                blurEnabled(true)
                backgroundColor(palette.background)
                colorEffects(listOf(tintEffect))
                fallbackColorEffect(tintEffect)
                blurRadius(22.dp)
                noiseFactor(0.01f)
                mask(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.13f to Color.Black,
                        1.00f to Color.Black
                    )
                )
            }
        }
    } else {
        null
    }

    val statusTopDp = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val showTopRow = simAccounts.size > 1 || showTopSettingsButton

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Layer 0: Scrollable Recents / T9 Match list extending underneath the glass keypad
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .hazeSource(state = hazeState),
            contentPadding = PaddingValues(
                top = if (showTopRow) maxOf(topBarHeightDp, statusTopDp + 52.dp) else statusTopDp + 8.dp,
                bottom = keypadPanelHeightDp + 16.dp
            )
        ) {
            if (input.isEmpty()) {
                if (recentCalls.isEmpty()) {
                    item(key = "dial_recent_empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recent calls",
                                color = palette.secondaryLabel,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    items(
                        items = recentCalls,
                        key = { "dial_recent_${it.id}" }
                    ) { log ->
                        DialerRecentCallRow(
                            item = log,
                            showSimLabel = simAccounts.size > 1,
                            onClick = {
                                if (log.number.isNotBlank()) {
                                    onCallClick(log.number, false)
                                }
                            }
                        )
                    }
                }
            } else {
                items(
                    items = t9Matches,
                    key = { "dial_match_${it.contact.id}_${it.matchedNumber}" }
                ) { match ->
                    DialerContactMatchRow(
                        match = match,
                        query = input,
                        onRowClick = {
                            if (match.matchedNumber.isNotBlank()) {
                                onCallClick(match.matchedNumber, false)
                            }
                        },
                        onOpenContact = { selectedContact = match.contact }
                    )
                }
            }
        }

        // Top overlay: Dual-SIM switcher pill (when > 1 SIM) + Settings button with soft fade
        if (showTopRow) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { topBarHeightPx = it.height }
                    .background(
                        Brush.verticalGradient(
                            0.00f to palette.background,
                            0.75f to palette.background.copy(alpha = 0.90f),
                            1.00f to Color.Transparent
                        )
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                var showSimMenu by remember { mutableStateOf(false) }
                val activeSimLabel = remember(simAccounts, preferredSimId) {
                    simAccounts.firstOrNull { it.id == preferredSimId }?.label ?: "Ask SIM"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (simAccounts.size > 1) Arrangement.SpaceBetween else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (simAccounts.size > 1) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clearLiquidGlassPill(
                                        shape = CircleShape,
                                        isDark = palette.isDark
                                    )
                                    .clip(CircleShape)
                                    .clickable { showSimMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SimCard,
                                    contentDescription = "Active SIM",
                                    tint = palette.accentBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeSimLabel,
                                    color = palette.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = palette.secondaryLabel,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSimMenu,
                                onDismissRequest = { showSimMenu = false },
                                containerColor = palette.cardSurface
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Ask Every Time",
                                            color = palette.label,
                                            fontWeight = if (preferredSimId == null) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSelectPreferredSim(null)
                                        showSimMenu = false
                                    }
                                )
                                HorizontalDivider(color = palette.separator.copy(alpha = 0.45f))
                                simAccounts.forEach { sim ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = sim.label,
                                                color = palette.label,
                                                fontWeight = if (preferredSimId == sim.id) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onSelectPreferredSim(sim.id)
                                            showSimMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (showTopSettingsButton) {
                        LiquidCircularGlassButton(
                            icon = ApplePhoneIcons.GearShapeFill,
                            contentDescription = "Settings",
                            onClick = onOpenSettings,
                            size = 40.dp,
                            iconSize = 18.dp,
                            tint = palette.label,
                            isDark = palette.isDark,
                        )
                    }
                }
            }
        }

        // Layer 1: Translucent Glass Keypad Panel anchored at bottom with soft top fade
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { keypadPanelHeightPx = it.height }
        ) {
            // Backdrop blur layer (Android 12+ RenderEffect via Haze; solid translucent fallback < 12)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (isBlurEnabled && keypadHazeStyle != null) {
                            Modifier
                                .hazeBlur(
                                    input = HazeInput.Sources(hazeState),
                                    style = keypadHazeStyle,
                                    performanceMode = HazePerformanceMode.Fixed(0.35f)
                                )
                                .background(
                                    Brush.verticalGradient(
                                        0.00f to Color.Transparent,
                                        0.14f to glassPanelTint,
                                        1.00f to glassPanelTint
                                    )
                                )
                        } else {
                            Modifier.background(
                                Brush.verticalGradient(
                                    0.00f to Color.Transparent,
                                    0.14f to fallbackPanelColor,
                                    1.00f to fallbackPanelColor
                                )
                            )
                        }
                    )
            )

            // Keypad foreground controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, start = 20.dp, end = 20.dp, bottom = bottomInsetPadding + 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Typed number display shown large between the list and the keypad
                AnimatedVisibility(
                    visible = input.isNotEmpty(),
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val displayFormatted = remember(input) {
                            PhoneFormatUtils.formatDisplayNumber(input)
                        }
                        val numberFontSize = when {
                            displayFormatted.length > 16 -> 24.sp
                            displayFormatted.length > 12 -> 30.sp
                            else -> 36.sp
                        }
                        Text(
                            text = displayFormatted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        val pasted = clipboard.getText()?.text?.toString().orEmpty()
                                        val cleaned = PhoneFormatUtils.normalizeDialInput(pasted)
                                        if (cleaned.isNotBlank()) onSetNumber(cleaned)
                                    }
                                )
                                .padding(horizontal = 8.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                fontSize = numberFontSize,
                                fontWeight = FontWeight.Normal,
                                color = palette.label,
                                fontFeatureSettings = "tnum"
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add Number",
                            color = palette.accentBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { openAddContact(context, input) }
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                        )
                    }
                }

                // 3x4 Borderless Translucent Keypad (no circles or borders)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DialPadRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                val longTapAction: (() -> Unit)? = when {
                                    key.longPressDigit != null -> {
                                        {
                                            playKeyFeedback(key.digit)
                                            onDigitPress(key.longPressDigit)
                                        }
                                    }
                                    key.digit == "1" && input.isEmpty() -> {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (voicemailNumber.isNotBlank()) {
                                                onCallClick(voicemailNumber, false)
                                            }
                                        }
                                    }
                                    key.digit in "2".."9" && input.isEmpty() && !speedDial[key.digit].isNullOrBlank() -> {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            speedDial[key.digit]?.let { target -> onCallClick(target, false) }
                                        }
                                    }
                                    else -> null
                                }
                                GlassDialKey(
                                    key = key,
                                    onTap = {
                                        playKeyFeedback(key.digit)
                                        onDigitPress(key.digit)
                                    },
                                    onLongTap = longTapAction
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom row: [Paste / Spacer]  [Centered Green Call Button]  [Backspace]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = input.isEmpty(),
                            enter = fadeIn() + scaleIn(initialScale = 0.85f),
                            exit = fadeOut() + scaleOut(targetScale = 0.85f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val pasted = clipboard.getText()?.text?.toString().orEmpty()
                                        val cleaned = PhoneFormatUtils.normalizeDialInput(pasted)
                                        if (cleaned.isNotBlank()) onSetNumber(cleaned)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "Paste number",
                                    tint = palette.secondaryLabel,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Center slot: Iconic Green Call Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(palette.callGreen, CircleShape)
                            .liquidBubbleHighlight(isDark = false, cornerFraction = 0.5f) { 0.75f }
                            .combinedClickable(
                                onClick = {
                                    if (input.isNotBlank()) {
                                        onCallClick(input, false)
                                    } else if (!lastDialedNumber.isNullOrBlank()) {
                                        onSetNumber(lastDialedNumber)
                                    }
                                },
                                onLongClick = {
                                    if (input.isNotBlank()) {
                                        onCallClick(input, true)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ApplePhoneIcons.PhoneFill,
                            contentDescription = "Call",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Right slot: Backspace (tap deletes one, long-press clears all)
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = input.isNotEmpty(),
                            enter = fadeIn() + scaleIn(initialScale = 0.85f),
                            exit = fadeOut() + scaleOut(targetScale = 0.85f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .combinedClickable(
                                        onClick = {
                                            if (dialpadVibrateEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            onBackspace()
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onClearAll()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ApplePhoneIcons.DeleteLeftFill,
                                    contentDescription = "Backspace",
                                    tint = palette.secondaryLabel,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val activeContact = selectedContact
    if (activeContact != null) {
        AppleContactDetailSheet(
            contact = activeContact,
            blockedNumbers = blockedNumbers,
            onDismiss = { selectedContact = null },
            onCallNumber = { number ->
                selectedContact = null
                onCallClick(number, false)
            },
            onToggleFavorite = onToggleFavorite,
            onToggleBlock = onToggleBlock
        )
    }
}

@Composable
private fun DialerRecentCallRow(
    item: CallLogItem,
    showSimLabel: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalApplePalette.current
    val context = LocalContext.current
    val isMissed = item.type == CallLogType.MISSED || item.type == CallLogType.REJECTED
    val isBlocked = item.type == CallLogType.BLOCKED
    val hasSavedContact = !item.cachedName.isNullOrBlank()
    val cnapName = if (!hasSavedContact) {
        item.cnapName ?: CallManager.lookupCachedCnapName(item.number, context)
    } else null

    val formattedNumber = remember(item.number) {
        if (item.number.isNotBlank()) PhoneFormatUtils.formatDisplayNumber(item.number) else "Private Number"
    }
    val primaryTitle = when {
        hasSavedContact -> item.displayTitle
        cnapName != null -> cnapName
        else -> formattedNumber
    }

    val dirIcon: ImageVector = when (item.type) {
        CallLogType.OUTGOING -> ApplePhoneIcons.PhoneArrowUpRight
        CallLogType.BLOCKED -> Icons.Rounded.Block
        else -> ApplePhoneIcons.PhoneArrowDownLeft
    }

    val isAlert = isMissed || isBlocked
    val dirTint = if (isAlert) palette.callRed else palette.secondaryLabel

    val subText = buildString {
        if (hasSavedContact || cnapName != null) {
            append(formattedNumber)
        } else {
            append(
                when (item.type) {
                    CallLogType.INCOMING -> "Incoming"
                    CallLogType.OUTGOING -> "Outgoing"
                    CallLogType.MISSED -> "Missed"
                    CallLogType.REJECTED -> "Declined"
                    CallLogType.BLOCKED -> "Blocked"
                    CallLogType.VOICEMAIL -> "Voicemail"
                    CallLogType.UNKNOWN -> "Call"
                }
            )
        }
        if (showSimLabel && !item.simLabel.isNullOrBlank()) {
            append(" • ")
            append(item.simLabel)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                name = primaryTitle,
                photoUri = item.photoUri,
                size = 36.dp,
                photoUriLarge = item.photoFullUri
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primaryTitle,
                    color = if (isAlert) palette.callRed else palette.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = dirIcon,
                        contentDescription = null,
                        tint = dirTint,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = subText,
                        color = palette.secondaryLabel,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = PhoneFormatUtils.formatAppleRecentTime(item.timestamp),
                color = palette.secondaryLabel,
                fontSize = 13.sp,
                maxLines = 1
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp),
            thickness = 0.5.dp,
            color = palette.separator.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun DialerContactMatchRow(
    match: DialerContactMatch,
    query: String,
    onRowClick: () -> Unit,
    onOpenContact: () -> Unit
) {
    val palette = LocalApplePalette.current
    val contact = match.contact
    val displayNumber = remember(match.matchedNumber) {
        PhoneFormatUtils.formatDisplayNumber(match.matchedNumber)
    }
    val matchedRanges = remember(displayNumber, query) {
        T9Matcher.findMatchedDigitCharRanges(displayNumber, query)
    }

    val highlightedNumber = remember(displayNumber, matchedRanges, palette.accentBlue, palette.secondaryLabel) {
        buildAnnotatedString {
            if (matchedRanges.isEmpty()) {
                withStyle(SpanStyle(color = palette.secondaryLabel)) {
                    append(displayNumber)
                }
            } else {
                var cursor = 0
                for (range in matchedRanges) {
                    if (range.first > cursor) {
                        withStyle(SpanStyle(color = palette.secondaryLabel)) {
                            append(displayNumber.substring(cursor, range.first))
                        }
                    }
                    withStyle(SpanStyle(color = palette.accentBlue, fontWeight = FontWeight.SemiBold)) {
                        append(displayNumber.substring(range.first, range.last + 1))
                    }
                    cursor = range.last + 1
                }
                if (cursor < displayNumber.length) {
                    withStyle(SpanStyle(color = palette.secondaryLabel)) {
                        append(displayNumber.substring(cursor))
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                name = contact.name,
                photoUri = contact.photoUri,
                size = 36.dp,
                photoUriLarge = contact.photoFullUri
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    color = palette.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = highlightedNumber,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onOpenContact,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = ApplePhoneIcons.ChevronRight,
                    contentDescription = "Open ${contact.name}",
                    tint = palette.secondaryLabel,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp),
            thickness = 0.5.dp,
            color = palette.separator.copy(alpha = 0.45f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassDialKey(
    key: DialKey,
    onTap: () -> Unit,
    onLongTap: (() -> Unit)? = null
) {
    val palette = LocalApplePalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "dialKeyScale"
    )
    val pressAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.11f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "dialKeyHighlight"
    )

    Column(
        modifier = Modifier
            .size(width = 84.dp, height = 62.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(palette.label.copy(alpha = pressAlpha))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
                onLongClick = onLongTap
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = key.digit,
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                color = palette.label,
                fontFeatureSettings = "tnum"
            )
        )
        Box(
            modifier = Modifier.height(14.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                key.digit == "1" -> {
                    Icon(
                        imageVector = ApplePhoneIcons.Voicemail,
                        contentDescription = "Voicemail",
                        tint = palette.label.copy(alpha = 0.58f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                key.letters.isNotEmpty() -> {
                    Text(
                        text = key.letters,
                        fontSize = if (key.digit == "0") 13.sp else 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = if (key.digit == "0") 0.sp else 1.4.sp,
                        color = palette.label.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}
