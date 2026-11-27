package com.telvique.phone.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom SF Symbols-inspired vector icons matching Apple's iOS Phone app.
 */
object ApplePhoneIcons {

    private fun symbol(
        name: String,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

    private val FillBrush = SolidColor(Color.Black)

    /** SF Symbol: phone.fill — curved Apple handset */
    val PhoneFill: ImageVector by lazy {
        symbol("PhoneFill") {
            path(fill = FillBrush) {
                moveTo(6.62f, 10.79f)
                curveTo(8.06f, 13.62f, 10.38f, 15.94f, 13.21f, 17.38f)
                lineTo(15.41f, 15.18f)
                curveTo(15.68f, 14.91f, 16.08f, 14.82f, 16.43f, 14.93f)
                curveTo(17.55f, 15.30f, 18.75f, 15.50f, 20.0f, 15.50f)
                curveTo(20.55f, 15.50f, 21.0f, 15.95f, 21.0f, 16.50f)
                verticalLineTo(20.0f)
                curveTo(21.0f, 20.55f, 20.55f, 21.0f, 20.0f, 21.0f)
                curveTo(10.61f, 21.0f, 3.0f, 13.39f, 3.0f, 4.0f)
                curveTo(3.0f, 3.45f, 3.45f, 3.0f, 4.0f, 3.0f)
                horizontalLineTo(7.50f)
                curveTo(8.05f, 3.0f, 8.50f, 3.45f, 8.50f, 4.0f)
                curveTo(8.50f, 5.25f, 8.70f, 6.45f, 9.07f, 7.57f)
                curveTo(9.18f, 7.92f, 9.09f, 8.31f, 8.82f, 8.59f)
                close()
            }
        }
    }

    /** SF Symbol: phone.down.fill — horizontal Apple hang-up handset */
    val PhoneDownFill: ImageVector by lazy {
        symbol("PhoneDownFill") {
            path(fill = FillBrush) {
                moveTo(12.0f, 9.0f)
                curveTo(9.18f, 9.0f, 6.53f, 9.81f, 4.29f, 11.21f)
                curveTo(3.75f, 11.55f, 3.38f, 12.11f, 3.30f, 12.74f)
                lineTo(2.96f, 15.32f)
                curveTo(2.86f, 16.06f, 3.44f, 16.72f, 4.19f, 16.72f)
                horizontalLineTo(7.05f)
                curveTo(7.68f, 16.72f, 8.21f, 16.25f, 8.29f, 15.63f)
                lineTo(8.52f, 13.85f)
                curveTo(9.61f, 13.45f, 10.78f, 13.23f, 12.0f, 13.23f)
                curveTo(13.22f, 13.23f, 14.39f, 13.45f, 15.48f, 13.85f)
                lineTo(15.71f, 15.63f)
                curveTo(15.79f, 16.25f, 16.32f, 16.72f, 16.95f, 16.72f)
                horizontalLineTo(19.81f)
                curveTo(20.56f, 16.72f, 21.14f, 16.06f, 21.04f, 15.32f)
                lineTo(20.70f, 12.74f)
                curveTo(20.62f, 12.11f, 20.25f, 11.55f, 19.71f, 11.21f)
                curveTo(17.47f, 9.81f, 14.82f, 9.0f, 12.0f, 9.0f)
                close()
            }
        }
    }

    /** SF Symbol: circle.grid.3x3.fill — exact 3x3 circular dot matrix for Keypad */
    val KeypadDots: ImageVector by lazy {
        symbol("KeypadDots") {
            val centers = listOf(5.5f, 12f, 18.5f)
            val r = 2.05f
            for (cy in centers) {
                for (cx in centers) {
                    path(fill = FillBrush) {
                        moveTo(cx + r, cy)
                        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, cx - r, cy)
                        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, cx + r, cy)
                        close()
                    }
                }
            }
        }
    }

    /** SF Symbol: clock.fill */
    val ClockFill: ImageVector by lazy {
        symbol("ClockFill") {
            path(fill = FillBrush, pathFillType = PathFillType.EvenOdd) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(12.85f, 6.75f)
                curveTo(12.85f, 6.28f, 12.47f, 5.9f, 12f, 5.9f)
                curveTo(11.53f, 5.9f, 11.15f, 6.28f, 11.15f, 6.75f)
                verticalLineTo(12.15f)
                curveTo(11.15f, 12.43f, 11.29f, 12.69f, 11.52f, 12.85f)
                lineTo(15.22f, 15.32f)
                curveTo(15.61f, 15.58f, 16.14f, 15.48f, 16.40f, 15.08f)
                curveTo(16.66f, 14.69f, 16.56f, 14.16f, 16.16f, 13.90f)
                lineTo(12.85f, 11.69f)
                close()
            }
        }
    }

    /** SF Symbol: star.fill */
    val StarFill: ImageVector by lazy {
        symbol("StarFill") {
            path(fill = FillBrush) {
                moveTo(12f, 2.5f)
                lineTo(14.72f, 8.38f)
                lineTo(21.18f, 9.16f)
                lineTo(16.40f, 13.59f)
                lineTo(17.67f, 19.96f)
                lineTo(12f, 16.79f)
                lineTo(6.33f, 19.96f)
                lineTo(7.60f, 13.59f)
                lineTo(2.82f, 9.16f)
                lineTo(9.28f, 8.38f)
                close()
            }
        }
    }

    /** SF Symbol: star (outline) */
    val StarOutline: ImageVector by lazy {
        symbol("StarOutline") {
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.75f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3.2f)
                lineTo(14.55f, 8.65f)
                lineTo(20.55f, 9.38f)
                lineTo(16.12f, 13.48f)
                lineTo(17.28f, 19.38f)
                lineTo(12f, 16.45f)
                lineTo(6.72f, 19.38f)
                lineTo(7.88f, 13.48f)
                lineTo(3.45f, 9.38f)
                lineTo(9.45f, 8.65f)
                close()
            }
        }
    }

    /** SF Symbol: person.crop.circle.fill */
    val PersonCropCircleFill: ImageVector by lazy {
        symbol("PersonCropCircleFill") {
            path(fill = FillBrush, pathFillType = PathFillType.EvenOdd) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                // Head
                moveTo(12f, 6.2f)
                curveTo(13.82f, 6.2f, 15.3f, 7.68f, 15.3f, 9.5f)
                curveTo(15.3f, 11.32f, 13.82f, 12.8f, 12f, 12.8f)
                curveTo(10.18f, 12.8f, 8.7f, 11.32f, 8.7f, 9.5f)
                curveTo(8.7f, 7.68f, 10.18f, 6.2f, 12f, 6.2f)
                close()
                // Shoulders
                moveTo(12f, 14.3f)
                curveTo(15.35f, 14.3f, 17.85f, 16.05f, 18.72f, 17.85f)
                curveTo(17.08f, 19.65f, 14.68f, 20.75f, 12f, 20.75f)
                curveTo(9.32f, 20.75f, 6.92f, 19.65f, 5.28f, 17.85f)
                curveTo(6.15f, 16.05f, 8.65f, 14.3f, 12f, 14.3f)
                close()
            }
        }
    }

    /** SF Symbol: delete.left.fill — exact Apple keypad backspace glyph */
    val DeleteLeftFill: ImageVector by lazy {
        symbol("DeleteLeftFill") {
            path(fill = FillBrush, pathFillType = PathFillType.EvenOdd) {
                moveTo(8.6f, 5.5f)
                horizontalLineTo(19.2f)
                curveTo(20.47f, 5.5f, 21.5f, 6.53f, 21.5f, 7.8f)
                verticalLineTo(16.2f)
                curveTo(21.5f, 17.47f, 20.47f, 18.5f, 19.2f, 18.5f)
                horizontalLineTo(8.6f)
                curveTo(7.92f, 18.5f, 7.28f, 18.18f, 6.86f, 17.64f)
                lineTo(2.85f, 12.55f)
                curveTo(2.58f, 12.21f, 2.58f, 11.79f, 2.85f, 11.45f)
                lineTo(6.86f, 6.36f)
                curveTo(7.28f, 5.82f, 7.92f, 5.5f, 8.6f, 5.5f)
                close()
                // Cut-out X inside
                moveTo(11.35f, 9.15f)
                lineTo(14.0f, 11.8f)
                lineTo(16.65f, 9.15f)
                lineTo(17.55f, 10.05f)
                lineTo(14.9f, 12.7f)
                lineTo(17.55f, 15.35f)
                lineTo(16.65f, 16.25f)
                lineTo(14.0f, 13.6f)
                lineTo(11.35f, 16.25f)
                lineTo(10.45f, 15.35f)
                lineTo(13.1f, 12.7f)
                lineTo(10.45f, 10.05f)
                close()
            }
        }
    }

    /** SF Symbol: info.circle — Apple ⓘ button */
    val InfoCircle: ImageVector by lazy {
        symbol("InfoCircle") {
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.65f,
            ) {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 3f)
                close()
            }
            path(fill = FillBrush) {
                // Dot
                moveTo(13.05f, 7.9f)
                arcTo(1.05f, 1.05f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10.95f, 7.9f)
                arcTo(1.05f, 1.05f, 0f, isMoreThanHalf = true, isPositiveArc = true, 13.05f, 7.9f)
                close()
                // Stem
                moveTo(11.15f, 10.9f)
                horizontalLineTo(12.85f)
                verticalLineTo(16.6f)
                horizontalLineTo(11.15f)
                close()
            }
        }
    }

    /** SF Symbol: phone.arrow.up.right — small outgoing call indicator */
    val PhoneArrowUpRight: ImageVector by lazy {
        symbol("PhoneArrowUpRight") {
            path(fill = FillBrush) {
                moveTo(6.62f, 10.79f)
                curveTo(8.06f, 13.62f, 10.38f, 15.94f, 13.21f, 17.38f)
                lineTo(15.41f, 15.18f)
                curveTo(15.68f, 14.91f, 16.08f, 14.82f, 16.43f, 14.93f)
                curveTo(17.55f, 15.30f, 18.75f, 15.50f, 20.0f, 15.50f)
                curveTo(20.55f, 15.50f, 21.0f, 15.95f, 21.0f, 16.50f)
                verticalLineTo(20.0f)
                curveTo(21.0f, 20.55f, 20.55f, 21.0f, 20.0f, 21.0f)
                curveTo(10.61f, 21.0f, 3.0f, 13.39f, 3.0f, 4.0f)
                curveTo(3.0f, 3.45f, 3.45f, 3.0f, 4.0f, 3.0f)
                horizontalLineTo(7.50f)
                curveTo(8.05f, 3.0f, 8.50f, 3.45f, 8.50f, 4.0f)
                curveTo(8.50f, 5.25f, 8.70f, 6.45f, 9.07f, 7.57f)
                curveTo(9.18f, 7.92f, 9.09f, 8.31f, 8.82f, 8.59f)
                close()
            }
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(14f, 9.5f)
                lineTo(20f, 3.5f)
                moveTo(15.5f, 3.5f)
                horizontalLineTo(20f)
                verticalLineTo(8f)
            }
        }
    }

    /** SF Symbol: phone.arrow.down.left */
    val PhoneArrowDownLeft: ImageVector by lazy {
        symbol("PhoneArrowDownLeft") {
            path(fill = FillBrush) {
                moveTo(6.62f, 10.79f)
                curveTo(8.06f, 13.62f, 10.38f, 15.94f, 13.21f, 17.38f)
                lineTo(15.41f, 15.18f)
                curveTo(15.68f, 14.91f, 16.08f, 14.82f, 16.43f, 14.93f)
                curveTo(17.55f, 15.30f, 18.75f, 15.50f, 20.0f, 15.50f)
                curveTo(20.55f, 15.50f, 21.0f, 15.95f, 21.0f, 16.50f)
                verticalLineTo(20.0f)
                curveTo(21.0f, 20.55f, 20.55f, 21.0f, 20.0f, 21.0f)
                curveTo(10.61f, 21.0f, 3.0f, 13.39f, 3.0f, 4.0f)
                curveTo(3.0f, 3.45f, 3.45f, 3.0f, 4.0f, 3.0f)
                horizontalLineTo(7.50f)
                curveTo(8.05f, 3.0f, 8.50f, 3.45f, 8.50f, 4.0f)
                curveTo(8.50f, 5.25f, 8.70f, 6.45f, 9.07f, 7.57f)
                curveTo(9.18f, 7.92f, 9.09f, 8.31f, 8.82f, 8.59f)
                close()
            }
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20f, 3.5f)
                lineTo(14f, 9.5f)
                moveTo(14f, 5f)
                verticalLineTo(9.5f)
                horizontalLineTo(18.5f)
            }
        }
    }

    /** SF Symbol: magnifyingglass */
    val MagnifyingGlass: ImageVector by lazy {
        symbol("MagnifyingGlass") {
            path(
                stroke = FillBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(10.5f, 4f)
                arcTo(6.5f, 6.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10.49f, 4f)
                close()
                moveTo(15.3f, 15.3f)
                lineTo(20.2f, 20.2f)
            }
        }
    }

    /** SF Symbol: line.3.horizontal.decrease — iOS 26 top-right Filter menu icon */
    val FilterLines: ImageVector by lazy {
        symbol("FilterLines") {
            path(
                stroke = FillBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(4f, 7f)
                horizontalLineTo(20f)
                moveTo(7f, 12f)
                horizontalLineTo(17f)
                moveTo(10f, 17f)
                horizontalLineTo(14f)
            }
        }
    }

    /** SF Symbol: gearshape.fill */
    val GearShapeFill: ImageVector by lazy {
        symbol("GearShapeFill") {
            path(fill = FillBrush, pathFillType = PathFillType.EvenOdd) {
                moveTo(19.43f, 12.98f)
                curveTo(19.47f, 12.66f, 19.5f, 12.34f, 19.5f, 12f)
                curveTo(19.5f, 11.66f, 19.47f, 11.34f, 19.43f, 11.02f)
                lineTo(21.54f, 9.37f)
                curveTo(21.73f, 9.22f, 21.78f, 8.95f, 21.66f, 8.73f)
                lineTo(19.66f, 5.27f)
                curveTo(19.54f, 5.05f, 19.27f, 4.97f, 19.05f, 5.05f)
                lineTo(16.56f, 6.05f)
                curveTo(16.04f, 5.65f, 15.48f, 5.32f, 14.87f, 5.07f)
                lineTo(14.49f, 2.42f)
                curveTo(14.46f, 2.18f, 14.25f, 2f, 14f, 2f)
                horizontalLineTo(10f)
                curveTo(9.75f, 2f, 9.54f, 2.18f, 9.51f, 2.42f)
                lineTo(9.13f, 5.07f)
                curveTo(8.52f, 5.32f, 7.96f, 5.66f, 7.44f, 6.05f)
                lineTo(4.95f, 5.05f)
                curveTo(4.72f, 4.96f, 4.46f, 5.05f, 4.34f, 5.27f)
                lineTo(2.34f, 8.73f)
                curveTo(2.21f, 8.95f, 2.27f, 9.22f, 2.46f, 9.37f)
                lineTo(4.57f, 11.02f)
                curveTo(4.53f, 11.34f, 4.5f, 11.67f, 4.5f, 12f)
                curveTo(4.5f, 12.33f, 4.53f, 12.66f, 4.57f, 12.98f)
                lineTo(2.46f, 14.63f)
                curveTo(2.27f, 14.78f, 2.22f, 15.05f, 2.34f, 15.27f)
                lineTo(4.34f, 18.73f)
                curveTo(4.46f, 18.95f, 4.73f, 19.03f, 4.95f, 18.95f)
                lineTo(7.44f, 17.95f)
                curveTo(7.96f, 18.35f, 8.52f, 18.68f, 9.13f, 18.93f)
                lineTo(9.51f, 21.58f)
                curveTo(9.54f, 21.82f, 9.75f, 22f, 10f, 22f)
                horizontalLineTo(14f)
                curveTo(14.25f, 22f, 14.46f, 21.82f, 14.49f, 21.58f)
                lineTo(14.87f, 18.93f)
                curveTo(15.48f, 18.68f, 16.04f, 18.34f, 16.56f, 17.95f)
                lineTo(19.05f, 18.95f)
                curveTo(19.28f, 19.04f, 19.54f, 18.95f, 19.66f, 18.73f)
                lineTo(21.66f, 15.27f)
                curveTo(21.78f, 15.05f, 21.73f, 14.78f, 21.54f, 14.63f)
                close()
                moveTo(12f, 8.5f)
                curveTo(13.93f, 8.5f, 15.5f, 10.07f, 15.5f, 12f)
                curveTo(15.5f, 13.93f, 13.93f, 15.5f, 12f, 15.5f)
                curveTo(10.07f, 15.5f, 8.5f, 13.93f, 8.5f, 12f)
                curveTo(8.5f, 10.07f, 10.07f, 8.5f, 12f, 8.5f)
                close()
            }
        }
    }

    /** SF Symbol: speaker.wave.3.fill */
    val SpeakerWave3Fill: ImageVector by lazy {
        symbol("SpeakerWave3Fill") {
            path(fill = FillBrush) {
                moveTo(3f, 9.2f)
                horizontalLineTo(7.1f)
                lineTo(12.2f, 4.7f)
                curveTo(12.8f, 4.2f, 13.7f, 4.6f, 13.7f, 5.4f)
                verticalLineTo(18.6f)
                curveTo(13.7f, 19.4f, 12.8f, 19.8f, 12.2f, 19.3f)
                lineTo(7.1f, 14.8f)
                horizontalLineTo(3f)
                curveTo(2.45f, 14.8f, 2f, 14.35f, 2f, 13.8f)
                verticalLineTo(10.2f)
                curveTo(2f, 9.65f, 2.45f, 9.2f, 3f, 9.2f)
                close()
            }
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(16.2f, 9.3f)
                curveTo(17.1f, 10.6f, 17.1f, 13.4f, 16.2f, 14.7f)
                moveTo(18.7f, 6.8f)
                curveTo(20.6f, 9.6f, 20.6f, 14.4f, 18.7f, 17.2f)
            }
        }
    }

    /** SF Symbol: mic.fill */
    val MicFill: ImageVector by lazy {
        symbol("MicFill") {
            path(fill = FillBrush) {
                moveTo(12f, 2.5f)
                curveTo(10.34f, 2.5f, 9f, 3.84f, 9f, 5.5f)
                verticalLineTo(11.5f)
                curveTo(9f, 13.16f, 10.34f, 14.5f, 12f, 14.5f)
                curveTo(13.66f, 14.5f, 15f, 13.16f, 15f, 11.5f)
                verticalLineTo(5.5f)
                curveTo(15f, 3.84f, 13.66f, 2.5f, 12f, 2.5f)
                close()
            }
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(6.2f, 11.2f)
                curveTo(6.2f, 14.5f, 8.8f, 17.0f, 12f, 17.0f)
                curveTo(15.2f, 17.0f, 17.8f, 14.5f, 17.8f, 11.2f)
                moveTo(12f, 17.0f)
                verticalLineTo(21.0f)
                moveTo(8.8f, 21.0f)
                horizontalLineTo(15.2f)
            }
        }
    }

    /** SF Symbol: mic.slash.fill */
    val MicSlashFill: ImageVector by lazy {
        symbol("MicSlashFill") {
            path(fill = FillBrush) {
                moveTo(12f, 2.5f)
                curveTo(10.34f, 2.5f, 9f, 3.84f, 9f, 5.5f)
                verticalLineTo(10.0f)
                lineTo(15f, 13.1f)
                verticalLineTo(5.5f)
                curveTo(15f, 3.84f, 13.66f, 2.5f, 12f, 2.5f)
                close()
            }
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(6.2f, 11.2f)
                curveTo(6.2f, 14.5f, 8.8f, 17.0f, 12f, 17.0f)
                curveTo(13.2f, 17.0f, 14.3f, 16.65f, 15.2f, 16.05f)
                moveTo(12f, 17.0f)
                verticalLineTo(21.0f)
                moveTo(8.8f, 21.0f)
                horizontalLineTo(15.2f)
                moveTo(4f, 4f)
                lineTo(20f, 20f)
            }
        }
    }

    /** SF Symbol: message.fill */
    val MessageFill: ImageVector by lazy {
        symbol("MessageFill") {
            path(fill = FillBrush) {
                moveTo(12f, 3.5f)
                curveTo(6.75f, 3.5f, 2.5f, 7.05f, 2.5f, 11.45f)
                curveTo(2.5f, 13.75f, 3.65f, 15.82f, 5.5f, 17.28f)
                lineTo(4.65f, 20.6f)
                curveTo(4.56f, 20.95f, 4.91f, 21.24f, 5.23f, 21.08f)
                lineTo(9.05f, 19.12f)
                curveTo(9.98f, 19.31f, 10.97f, 19.4f, 12f, 19.4f)
                curveTo(17.25f, 19.4f, 21.5f, 15.85f, 21.5f, 11.45f)
                curveTo(21.5f, 7.05f, 17.25f, 3.5f, 12f, 3.5f)
                close()
            }
        }
    }

    /** SF Symbol: message (outline) — Apple thin rounded message speech bubble */
    val MessageOutline: ImageVector by lazy {
        symbol("MessageOutline") {
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3.8f)
                curveTo(6.8f, 3.8f, 2.6f, 7.3f, 2.6f, 11.6f)
                curveTo(2.6f, 13.85f, 3.75f, 15.85f, 5.55f, 17.25f)
                curveTo(5.62f, 17.3f, 5.66f, 17.4f, 5.62f, 17.5f)
                lineTo(4.75f, 20.35f)
                curveTo(4.68f, 20.6f, 4.95f, 20.82f, 5.18f, 20.7f)
                lineTo(8.85f, 18.95f)
                curveTo(8.95f, 18.9f, 9.05f, 18.92f, 9.15f, 18.95f)
                curveTo(10.05f, 19.25f, 11.0f, 19.4f, 12f, 19.4f)
                curveTo(17.2f, 19.4f, 21.4f, 15.9f, 21.4f, 11.6f)
                curveTo(21.4f, 7.3f, 17.2f, 3.8f, 12f, 3.8f)
                close()
            }
        }
    }

    /** SF Symbol: speaker.slash (outline) — Apple thin rounded silence / mute ringer */
    val SpeakerSlashOutline: ImageVector by lazy {
        symbol("SpeakerSlashOutline") {
            // Speaker body outline
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9.8f, 7.5f)
                lineTo(12.5f, 5.1f)
                curveTo(13.1f, 4.6f, 14.0f, 5.0f, 14.0f, 5.8f)
                verticalLineTo(11.5f)
                moveTo(14.0f, 15.5f)
                verticalLineTo(18.2f)
                curveTo(14.0f, 19.0f, 13.1f, 19.4f, 12.5f, 18.9f)
                lineTo(7.5f, 14.8f)
                horizontalLineTo(4.2f)
                curveTo(3.55f, 14.8f, 3.0f, 14.25f, 3.0f, 13.6f)
                verticalLineTo(10.4f)
                curveTo(3.0f, 9.75f, 3.55f, 9.2f, 4.2f, 9.2f)
                horizontalLineTo(7.5f)
            }
            // Diagonal slash
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(3.5f, 3.5f)
                lineTo(20.5f, 20.5f)
            }
        }
    }

    /** SF Symbol: bell.slash.fill — Apple silence / bell-slash */
    val BellSlash: ImageVector by lazy {
        symbol("BellSlash") {
            // Diagonal slash
            path(
                stroke = FillBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.5f, 3.5f)
                lineTo(20.5f, 20.5f)
            }
            // Clapper
            path(fill = FillBrush) {
                moveTo(10.0f, 20.0f)
                curveTo(10.0f, 21.1f, 10.9f, 22.0f, 12.0f, 22.0f)
                curveTo(13.1f, 22.0f, 14.0f, 21.1f, 14.0f, 20.0f)
                horizontalLineTo(10.0f)
                close()
            }
            // Upper Bell body (interrupted by slash)
            path(fill = FillBrush) {
                moveTo(12.0f, 2.5f)
                curveTo(11.45f, 2.5f, 11.0f, 2.95f, 11.0f, 3.5f)
                verticalLineTo(4.25f)
                curveTo(8.65f, 4.85f, 7.0f, 7.05f, 7.0f, 9.65f)
                verticalLineTo(10.2f)
                lineTo(13.8f, 17.0f)
                horizontalLineTo(18.5f)
                lineTo(17.5f, 15.0f)
                verticalLineTo(9.65f)
                curveTo(17.5f, 7.05f, 15.85f, 4.85f, 13.0f, 4.25f)
                verticalLineTo(3.5f)
                curveTo(13.0f, 2.95f, 12.55f, 2.5f, 12.0f, 2.5f)
                close()
            }
            // Lower Left Flare
            path(fill = FillBrush) {
                moveTo(6.2f, 15.0f)
                lineTo(4.5f, 17.0f)
                curveTo(4.2f, 17.35f, 4.45f, 18.0f, 4.95f, 18.0f)
                horizontalLineTo(14.8f)
                lineTo(11.8f, 15.0f)
                horizontalLineTo(6.2f)
                close()
            }
        }
    }

    /** SF Symbol: phone (thin rounded) — thin handset with rounded corners */
    val PhoneThinRounded: ImageVector by lazy {
        symbol("PhoneThinRounded") {
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.80f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.62f, 10.79f)
                curveTo(8.06f, 13.62f, 10.38f, 15.94f, 13.21f, 17.38f)
                lineTo(15.41f, 15.18f)
                curveTo(15.68f, 14.91f, 16.08f, 14.82f, 16.43f, 14.93f)
                curveTo(17.55f, 15.30f, 18.75f, 15.50f, 20.0f, 15.50f)
                curveTo(20.55f, 15.50f, 21.0f, 15.95f, 21.0f, 16.50f)
                verticalLineTo(19.8f)
                curveTo(21.0f, 20.45f, 20.45f, 21.0f, 19.8f, 21.0f)
                curveTo(10.61f, 21.0f, 3.0f, 13.39f, 3.0f, 4.2f)
                curveTo(3.0f, 3.55f, 3.55f, 3.0f, 4.2f, 3.0f)
                horizontalLineTo(7.40f)
                curveTo(8.05f, 3.0f, 8.50f, 3.45f, 8.50f, 4.1f)
                curveTo(8.50f, 5.25f, 8.70f, 6.45f, 9.07f, 7.57f)
                curveTo(9.18f, 7.92f, 9.09f, 8.31f, 8.82f, 8.59f)
                close()
            }
        }
    }

    /** SF Symbol: plus */
    val Plus: ImageVector by lazy {
        symbol("Plus") {
            path(
                stroke = FillBrush,
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 4.5f)
                verticalLineTo(19.5f)
                moveTo(4.5f, 12f)
                horizontalLineTo(19.5f)
            }
        }
    }

    /** SF Symbol: xmark */
    val XMark: ImageVector by lazy {
        symbol("XMark") {
            path(
                stroke = FillBrush,
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
        }
    }

    /** SF Symbol: chevron.right */
    val ChevronRight: ImageVector by lazy {
        symbol("ChevronRight") {
            path(
                stroke = FillBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 5.5f)
                lineTo(15.5f, 12f)
                lineTo(9f, 18.5f)
            }
        }
    }

    /** SF Symbol: checkmark */
    val Checkmark: ImageVector by lazy {
        symbol("Checkmark") {
            path(
                stroke = FillBrush,
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 12.5f)
                lineTo(9.8f, 17.2f)
                lineTo(19f, 7.2f)
            }
        }
    }

    /** SF Symbol: pause.fill */
    val PauseFill: ImageVector by lazy {
        symbol("PauseFill") {
            path(fill = FillBrush) {
                moveTo(6.5f, 5f)
                horizontalLineTo(10f)
                verticalLineTo(19f)
                horizontalLineTo(6.5f)
                close()
                moveTo(14f, 5f)
                horizontalLineTo(17.5f)
                verticalLineTo(19f)
                horizontalLineTo(14f)
                close()
            }
        }
    }

    /** SF Symbol: play.fill */
    val PlayFill: ImageVector by lazy {
        symbol("PlayFill") {
            path(fill = FillBrush) {
                moveTo(7f, 4.8f)
                curveTo(7f, 4.1f, 7.75f, 3.65f, 8.35f, 4.0f)
                lineTo(19.3f, 10.7f)
                curveTo(19.9f, 11.05f, 19.9f, 11.95f, 19.3f, 12.3f)
                lineTo(8.35f, 19.0f)
                curveTo(7.75f, 19.35f, 7f, 18.9f, 7f, 18.2f)
                close()
            }
        }
    }

    /** SF Symbol: recordingtape — classic two-reel voicemail glyph */
    val Voicemail: ImageVector by lazy {
        symbol("Voicemail") {
            path(
                stroke = FillBrush,
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Left reel
                moveTo(10.0f, 11.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 3.0f, 11.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10.0f, 11.5f)
                close()
                // Right reel
                moveTo(21.0f, 11.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14.0f, 11.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 21.0f, 11.5f)
                close()
                // Bottom connecting tangent bar
                moveTo(6.5f, 15.0f)
                lineTo(17.5f, 15.0f)
            }
        }
    }
}
