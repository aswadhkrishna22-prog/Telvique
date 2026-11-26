package com.telvique.phone.ui.glass

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
data class LiquidPhoneTabItem(
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector = icon,
)

private val LensShape = CircleShape

/**
 * Floating Apple iOS 26 Liquid Glass tab bar ported from OzonePlayer's LiquidNavigationBar.
 * Uses fletchmckee.liquid real-time refraction + draggable water-drop lens on Android 13+,
 * and crisp frosted glass pill on Android 8-12.
 */
@Composable
fun LiquidPhoneTabBar(
    liquidState: LiquidState?,
    items: List<LiquidPhoneTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    maxWidthDp: Dp = 330.dp,
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
        animationSpec = spring(dampingRatio = 0.90f, stiffness = 520f),
        label = "liquidDropLift",
    )
    val currentPosition = {
        if (touchPosition.isFinite()) touchPosition else settledPosition.value
    }

    LaunchedEffect(selectedIndex, pressed, releaseGeneration) {
        if (!pressed) {
            if (touchPosition.isFinite()) {
                settledPosition.snapTo(touchPosition)
                touchPosition = Float.NaN
            }
            settledPosition.animateTo(
                visualIndex(selectedIndex, count, isRtl),
                spring(dampingRatio = 0.8f, stiffness = 400f),
            )
        }
    }

    LaunchedEffect(count, isRtl) {
        snapshotFlow { currentPosition() }.collectLatest {
            trailingPosition.animateTo(it, spring(dampingRatio = 0.85f, stiffness = 450f))
        }
    }

    val neutralInk = if (isDark) Color.White.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.86f)
    val activeInk = palette.accentBlue
    val pageColor = palette.background
    val surfaceColor = palette.cardSurface
    val railTint = if (isDark) Color(0xFF14161C).copy(alpha = 0.36f) else Color.White.copy(alpha = 0.56f)
    val railState = if (useRealLiquid) rememberLiquidState() else null
    val railHeight = 60.dp
    val pressedScale = railHeight.value / (railHeight.value - 8f)
    val captionIndex = if (pressed) captionAtPress else selectedIndex
    val activeIndex = captionIndex.coerceIn(0, count - 1)

    val animFractions = List(count) { i ->
        animateFloatAsState(
            targetValue = if (activeIndex == i) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
            label = "tab_expand_$i",
        ).value
    }
    val weights = animFractions.map { lerp(1.0f, 1.58f, it) }
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
            // Acyclic optical capture plane for liquid lens
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
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(surfaceColor, CircleShape)
                    .navigationRail(liquidState, railTint),
            )
        } else {
            val hazeState = LocalHazeState.current
            Box(
                Modifier
                    .matchParentSize()
                    .clearLiquidGlassPill(
                        shape = CircleShape,
                        hazeState = hazeState,
                        isDark = isDark,
                        blurRadius = 12.dp,
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
                                        if (destination != selected) select(destination)
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
                        .clip(LensShape)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.07f),
                            LensShape,
                        )
                        .liquidBubbleHighlight(isDark) { 0.35f + 0.65f * lift },
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
                            tint = Color.White.copy(alpha = if (isDark) 0.018f else 0.028f)
                        },
                )
            }
        }
    }
}

/**
 * Floating circular Liquid Glass button (used for iOS 26 Search pill and top-right Glass actions).
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
) {
    val backdrop = LocalGlassBackdrop.current
    val hazeState = LocalHazeState.current

    val glassModifier = if (backdrop != null) {
        Modifier.backdropLiquidGlass(
            backdrop = backdrop,
            shape = CircleShape,
            isDark = isDark,
            interactive = true,
            pressedScale = 1.08f,
        )
    } else {
        Modifier.clearLiquidGlassPill(
            shape = CircleShape,
            hazeState = hazeState,
            isDark = isDark,
            blurRadius = 10.dp,
            shadowElevation = 6.dp,
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .then(glassModifier)
            .clip(CircleShape)
            .clickable(onClick = onClick),
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
                    Icon(
                        imageVector = if (isSelected) item.activeIcon else item.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                    if (item.label.isNotBlank()) {
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(animationSpec = tween(150)) + expandHorizontally(
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                expandFrom = Alignment.Start,
                            ),
                            exit = fadeOut(animationSpec = tween(100)) + shrinkHorizontally(
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                shrinkTowards = Alignment.Start,
                            ),
                        ) {
                            Row(
                                modifier = textSemantics,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(Modifier.width(5.dp))
                                StaggeredLabelReveal(
                                    text = item.label,
                                    tint = tint,
                                    captured = captured,
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
) {
    if (captured) {
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
        return
    }

    val density = LocalDensity.current
    val slidePx = with(density) { 5.dp.toPx() }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        text.forEachIndexed { index, char ->
            val charAnim = remember(text, index) { Animatable(0f) }
            LaunchedEffect(charAnim) {
                delay(index * 20L)
                charAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 140, easing = LinearOutSlowInEasing),
                )
            }
            Text(
                text = char.toString(),
                color = tint,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.graphicsLayer {
                    val p = charAnim.value.coerceIn(0f, 1f)
                    alpha = p
                    translationY = (1f - p) * slidePx
                },
            )
        }
    }
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

private fun Modifier.navigationRail(state: LiquidState, color: Color, blurRadius: Dp = 12.dp): Modifier =
    liquid(state) {
        shape = CircleShape
        frost = blurRadius
        refraction = 0.07f
        curve = 0.12f
        edge = 0.012f
        saturation = 1.1f
        dispersion = 0f
        tint = color
    }
