package com.telvique.phone.ui.glass

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.telvique.phone.ui.theme.LocalApplePalette
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.roundToInt

val LocalDisableAnimations = staticCompositionLocalOf { false }

@Immutable
data class LiquidPhoneTabItem(
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector = icon,
    val badgeCount: Int = 0,
)

private val LensShape = CircleShape

private object TabBarMotion {
    const val Damping = 0.8f
    const val Stiffness = 400f
    const val TrailingDamping = 0.85f
    const val TrailingStiffness = 450f
    const val LiftDamping = 0.90f
    const val LiftStiffness = 520f
}

/**
 * Floating Apple iOS Liquid Glass tab bar ported from OzonePlayer's LiquidNavigationBar architecture.
 * Uses fletchmckee.liquid real-time refraction + draggable water-drop lens on capable Android 13+ devices,
 * and crisp frosted glass pill with liquidBubbleHighlight on low-RAM and Android 8-12 devices.
 */
@Composable
fun LiquidPhoneTabBar(
    liquidState: LiquidState?,
    items: List<LiquidPhoneTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onScrubSelect: ((Int) -> Unit)? = null,
    isDark: Boolean = isSystemInDarkTheme(),
    maxWidthDp: Dp = 330.dp,
    disableAnimations: Boolean = LocalDisableAnimations.current,
) {
    if (items.isEmpty()) return
    val useRealLiquid = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && liquidState != null
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val palette = LocalApplePalette.current

    val count = items.size
    val selected by rememberUpdatedState(selectedIndex)
    val select by rememberUpdatedState(onSelect)
    val scrub by rememberUpdatedState(onScrubSelect ?: onSelect)
    val start = visualIndex(selectedIndex, count, isRtl)
    val settledPosition = remember(count, isRtl) { Animatable(start) }
    val trailingPosition = remember(count, isRtl) { Animatable(start) }

    var pressed by remember { mutableStateOf(false) }
    var touchPosition by remember { mutableFloatStateOf(Float.NaN) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var captionAtPress by remember { mutableIntStateOf(selectedIndex) }
    var releaseGeneration by remember { mutableIntStateOf(0) }
    var lastHapticIndex by remember { mutableIntStateOf(selectedIndex) }

    val lift by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (disableAnimations) {
            snap()
        } else {
            spring(dampingRatio = TabBarMotion.LiftDamping, stiffness = TabBarMotion.LiftStiffness)
        },
        label = "liquidDropLift",
    )
    val currentPosition = {
        if (touchPosition.isFinite()) touchPosition else settledPosition.value
    }

    LaunchedEffect(selectedIndex, pressed, releaseGeneration, disableAnimations) {
        if (!pressed) {
            val target = visualIndex(selectedIndex, count, isRtl)
            if (touchPosition.isFinite()) {
                settledPosition.snapTo(touchPosition)
                touchPosition = Float.NaN
            }
            if (disableAnimations) {
                settledPosition.snapTo(target)
            } else {
                settledPosition.animateTo(
                    target,
                    spring(dampingRatio = TabBarMotion.Damping, stiffness = TabBarMotion.Stiffness),
                )
            }
        }
    }

    LaunchedEffect(count, isRtl, disableAnimations) {
        snapshotFlow { currentPosition() }.collectLatest { pos ->
            if (disableAnimations) {
                trailingPosition.snapTo(pos)
            } else {
                trailingPosition.animateTo(
                    pos,
                    spring(dampingRatio = TabBarMotion.TrailingDamping, stiffness = TabBarMotion.TrailingStiffness),
                )
            }
        }
    }

    val neutralInk = if (isDark) Color.White.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.86f)
    val activeInk = palette.accentBlue
    val pageColor = palette.background
    val surfaceColor = palette.cardSurface
    val railTint = railTint(isDark)
    val railState = if (useRealLiquid) rememberLiquidState() else null
    val railHeight = 58.dp
    val pressedScale = railHeight.value / (railHeight.value - 8f)
    val captionIndex = if (pressed) captionAtPress else selectedIndex
    val activeIndex = captionIndex.coerceIn(0, count - 1)

    val animFractions = List(count) { i ->
        val target = if (activeIndex == i) 1f else 0f
        if (disableAnimations) {
            target
        } else {
            animateFloatAsState(
                targetValue = target,
                animationSpec = spring(dampingRatio = TabBarMotion.Damping, stiffness = TabBarMotion.Stiffness),
                label = "tab_expand_$i",
            ).value
        }
    }
    val weights = animFractions.map { lerp(1.0f, 1.85f, it) }
    val totalWeight = weights.sum().coerceAtLeast(1f)

    BoxWithConstraints(
        modifier = modifier
            .widthIn(max = maxWidthDp)
            .fillMaxWidth()
            .height(railHeight)
            .railShadow(),
    ) {
        val innerWidth = maxWidth - 8.dp
        val innerWidthPx = with(density) { innerWidth.toPx() }
        val tabWidthsPx = weights.map { innerWidthPx * (it / totalWeight) }
        val tabStartsPx = weights.indices.map { i ->
            var sum = 0f
            for (j in 0 until i) sum += tabWidthsPx[j]
            sum
        }

        val visualPos = currentPosition().coerceIn(0f, (count - 1).toFloat())
        val baseIndex = visualPos.toInt().coerceIn(0, count - 1)
        val nextIndex = (baseIndex + 1).coerceAtMost(count - 1)
        val frac = visualPos - baseIndex

        val lensXPx = lerp(
            tabStartsPx.getOrElse(baseIndex) { 0f },
            tabStartsPx.getOrElse(nextIndex) { 0f },
            frac,
        )
        val lensWidthPx = lerp(
            tabWidthsPx.getOrElse(baseIndex) { innerWidthPx / count },
            tabWidthsPx.getOrElse(nextIndex) { innerWidthPx / count },
            frac,
        )
        val lensWidth = with(density) { lensWidthPx.toDp() }

        if (useRealLiquid && liquidState != null && railState != null) {
            // A -> B -> C acyclic capture pipeline:
            // B has light frost (1.5.dp) so lens C is clearer than the outer rail
            Box(
                Modifier
                    .matchParentSize()
                    .clearAndSetSemantics { }
                    .clip(CircleShape)
                    .captureHeadroom(20.dp)
                    .liquefiable(railState)
                    .background(pageColor)
                    .liquid(liquidState) {
                        shape = RoundedCornerShape(0.dp)
                        frost = 0.dp
                        refraction = 0f
                        curve = 0f
                        edge = 0f
                    }
                    .padding(20.dp),
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(surfaceColor, CircleShape)
                        .navigationRail(liquidState, railTint, blurRadius = 1.5.dp),
                )
                PhoneNavigationGlyphs(
                    items = items,
                    captionIndex = activeIndex,
                    weights = weights,
                    neutral = neutralInk,
                    active = activeInk,
                    captured = true,
                    onSelect = {},
                    disableAnimations = disableAnimations,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                )
            }
            // A has an opaque theme background covering B's original draw, with 12.dp base frost
            Box(
                Modifier
                    .matchParentSize()
                    .background(surfaceColor, CircleShape)
                    .navigationRail(liquidState, railTint, blurRadius = 12.dp),
            )
        } else {
            // Static / non-liquid fallback for unsupported / low-RAM devices
            Box(
                Modifier
                    .matchParentSize()
                    .clearLiquidGlassPill(
                        shape = CircleShape,
                        hazeState = null,
                        isDark = isDark,
                        blurRadius = 12.dp,
                        tintAlpha = if (isDark) 0.52f else 0.78f,
                        showBorder = false,
                        shadowElevation = 0.dp,
                    ),
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .selectableGroup()
                .pointerInput(count, isRtl) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val width = size.width
                        val height = size.height
                        val fingerStart = rawPosition(down.position.x, width.toFloat(), count)
                        val visualStart = currentPosition()
                        val onExistingLens = abs(fingerStart - visualStart) <= 0.5f
                        val grabOffset = if (onExistingLens) visualStart - fingerStart else 0f
                        captionAtPress = selected
                        touchPosition = rubberBand(fingerStart + grabOffset, count)
                        lastHapticIndex = logicalIndex(touchPosition, count, isRtl)
                        touchY = 0f
                        pressed = true
                        var dragged = false
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                val dx = change.position.x - down.position.x
                                val dy = change.position.y - down.position.y
                                if (event.changes.count { it.pressed } > 1 ||
                                    size.width != width || size.height != height || change.isConsumed ||
                                    change.position.y < -24.dp.toPx() || change.position.y > height + 24.dp.toPx()
                                ) {
                                    event.changes.forEach { it.consume() }
                                    break
                                }
                                if (!dragged && abs(dy) > viewConfiguration.touchSlop && abs(dy) >= abs(dx)) {
                                    change.consume()
                                    break
                                }
                                touchPosition = rubberBand(
                                    rawPosition(change.position.x, width.toFloat(), count) + grabOffset,
                                    count,
                                )
                                val currentHapticIndex = logicalIndex(touchPosition, count, isRtl)
                                if (currentHapticIndex != lastHapticIndex) {
                                    lastHapticIndex = currentHapticIndex
                                    captionAtPress = currentHapticIndex
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                touchY = (dy / height.coerceAtLeast(1)).coerceIn(-0.2f, 0.2f)
                                if (!change.pressed) {
                                    if (dragged) {
                                        change.consume()
                                        val destination = logicalIndex(touchPosition, count, isRtl)
                                        if (destination != selected) scrub(destination)
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    break
                                }
                                if (abs(dx) > viewConfiguration.touchSlop && abs(dx) > abs(dy)) dragged = true
                                if (dragged) change.consume()
                            }
                        } finally {
                            pressed = false
                            touchY = 0f
                            releaseGeneration++
                        }
                    }
                },
        ) {
            if (!useRealLiquid) {
                Box(
                    Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset { IntOffset(lensXPx.roundToInt(), 0) }
                        .width(lensWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            val amount = lift.coerceIn(0f, 1f)
                            scaleX = 1f + amount * 0.05f
                            scaleY = scaleX
                            shape = LensShape
                            clip = false
                            shadowElevation = 1.dp.toPx() + 2.dp.toPx() * amount
                            ambientShadowColor = Color.Black.copy(alpha = 0.08f)
                            spotShadowColor = Color.Black.copy(alpha = 0.12f)
                        }
                        .clip(LensShape)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                            LensShape,
                        )
                        .liquidBubbleHighlight(isDark) { 0.35f + 0.45f * lift.coerceIn(0f, 1f) },
                )
            }

            PhoneNavigationGlyphs(
                items = items,
                captionIndex = activeIndex,
                weights = weights,
                neutral = neutralInk,
                active = if (pressed) neutralInk else activeInk,
                captured = false,
                onSelect = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    select(it)
                },
                disableAnimations = disableAnimations,
                modifier = Modifier.fillMaxSize(),
            )

            if (useRealLiquid && railState != null) {
                Box(
                    Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset { IntOffset(lensXPx.roundToInt(), 0) }
                        .width(lensWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            val amount = lift.coerceIn(0f, 1f)
                            val flow = ((currentPosition() - trailingPosition.value) * 2f).coerceIn(-1f, 1f)
                            scaleX = 1f + amount * (pressedScale - 1f + 0.012f * abs(flow))
                            scaleY = scaleX
                            translationY = (-1.dp.toPx() + touchY * 4.dp.toPx()) * amount
                            shape = LensShape
                            clip = false
                            shadowElevation = 1.5.dp.toPx() + 3.5.dp.toPx() * amount
                            ambientShadowColor = Color.Black.copy(alpha = 0.08f)
                            spotShadowColor = Color.Black.copy(alpha = 0.16f)
                        }
                        .liquidBubbleHighlight(isDark) { 0.25f + 0.75f * lift.coerceIn(0f, 1f) }
                        .liquid(railState) {
                            val amount = lift.coerceIn(0f, 1f)
                            shape = LensShape
                            frost = 0.dp
                            refraction = 0.08f + 0.37f * amount
                            curve = 0.04f + 0.16f * amount
                            edge = 0.012f + 0.023f * amount
                            dispersion = 0.14f * amount
                            saturation = 1.10f
                            tint = Color.White.copy(alpha = if (isDark) 0.012f else 0.018f)
                        },
                )
            }
        }
    }
}

/**
 * Floating circular Liquid Glass button (used for iOS Search pill and top-right Glass actions).
 */
@Composable
fun LiquidCircularGlassButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 22.dp,
    tint: Color = LocalApplePalette.current.label,
    isDark: Boolean = isSystemInDarkTheme(),
    liquidState: LiquidState? = null,
) {
    val palette = LocalApplePalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 550f),
        label = "circlePressScale",
    )
    val surfaceTint = if (isDark) Color(0xFF14161C).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.64f)

    val glassModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && liquidState != null) {
        Modifier
            .shadow(4.dp, CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.08f))
            .background(palette.cardSurface, CircleShape)
            .liquid(liquidState) {
                shape = CircleShape
                frost = 8.dp
                refraction = 0.05f
                curve = 0.10f
                edge = 0.012f
                saturation = 1.08f
                dispersion = 0f
                this.tint = surfaceTint
            }
    } else {
        Modifier.clearLiquidGlassPill(
            shape = CircleShape,
            hazeState = null,
            isDark = isDark,
            blurRadius = 8.dp,
            showBorder = false,
            shadowElevation = 2.dp,
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(glassModifier)
            .clip(CircleShape)
            .drawBehind {
                val w = this.size.width
                val h = this.size.height
                val rimGlowAlpha = if (isDark) 0.10f else 0.20f
                drawCircle(
                    brush = Brush.linearGradient(
                        0.00f to Color.White.copy(alpha = rimGlowAlpha),
                        0.30f to Color.White.copy(alpha = rimGlowAlpha * 0.35f),
                        0.60f to Color.Transparent,
                        1.00f to Color.Transparent,
                        start = Offset(0f, 0f),
                        end = Offset(w * 0.65f, h * 0.65f),
                    ),
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun PhoneNavigationGlyphs(
    items: List<LiquidPhoneTabItem>,
    captionIndex: Int,
    weights: List<Float>,
    neutral: Color,
    active: Color,
    captured: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    disableAnimations: Boolean = LocalDisableAnimations.current,
) {
    val activeIndex = captionIndex.coerceIn(items.indices)
    Row(modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, item ->
            val isSelected = activeIndex == index
            val tint = if (isSelected) active else neutral
            val weight = weights.getOrElse(index) { 1f }
            val inputModifier = if (captured) {
                Modifier.clearAndSetSemantics { }
            } else {
                Modifier
                    .semantics { contentDescription = item.label }
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(index) },
                        role = Role.Tab,
                        interactionSource = remember(index) { MutableInteractionSource() },
                        indication = null,
                    )
            }
            val textSemantics = if (captured) Modifier.clearAndSetSemantics { } else Modifier

            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .then(inputModifier),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.wrapContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSelected) item.activeIcon else item.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(24.dp),
                        )
                        if (item.badgeCount > 0 && !captured) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 7.dp, y = (-4).dp)
                                    .defaultMinSize(minWidth = 16.dp)
                                    .height(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30), CircleShape)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (item.badgeCount > 9) "9+" else item.badgeCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    if (item.label.isNotBlank()) {
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = if (disableAnimations) {
                                fadeIn(animationSpec = snap())
                            } else {
                                fadeIn(animationSpec = tween(150)) + expandHorizontally(
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                    expandFrom = Alignment.Start,
                                )
                            },
                            exit = if (disableAnimations) {
                                fadeOut(animationSpec = snap())
                            } else {
                                fadeOut(animationSpec = tween(100)) + shrinkHorizontally(
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                    shrinkTowards = Alignment.Start,
                                )
                            },
                        ) {
                            Row(
                                modifier = textSemantics,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(Modifier.width(6.dp))
                                StaggeredLabelReveal(
                                    text = item.label,
                                    tint = tint,
                                    captured = captured,
                                    disableAnimations = disableAnimations,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredLabelReveal(
    text: String,
    tint: Color,
    captured: Boolean,
    modifier: Modifier = Modifier,
    disableAnimations: Boolean = false,
) {
    Text(
        text = text,
        color = tint,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier,
    )
}

private fun rawPosition(x: Float, width: Float, itemCount: Int): Float {
    if (itemCount <= 1 || width <= 0f || !x.isFinite() || !width.isFinite()) return 0f
    return x / (width / itemCount) - 0.5f
}

private fun rubberBand(position: Float, itemCount: Int): Float {
    if (itemCount <= 1 || !position.isFinite()) return 0f
    val last = (itemCount - 1).toFloat()
    val limit = 0.18f
    return when {
        position < 0f -> -limit * (1f - 1f / (1f - position / limit))
        position > last -> last + limit * (1f - 1f / (1f + (position - last) / limit))
        else -> position
    }
}

private fun logicalIndex(position: Float, itemCount: Int, isRtl: Boolean): Int {
    if (itemCount <= 1) return 0
    val physicalIndex = if (position.isFinite()) position.roundToInt().coerceIn(0, itemCount - 1) else 0
    return if (isRtl) itemCount - 1 - physicalIndex else physicalIndex
}

private fun visualIndex(index: Int, itemCount: Int, isRtl: Boolean): Float {
    if (itemCount <= 1) return 0f
    val logicalIndex = index.coerceIn(0, itemCount - 1)
    return (if (isRtl) itemCount - 1 - logicalIndex else logicalIndex).toFloat()
}

private fun railTint(isDark: Boolean): Color =
    if (isDark) Color(0xFF14161F).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.32f)

private fun Modifier.railShadow(): Modifier = shadow(
    elevation = 8.dp,
    shape = CircleShape,
    clip = false,
    ambientColor = Color.Black.copy(alpha = 0.10f),
    spotColor = Color.Black.copy(alpha = 0.14f),
)

private fun Modifier.captureHeadroom(padding: Dp): Modifier = layout { measurable, constraints ->
    val inset = padding.roundToPx()
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val placeable = measurable.measure(Constraints.fixed(width + inset * 2, height + inset * 2))
    layout(width, height) { placeable.place(-inset, -inset) }
}

private fun Modifier.navigationRail(
    state: LiquidState,
    color: Color,
    blurRadius: Dp = 12.dp,
): Modifier = liquid(state) {
    shape = CircleShape
    frost = blurRadius
    refraction = 0.07f
    curve = 0.12f
    edge = 0.012f
    saturation = 1.10f
    dispersion = 0f
    tint = color
}
