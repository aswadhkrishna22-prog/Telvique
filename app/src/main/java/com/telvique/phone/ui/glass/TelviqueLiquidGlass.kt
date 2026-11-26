@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package com.telvique.phone.ui.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.sign

private object BackdropGlassRecipe {
    const val BLUR_BASE_DP = 12f
    const val BLUR_BRIGHT_DP = 20f
    const val BLUR_DARK_DP = 6f
    const val BRIGHTNESS = 0.05f
    const val CONTRAST = 1f
    const val SATURATION = 1.1f
    const val LENS_DIVISOR = 6f
    const val MIN_SCRIM = 0.18f
    const val MAX_SCRIM = 0.56f
    const val PRESS_BLUR_DP = 2f
    const val HAZE_QUALITY_FRACTION = 0.5f
}

val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun isBackdropGlassCapable(): Boolean {
    val view = LocalView.current
    if (view.isInEditMode) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    if (view.isAttachedToWindow && !view.isHardwareAccelerated) return false
    val context = LocalContext.current
    val am = remember(context) {
        runCatching { context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager }.getOrNull()
    }
    return am?.isLowRamDevice != true
}

internal fun lensSafeGlassShape(shape: Shape): Shape =
    if (shape is CornerBasedShape) shape else RoundedCornerShape(0.dp)

class BackdropGlassInteraction(
    private val animationScope: CoroutineScope,
) {
    private val pressSpec = spring<Float>(
        dampingRatio = 0.52f,
        stiffness = 320f,
        visibilityThreshold = 0.001f,
    )
    private val pressAnimation = Animatable(0f, 0.001f)

    val pressProgress: Float get() = pressAnimation.value
    var touchPosition by mutableStateOf(Offset.Zero)
        private set

    suspend fun detectPress(pointer: PointerInputScope) =
        with(pointer) {
            inspectDragGestures(
                onDragStart = { down ->
                    touchPosition = down.position
                    animationScope.launch { pressAnimation.animateTo(1f, pressSpec) }
                },
                onDragEnd = { animationScope.launch { pressAnimation.animateTo(0f, pressSpec) } },
                onDragCancel = { animationScope.launch { pressAnimation.animateTo(0f, pressSpec) } },
            ) { change, _ ->
                touchPosition = change.position
            }
        }
}

@Composable
fun rememberBackdropGlassInteraction(): BackdropGlassInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope) { BackdropGlassInteraction(scope) }
}

/**
 * Specular meniscus rim highlight (above-left bright sheen + bottom edge depth).
 */
fun Modifier.liquidBubbleHighlight(
    isDark: Boolean,
    cornerFraction: Float = 0.5f,
    intensity: () -> Float = { 1f },
): Modifier = drawWithCache {
    if (size.width <= 0f || size.height <= 0f) {
        onDrawWithContent { drawContent() }
    } else {
        val radius = size.minDimension * cornerFraction.coerceIn(0f, 0.5f)
        val silhouette = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius)))
        }
        val rimWidth = 0.85.dp.toPx().coerceAtMost(radius.coerceAtLeast(0.85.dp.toPx()))
        val rimInset = rimWidth / 2f
        val rim = Path().apply {
            addRoundRect(
                RoundRect(
                    rimInset,
                    rimInset,
                    size.width - rimInset,
                    size.height - rimInset,
                    CornerRadius((radius - rimInset).coerceAtLeast(0f)),
                ),
            )
        }
        val rimStroke = Stroke(rimWidth)
        val oppositeStroke = Stroke(0.5.dp.toPx().coerceAtMost(radius.coerceAtLeast(0.5.dp.toPx())))
        val broadShade = Stroke(5.dp.toPx().coerceAtMost(radius.coerceAtLeast(1f)))
        val narrowShade = Stroke(1.5.dp.toPx().coerceAtMost(radius.coerceAtLeast(1f)))

        val upperReflection = Brush.linearGradient(
            0f to Color.White.copy(alpha = if (isDark) 0.65f else 0.85f),
            0.26f to Color.White.copy(alpha = if (isDark) 0.28f else 0.42f),
            0.54f to Color.Transparent,
            1f to Color.Transparent,
            start = Offset.Zero,
            end = Offset(size.width * 0.78f, size.height),
        )
        val oppositeReflection = Brush.linearGradient(
            0f to Color.Transparent,
            0.62f to Color.Transparent,
            0.84f to Color.White.copy(alpha = 0.08f),
            1f to Color.White.copy(alpha = if (isDark) 0.25f else 0.38f),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
        val lowerShade = Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = if (isDark) 0.14f else 0.08f),
            endY = size.height,
        )
        val surfaceReflection = Brush.verticalGradient(
            0f to Color.White.copy(alpha = if (isDark) 0.06f else 0.08f),
            0.40f to Color.White.copy(alpha = 0.01f),
            1f to Color.Transparent,
            endY = size.height,
        )

        onDrawWithContent {
            drawContent()
            val strength = intensity().coerceIn(0f, 1f)
            if (strength > 0f) {
                clipPath(silhouette) {
                    drawRect(surfaceReflection, alpha = strength)
                    drawPath(silhouette, lowerShade, alpha = strength * 0.45f, style = broadShade)
                    drawPath(silhouette, lowerShade, alpha = strength * 0.65f, style = narrowShade)
                    drawPath(rim, upperReflection, alpha = strength, style = rimStroke)
                    drawPath(rim, oppositeReflection, alpha = strength, style = oppositeStroke)
                }
            }
        }
    }
}

/**
 * Interactive Liquid Glass modifier powered by Kyant Backdrop (API 31+/33+ real blur & lens refraction)
 * with Haze/translucent fallback when Backdrop isn't provided.
 */
fun Modifier.backdropLiquidGlass(
    backdrop: Backdrop?,
    shape: Shape,
    isDark: Boolean,
    interactive: Boolean = true,
    pressedScale: Float = 1.07f,
    luminance: Float = if (isDark) 0.2f else 0.65f,
    highlight: Highlight = Highlight.Default,
    lensDivisor: Float = BackdropGlassRecipe.LENS_DIVISOR,
    lensMaxRadiusDivisor: Float = 2f,
    hasLens: Boolean = true,
    scrimAlpha: Float? = null,
    blurScale: Float = 1f,
    fallbackTint: Color = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.72f),
): Modifier = composed {
    if (backdrop == null) {
        return@composed this
            .shadow(
                elevation = 6.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            )
            .clip(shape)
            .background(fallbackTint, shape)
            .liquidBubbleHighlight(isDark = isDark, cornerFraction = 0.5f)
    }

    val interaction = if (interactive) rememberBackdropGlassInteraction() else null
    this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { lensSafeGlassShape(shape) },
            highlight = { highlight },
            effects = {
                if (size.minDimension >= 1f) {
                    val l = if (isDark) -0.45f else (luminance * 2f - 1f).let { sign(it) * it * it }
                    val press = interaction?.pressProgress ?: 0f
                    colorControls(
                        brightness = BackdropGlassRecipe.BRIGHTNESS,
                        contrast = BackdropGlassRecipe.CONTRAST,
                        saturation = BackdropGlassRecipe.SATURATION,
                    )
                    blur(
                        (
                            if (l > 0f) {
                                lerp(
                                    BackdropGlassRecipe.BLUR_BASE_DP.dp.toPx(),
                                    BackdropGlassRecipe.BLUR_BRIGHT_DP.dp.toPx(),
                                    l,
                                )
                            } else {
                                lerp(
                                    BackdropGlassRecipe.BLUR_BASE_DP.dp.toPx(),
                                    BackdropGlassRecipe.BLUR_DARK_DP.dp.toPx(),
                                    -l,
                                )
                            }
                        ) * blurScale + BackdropGlassRecipe.PRESS_BLUR_DP.dp.toPx() * press,
                    )
                    if (hasLens) {
                        val baseLens = (size.minDimension / lensDivisor +
                            BackdropGlassRecipe.PRESS_BLUR_DP.dp.toPx() * press).coerceAtLeast(0.1f)
                        val maxRadius = (size.minDimension / lensMaxRadiusDivisor).coerceAtLeast(baseLens + 0.1f)
                        lens(
                            baseLens,
                            maxRadius,
                            false,
                        )
                    }
                }
            },
            onDrawBackdrop = { drawBackdrop ->
                drawBackdrop()
            },
            onDrawSurface = {
                if (size.minDimension >= 1f) {
                    val tintAlpha = scrimAlpha ?: if (isDark) 0.18f else 0.36f
                    val surfaceColor = if (isDark) Color(0xFF1A1C22) else Color.White
                    drawRect(surfaceColor.copy(alpha = tintAlpha))
                    val press = interaction?.pressProgress ?: 0f
                    if (press > 0f) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f * press),
                                    Color.Transparent,
                                ),
                                center = interaction?.touchPosition?.takeIf { it != Offset.Zero }
                                    ?: Offset(size.width / 2f, size.height / 2f),
                                radius = (size.minDimension * 1.2f).coerceAtLeast(1f),
                            ),
                            blendMode = BlendMode.Plus,
                        )
                    }
                }
            },
            layerBlock = if (interaction != null) {
                {
                    val scale = lerp(1f, pressedScale, interaction.pressProgress)
                    scaleX = scale
                    scaleY = scale
                }
            } else {
                null
            },
        )
        .then(
            if (interaction != null) {
                Modifier.pointerInput(interaction) { interaction.detectPress(this) }
            } else {
                Modifier
            },
        )
}

/**
 * Haze-backed clear liquid glass pill (fast for scrolling overlays, chips, and search bars).
 */
fun Modifier.clearLiquidGlassPill(
    shape: Shape = RoundedCornerShape(28.dp),
    hazeState: HazeState? = null,
    isDark: Boolean = false,
    blurRadius: Dp = 8.dp,
    tintAlpha: Float = if (isDark) 0.14f else 0.62f,
    showBorder: Boolean = true,
    shadowElevation: Dp = 6.dp,
): Modifier = composed {
    val glassTint = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = tintAlpha + 0.28f)
    } else {
        Color.White.copy(alpha = tintAlpha)
    }

    val hazeStyle = remember(glassTint, blurRadius) {
        val tintEffect = HazeColorEffect.tint(glassTint)
        HazeBlurStyle {
            blurEnabled(true)
            backgroundColor(Color.Transparent)
            colorEffects(listOf(tintEffect))
            fallbackColorEffect(tintEffect)
            blurRadius(blurRadius)
            noiseFactor(0.01f)
        }
    }

    this
        .then(
            if (shadowElevation > 0.dp) {
                Modifier.shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                )
            } else {
                Modifier
            },
        )
        .clip(shape)
        .then(
            if (hazeState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.hazeBlur(
                    input = HazeInput.Sources(hazeState),
                    style = hazeStyle,
                    performanceMode = HazePerformanceMode.Fixed(BackdropGlassRecipe.HAZE_QUALITY_FRACTION),
                )
            } else {
                Modifier.background(glassTint, shape = shape)
            },
        )
        .then(
            if (showBorder) {
                Modifier.liquidBubbleHighlight(isDark = isDark, cornerFraction = 0.5f)
            } else {
                Modifier
            },
        )
}

/**
 * Collapsing top header frosted glass backdrop ported from OzonePlayer HeaderHazeBackdrop.
 */
@Composable
fun HeaderGlassBackdrop(
    progress: Float,
    surfaceColor: Color,
    hazeState: HazeState?,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val statusBarTop = with(LocalDensity.current) { WindowInsets.systemBars.getTop(this).toDp() }
    val totalHeight = height + statusBarTop + 14.dp
    val clamped = progress.coerceIn(0f, 1f)
    val isBlurActive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null

    val hazeStyle = if (isBlurActive) {
        remember(surfaceColor) {
            val tintEffect = HazeColorEffect.tint(surfaceColor.copy(alpha = 0.68f))
            HazeBlurStyle {
                blurEnabled(true)
                backgroundColor(surfaceColor)
                colorEffects(listOf(tintEffect))
                fallbackColorEffect(tintEffect)
                blurRadius(18.dp)
                noiseFactor(0.01f)
                mask(
                    Brush.verticalGradient(
                        0.0f to Color.Black,
                        0.70f to Color.Black.copy(alpha = 0.85f),
                        1.0f to Color.Transparent,
                    ),
                )
            }
        }
    } else {
        null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .graphicsLayer { alpha = clamped }
            .then(
                if (isBlurActive && hazeStyle != null) {
                    Modifier
                        .hazeBlur(
                            input = HazeInput.Sources(hazeState),
                            style = hazeStyle,
                            performanceMode = HazePerformanceMode.Fixed(BackdropGlassRecipe.HAZE_QUALITY_FRACTION),
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    surfaceColor.copy(alpha = 0.72f),
                                    surfaceColor.copy(alpha = 0.45f),
                                    Color.Transparent,
                                ),
                            ),
                        )
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                surfaceColor.copy(alpha = 0.96f),
                                surfaceColor.copy(alpha = 0.85f),
                                Color.Transparent,
                            ),
                        ),
                    )
                },
            ),
    )
}

internal suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down)
        onDrag(down, Offset.Zero)
        val upEvent = drag(
            pointerId = down.id,
            onDrag = { onDrag(it, it.positionChange()) },
        )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) return null
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) return null
        if (change.changedToUpIgnoreConsumed()) return change
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else {
            if (dragEvent.previousPosition != dragEvent.position) {
                return dragEvent
            }
        }
    }
}
