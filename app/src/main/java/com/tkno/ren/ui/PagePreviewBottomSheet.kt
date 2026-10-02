package com.tkno.ren.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
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
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tkno.ren.R
import com.tkno.ren.ui.theme.ThemeManager
import com.tkno.ren.util.AdBlockManager
import com.tkno.ren.util.AntiFingerprintManager
import com.tkno.ren.util.SandboxManager
import com.tkno.ren.util.SiteConfigManager
import com.tkno.ren.util.UserAgentManager

class PagePreviewBottomSheet(
    context: Context,
    private val previewUrl: String,
    private val isIncognito: Boolean = false,
    private val isDarkTheme: Boolean = false,
    private val onOpenInTab: (String) -> Unit,
    private val onShare: ((String) -> Unit)? = null
) : Dialog(context) {

    private lateinit var cardView: LinearLayout
    private lateinit var previewWebView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var titleView: TextView
    private lateinit var hostView: TextView
    private var isDismissing = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val colorScheme = ThemeManager.getColorScheme(context)
        val bgColor = colorScheme.surfaceContainerHigh.toArgb()
        val borderColor = colorScheme.outlineVariant.copy(alpha = 0.4f).toArgb()
        val onSurfaceColor = colorScheme.onSurface.toArgb()
        val onSurfaceVariantColor = colorScheme.onSurfaceVariant.toArgb()
        val primaryColor = colorScheme.primary.toArgb()

        val parsedHost = try {
            Uri.parse(previewUrl).host ?: previewUrl
        } catch (_: Exception) {
            previewUrl
        }

        cardView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    dp(24).toFloat(), dp(24).toFloat(),
                    dp(24).toFloat(), dp(24).toFloat(),
                    0f, 0f,
                    0f, 0f
                )
                setColor(bgColor)
                setStroke(dp(1).coerceAtLeast(1), borderColor)
            }
            elevation = dp(16).toFloat()
            alpha = 0f
            translationY = dp(60).toFloat()
        }

        // Top Drag Handle
        val handleBar = View(context).apply {
            val w = dp(36)
            val h = dp(4)
            layoutParams = LinearLayout.LayoutParams(w, h).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(8)
                bottomMargin = dp(6)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(2).toFloat()
                setColor(onSurfaceVariantColor.let { Color.argb(80, Color.red(it), Color.green(it), Color.blue(it)) })
            }
        }
        cardView.addView(handleBar)

        // Header Action Bar
        val headerBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
            setPadding(dp(14), 0, dp(14), 0)
        }

        val closeBtn = ImageView(context).apply {
            val size = dp(32)
            val pad = dp(6)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(8)
            }
            setPadding(pad, pad, pad, pad)
            setImageResource(R.drawable.ic_close)
            setColorFilter(onSurfaceColor)
            background = createRippleDrawable(colorScheme.surfaceContainerHighest.toArgb())
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dismissWithAnimation()
            }
        }
        headerBar.addView(closeBtn)

        val titleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        titleView = TextView(context).apply {
            text = parsedHost
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(onSurfaceColor)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        titleContainer.addView(titleView)

        hostView = TextView(context).apply {
            text = previewUrl
            textSize = 11.5f
            setTextColor(onSurfaceVariantColor)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
        }
        titleContainer.addView(hostView)
        headerBar.addView(titleContainer)

        val shareBtn = ImageView(context).apply {
            val size = dp(32)
            val pad = dp(6)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(6)
            }
            setPadding(pad, pad, pad, pad)
            setImageResource(R.drawable.ic_share)
            setColorFilter(onSurfaceColor)
            background = createRippleDrawable(colorScheme.surfaceContainerHighest.toArgb())
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onShare?.invoke(previewWebView.url ?: previewUrl)
            }
        }
        headerBar.addView(shareBtn)

        val openInTabBtn = ImageView(context).apply {
            val size = dp(32)
            val pad = dp(6)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setPadding(pad, pad, pad, pad)
            setImageResource(R.drawable.ic_open_in_new)
            setColorFilter(primaryColor)
            background = createRippleDrawable(colorScheme.surfaceContainerHighest.toArgb())
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val finalUrl = previewWebView.url ?: previewUrl
                dismissWithAnimation {
                    onOpenInTab(finalUrl)
                }
            }
        }
        headerBar.addView(openInTabBtn)

        cardView.addView(headerBar)

        // Progress Bar
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(2)
            )
            max = 100
            progress = 10
            isIndeterminate = false
        }
        cardView.addView(progressBar)

        // Embedded WebView Container
        val webContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        previewWebView = WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = !isIncognito
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                allowFileAccess = false
            }

            UserAgentManager.applyToWebView(context, this)
            SiteConfigManager.applyToWebView(context, this)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                settings.isAlgorithmicDarkeningAllowed = isDarkTheme
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                settings.forceDark = if (isDarkTheme) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progressBar.progress = newProgress
                    progressBar.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (!title.isNullOrBlank()) {
                        titleView.text = title
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url ?: return false
                    val scheme = uri.scheme?.lowercase() ?: ""
                    return !(scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data")
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    progressBar.visibility = View.VISIBLE
                    if (!url.isNullOrBlank()) {
                        hostView.text = url
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.visibility = View.GONE
                    if (!url.isNullOrBlank()) {
                        hostView.text = url
                    }
                }
            }

            loadUrl(previewUrl)
        }
        webContainer.addView(previewWebView)
        cardView.addView(webContainer)

        // Root Dialog Layout
        val rootLayout = object : FrameLayout(context) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val y = event.y.toInt()
                    if (y < cardView.top) {
                        dismissWithAnimation()
                        return true
                    }
                }
                return super.onTouchEvent(event)
            }
        }.apply {
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val displayMetrics = context.resources.displayMetrics
        val sheetHeight = (displayMetrics.heightPixels * 0.82f).toInt()

        val cardLayoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            sheetHeight
        ).apply {
            gravity = Gravity.BOTTOM
        }

        rootLayout.addView(cardView, cardLayoutParams)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime())
            val lp = cardView.layoutParams as? FrameLayout.LayoutParams
            if (lp != null) {
                lp.bottomMargin = navInsets.bottom
                cardView.layoutParams = lp
            }
            insets
        }

        setContentView(rootLayout)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            setDimAmount(0.4f)
        }

        cardView.post {
            animateEntrance()
        }
    }

    private fun createRippleDrawable(highlightColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(Color.TRANSPARENT)
        }
    }

    private fun animateEntrance() {
        cardView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator(1.8f))
            .start()
    }

    fun dismissWithAnimation(onEnd: (() -> Unit)? = null) {
        if (isDismissing) return
        isDismissing = true
        cardView.animate()
            .alpha(0f)
            .translationY(dp(60).toFloat())
            .setDuration(180)
            .setInterpolator(AccelerateInterpolator(1.4f))
            .withEndAction {
                try {
                    previewWebView.stopLoading()
                    previewWebView.destroy()
                } catch (_: Exception) {}
                dismiss()
                onEnd?.invoke()
            }
            .start()
    }

    override fun dismiss() {
        try {
            previewWebView.stopLoading()
            previewWebView.destroy()
        } catch (_: Exception) {}
        super.dismiss()
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
