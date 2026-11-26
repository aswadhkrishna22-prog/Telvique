package com.telvique.phone.ui.dialer

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.SimAccountInfo
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.components.openAddContact
import com.telvique.phone.ui.glass.LiquidCircularGlassButton
import com.telvique.phone.ui.glass.clearLiquidGlassPill
import com.telvique.phone.ui.glass.liquidBubbleHighlight
import com.telvique.phone.ui.icons.ApplePhoneIcons
import com.telvique.phone.ui.theme.LocalApplePalette

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
    t9Matches: List<ContactItem>,
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
    onCallClick: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val palette = LocalApplePalette.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 65) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { toneGenerator?.release() } }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var showSimMenu by remember { mutableStateOf(false) }
        val activeSimLabel = remember(simAccounts, preferredSimId) {
            simAccounts.firstOrNull { it.id == preferredSimId }?.label ?: "Ask SIM"
        }

        // Top row: Dual-SIM switcher pill (when > 1 SIM) + Settings button in Unified mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
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
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // T9 Contact suggestions + Centered Apple phone number display
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (t9Matches.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(t9Matches.take(3), key = { it.id }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clearLiquidGlassPill(
                                        shape = RoundedCornerShape(16.dp),
                                        hazeState = null,
                                        isDark = palette.isDark,
                                        blurRadius = 8.dp,
                                        shadowElevation = 2.dp,
                                    )
                                    .clickable {
                                        if (contact.primaryNumber.isNotBlank()) {
                                            onCallClick(contact.primaryNumber, false)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatar(
                                    name = contact.name,
                                    photoUri = contact.photoUri,
                                    size = 34.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name,
                                        color = palette.label,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = contact.primaryNumber,
                                        color = palette.secondaryLabel,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = ApplePhoneIcons.PhoneFill,
                                    contentDescription = "Call ${contact.name}",
                                    tint = palette.callGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Centered Apple Phone number display (auto-scaling tabular numerals)
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
                        fontFeatureSettings = "tnum",
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier.height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (input.isNotBlank()) {
                        Text(
                            text = "Add Number",
                            color = palette.accentBlue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { openAddContact(context, input) }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Apple 3x4 Circular Liquid Glass Keypad
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                        AppleLiquidDialKey(
                            key = key,
                            isDark = palette.isDark,
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

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom iOS row: [Paste / Spacer]  [Apple Green Call Button]  [Backspace]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomInsetPadding + 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left slot: quick Paste button when input is empty
            Box(
                modifier = Modifier.size(76.dp),
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

            // Center slot: Iconic Apple Green Call Circle
            Box(
                modifier = Modifier
                    .size(76.dp)
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
                    modifier = Modifier.size(32.dp)
                )
            }

            // Right slot: Apple delete.left.fill backspace glyph
            Box(
                modifier = Modifier.size(76.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppleLiquidDialKey(
    key: DialKey,
    isDark: Boolean,
    onTap: () -> Unit,
    onLongTap: (() -> Unit)? = null
) {
    val palette = LocalApplePalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.07f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 650f),
        label = "dialKeyScale"
    )
    val keyFill by animateColorAsState(
        targetValue = when {
            isPressed && isDark -> Color(0xFF4C4C52)
            isPressed -> Color(0xFFD1D1D6)
            isDark -> Color(0xFF262629)
            else -> palette.keypadKeyFill
        },
        label = "dialKeyFill"
    )

    Column(
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        keyFill.copy(alpha = 0.95f),
                        keyFill.copy(alpha = 0.85f),
                    )
                ),
                shape = CircleShape
            )
            .liquidBubbleHighlight(isDark = isDark, cornerFraction = 0.5f) { if (isPressed) 1f else 0.72f }
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
                fontFeatureSettings = "tnum",
            )
        )
        if (key.letters.isNotEmpty()) {
            Text(
                text = key.letters,
                fontSize = if (key.digit == "0") 14.sp else 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = if (key.digit == "0") 0.sp else 1.5.sp,
                color = palette.label.copy(alpha = 0.72f),
            )
        } else {
            Spacer(modifier = Modifier.height(11.dp))
        }
    }
}
