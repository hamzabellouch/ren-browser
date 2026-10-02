package com.tkno.ren.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.tkno.ren.R

val Md3HomeOutlined: ImageVector by lazy {
    ImageVector.Builder(
        name = "Md3HomeOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    .apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(10.9f, 4.8f)
            quadTo(12.0f, 4.0f, 13.1f, 4.8f)
            lineTo(18.9f, 9.2f)
            quadTo(19.8f, 9.9f, 19.8f, 11.2f)
            lineTo(19.8f, 18.5f)
            quadTo(19.8f, 19.8f, 18.5f, 19.8f)
            lineTo(14.5f, 19.8f)
            lineTo(14.5f, 15.5f)
            curveTo(14.5f, 13.5f, 9.5f, 13.5f, 9.5f, 15.5f)
            lineTo(9.5f, 19.8f)
            lineTo(5.5f, 19.8f)
            quadTo(4.2f, 19.8f, 4.2f, 18.5f)
            lineTo(4.2f, 11.2f)
            quadTo(4.2f, 9.9f, 5.1f, 9.2f)
            lineTo(10.9f, 4.8f)
            close()
        }
    }
    .build()
}

val Md3HomeFilled: ImageVector by lazy {
    ImageVector.Builder(
        name = "Md3HomeFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    .apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(10.8f, 4.4f)
            quadTo(12.0f, 3.5f, 13.2f, 4.4f)
            lineTo(19.3f, 9.1f)
            quadTo(20.5f, 10.0f, 20.5f, 11.5f)
            lineTo(20.5f, 19.0f)
            quadTo(20.5f, 20.5f, 19.0f, 20.5f)
            lineTo(14.5f, 20.5f)
            lineTo(14.5f, 15.5f)
            curveTo(14.5f, 13.0f, 9.5f, 13.0f, 9.5f, 15.5f)
            lineTo(9.5f, 20.5f)
            lineTo(5.0f, 20.5f)
            quadTo(3.5f, 20.5f, 3.5f, 19.0f)
            lineTo(3.5f, 11.5f)
            quadTo(3.5f, 10.0f, 4.7f, 9.1f)
            lineTo(10.8f, 4.4f)
            close()
        }
    }
    .build()
}

val Md3HistoryOutlined: ImageVector by lazy {
    ImageVector.Builder(
        name = "Md3HistoryOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(13f, 3f)
            curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
            horizontalLineTo(1f)
            lineToRelative(4f, 4f)
            lineToRelative(4f, -4f)
            horizontalLineTo(6f)
            curveToRelative(0f, -3.87f, 3.13f, -7f, 7f, -7f)
            curveToRelative(3.87f, 0f, 7f, 3.13f, 7f, 7f)
            reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f)
            curveToRelative(-1.93f, 0f, -3.68f, -0.79f, -4.94f, -2.06f)
            lineToRelative(-1.42f, 1.42f)
            curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
            curveToRelative(4.97f, 0f, 9f, -4.03f, 9f, -9f)
            reflectiveCurveToRelative(-4.03f, -9f, -9f, -9f)
            close()
            moveTo(12f, 8f)
            verticalLineToRelative(5f)
            lineToRelative(4.25f, 2.52f)
            lineToRelative(0.77f, -1.28f)
            lineToRelative(-3.52f, -2.09f)
            verticalLineTo(8f)
            horizontalLineToRelative(-1.5f)
            close()
        }
    }.build()
}

val Md3HistoryFilled: ImageVector = Md3HistoryOutlined

val Md3TabOutlined: ImageVector by lazy {
    ImageVector.Builder(
        name = "Md3TabOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(21f, 3f)
            horizontalLineTo(3f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(18f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(21f, 19f)
            horizontalLineTo(3f)
            verticalLineTo(5f)
            horizontalLineToRelative(10f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(10f)
            close()
        }
    }.build()
}

val Md3TabFilled: ImageVector = Md3TabOutlined

val Md3CategorySearchOutlined: ImageVector by lazy {
    ImageVector.Builder(
        name = "Md3CategorySearchOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.White),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(2f, 20.5f)
            verticalLineToRelative(-8f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(8f)
            horizontalLineTo(2f)
            close()
            moveToRelative(2f, -2f)
            horizontalLineTo(8f)
            verticalLineToRelative(-4f)
            horizontalLineTo(4f)
            verticalLineToRelative(4f)
            close()
            moveTo(5.5f, 10f)
            lineTo(11f, 1f)
            lineToRelative(5.5f, 9f)
            horizontalLineTo(5.5f)
            close()
            moveTo(9.05f, 8f)
            horizontalLineToRelative(3.9f)
            lineTo(11f, 4.85f)
            lineTo(9.05f, 8f)
            close()
            moveTo(21.58f, 22.95f)
            lineTo(18.93f, 20.3f)
            quadToRelative(-0.52f, 0.35f, -1.14f, 0.52f)
            reflectiveQuadTo(16.5f, 21f)
            quadToRelative(-1.88f, 0f, -3.19f, -1.31f)
            reflectiveQuadTo(12f, 16.5f)
            reflectiveQuadToRelative(1.31f, -3.19f)
            reflectiveQuadTo(16.5f, 12f)
            reflectiveQuadToRelative(3.19f, 1.31f)
            reflectiveQuadTo(21f, 16.5f)
            quadToRelative(0f, 0.65f, -0.17f, 1.26f)
            reflectiveQuadToRelative(-0.5f, 1.14f)
            lineToRelative(2.65f, 2.65f)
            lineToRelative(-1.4f, 1.4f)
            close()
            moveToRelative(-3.3f, -4.68f)
            quadTo(19f, 17.55f, 19f, 16.5f)
            reflectiveQuadTo(18.28f, 14.73f)
            reflectiveQuadTo(16.5f, 14f)
            reflectiveQuadToRelative(-1.77f, 0.72f)
            reflectiveQuadTo(14f, 16.5f)
            reflectiveQuadToRelative(0.73f, 1.77f)
            reflectiveQuadTo(16.5f, 19f)
            reflectiveQuadToRelative(1.78f, -0.73f)
            close()
            moveTo(8f, 14.5f)
            close()
            moveTo(11f, 8f)
            close()
        }
    }.build()
}

val Md3CategorySearchFilled: ImageVector = Md3CategorySearchOutlined

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingBottomBar(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onTabLongClick: ((Int) -> Unit)? = null,
    isVisible: Boolean = true,
    isIncognito: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE) }
    var hideLabels by remember { mutableStateOf(prefs.getBoolean("hide_navigation_labels", false)) }
    var useClassicTaskbar by remember { mutableStateOf(prefs.getBoolean("use_classic_taskbar", false)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "hide_navigation_labels") {
                hideLabels = p.getBoolean("hide_navigation_labels", false)
            } else if (key == "use_classic_taskbar") {
                useClassicTaskbar = p.getBoolean("use_classic_taskbar", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val animProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "taskBarVisibility"
    )

    if (!isVisible && animProgress <= 0.001f) {
        return
    }

    val offsetY = lerp(90.dp, 0.dp, animProgress)
    val alpha = animProgress.coerceIn(0f, 1f)

    com.tkno.ren.ui.theme.RenTheme {
        Box(
            modifier =
                modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .offset(y = offsetY)
                    .alpha(alpha)
                    .padding(
                        bottom = if (useClassicTaskbar) 0.dp else 16.dp,
                        start = if (useClassicTaskbar) 0.dp else 22.dp,
                        end = if (useClassicTaskbar) 0.dp else 22.dp
                    ),
            contentAlignment = Alignment.Center,
        ) {
            // Pill Navigation Bar Dock / Classic Taskbar
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                shape = if (useClassicTaskbar) RoundedCornerShape(0.dp) else RoundedCornerShape(percent = 50),
                shadowElevation = if (useClassicTaskbar) 4.dp else 10.dp,
                modifier = Modifier.height(if (hideLabels) 54.dp else 64.dp).fillMaxWidth(),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val itemCount = 4
                        val slotWidth = maxWidth / itemCount

                        val clampedIndex = selectedTab.coerceIn(0, itemCount - 1)
                        val targetOffsetX = slotWidth * clampedIndex

                        val animatedOffsetX by animateDpAsState(
                            targetValue = targetOffsetX,
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "indicatorOffsetX"
                        )

                        // Smooth sliding halo/pill indicator background
                        Box(
                            modifier = Modifier
                                .offset(x = animatedOffsetX)
                                .width(slotWidth)
                                .fillMaxHeight()
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .clip(if (useClassicTaskbar) RoundedCornerShape(12.dp) else RoundedCornerShape(percent = 50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        )

                        // Tab buttons Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            val items =
                                listOf(
                                    Triple(0, Md3HomeOutlined, Md3HomeFilled),
                                    Triple(1, Md3TabOutlined, Md3TabFilled),
                                    Triple(2, Md3HistoryOutlined, Md3HistoryFilled),
                                    Triple(3, Md3CategorySearchOutlined, Md3CategorySearchFilled),
                                )

                            items.forEach { (index, outlineIcon, filledIcon) ->
                                val isSelected = selectedTab == index
                                val icon = if (isSelected) filledIcon else outlineIcon
                                val label =
                                    when (index) {
                                        0 -> if (isIncognito) "Incognito" else "Home"
                                        1 -> "Tabs"
                                        2 -> "History"
                                        else -> "Options"
                                    }

                                val iconTint by
                                    animateColorAsState(
                                        targetValue =
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        animationSpec = tween(250),
                                        label = "iconTint",
                                    )

                                val textColor by
                                    animateColorAsState(
                                        targetValue =
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        animationSpec = tween(250),
                                        label = "textColor",
                                    )

                                Box(
                                    modifier =
                                        Modifier.weight(1f).fillMaxHeight().combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                onTabSelect(index)
                                            },
                                            onLongClick = {
                                                onTabLongClick?.invoke(index)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                    ) {
                                        if (index == 0 && isIncognito) {
                                             Icon(
                                                painter = painterResource(id = R.drawable.ic_incognito_glasses),
                                                contentDescription = label,
                                                tint = iconTint,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = iconTint,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                        if (!hideLabels) {
                                            Text(
                                                text = label,
                                                style =
                                                    MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        fontSize = 11.sp,
                                                    ),
                                                color = textColor,
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
