package com.tkno.ren.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons

private var _report: ImageVector? = null
val CustomIcons.Report: ImageVector
    get() {
        if (_report != null) return _report!!
        _report = ImageVector.Builder(
            name = "report",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero
            ) {
                moveTo(12f, 17f)
                quadToRelative(0.43f, 0f, 0.71f, -0.29f)
                quadTo(13f, 16.43f, 13f, 16f)
                reflectiveQuadTo(12.71f, 15.29f)
                reflectiveQuadTo(12f, 15f)
                reflectiveQuadToRelative(-0.71f, 0.29f)
                reflectiveQuadTo(11f, 16f)
                reflectiveQuadToRelative(0.29f, 0.71f)
                reflectiveQuadTo(12f, 17f)
                close()
                moveTo(11f, 13f)
                horizontalLineToRelative(2f)
                verticalLineTo(7f)
                horizontalLineTo(11f)
                verticalLineToRelative(6f)
                close()
                moveTo(8.25f, 21f)
                lineTo(3f, 15.75f)
                verticalLineTo(8.25f)
                lineTo(8.25f, 3f)
                horizontalLineToRelative(7.5f)
                lineTo(21f, 8.25f)
                verticalLineToRelative(7.5f)
                lineTo(15.75f, 21f)
                horizontalLineTo(8.25f)
                close()
                moveTo(9.1f, 19f)
                horizontalLineToRelative(5.8f)
                lineTo(19f, 14.9f)
                verticalLineTo(9.1f)
                lineTo(14.9f, 5f)
                horizontalLineTo(9.1f)
                lineTo(5f, 9.1f)
                verticalLineToRelative(5.8f)
                lineTo(9.1f, 19f)
                close()
                moveTo(12f, 12f)
                close()
            }
        }.build()
        return _report!!
    }

private var _refresh: ImageVector? = null
val CustomIcons.Refresh: ImageVector
    get() {
        if (_refresh != null) return _refresh!!
        _refresh = ImageVector.Builder(
            name = "refresh",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero
            ) {
                moveTo(12f, 20f)
                quadTo(8.65f, 20f, 6.33f, 17.68f)
                reflectiveQuadTo(4f, 12f)
                reflectiveQuadTo(6.33f, 6.32f)
                reflectiveQuadTo(12f, 4f)
                quadToRelative(1.73f, 0f, 3.3f, 0.71f)
                quadTo(16.88f, 5.43f, 18f, 6.75f)
                verticalLineTo(4f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(7f)
                horizontalLineTo(13f)
                verticalLineTo(9f)
                horizontalLineToRelative(4.2f)
                quadTo(16.4f, 7.6f, 15.01f, 6.8f)
                reflectiveQuadTo(12f, 6f)
                quadTo(9.5f, 6f, 7.75f, 7.75f)
                reflectiveQuadTo(6f, 12f)
                reflectiveQuadToRelative(1.75f, 4.25f)
                reflectiveQuadTo(12f, 18f)
                quadToRelative(1.93f, 0f, 3.48f, -1.1f)
                reflectiveQuadTo(17.65f, 14f)
                horizontalLineToRelative(2.1f)
                quadToRelative(-0.7f, 2.65f, -2.85f, 4.32f)
                reflectiveQuadTo(12f, 20f)
                close()
            }
        }.build()
        return _refresh!!
    }

private var _close: ImageVector? = null
val CustomIcons.Close: ImageVector
    get() {
        if (_close != null) return _close!!
        _close = ImageVector.Builder(
            name = "close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero
            ) {
                moveTo(6.4f, 19f)
                lineTo(5f, 17.6f)
                lineTo(10.6f, 12f)
                lineTo(5f, 6.4f)
                lineTo(6.4f, 5f)
                lineTo(12f, 10.6f)
                lineTo(17.6f, 5f)
                lineTo(19f, 6.4f)
                lineTo(13.4f, 12f)
                lineTo(19f, 17.6f)
                lineTo(17.6f, 19f)
                lineTo(12f, 13.4f)
                lineTo(6.4f, 19f)
                close()
            }
        }.build()
        return _close!!
    }
