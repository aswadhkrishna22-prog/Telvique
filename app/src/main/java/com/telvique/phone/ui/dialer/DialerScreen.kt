package com.telvique.phone.ui.dialer

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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

// ── LIQUID GLASS AGSL SHADER TUNING CONSTANTS ────────────────────────────────
const val KEYPAD_REFRACTION_STRENGTH = 0.28f // Refraction bending & magnification strength
const val KEYPAD_DISPERSION_STRENGTH = 0.045f // Subtle chromatic dispersion (rainbow fringe) at the rim
const val KEYPAD_SPECULAR_STRENGTH = 0.35f // Soft top-left specular highlight intensity
const val KEYPAD_FADE_TOP_DP = 12f // Height of the fade-in gradient at the top of the glass region
const val KEYPAD_GLASS_TOP_OFFSET_DP = 4f // Distance above the first key row where glass starts (4px/dp)

// ── AGSL RUNTIME SHADER (LIQUID GLASS REFRACTION, CHROMATIC DISPERSION & SPECULAR) ──
private const val KEYPAD_LIQUID_GLASS_SHADER_SRC = """
    uniform shader content;
    uniform float2 uSize;
    uniform float uKeyRadius;
    uniform float uKeyCenters[24];
    uniform float uRefraction;
    uniform float uDispersion;
    uniform float uSpecular;
    uniform float uFadeTopPx;
    uniform float uDarkTheme;
    uniform float uPressedIndex;
    uniform float uPressedProgress;

    half4 main(float2 p) {
        // 1. Fade-in gradient at top of glass region (starts 4px above row 1, fades over uFadeTopPx)
        float topFade = clamp(p.y / max(uFadeTopPx, 1.0), 0.0, 1.0);
        topFade = topFade * topFade * (3.0 - 2.0 * topFade);

        if (topFade <= 0.001) {
            return half4(0.0);
        }

        // 2. Find closest key among the 12 keys using circle SDF
        float minSdf = 10000.0;
        int closestIdx = -1;
        float2 closestCenter = float2(0.0);
        float closestDist = 10000.0;

        for (int i = 0; i < 12; ++i) {
            float2 c = float2(uKeyCenters[2 * i], uKeyCenters[2 * i + 1]);
            float2 d = p - c;
            float dist = length(d);
            float sdf = dist - uKeyRadius;
            if (sdf < minSdf) {
                minSdf = sdf;
                closestIdx = i;
                closestCenter = c;
                closestDist = dist;
            }
        }

        float keyAlpha = 1.0 - smoothstep(-1.2, 0.8, minSdf);

        // 3. Background glass scrim outside/between keys
        float2 clampedP = clamp(p, float2(0.0), uSize - float2(1.0));
        half4 bgSample = content.eval(clampedP);
        half3 bgScrim = (uDarkTheme > 0.5) ? half3(0.06, 0.06, 0.08) : half3(1.0, 1.0, 1.0);
        half scrimAlpha = (uDarkTheme > 0.5) ? 0.52 : 0.60;
        half3 bgMixed = mix(bgSample.rgb, bgScrim, scrimAlpha);
        half4 bgColor = half4(bgMixed, bgSample.a * half(topFade));

        if (keyAlpha <= 0.001) {
            return bgColor;
        }

        // 4. Inside key: Liquid Glass Refraction & Chromatic Dispersion
        float r = closestDist;
        float R = uKeyRadius;
        float rNorm = clamp(r / R, 0.0, 1.0);
        float2 dir = (r > 0.001) ? ((p - closestCenter) / r) : float2(0.0, 0.0);

        // Slight magnification in center + strong bending toward edge at rim
        float mag = uRefraction * 0.14 * (1.0 - rNorm);
        float rimBend = uRefraction * 0.42 * (rNorm * rNorm * rNorm);
        float totalOffset = (mag - rimBend) * R;
        float2 pRefract = p + dir * totalOffset;

        // Subtle chromatic dispersion: thin rainbow fringe at rim only
        float disp = uDispersion * pow(rNorm, 3.5) * R;
        float2 coordR = clamp(pRefract - dir * disp, float2(0.0), uSize - float2(1.0));
        float2 coordG = clamp(pRefract, float2(0.0), uSize - float2(1.0));
        float2 coordB = clamp(pRefract + dir * disp, float2(0.0), uSize - float2(1.0));

        half rChannel = content.eval(coordR).r;
        half gChannel = content.eval(coordG).g;
        half bChannel = content.eval(coordB).b;
        half aChannel = content.eval(coordG).a;
        half3 keyRefract = half3(rChannel, gChannel, bChannel);

        // 5. Soft specular highlight on top-left of each key (no stroke/border)
        float2 specCenter = closestCenter + float2(-0.35, -0.35) * R;
        float specDist = length(p - specCenter) / (0.45 * R);
        float specSpot = exp(-specDist * specDist * 2.4);

        float angleToLight = dot(dir, float2(-0.7071, -0.7071));
        float rimSheen = max(0.0, angleToLight) * pow(rNorm, 2.2) * 0.35;
        float totalSpec = (specSpot * 0.75 + rimSheen * 0.25) * uSpecular;

        // 6. Base glass translucent tint & press brightening
        half3 baseKeyTint = (uDarkTheme > 0.5) ? half3(1.0, 1.0, 1.0) : half3(1.0, 1.0, 1.0);
        half baseKeyAlpha = (uDarkTheme > 0.5) ? 0.10 : 0.58;

        if (float(closestIdx) == uPressedIndex) {
            baseKeyAlpha += half(0.12 * uPressedProgress);
            totalSpec += 0.15 * uPressedProgress;
        }

        half3 keyFinalRgb = mix(keyRefract, baseKeyTint, baseKeyAlpha) + half3(totalSpec);
        half4 keyColor = half4(keyFinalRgb, aChannel);

        // 7. Blend key with background glass scrim based on key circle anti-aliased edge
        half4 finalColor = mix(bgColor, keyColor, half(keyAlpha));
        finalColor.a *= half(topFade);
        return finalColor;
    }
"""

private object KeypadLiquidGlassShader {
    private var cachedShader: RuntimeShader? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getOrCreateShader(): RuntimeShader? {
        if (cachedShader == null) {
            try {
                cachedShader = RuntimeShader(KEYPAD_LIQUID_GLASS_SHADER_SRC)
            } catch (_: Throwable) {
                cachedShader = null
            }
        }
        return cachedShader
    }
}

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

    val listState = rememberLazyListState()
    val listGraphicsLayer = rememberGraphicsLayer()

    var keypadPanelHeightPx by remember { mutableIntStateOf(0) }
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    var keyGridTopInPanelPx by remember { mutableFloatStateOf(with(density) { 16.dp.toPx() }) }
    var panelTopInRootPx by remember { mutableFloatStateOf(0f) }
    var panelWidthPx by remember { mutableFloatStateOf(0f) }

    var pressedKeyIndex by remember { mutableIntStateOf(-1) }
    var pressedKeyProgress by remember { mutableFloatStateOf(0f) }

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

    val isLiquidShaderCapable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isBackdropGlassCapable()
    val fallbackPanelColor = palette.background.copy(alpha = 0.94f)

    val statusTopDp = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val showTopRow = simAccounts.size > 1 || showTopSettingsButton
    val topBarReservedHeightDp = if (showTopRow) statusTopDp + 52.dp else statusTopDp + 8.dp

    // When typing, the list's visible area ends above the number display, with a bottom fade
    val listBottomPaddingDp by animateDpAsState(
        targetValue = if (input.isNotEmpty()) keypadPanelHeightDp else 0.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f),
        label = "listBottomPadding"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Layer 0: Scrollable Recents / T9 Match list captured into offscreen graphics layer
        // Extends underneath the keypad when input is empty;
        // Ends above the number display when input is not empty.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = listBottomPaddingDp)
                .background(palette.background)
                .drawWithContent {
                    listGraphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(listGraphicsLayer)
                },
            contentPadding = PaddingValues(
                top = topBarReservedHeightDp + 6.dp,
                bottom = if (input.isNotEmpty()) 20.dp else keypadPanelHeightDp + 20.dp
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

        // Soft bottom fade above the number display when typing
        androidx.compose.animation.AnimatedVisibility(
            visible = input.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = listBottomPaddingDp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            1.0f to palette.background
                        )
                    )
            )
        }

        // Top overlay: Dual-SIM switcher pill (when > 1 SIM) + Settings button with reserved top bar space & soft fade
        if (showTopRow) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.00f to palette.background,
                            0.75f to palette.background,
                            1.00f to Color.Transparent
                        )
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(52.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
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

        // Layer 1: Keypad Panel anchored at bottom with AGSL liquid-glass backdrop
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) {
                        panelTopInRootPx = coords.positionInRoot().y
                    }
                    panelWidthPx = coords.size.width.toFloat()
                    keypadPanelHeightPx = coords.size.height
                }
        ) {
            // Glass region starts 4px above the top edge of key row 1 (key 1, 2, 3), not at row 4
            val glassTopOffsetPx = maxOf(0f, keyGridTopInPanelPx - with(density) { KEYPAD_GLASS_TOP_OFFSET_DP.dp.toPx() })
            val glassHeightPx = (keypadPanelHeightPx - glassTopOffsetPx).coerceAtLeast(0f)
            val glassTopInRootPx = panelTopInRootPx + glassTopOffsetPx

            // ── ONE SINGLE LIQUID GLASS AGSL SHADER OVER THE WHOLE KEYPAD REGION (API 33+) ──
            if (glassHeightPx > 0f) {
                if (isLiquidShaderCapable) {
                    val keyRadiusPx = with(density) { 38.dp.toPx() }
                    val fadeTopPx = with(density) { KEYPAD_FADE_TOP_DP.dp.toPx() }
                    val keyCentersArray = remember(panelWidthPx, density) { FloatArray(24) }

                    // Compute exact center coordinates of all 12 keys relative to the glass region
                    if (panelWidthPx > 0f) {
                        val gridSidePaddingPx = with(density) { 20.dp.toPx() }
                        val rowWidthPx = panelWidthPx - gridSidePaddingPx * 2f
                        val keyDiameterPx = keyRadiusPx * 2f
                        val availableSpacePx = rowWidthPx - (keyDiameterPx * 3f)
                        val gapPx = availableSpacePx / 4f

                        val cx0 = gridSidePaddingPx + gapPx + keyRadiusPx
                        val cx1 = panelWidthPx / 2f
                        val cx2 = panelWidthPx - cx0

                        val cy0 = with(density) { KEYPAD_GLASS_TOP_OFFSET_DP.dp.toPx() } + keyRadiusPx
                        val rowStepPx = with(density) { (76.dp + 8.dp).toPx() }

                        for (r in 0 until 4) {
                            val cy = cy0 + r * rowStepPx
                            keyCentersArray[(r * 3 + 0) * 2] = cx0
                            keyCentersArray[(r * 3 + 0) * 2 + 1] = cy
                            keyCentersArray[(r * 3 + 1) * 2] = cx1
                            keyCentersArray[(r * 3 + 1) * 2 + 1] = cy
                            keyCentersArray[(r * 3 + 2) * 2] = cx2
                            keyCentersArray[(r * 3 + 2) * 2 + 1] = cy
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(density) { glassHeightPx.toDp() })
                            .align(Alignment.BottomCenter)
                            .graphicsLayer {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && size.width > 0f && size.height > 0f) {
                                    val shader = KeypadLiquidGlassShader.getOrCreateShader()
                                    if (shader != null) {
                                        shader.setFloatUniform("uSize", size.width, size.height)
                                        shader.setFloatUniform("uKeyRadius", keyRadiusPx)
                                        shader.setFloatUniform("uKeyCenters", keyCentersArray)
                                        shader.setFloatUniform("uRefraction", KEYPAD_REFRACTION_STRENGTH)
                                        shader.setFloatUniform("uDispersion", KEYPAD_DISPERSION_STRENGTH)
                                        shader.setFloatUniform("uSpecular", KEYPAD_SPECULAR_STRENGTH)
                                        shader.setFloatUniform("uFadeTopPx", fadeTopPx)
                                        shader.setFloatUniform("uDarkTheme", if (palette.isDark) 1f else 0f)
                                        shader.setFloatUniform("uPressedIndex", pressedKeyIndex.toFloat())
                                        shader.setFloatUniform("uPressedProgress", pressedKeyProgress)

                                        renderEffect = RenderEffect
                                            .createRuntimeShaderEffect(shader, "content")
                                            .asComposeRenderEffect()
                                    } else {
                                        renderEffect = null
                                    }
                                } else {
                                    renderEffect = null
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            if (listGraphicsLayer.size != IntSize.Zero) {
                                translate(left = 0f, top = -glassTopInRootPx) {
                                    drawLayer(listGraphicsLayer)
                                }
                            }
                        }
                    }
                } else {
                    // API < 33 fallback: simple translucent dark fill with gentle gradient fade-in
                    val fallbackGradient = remember(fallbackPanelColor) {
                        Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.06f to fallbackPanelColor.copy(alpha = 0.50f),
                            0.16f to fallbackPanelColor,
                            1.00f to fallbackPanelColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(density) { glassHeightPx.toDp() })
                            .align(Alignment.BottomCenter)
                            .background(fallbackGradient)
                    )
                }
            }

            // Keypad foreground controls (keys, digits, and letters sit ABOVE the glass layer)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 16.dp,
                        start = 20.dp,
                        end = 20.dp,
                        bottom = bottomInsetPadding + 10.dp
                    ),
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
                            .padding(bottom = 8.dp),
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
                                .padding(horizontal = 16.dp),
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

                // 3x4 Liquid Glass Round Keys (Circle shape 76.dp, no border)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                keyGridTopInPanelPx = coords.positionInRoot().y - panelTopInRootPx
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DialPadRows.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEachIndexed { colIndex, key ->
                                val keyIndex = rowIndex * 3 + colIndex
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
                                    keyIndex = keyIndex,
                                    isLiquidShaderActive = isLiquidShaderCapable,
                                    onPressedChange = { idx, isPressed, progress ->
                                        if (isPressed) {
                                            pressedKeyIndex = idx
                                            pressedKeyProgress = progress
                                        } else if (pressedKeyIndex == idx) {
                                            pressedKeyIndex = -1
                                            pressedKeyProgress = 0f
                                        }
                                    },
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
                                    .size(56.dp)
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
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Center slot: Iconic Green Call Button (76.dp)
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
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Right slot: Backspace (tap deletes one, long-press clears all)
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
                                    modifier = Modifier.size(28.dp)
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
private fun AppleLiquidDialKey(
    key: DialKey,
    keyIndex: Int,
    isLiquidShaderActive: Boolean,
    onPressedChange: (Int, Boolean, Float) -> Unit,
    onTap: () -> Unit,
    onLongTap: (() -> Unit)? = null
) {
    val palette = LocalApplePalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 800f),
        label = "dialKeyScale"
    )

    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 800f),
        label = "dialKeyPressProgress"
    )

    LaunchedEffect(isPressed, pressProgress) {
        onPressedChange(keyIndex, isPressed, pressProgress)
    }

    // Translucent fill fallback for API < 33 (or when liquid shader is inactive)
    val restTop = if (palette.isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.68f)
    val restBottom = if (palette.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.52f)
    val pressedTop = if (palette.isDark) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.88f)
    val pressedBottom = if (palette.isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.76f)

    val topFill = lerp(restTop, pressedTop, pressProgress)
    val bottomFill = lerp(restBottom, pressedBottom, pressProgress)

    val keyBackgroundModifier = if (isLiquidShaderActive) {
        // Liquid glass shader renders the key body, refraction, dispersion, and specular highlight.
        // On press, a subtle touch tint enhances feedback.
        if (isPressed) {
            Modifier.background(
                color = Color.White.copy(alpha = if (palette.isDark) 0.10f * pressProgress else 0.20f * pressProgress),
                shape = CircleShape
            )
        } else {
            Modifier
        }
    } else {
        Modifier.background(
            brush = Brush.verticalGradient(listOf(topFill, bottomFill)),
            shape = CircleShape
        )
    }

    Column(
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .then(keyBackgroundModifier)
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
                fontSize = if (key.digit == "*" || key.digit == "#") 34.sp else 32.sp,
                fontWeight = FontWeight.Normal,
                color = palette.label,
                fontFeatureSettings = "tnum"
            ),
            lineHeight = if (key.digit == "*" || key.digit == "#") 34.sp else 32.sp
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
                        tint = palette.label.copy(alpha = 0.72f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                key.digit == "0" -> {
                    Text(
                        text = "+",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = palette.label.copy(alpha = 0.75f),
                        lineHeight = 13.sp
                    )
                }
                key.letters.isNotEmpty() -> {
                    Text(
                        text = key.letters,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                        color = palette.label.copy(alpha = 0.75f),
                        maxLines = 1,
                        lineHeight = 10.sp
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}
