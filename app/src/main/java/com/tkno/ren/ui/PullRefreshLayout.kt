package com.tkno.ren.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Custom touch-intercepting ViewGroup that wraps the active WebView and provides
 * smooth, native pull-to-refresh gestures.
 */
class PullRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val refreshTriggerDistancePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 72f, resources.displayMetrics
    )
    private val maxPullDistancePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 115f, resources.displayMetrics
    )

    var isPullToRefreshEnabled: Boolean = true
    var onRefresh: (() -> Unit)? = null
    var onPullProgress: ((progress: Float, offsetPx: Float) -> Unit)? = null

    private var isRefreshing: Boolean = false
    private var isBeingDragged: Boolean = false
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var activePointerId: Int = INVALID_POINTER_ID

    companion object {
        private const val INVALID_POINTER_ID = -1
        private const val DRAG_RESISTANCE = 0.48f
    }

    fun setRefreshing(refreshing: Boolean) {
        if (isRefreshing != refreshing) {
            isRefreshing = refreshing
            if (!refreshing) {
                onPullProgress?.invoke(0f, 0f)
            }
        }
    }

    private fun findTargetWebView(parent: ViewGroup = this): WebView? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is WebView && child.visibility == View.VISIBLE) {
                return child
            } else if (child is ViewGroup) {
                val nested = findTargetWebView(child)
                if (nested != null) return nested
            }
        }
        return null
    }

    private fun canChildScrollUp(): Boolean {
        val target = findTargetWebView() ?: return false
        return target.canScrollVertically(-1) || target.scrollY > 0
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isPullToRefreshEnabled || isRefreshing) {
            return false
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                downX = ev.getX(0)
                downY = ev.getY(0)
                isBeingDragged = false
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex < 0) return false

                val x = ev.getX(pointerIndex)
                val y = ev.getY(pointerIndex)
                val yDiff = y - downY
                val xDiff = abs(x - downX)

                if (!canChildScrollUp() && yDiff > touchSlop && yDiff > xDiff * 1.25f) {
                    downY = y
                    isBeingDragged = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBeingDragged = false
                activePointerId = INVALID_POINTER_ID
            }
        }

        return isBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isPullToRefreshEnabled || isRefreshing) {
            return super.onTouchEvent(ev)
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                isBeingDragged = false
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex < 0) return false

                val y = ev.getY(pointerIndex)
                val yDiff = y - downY

                if (!isBeingDragged && yDiff > touchSlop) {
                    isBeingDragged = true
                }

                if (isBeingDragged) {
                    val overscroll = (yDiff - touchSlop).coerceAtLeast(0f)
                    val currentOffset = (overscroll * DRAG_RESISTANCE).coerceAtMost(maxPullDistancePx)
                    val progress = (currentOffset / refreshTriggerDistancePx)
                    onPullProgress?.invoke(progress, currentOffset)
                    return true
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isBeingDragged) {
                    val pointerIndex = ev.findPointerIndex(activePointerId)
                    val y = if (pointerIndex >= 0) ev.getY(pointerIndex) else downY
                    val yDiff = y - downY
                    val currentOffset = ((yDiff - touchSlop).coerceAtLeast(0f) * DRAG_RESISTANCE).coerceAtMost(maxPullDistancePx)
                    val progress = (currentOffset / refreshTriggerDistancePx)

                    if (progress >= 1.0f && ev.actionMasked == MotionEvent.ACTION_UP) {
                        isRefreshing = true
                        onPullProgress?.invoke(1f, refreshTriggerDistancePx)
                        onRefresh?.invoke()
                    } else {
                        onPullProgress?.invoke(0f, 0f)
                    }

                    isBeingDragged = false
                    activePointerId = INVALID_POINTER_ID
                    return true
                }
            }
        }

        return super.onTouchEvent(ev)
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.actionIndex
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == activePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            if (newPointerIndex < ev.pointerCount) {
                downY = ev.getY(newPointerIndex)
                activePointerId = ev.getPointerId(newPointerIndex)
            }
        }
    }
}
