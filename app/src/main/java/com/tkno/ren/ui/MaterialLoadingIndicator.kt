package com.tkno.ren.ui

import android.graphics.Matrix as AndroidMatrix
import android.graphics.Path as AndroidPath
import android.graphics.RectF as AndroidRectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Official Material 3 Expressive Loading Indicator (Contained).
 * Implements Google's official M3 Expressive morphing polygon shapes:
 * (Circle, SoftBurst, Cookie9Sided, Pentagon, Pill, Sunny, Cookie4Sided)
 * Based on Android Developer specification:
 * https://developer.android.com/reference/kotlin/androidx/compose/material3/LoadingIndicator.composable
 */
@Composable
fun MaterialLoadingIndicatorOverlay(
    isRefreshing: Boolean,
    pullProgress: Float,
    pullOffsetPx: Float,
    statusBarHeightPx: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val restingOffsetPx = with(density) { 18.dp.toPx() }

    val animatedY = remember { Animatable(0f) }
    val isVisible = isRefreshing || pullOffsetPx > 0.5f || pullProgress > 0.05f

    LaunchedEffect(isRefreshing, pullOffsetPx) {
        if (isRefreshing) {
            animatedY.animateTo(
                targetValue = statusBarHeightPx + restingOffsetPx,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
            )
        } else if (pullOffsetPx > 0f) {
            animatedY.snapTo(statusBarHeightPx + pullOffsetPx)
        } else {
            animatedY.animateTo(
                targetValue = statusBarHeightPx.toFloat() - with(density) { 60.dp.toPx() },
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            )
        }
    }

    val targetScale = when {
        isRefreshing -> 1f
        pullProgress > 0f -> (0.35f + 0.65f * pullProgress.coerceIn(0f, 1f))
        else -> 0f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 180),
        label = "indicatorScale"
    )

    val targetAlpha = when {
        isRefreshing -> 1f
        pullProgress > 0f -> (pullProgress * 1.5f).coerceIn(0f, 1f)
        else -> 0f
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 180),
        label = "indicatorAlpha"
    )

    if (isVisible || animatedAlpha > 0.01f) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = animatedY.value.roundToInt()
                        )
                    }
                    .size(48.dp)
                    .scale(animatedScale)
                    .alpha(animatedAlpha)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Material3ExpressiveMorphingIndicator(
                        isRefreshing = isRefreshing,
                        pullProgress = pullProgress,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

/**
 * Authentic Material 3 Expressive Morphing Shapes Indicator.
 * Morphs seamlessly between the official sequence of rounded polygon shapes.
 */
@Composable
fun Material3ExpressiveMorphingIndicator(
    isRefreshing: Boolean,
    pullProgress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    // 1. Define the 7 Official Material 3 Expressive Shapes
    val m3Shapes = remember {
        val circleShape = RoundedPolygon.circle(numVertices = 16)
        val softBurstShape = RoundedPolygon.star(
            numVerticesPerRadius = 10,
            innerRadius = 0.72f,
            rounding = CornerRounding(0.25f)
        )
        val cookie9Shape = RoundedPolygon.star(
            numVerticesPerRadius = 9,
            innerRadius = 0.82f,
            rounding = CornerRounding(0.35f)
        )
        val pentagonShape = RoundedPolygon(
            numVertices = 5,
            rounding = CornerRounding(0.3f)
        )
        val pillShape = RoundedPolygon.pill(
            width = 1f,
            height = 0.56f,
            smoothing = 0.2f
        )
        val sunnyShape = RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.68f,
            rounding = CornerRounding(0.28f)
        )
        val cookie4Shape = RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.62f,
            rounding = CornerRounding(0.42f)
        )

        listOf(
            softBurstShape,
            cookie9Shape,
            pentagonShape,
            pillShape,
            sunnyShape,
            cookie4Shape,
            circleShape
        )
    }

    // 2. Pre-create Morph pairs for cyclic indeterminate animation
    val cyclicMorphs = remember(m3Shapes) {
        val list = mutableListOf<Morph>()
        for (i in m3Shapes.indices) {
            val current = m3Shapes[i]
            val next = m3Shapes[(i + 1) % m3Shapes.size]
            list.add(Morph(current, next))
        }
        list
    }

    // 3. Morph for pull gesture (Circle -> SoftBurst)
    val pullMorph = remember(m3Shapes) {
        val circle = RoundedPolygon.circle(numVertices = 16)
        val softBurst = RoundedPolygon.star(
            numVerticesPerRadius = 10,
            innerRadius = 0.72f,
            rounding = CornerRounding(0.25f)
        )
        Morph(circle, softBurst)
    }

    if (isRefreshing) {
        // Continuous Global Rotation: 360 degrees over 4600ms
        val infiniteTransition = rememberInfiniteTransition(label = "globalM3Rotation")
        val globalRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "globalRotation"
        )

        var currentMorphIndex by remember { mutableIntStateOf(0) }
        val morphProgress = remember { Animatable(0f) }
        var stepRotationTarget by remember { mutableFloatStateOf(0f) }

        // Shape morphing loop with spring physics & step rotation
        LaunchedEffect(isRefreshing) {
            val springSpec = spring<Float>(
                dampingRatio = 0.62f,
                stiffness = 220f,
                visibilityThreshold = 0.05f
            )

            while (true) {
                val deferred = async {
                    val result = morphProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = springSpec
                    )
                    if (result.endReason == androidx.compose.animation.core.AnimationEndReason.Finished) {
                        currentMorphIndex = (currentMorphIndex + 1) % cyclicMorphs.size
                        morphProgress.snapTo(0f)
                        stepRotationTarget = (stepRotationTarget + 90f) % 360f
                    }
                }
                delay(650L)
                deferred.await()
            }
        }

        val rawPath = remember { AndroidPath() }
        val transformedPath = remember { AndroidPath() }
        val bounds = remember { AndroidRectF() }
        val matrix = remember { AndroidMatrix() }

        val activeMorph = cyclicMorphs[currentMorphIndex]
        val progressVal = morphProgress.value
        val totalRotation = globalRotation + (progressVal * 90f) + stepRotationTarget

        Canvas(modifier = modifier) {
            rawPath.rewind()
            activeMorph.toPath(progress = progressVal, path = rawPath)

            @Suppress("DEPRECATION")
            rawPath.computeBounds(bounds, true)
            val shapeDimension = maxOf(bounds.width(), bounds.height()).coerceAtLeast(0.001f)
            val targetSize = size.minDimension * 0.92f
            val scaleFactor = targetSize / shapeDimension

            matrix.reset()
            matrix.postTranslate(-bounds.centerX(), -bounds.centerY())
            matrix.postScale(scaleFactor, scaleFactor)
            matrix.postRotate(totalRotation)
            matrix.postTranslate(size.width / 2f, size.height / 2f)

            transformedPath.rewind()
            rawPath.transform(matrix, transformedPath)

            drawPath(
                path = transformedPath.asComposePath(),
                color = color
            )
        }
    } else {
        // Determinate Pull State: Morph from Circle to SoftBurst with pull rotation
        val progress = pullProgress.coerceIn(0f, 1f)
        val rotation = progress * 180f

        val rawPath = remember { AndroidPath() }
        val transformedPath = remember { AndroidPath() }
        val bounds = remember { AndroidRectF() }
        val matrix = remember { AndroidMatrix() }

        Canvas(modifier = modifier) {
            rawPath.rewind()
            pullMorph.toPath(progress = progress, path = rawPath)

            @Suppress("DEPRECATION")
            rawPath.computeBounds(bounds, true)
            val shapeDimension = maxOf(bounds.width(), bounds.height()).coerceAtLeast(0.001f)
            val targetSize = size.minDimension * (0.75f + 0.20f * progress)
            val scaleFactor = targetSize / shapeDimension

            matrix.reset()
            matrix.postTranslate(-bounds.centerX(), -bounds.centerY())
            matrix.postScale(scaleFactor, scaleFactor)
            matrix.postRotate(rotation)
            matrix.postTranslate(size.width / 2f, size.height / 2f)

            transformedPath.rewind()
            rawPath.transform(matrix, transformedPath)

            drawPath(
                path = transformedPath.asComposePath(),
                color = color
            )
        }
    }
}
