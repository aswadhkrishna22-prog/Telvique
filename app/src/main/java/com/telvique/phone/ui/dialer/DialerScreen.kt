@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package com.telvique.phone.ui.dialer

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
import com.telvique.phone.data.GroupedCallLogItem
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.SimAccountInfo
import com.telvique.phone.data.T9Matcher
import com.telvique.phone.data.groupConsecutiveCallLogs
import com.telvique.phone.telecom.CallManager
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.components.copyNumberToClipboard
import com.telvique.phone.ui.components.openAddContact
import com.telvique.phone.ui.components.openSmsApp
import com.telvique.phone.ui.contacts.AppleContactDetailSheet
import com.telvique.phone.ui.glass.LiquidCircularGlassButton
import com.telvique.phone.ui.glass.clearLiquidGlassPill
import com.telvique.phone.ui.glass.isBackdropGlassCapable
import com.telvique.phone.ui.glass.liquidBubbleHighlight
import com.telvique.phone.ui.icons.ApplePhoneIcons
import com.telvique.phone.ui.theme.LocalApplePalette
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource

// ── KEYPAD BACKDROP BLUR & LIQUID GLASS TUNING CONSTANTS ───────────────────────
const val KEYPAD_RAMP_DISTANCE_DP = 160f // Lengthened smooth ramp distance (160dp ending at key row 1 top edge)
const val KEYPAD_BACKDROP_DARK_TINT = 0.28f // Max dark tint over backdrop (~28% black, 25-30% tunable)
const val KEYPAD_BACKDROP_NOISE_FACTOR = 0.06f // Low noise (0.05-0.10) for glass-grain feel
const val KEYPAD_BACKDROP_INPUT_SCALE = 0.5f // Downscale input content to half-resolution for 60fps performance
const val KEYPAD_FALLBACK_DARK_TINT = 0.80f // Fallback tint opacity for API < 31 (~80% opacity)

// Stacked blur band radii: 0 (sharp list), 3dp, 8dp, 16dp, 32dp, 56dp (colour blobs)
const val BLUR_RADIUS_BAND_0_DP = 0f // Sharp list (base LazyColumn)
const val BLUR_RADIUS_BAND_1_DP = 3f // Band 1: slightly soft (edges lose sharpness)
const val BLUR_RADIUS_BAND_2_DP = 8f // Band 2: soft blur (characters merge)
const val BLUR_RADIUS_BAND_3_DP = 16f // Band 3: medium blur (words unreadable)
const val BLUR_RADIUS_BAND_4_DP = 32f // Band 4: strong blur (shapes merge)
const val BLUR_RADIUS_BAND_5_DP = 56f // Band 5: full blur (colour blobs)

// Smooth ease-in intensity curve across 160dp ramp (0.00 at ramp top, rising to 1.00 at key row 1)
// Note: Maximum rate of change is strictly <= 9% within any 12dp (< 10% constraint)
const val KEYPAD_RAMP_POS_0 = 0.00f // 0dp: 0% effect (160dp above key row 1)
const val KEYPAD_RAMP_INTENSITY_0 = 0.00f

const val KEYPAD_RAMP_POS_1 = 0.10f // 16dp: 2% effect
const val KEYPAD_RAMP_INTENSITY_1 = 0.02f

const val KEYPAD_RAMP_POS_2 = 0.25f // 40dp: 8% effect
const val KEYPAD_RAMP_INTENSITY_2 = 0.08f

const val KEYPAD_RAMP_POS_3 = 0.45f // 72dp: 20% effect
const val KEYPAD_RAMP_INTENSITY_3 = 0.20f

const val KEYPAD_RAMP_POS_4 = 0.70f // 112dp: 45% effect
const val KEYPAD_RAMP_INTENSITY_4 = 0.45f

const val KEYPAD_RAMP_POS_5 = 0.88f // 140dp: 75% effect
const val KEYPAD_RAMP_INTENSITY_5 = 0.75f

const val KEYPAD_RAMP_POS_6 = 1.00f // 160dp (at key row 1): 100% full strength
const val KEYPAD_RAMP_INTENSITY_6 = 1.00f

// Row alpha dimming in ramp zone: follows the exact same smooth ease-in curve
const val KEYPAD_ROW_ALPHA_START = 1.00f // 100% opacity outside ramp
const val KEYPAD_ROW_ALPHA_AT_ROW1 = 0.55f // 55% opacity at key row 1

// Helper function to evaluate the smooth ease-in ramp intensity curve
fun computeRampIntensity(fraction: Float): Float {
    val f = fraction.coerceIn(0f, 1f)
    return when {
        f <= KEYPAD_RAMP_POS_1 -> KEYPAD_RAMP_INTENSITY_1 * (f / KEYPAD_RAMP_POS_1)
        f <= KEYPAD_RAMP_POS_2 -> KEYPAD_RAMP_INTENSITY_1 + (KEYPAD_RAMP_INTENSITY_2 - KEYPAD_RAMP_INTENSITY_1) * ((f - KEYPAD_RAMP_POS_1) / (KEYPAD_RAMP_POS_2 - KEYPAD_RAMP_POS_1))
        f <= KEYPAD_RAMP_POS_3 -> KEYPAD_RAMP_INTENSITY_2 + (KEYPAD_RAMP_INTENSITY_3 - KEYPAD_RAMP_INTENSITY_2) * ((f - KEYPAD_RAMP_POS_2) / (KEYPAD_RAMP_POS_3 - KEYPAD_RAMP_POS_2))
        f <= KEYPAD_RAMP_POS_4 -> KEYPAD_RAMP_INTENSITY_3 + (KEYPAD_RAMP_INTENSITY_4 - KEYPAD_RAMP_INTENSITY_3) * ((f - KEYPAD_RAMP_POS_3) / (KEYPAD_RAMP_POS_4 - KEYPAD_RAMP_POS_3))
        f <= KEYPAD_RAMP_POS_5 -> KEYPAD_RAMP_INTENSITY_4 + (KEYPAD_RAMP_INTENSITY_5 - KEYPAD_RAMP_INTENSITY_4) * ((f - KEYPAD_RAMP_POS_4) / (KEYPAD_RAMP_POS_5 - KEYPAD_RAMP_POS_4))
        else -> KEYPAD_RAMP_INTENSITY_5 + (KEYPAD_RAMP_INTENSITY_6 - KEYPAD_RAMP_INTENSITY_5) * ((f - KEYPAD_RAMP_POS_5) / (KEYPAD_RAMP_POS_6 - KEYPAD_RAMP_POS_5))
    }
}

// Flat liquid-glass key constants: 6-9% alpha flat fill, no bulky center sphere, low refraction & dispersion
const val KEYPAD_KEY_FILL_ALPHA_TOP = 0.08f // Translucent white fill top (~8% in dark mode, range 6-9%)
const val KEYPAD_KEY_FILL_ALPHA_BOTTOM = 0.06f // Translucent white fill bottom (~6% in dark mode, range 6-9%)
const val KEYPAD_KEY_REFRACTION_BLUR_DP = 6f // Light blur radius for key refraction layer (~6dp)
const val KEYPAD_DISPERSION_STRENGTH = 0.012f // Low strength chromatic dispersion at rim only
const val KEYPAD_REFRACTION_STRENGTH = 0.18f // Low strength rim-only lens refraction
const val KEYPAD_SPECULAR_STRENGTH = 0.18f // Dim top-left rim highlight intensity (no center spot)
const val KEYPAD_FADE_TOP_DP = 6f // Short fade-in gradient at the top of the glass region

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
    uniform float uDarkTint;
    uniform float uPressedIndex;
    uniform float uPressedProgress;

    half4 main(float2 p) {
        float2 clampedP = clamp(p, float2(0.0), uSize - float2(1.0));
        half4 bgSample = content.eval(clampedP);

        // 1. Find closest key among the 12 keys using circle SDF
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

        // Outside keys: return content (progressive blur + matching gradient tint scrim) untouched
        if (keyAlpha <= 0.001) {
            return bgSample;
        }

        // 2. Inside key: Liquid Glass Lens Refraction (rim-only bending) & Chromatic Dispersion
        float r = closestDist;
        float R = uKeyRadius;
        float rNorm = clamp(r / R, 0.0, 1.0);
        float2 dir = (r > 0.001) ? ((p - closestCenter) / r) : float2(0.0, 0.0);

        // Rim-only bending of the blurred backdrop (no center distortion/magnification)
        float rimFactor = smoothstep(0.55, 1.0, rNorm);
        float rimBend = uRefraction * 0.35 * rimFactor;
        float totalOffset = -rimBend * R;
        float2 pRefract = p + dir * totalOffset;

        // Dispersion strictly at the rim at low strength
        float disp = uDispersion * pow(rNorm, 4.0) * R;
        float2 coordR = clamp(pRefract - dir * disp, float2(0.0), uSize - float2(1.0));
        float2 coordG = clamp(pRefract, float2(0.0), uSize - float2(1.0));
        float2 coordB = clamp(pRefract + dir * disp, float2(0.0), uSize - float2(1.0));

        half rChannel = content.eval(coordR).r;
        half gChannel = content.eval(coordG).g;
        half bChannel = content.eval(coordB).b;
        half aChannel = content.eval(coordG).a;
        half3 keyRefract = half3(rChannel, gChannel, bChannel);

        // 3. Thin soft rim sheen along top-left (dim gradient, no center spot)
        float angleToLight = dot(dir, float2(-0.7071, -0.7071));
        float rimSheen = max(0.0, angleToLight) * pow(rNorm, 3.0) * 0.25;
        float totalSpec = rimSheen * uSpecular;

        // 4. Flat base glass tint (translucent white 6-9%)
        half3 baseKeyTint = half3(1.0, 1.0, 1.0);
        half baseKeyAlpha = (uDarkTheme > 0.5) ? 0.07 : 0.50;

        if (float(closestIdx) == uPressedIndex) {
            baseKeyAlpha += half(0.08 * uPressedProgress);
            totalSpec += 0.08 * uPressedProgress;
        }

        half3 keyFinalRgb = mix(keyRefract, baseKeyTint, baseKeyAlpha) + half3(totalSpec);
        half4 keyColor = half4(keyFinalRgb, aChannel);

        // 5. Anti-aliased blend of key with blurred backdrop
        return mix(bgSample, keyColor, half(keyAlpha));
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
    val hazeState = remember { HazeState() }

    var keypadPanelHeightPx by remember { mutableIntStateOf(0) }
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    var keyGridTopInPanelPx by remember { mutableFloatStateOf(with(density) { 16.dp.toPx() }) }
    var panelTopInRootPx by remember { mutableFloatStateOf(0f) }
    var panelWidthPx by remember { mutableFloatStateOf(0f) }

    var pressedKeyIndex by remember { mutableIntStateOf(-1) }
    var pressedKeyProgress by remember { mutableFloatStateOf(0f) }

    var selectedContact by remember { mutableStateOf<ContactItem?>(null) }
    val groupedRecentCalls = remember(recentCalls) { groupConsecutiveCallLogs(recentCalls) }
    var expandedGroupId by remember { mutableStateOf<String?>(null) }
    var selectedGroupDetail by remember { mutableStateOf<GroupedCallLogItem?>(null) }

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

    val estimatedKeypadHeightDp = if (keypadPanelHeightDp > 150.dp) keypadPanelHeightDp else 480.dp
    val listBottomContentPadding = estimatedKeypadHeightDp + bottomInsetPadding + 48.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Scrollable Recents / T9 Match list registered as Haze blur source
        // Extends underneath the keypad when input is empty;
        // Ends above the number display when input is not empty.
        val rampDistancePx = with(density) { KEYPAD_RAMP_DISTANCE_DP.dp.toPx() }
        val keyRow1InRootPx = if (panelTopInRootPx > 0f) panelTopInRootPx + keyGridTopInPanelPx else 0f
        val rampTopInRootPx = if (keyRow1InRootPx > 0f) (keyRow1InRootPx - rampDistancePx).coerceAtLeast(0f) else 0f

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = listBottomPaddingDp)
                .hazeSource(hazeState)
                .background(palette.background),
            contentPadding = PaddingValues(
                top = topBarReservedHeightDp + 10.dp,
                bottom = if (input.isNotEmpty()) 24.dp else listBottomContentPadding
            )
        ) {
            if (input.isEmpty()) {
                if (groupedRecentCalls.isEmpty()) {
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
                        items = groupedRecentCalls,
                        key = { "dial_grouped_${it.primary.id}_${it.count}" }
                    ) { group ->
                        DialerGroupedRecentCallRow(
                            group = group,
                            showSimLabel = simAccounts.size > 1,
                            isExpanded = expandedGroupId == "${group.primary.id}",
                            onToggleExpand = {
                                expandedGroupId = if (expandedGroupId == "${group.primary.id}") null else "${group.primary.id}"
                            },
                            onOpenDetails = {
                                selectedGroupDetail = group
                            },
                            onCallClick = { number ->
                                if (number.isNotBlank()) {
                                    onCallClick(number, false)
                                }
                            },
                            rampTopInRootPx = rampTopInRootPx,
                            keyRow1InRootPx = keyRow1InRootPx,
                            rampDistancePx = rampDistancePx,
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
                        onOpenContact = { selectedContact = match.contact },
                        rampTopInRootPx = rampTopInRootPx,
                        keyRow1InRootPx = keyRow1InRootPx,
                        rampDistancePx = rampDistancePx,
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
            val totalTopOverlayHeight = topBarReservedHeightDp + 24.dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(totalTopOverlayHeight)
                    .background(
                        Brush.verticalGradient(
                            0.00f to palette.background,
                            0.70f to palette.background,
                            1.00f to Color.Transparent
                        )
                    )
            ) {
                var showSimMenu by remember { mutableStateOf(false) }
                val activeSimLabel = remember(simAccounts, preferredSimId) {
                    simAccounts.firstOrNull { it.id == preferredSimId }?.label ?: "Ask SIM"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(52.dp)
                        .padding(horizontal = 20.dp),
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

        // Layer 1: Backdrop Glass Layer with stacked blur bands (3dp, 8dp, 16dp, 32dp, 56dp)
        // Extends 160dp above key row 1 and down to the bottom of the screen
        val glassHeightPx = if (keypadPanelHeightPx > 0) {
            (keypadPanelHeightPx - keyGridTopInPanelPx + rampDistancePx).coerceAtLeast(0f)
        } else 0f

        if (glassHeightPx > 0f) {
            val isAndroid12OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val rampFraction = (rampDistancePx / glassHeightPx).coerceIn(0f, 1f)

            // Overlapping smooth gradient masks for the 5 blur bands:
            // Band 1: slightly soft (3dp blur)
            val band1Style = remember(rampFraction) {
                HazeBlurStyle {
                    blurEnabled(true)
                    backgroundColor(Color.Transparent)
                    blurRadius(BLUR_RADIUS_BAND_1_DP.dp)
                    noiseFactor(0f)
                    mask(
                        Brush.verticalGradient(
                            0.00f * rampFraction to Color.Transparent,
                            0.08f * rampFraction to Color.Black.copy(alpha = 0.50f),
                            0.16f * rampFraction to Color.Black,
                            0.24f * rampFraction to Color.Black,
                            0.32f * rampFraction to Color.Black.copy(alpha = 0.40f),
                            0.40f * rampFraction to Color.Transparent,
                            1.00f to Color.Transparent
                        )
                    )
                }
            }

            // Band 2: soft blur (8dp blur)
            val band2Style = remember(rampFraction) {
                HazeBlurStyle {
                    blurEnabled(true)
                    backgroundColor(Color.Transparent)
                    blurRadius(BLUR_RADIUS_BAND_2_DP.dp)
                    noiseFactor(0f)
                    mask(
                        Brush.verticalGradient(
                            0.00f * rampFraction to Color.Transparent,
                            0.20f * rampFraction to Color.Transparent,
                            0.30f * rampFraction to Color.Black.copy(alpha = 0.60f),
                            0.38f * rampFraction to Color.Black,
                            0.46f * rampFraction to Color.Black,
                            0.56f * rampFraction to Color.Black.copy(alpha = 0.40f),
                            0.64f * rampFraction to Color.Transparent,
                            1.00f to Color.Transparent
                        )
                    )
                }
            }

            // Band 3: medium blur (16dp blur)
            val band3Style = remember(rampFraction) {
                HazeBlurStyle {
                    blurEnabled(true)
                    backgroundColor(Color.Transparent)
                    blurRadius(BLUR_RADIUS_BAND_3_DP.dp)
                    noiseFactor(0f)
                    mask(
                        Brush.verticalGradient(
                            0.00f * rampFraction to Color.Transparent,
                            0.42f * rampFraction to Color.Transparent,
                            0.52f * rampFraction to Color.Black.copy(alpha = 0.60f),
                            0.60f * rampFraction to Color.Black,
                            0.68f * rampFraction to Color.Black,
                            0.78f * rampFraction to Color.Black.copy(alpha = 0.40f),
                            0.84f * rampFraction to Color.Transparent,
                            1.00f to Color.Transparent
                        )
                    )
                }
            }

            // Band 4: strong blur (32dp blur)
            val band4Style = remember(rampFraction) {
                HazeBlurStyle {
                    blurEnabled(true)
                    backgroundColor(Color.Transparent)
                    blurRadius(BLUR_RADIUS_BAND_4_DP.dp)
                    noiseFactor(0f)
                    mask(
                        Brush.verticalGradient(
                            0.00f * rampFraction to Color.Transparent,
                            0.62f * rampFraction to Color.Transparent,
                            0.72f * rampFraction to Color.Black.copy(alpha = 0.60f),
                            0.80f * rampFraction to Color.Black,
                            0.88f * rampFraction to Color.Black,
                            0.94f * rampFraction to Color.Black.copy(alpha = 0.40f),
                            0.98f * rampFraction to Color.Transparent,
                            1.00f to Color.Transparent
                        )
                    )
                }
            }

            // Band 5: full blur (56dp blur, colour blobs)
            val band5Style = remember(rampFraction) {
                HazeBlurStyle {
                    blurEnabled(true)
                    backgroundColor(Color.Transparent)
                    blurRadius(BLUR_RADIUS_BAND_5_DP.dp)
                    noiseFactor(KEYPAD_BACKDROP_NOISE_FACTOR)
                    mask(
                        Brush.verticalGradient(
                            0.00f * rampFraction to Color.Transparent,
                            0.78f * rampFraction to Color.Transparent,
                            0.88f * rampFraction to Color.Black.copy(alpha = 0.45f),
                            0.96f * rampFraction to Color.Black.copy(alpha = 0.85f),
                            1.00f * rampFraction to Color.Black,
                            1.00f to Color.Black
                        )
                    )
                }
            }

            // Smooth ease-in dark tint scrim
            val tintScrimBrush = remember(rampFraction) {
                Brush.verticalGradient(
                    0.00f * rampFraction to Color.Black.copy(alpha = KEYPAD_BACKDROP_DARK_TINT * KEYPAD_RAMP_INTENSITY_0),
                    KEYPAD_RAMP_POS_1 * rampFraction to Color.Black.copy(alpha = KEYPAD_BACKDROP_DARK_TINT * KEYPAD_RAMP_INTENSITY_1),
                    KEYPAD_RAMP_POS_2 * rampFraction to Color.Black.copy(alpha = KEYPAD_BACKDROP_DARK_TINT * KEYPAD_RAMP_INTENSITY_2),
                    KEYPAD_RAMP_POS_3 * rampFraction to Color.Black.copy(alpha = KEYPAD_BACKDROP_DARK_TINT * KEYPAD_RAMP_INTENSITY_3),
                    KEYPAD_RAMP_POS_4 * rampFraction to Color.Black.copy(alpha = KEYPAD_BACKDROP_DARK_TINT * KEYPAD_RAMP_INTENSITY_4),
                    KEYPAD_RAMP_POS_5 * rampFraction to Color.Black.copy(alpha = KEYPAD_BACKDROP_DARK_TINT * KEYPAD_RAMP_INTENSITY_5),
                    KEYPAD_RAMP_POS_6 * rampFraction to Color.Black.copy(alpha = KEYPAD_BACKDROP_DARK_TINT * KEYPAD_RAMP_INTENSITY_6),
                    1.00f to Color.Black.copy(alpha = KEYPAD_BACKDROP_DARK_TINT)
                )
            }

            // Fallback tint gradient for API < 31
            val fallbackTintBrush = remember(rampFraction) {
                Brush.verticalGradient(
                    0.00f * rampFraction to Color.Black.copy(alpha = KEYPAD_FALLBACK_DARK_TINT * KEYPAD_RAMP_INTENSITY_0),
                    KEYPAD_RAMP_POS_1 * rampFraction to Color.Black.copy(alpha = KEYPAD_FALLBACK_DARK_TINT * KEYPAD_RAMP_INTENSITY_1),
                    KEYPAD_RAMP_POS_2 * rampFraction to Color.Black.copy(alpha = KEYPAD_FALLBACK_DARK_TINT * KEYPAD_RAMP_INTENSITY_2),
                    KEYPAD_RAMP_POS_3 * rampFraction to Color.Black.copy(alpha = KEYPAD_FALLBACK_DARK_TINT * KEYPAD_RAMP_INTENSITY_3),
                    KEYPAD_RAMP_POS_4 * rampFraction to Color.Black.copy(alpha = KEYPAD_FALLBACK_DARK_TINT * KEYPAD_RAMP_INTENSITY_4),
                    KEYPAD_RAMP_POS_5 * rampFraction to Color.Black.copy(alpha = KEYPAD_FALLBACK_DARK_TINT * KEYPAD_RAMP_INTENSITY_5),
                    KEYPAD_RAMP_POS_6 * rampFraction to Color.Black.copy(alpha = KEYPAD_FALLBACK_DARK_TINT * KEYPAD_RAMP_INTENSITY_6),
                    1.00f to Color.Black.copy(alpha = KEYPAD_FALLBACK_DARK_TINT)
                )
            }

            val keyRadiusPx = with(density) { 38.dp.toPx() }
            val keyCentersArray = remember(panelWidthPx, density, rampDistancePx) { FloatArray(24) }

            // Compute exact center coordinates of all 12 keys relative to the glass container
            if (panelWidthPx > 0f) {
                val gridSidePaddingPx = with(density) { 20.dp.toPx() }
                val rowWidthPx = panelWidthPx - gridSidePaddingPx * 2f
                val keyDiameterPx = keyRadiusPx * 2f
                val availableSpacePx = rowWidthPx - (keyDiameterPx * 3f)
                val gapPx = availableSpacePx / 4f

                val cx0 = gridSidePaddingPx + gapPx + keyRadiusPx
                val cx1 = panelWidthPx / 2f
                val cx2 = panelWidthPx - cx0

                val cy0 = rampDistancePx + keyRadiusPx
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
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(with(density) { glassHeightPx.toDp() })
                    .then(
                        if (isAndroid12OrAbove) {
                            Modifier
                        } else {
                            Modifier.background(fallbackTintBrush)
                        }
                    )
                    .then(
                        if (isLiquidShaderCapable) {
                            Modifier.graphicsLayer {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && size.width > 0f && size.height > 0f) {
                                    val shader = KeypadLiquidGlassShader.getOrCreateShader()
                                    if (shader != null) {
                                        shader.setFloatUniform("uSize", size.width, size.height)
                                        shader.setFloatUniform("uKeyRadius", keyRadiusPx)
                                        shader.setFloatUniform("uKeyCenters", keyCentersArray)
                                        shader.setFloatUniform("uRefraction", KEYPAD_REFRACTION_STRENGTH)
                                        shader.setFloatUniform("uDispersion", KEYPAD_DISPERSION_STRENGTH)
                                        shader.setFloatUniform("uSpecular", KEYPAD_SPECULAR_STRENGTH)
                                        shader.setFloatUniform("uFadeTopPx", 0f)
                                        shader.setFloatUniform("uDarkTheme", if (palette.isDark) 1f else 0f)
                                        shader.setFloatUniform("uDarkTint", 0f)
                                        shader.setFloatUniform("uPressedIndex", pressedKeyIndex.toFloat())
                                        shader.setFloatUniform("uPressedProgress", pressedKeyProgress)

                                        renderEffect = RenderEffect.createRuntimeShaderEffect(
                                            shader,
                                            "content"
                                        ).asComposeRenderEffect()
                                    } else {
                                        renderEffect = null
                                    }
                                } else {
                                    renderEffect = null
                                }
                            }
                        } else Modifier
                    )
            ) {
                if (isAndroid12OrAbove) {
                    // Band 1: slightly soft (3dp blur)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = band1Style,
                                performanceMode = HazePerformanceMode.Fixed(KEYPAD_BACKDROP_INPUT_SCALE)
                            )
                    )
                    // Band 2: soft blur (8dp blur)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = band2Style,
                                performanceMode = HazePerformanceMode.Fixed(KEYPAD_BACKDROP_INPUT_SCALE)
                            )
                    )
                    // Band 3: medium blur (16dp blur)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = band3Style,
                                performanceMode = HazePerformanceMode.Fixed(KEYPAD_BACKDROP_INPUT_SCALE)
                            )
                    )
                    // Band 4: strong blur (32dp blur)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = band4Style,
                                performanceMode = HazePerformanceMode.Fixed(KEYPAD_BACKDROP_INPUT_SCALE)
                            )
                    )
                    // Band 5: full blur (56dp blur, colour blobs)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = band5Style,
                                performanceMode = HazePerformanceMode.Fixed(KEYPAD_BACKDROP_INPUT_SCALE)
                            )
                    )
                    // Smooth ease-in dark tint scrim
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(tintScrimBrush)
                    )
                }
            }
        }

        // Layer 2: Keypad Foreground Panel (keys, numbers, call button)
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
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val pasteScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.92f else 1f,
                                animationSpec = spring(dampingRatio = 0.70f, stiffness = 700f),
                                label = "pasteScale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .graphicsLayer {
                                        scaleX = pasteScale
                                        scaleY = pasteScale
                                    }
                                    .clearLiquidGlassPill(
                                        shape = CircleShape,
                                        hazeState = null,
                                        isDark = palette.isDark,
                                        blurRadius = 12.dp,
                                        tintAlpha = if (palette.isDark) 0.12f else 0.65f,
                                        showBorder = false,
                                        shadowElevation = 2.dp
                                    )
                                    .drawBehind {
                                        val rimAlpha = if (palette.isDark) 0.16f else 0.35f
                                        drawCircle(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = rimAlpha),
                                                    Color.Transparent
                                                ),
                                                start = Offset(0f, 0f),
                                                end = Offset(size.width * 0.75f, size.height * 0.75f)
                                            )
                                        )
                                    }
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        if (dialpadVibrateEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        val pasted = clipboard.getText()?.text?.toString().orEmpty()
                                        val cleaned = PhoneFormatUtils.normalizeDialInput(pasted)
                                        if (cleaned.isNotBlank()) onSetNumber(cleaned)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "Paste number",
                                    tint = palette.label,
                                    modifier = Modifier.size(24.dp)
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

    val activeGroup = selectedGroupDetail
    if (activeGroup != null) {
        DialerCallDetailSheet(
            group = activeGroup,
            blockedNumbers = blockedNumbers,
            onDismiss = { selectedGroupDetail = null },
            onCallNumber = { number ->
                selectedGroupDetail = null
                onCallClick(number, false)
            },
            onToggleBlock = onToggleBlock
        )
    }
}

@Composable
private fun DialerGroupedRecentCallRow(
    group: GroupedCallLogItem,
    showSimLabel: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenDetails: () -> Unit,
    onCallClick: (String) -> Unit,
    rampTopInRootPx: Float = 0f,
    keyRow1InRootPx: Float = 0f,
    rampDistancePx: Float = 0f
) {
    val palette = LocalApplePalette.current
    val context = LocalContext.current
    val item = group.primary
    val isMissed = item.type == CallLogType.MISSED || item.type == CallLogType.REJECTED
    val isBlocked = item.type == CallLogType.BLOCKED
    val hasSavedContact = !item.cachedName.isNullOrBlank()
    val cnapName = if (!hasSavedContact) {
        item.cnapName ?: CallManager.lookupCachedCnapName(item.number, context)
    } else null

    val formattedNumber = remember(item.number) {
        if (item.number.isNotBlank()) PhoneFormatUtils.formatDisplayNumber(item.number) else "Private Number"
    }
    val baseTitle = when {
        hasSavedContact -> item.displayTitle
        cnapName != null -> cnapName
        else -> formattedNumber
    }
    val primaryTitle = if (group.count > 1) "$baseTitle (${group.count})" else baseTitle

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

    var rowTopInRootPx by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    val rowAlpha by remember(rowTopInRootPx, rowHeightPx, rampTopInRootPx, rampDistancePx) {
        derivedStateOf {
            if (rampTopInRootPx <= 0f || rampDistancePx <= 0f || rowHeightPx <= 0f) {
                1.0f
            } else {
                val rowCenterInRoot = rowTopInRootPx + rowHeightPx * 0.5f
                if (rowCenterInRoot <= rampTopInRootPx) {
                    KEYPAD_ROW_ALPHA_START
                } else if (rowCenterInRoot >= keyRow1InRootPx) {
                    KEYPAD_ROW_ALPHA_AT_ROW1
                } else {
                    val fraction = ((rowCenterInRoot - rampTopInRootPx) / rampDistancePx).coerceIn(0f, 1f)
                    val intensity = computeRampIntensity(fraction)
                    KEYPAD_ROW_ALPHA_START + (KEYPAD_ROW_ALPHA_AT_ROW1 - KEYPAD_ROW_ALPHA_START) * intensity
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    rowTopInRootPx = coords.positionInRoot().y
                    rowHeightPx = coords.size.height.toFloat()
                }
            }
            .graphicsLayer {
                alpha = rowAlpha
            }
            .clickable {
                if (group.count > 1) {
                    onToggleExpand()
                } else {
                    onOpenDetails()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                name = item.displayTitle,
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

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = PhoneFormatUtils.formatAppleRecentTime(item.timestamp),
                color = palette.secondaryLabel,
                fontSize = 13.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onOpenDetails,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = ApplePhoneIcons.InfoCircle,
                    contentDescription = "Call details",
                    tint = palette.accentBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Expandable cluster of consecutive calls
        AnimatedVisibility(
            visible = isExpanded && group.count > 1,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 68.dp, end = 20.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.tertiaryFill)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                group.entries.forEachIndexed { idx, entry ->
                    val entryIsMissed = entry.type == CallLogType.MISSED || entry.type == CallLogType.REJECTED
                    val entryDirIcon = when (entry.type) {
                        CallLogType.OUTGOING -> ApplePhoneIcons.PhoneArrowUpRight
                        CallLogType.BLOCKED -> Icons.Rounded.Block
                        else -> ApplePhoneIcons.PhoneArrowDownLeft
                    }
                    val entryDirTint = if (entryIsMissed || entry.type == CallLogType.BLOCKED) palette.callRed else palette.secondaryLabel
                    val entryTypeLabel = when (entry.type) {
                        CallLogType.OUTGOING -> "Outgoing"
                        CallLogType.INCOMING -> "Incoming"
                        CallLogType.MISSED -> "Missed"
                        CallLogType.REJECTED -> "Declined"
                        CallLogType.BLOCKED -> "Blocked"
                        CallLogType.VOICEMAIL -> "Voicemail"
                        CallLogType.UNKNOWN -> "Call"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = entryDirIcon,
                                contentDescription = null,
                                tint = entryDirTint,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = PhoneFormatUtils.formatRelativeTime(entry.timestamp),
                                color = palette.secondaryLabel,
                                fontSize = 12.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entryTypeLabel,
                                color = if (entryIsMissed) palette.callRed else palette.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (entry.durationSeconds > 0L) {
                                Text(
                                    text = " (${PhoneFormatUtils.formatDuration(entry.durationSeconds)})",
                                    color = palette.tertiaryLabel,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onCallClick(entry.number) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = ApplePhoneIcons.PhoneFill,
                                    contentDescription = "Call",
                                    tint = palette.callGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    if (idx < group.entries.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = palette.separator.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp),
            thickness = 0.5.dp,
            color = palette.separator.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun DialerSheetActionCircle(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val palette = LocalApplePalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(palette.tertiaryFill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = palette.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialerCallDetailSheet(
    group: GroupedCallLogItem,
    blockedNumbers: Set<String>,
    onDismiss: () -> Unit,
    onCallNumber: (String) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalApplePalette.current
    val primaryItem = group.primary
    val hasSavedContact = !primaryItem.cachedName.isNullOrBlank()
    val cnapName = if (!hasSavedContact) {
        primaryItem.cnapName ?: CallManager.lookupCachedCnapName(primaryItem.number, context)
    } else null
    val effectiveTitle = if (!hasSavedContact && cnapName != null) cnapName else primaryItem.displayTitle
    val formattedNumber = remember(primaryItem.number) {
        PhoneFormatUtils.formatDisplayNumber(primaryItem.number)
    }
    val isBlocked = blockedNumbers.any {
        it == primaryItem.number || PhoneFormatUtils.numbersMatch(it, primaryItem.number)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.cardSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ContactAvatar(
                name = effectiveTitle,
                photoUri = primaryItem.photoUri,
                photoUriLarge = primaryItem.photoFullUri,
                size = 76.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = effectiveTitle,
                color = palette.label,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (cnapName != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.cnap_operator_records_label),
                    color = palette.secondaryLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            if (primaryItem.number.isNotBlank() && primaryItem.number != effectiveTitle) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedNumber,
                    color = palette.secondaryLabel,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            val subtitle = buildList {
                add(PhoneFormatUtils.formatRelativeTime(primaryItem.timestamp))
                if (primaryItem.durationSeconds > 0L) {
                    add(PhoneFormatUtils.formatDuration(primaryItem.durationSeconds))
                }
                if (!primaryItem.simLabel.isNullOrBlank()) {
                    add(primaryItem.simLabel)
                }
            }.joinToString(" • ")
            Text(
                text = subtitle,
                color = palette.tertiaryLabel,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Apple circular action pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DialerSheetActionCircle(
                    icon = ApplePhoneIcons.MessageFill,
                    label = "message",
                    tint = palette.accentBlue,
                    onClick = {
                        if (primaryItem.number.isNotBlank()) openSmsApp(context, primaryItem.number)
                    }
                )
                DialerSheetActionCircle(
                    icon = ApplePhoneIcons.PhoneFill,
                    label = "call",
                    tint = palette.callGreen,
                    onClick = {
                        onDismiss()
                        if (primaryItem.number.isNotBlank()) onCallNumber(primaryItem.number)
                    }
                )
                DialerSheetActionCircle(
                    icon = Icons.Rounded.ContentCopy,
                    label = "copy",
                    tint = palette.accentBlue,
                    onClick = {
                        if (primaryItem.number.isNotBlank()) copyNumberToClipboard(context, primaryItem.number)
                    }
                )
                if (primaryItem.cachedName.isNullOrBlank() && primaryItem.number.isNotBlank()) {
                    DialerSheetActionCircle(
                        icon = Icons.Rounded.PersonAdd,
                        label = "save",
                        tint = palette.accentBlue,
                        onClick = {
                            onDismiss()
                            openAddContact(context, primaryItem.number)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (group.entries.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.tertiaryFill)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (group.entries.size == 1) "Call Detail" else "Call History (${group.entries.size})",
                        color = palette.secondaryLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    group.entries.take(8).forEachIndexed { idx, entry ->
                        val typeLabel = when (entry.type) {
                            CallLogType.OUTGOING -> "Outgoing Call"
                            CallLogType.INCOMING -> "Incoming Call"
                            CallLogType.MISSED -> "Missed Call"
                            CallLogType.REJECTED -> "Declined Call"
                            CallLogType.BLOCKED -> "Blocked Call"
                            CallLogType.VOICEMAIL -> "Voicemail"
                            CallLogType.UNKNOWN -> "Call"
                        }
                        val entryIsMissed = entry.type == CallLogType.MISSED || entry.type == CallLogType.REJECTED
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = PhoneFormatUtils.formatRelativeTime(entry.timestamp),
                                color = palette.secondaryLabel,
                                fontSize = 13.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = typeLabel,
                                    color = if (entryIsMissed) palette.callRed else palette.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (entry.durationSeconds > 0L) {
                                    Text(
                                        text = " • ${PhoneFormatUtils.formatDuration(entry.durationSeconds)}",
                                        color = palette.tertiaryLabel,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        if (idx < group.entries.take(8).lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = palette.separator.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (primaryItem.number.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.tertiaryFill)
                        .clickable {
                            onToggleBlock(primaryItem.number, !isBlocked)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (isBlocked) "Unblock this Caller" else "Block this Caller",
                        color = if (isBlocked) palette.accentBlue else palette.callRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DialerContactMatchRow(
    match: DialerContactMatch,
    query: String,
    onRowClick: () -> Unit,
    onOpenContact: () -> Unit,
    rampTopInRootPx: Float = 0f,
    keyRow1InRootPx: Float = 0f,
    rampDistancePx: Float = 0f
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

    var rowTopInRootPx by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    val rowAlpha by remember(rowTopInRootPx, rowHeightPx, rampTopInRootPx, rampDistancePx) {
        derivedStateOf {
            if (rampTopInRootPx <= 0f || rampDistancePx <= 0f || rowHeightPx <= 0f) {
                1.0f
            } else {
                val rowCenterInRoot = rowTopInRootPx + rowHeightPx * 0.5f
                if (rowCenterInRoot <= rampTopInRootPx) {
                    KEYPAD_ROW_ALPHA_START
                } else if (rowCenterInRoot >= keyRow1InRootPx) {
                    KEYPAD_ROW_ALPHA_AT_ROW1
                } else {
                    val fraction = ((rowCenterInRoot - rampTopInRootPx) / rampDistancePx).coerceIn(0f, 1f)
                    val intensity = computeRampIntensity(fraction)
                    KEYPAD_ROW_ALPHA_START + (KEYPAD_ROW_ALPHA_AT_ROW1 - KEYPAD_ROW_ALPHA_START) * intensity
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    rowTopInRootPx = coords.positionInRoot().y
                    rowHeightPx = coords.size.height.toFloat()
                }
            }
            .graphicsLayer {
                alpha = rowAlpha
            }
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

    // Flat translucent fill: white 6-9% in dark mode, flat with subtle top-to-bottom vertical gradient
    val restTop = if (palette.isDark) Color.White.copy(alpha = KEYPAD_KEY_FILL_ALPHA_TOP) else Color.White.copy(alpha = 0.65f)
    val restBottom = if (palette.isDark) Color.White.copy(alpha = KEYPAD_KEY_FILL_ALPHA_BOTTOM) else Color.White.copy(alpha = 0.50f)
    val pressedTop = if (palette.isDark) Color.White.copy(alpha = KEYPAD_KEY_FILL_ALPHA_TOP + 0.10f) else Color.White.copy(alpha = 0.80f)
    val pressedBottom = if (palette.isDark) Color.White.copy(alpha = KEYPAD_KEY_FILL_ALPHA_BOTTOM + 0.08f) else Color.White.copy(alpha = 0.65f)

    val topFill = lerp(restTop, pressedTop, pressProgress)
    val bottomFill = lerp(restBottom, pressedBottom, pressProgress)

    Box(
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .drawBehind {
                val w = size.width
                val h = size.height

                // 1. Translucent fill with flat / subtle top-to-bottom change (6-9% white alpha in dark mode)
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(topFill, bottomFill),
                        startY = 0f,
                        endY = h
                    )
                )

                // 2. Thin soft rim highlight along top-left edge as a dim gradient (no bulky center sphere spot, no stroke)
                val rimAlpha = (if (palette.isDark) KEYPAD_SPECULAR_STRENGTH else 0.30f) + 0.06f * pressProgress
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = rimAlpha),
                            Color.White.copy(alpha = rimAlpha * 0.25f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w * 0.70f, h * 0.70f)
                    )
                )
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
                onLongClick = onLongTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
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
}
