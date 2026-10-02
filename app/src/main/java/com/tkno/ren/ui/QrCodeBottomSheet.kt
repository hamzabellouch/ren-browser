package com.tkno.ren.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tkno.ren.R
import com.tkno.ren.ui.theme.ThemeManager
import com.tkno.ren.util.QrCodeGenerator

class QrCodeBottomSheet(
    context: Context,
    private val url: String,
    private val pageTitle: String? = null,
    private val onShare: (() -> Unit)? = null
) : Dialog(context) {

    private lateinit var cardView: LinearLayout
    private var isDismissing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val prefs = context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE)
        val useClassicTaskbar = prefs.getBoolean("use_classic_taskbar", false)
        val hideLabels = prefs.getBoolean("hide_navigation_labels", false)

        val taskbarHeight = if (hideLabels) dp(54) else dp(64)
        val taskbarBottomMargin = if (useClassicTaskbar) 0 else dp(16)
        val gap = dp(10)
        val baseBottomOffset = taskbarBottomMargin + taskbarHeight + gap
        val sidePad = if (useClassicTaskbar) dp(12) else dp(22)

        val colorScheme = ThemeManager.getColorScheme(context)
        val taskbarBgColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.94f).toArgb()
        val taskbarBorderColor = colorScheme.outlineVariant.copy(alpha = 0.4f).toArgb()
        val onSurfaceColor = colorScheme.onSurface.toArgb()
        val onSurfaceVariantColor = colorScheme.onSurfaceVariant.toArgb()
        val primaryColor = colorScheme.primary.toArgb()

        cardView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = if (useClassicTaskbar) 0f else dp(28).toFloat()
                setColor(taskbarBgColor)
                if (!useClassicTaskbar) {
                    setStroke(dp(1).coerceAtLeast(1), taskbarBorderColor)
                }
            }
            elevation = dp(10).toFloat()
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setOnClickListener {
                // Consume click inside card
            }
            alpha = 0f
            translationY = dp(24).toFloat()
            scaleX = 0.94f
            scaleY = 0.94f
        }

        // Header Row
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(32)
            )
        }

        val qrIcon = ImageView(context).apply {
            val size = dp(20)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(8)
            }
            setImageResource(R.drawable.ic_scan_qr_code)
            setColorFilter(primaryColor)
        }
        headerLayout.addView(qrIcon)

        val titleView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text = "QR Code"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(onSurfaceColor)
        }
        headerLayout.addView(titleView)

        // Share button
        if (onShare != null && url.isNotBlank() && url != "about:blank") {
            val shareBtn = ImageView(context).apply {
                val size = dp(28)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(6)
                }
                setPadding(dp(4), dp(4), dp(4), dp(4))
                setImageResource(R.drawable.ic_share)
                setColorFilter(onSurfaceVariantColor)
                val outValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
                setBackgroundResource(outValue.resourceId)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    dismiss()
                    onShare.invoke()
                }
            }
            headerLayout.addView(shareBtn)
        }

        // Close button
        val closeBtn = ImageView(context).apply {
            val size = dp(28)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setImageResource(R.drawable.ic_close)
            setColorFilter(onSurfaceVariantColor)
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dismiss()
            }
        }
        headerLayout.addView(closeBtn)

        cardView.addView(headerLayout)

        // Center Content with QR Code Card
        val centerContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(152)
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(6)
            }
        }

        val qrCodeContainer = FrameLayout(context).apply {
            val containerSize = dp(142)
            layoutParams = LinearLayout.LayoutParams(containerSize, containerSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(Color.WHITE)
            }
            elevation = dp(2).toFloat()
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        val qrImageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        qrCodeContainer.addView(qrImageView)
        centerContent.addView(qrCodeContainer)
        cardView.addView(centerContent)

        // Generate QR code asynchronously / on card creation
        val targetUrl = url.ifBlank { "about:blank" }
        val qrBitmap = QrCodeGenerator.generateQrCode(
            content = targetUrl,
            size = 512,
            foregroundColor = Color.BLACK,
            backgroundColor = Color.WHITE
        )
        if (qrBitmap != null) {
            qrImageView.setImageBitmap(qrBitmap)
        }

        // Footer / URL string (Click to copy)
        val urlView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            text = if (url.isBlank() || url == "about:blank") "Blank page" else url
            textSize = 12f
            setTextColor(onSurfaceVariantColor)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
            setPadding(dp(12), dp(2), dp(12), dp(4))
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (url.isNotBlank() && url != "about:blank") {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("URL", url)
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            }
        }
        cardView.addView(urlView)

        val container = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(sidePad, 0, sidePad, 0)
        }

        val cardParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        container.addView(cardView, cardParams)

        setContentView(container)

        val navInsetsBottom = (context as? android.app.Activity)?.window?.decorView?.let {
            ViewCompat.getRootWindowInsets(it)?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom
        } ?: 0
        val totalBottomOffset = baseBottomOffset + navInsetsBottom

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            val lp = attributes
            lp.y = totalBottomOffset
            attributes = lp
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0f)
            setWindowAnimations(0)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
            addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            setCanceledOnTouchOutside(true)
        }

        // Entrance animation
        cardView.post {
            cardView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator(1.8f))
                .start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_OUTSIDE) {
            dismiss()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun dismiss() {
        if (isDismissing) return
        isDismissing = true
        if (::cardView.isInitialized) {
            cardView.animate()
                .alpha(0f)
                .translationY(dp(20).toFloat())
                .scaleX(0.94f)
                .scaleY(0.94f)
                .setDuration(160)
                .setInterpolator(AccelerateInterpolator(1.5f))
                .withEndAction {
                    super.dismiss()
                }
                .start()
        } else {
            super.dismiss()
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
