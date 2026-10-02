package com.tkno.ren.ui

import android.app.Dialog
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
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tkno.ren.R
import com.tkno.ren.ui.theme.ThemeManager

class LinkContextMenuBottomSheet(
    context: Context,
    private val targetUrl: String? = null,
    private val linkText: String? = null,
    private val imageUrl: String? = null,
    private val onOpenInNewTab: ((String) -> Unit)? = null,
    private val onOpenInIncognito: ((String) -> Unit)? = null,
    private val onOpenInBackground: ((String) -> Unit)? = null,
    private val onPreviewPage: ((String) -> Unit)? = null,
    private val onCopyLinkAddress: ((String) -> Unit)? = null,
    private val onCopyLinkText: ((String) -> Unit)? = null,
    private val onDownloadLink: ((String) -> Unit)? = null,
    private val onShareLink: ((String) -> Unit)? = null,
    private val onOpenInReadingMode: ((String) -> Unit)? = null,
    private val onOpenImageInNewTab: ((String) -> Unit)? = null,
    private val onDownloadImage: ((String) -> Unit)? = null,
    private val onCopyImageAddress: ((String) -> Unit)? = null,
    private val onShareImage: ((String) -> Unit)? = null
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
        val sidePad = if (useClassicTaskbar) dp(12) else dp(20)

        val colorScheme = ThemeManager.getColorScheme(context)
        val taskbarBgColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.96f).toArgb()
        val taskbarBorderColor = colorScheme.outlineVariant.copy(alpha = 0.4f).toArgb()
        val onSurfaceColor = colorScheme.onSurface.toArgb()
        val onSurfaceVariantColor = colorScheme.onSurfaceVariant.toArgb()
        val primaryColor = colorScheme.primary.toArgb()

        cardView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = if (useClassicTaskbar) 0f else dp(24).toFloat()
                setColor(taskbarBgColor)
                if (!useClassicTaskbar) {
                    setStroke(dp(1).coerceAtLeast(1), taskbarBorderColor)
                }
            }
            elevation = dp(10).toFloat()
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnClickListener {
                // Consume click inside card
            }
            alpha = 0f
            translationY = dp(24).toFloat()
            scaleX = 0.95f
            scaleY = 0.95f
        }

        // Header Section
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }

        val headerIcon = ImageView(context).apply {
            val size = dp(22)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(10)
            }
            setImageResource(if (!imageUrl.isNullOrBlank() && targetUrl.isNullOrBlank()) R.drawable.ic_image else R.drawable.ic_link)
            setColorFilter(primaryColor)
        }
        headerLayout.addView(headerIcon)

        val titleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val displayTitle = when {
            !linkText.isNullOrBlank() -> linkText.trim()
            !targetUrl.isNullOrBlank() -> targetUrl
            !imageUrl.isNullOrBlank() -> imageUrl
            else -> context.getString(R.string.link)
        }

        val titleView = TextView(context).apply {
            text = displayTitle
            textSize = 14.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(onSurfaceColor)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        titleContainer.addView(titleView)

        val subUrl = when {
            !targetUrl.isNullOrBlank() && !linkText.isNullOrBlank() -> targetUrl
            !imageUrl.isNullOrBlank() && !targetUrl.isNullOrBlank() -> imageUrl
            else -> ""
        }

        if (subUrl.isNotBlank()) {
            val subUrlView = TextView(context).apply {
                text = subUrl
                textSize = 12f
                setTextColor(onSurfaceVariantColor)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.MIDDLE
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(2)
                }
            }
            titleContainer.addView(subUrlView)
        }
        headerLayout.addView(titleContainer)

        cardView.addView(headerLayout)

        // Divider
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1).coerceAtLeast(1)
            ).apply {
                bottomMargin = dp(6)
            }
            setBackgroundColor(taskbarBorderColor)
        }
        cardView.addView(divider)

        // Actions List in ScrollView
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }

        val actionsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val effectiveLinkUrl = targetUrl?.takeIf { it.isNotBlank() }
        val effectiveImageUrl = imageUrl?.takeIf { it.isNotBlank() }

        if (effectiveLinkUrl != null) {
            // Open in new tab
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_open_in_new,
                title = context.getString(R.string.open_in_new_tab),
                iconColor = primaryColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onOpenInNewTab?.invoke(effectiveLinkUrl) }
            })

            // Open in incognito tab
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_incognito_glasses,
                title = context.getString(R.string.open_in_incognito_tab),
                iconColor = onSurfaceVariantColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onOpenInIncognito?.invoke(effectiveLinkUrl) }
            })

            // Open in background
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_open_in_background,
                title = context.getString(R.string.open_in_background),
                iconColor = onSurfaceVariantColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onOpenInBackground?.invoke(effectiveLinkUrl) }
            })

            // Preview page
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_preview,
                title = context.getString(R.string.preview_page),
                iconColor = onSurfaceVariantColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onPreviewPage?.invoke(effectiveLinkUrl) }
            })

            // Copy link address
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_link,
                title = context.getString(R.string.copy_link_address),
                iconColor = onSurfaceVariantColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onCopyLinkAddress?.invoke(effectiveLinkUrl) }
            })

            // Copy link text (if meaningful)
            if (!linkText.isNullOrBlank() && linkText.trim() != effectiveLinkUrl.trim()) {
                actionsContainer.addView(createActionRow(
                    iconRes = R.drawable.ic_content_copy,
                    title = context.getString(R.string.copy_link_text),
                    iconColor = onSurfaceVariantColor,
                    textColor = onSurfaceColor
                ) {
                    dismissWithAnimation { onCopyLinkText?.invoke(linkText.trim()) }
                })
            }

            // Download link
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_downloads,
                title = context.getString(R.string.download_link),
                iconColor = onSurfaceVariantColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onDownloadLink?.invoke(effectiveLinkUrl) }
            })

            // Share link
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_share,
                title = context.getString(R.string.share_link),
                iconColor = onSurfaceVariantColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onShareLink?.invoke(effectiveLinkUrl) }
            })

            // Open in reading mode
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_reader_mode,
                title = context.getString(R.string.open_in_reading_mode),
                iconColor = onSurfaceVariantColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onOpenInReadingMode?.invoke(effectiveLinkUrl) }
            })
        }

        if (effectiveImageUrl != null) {
            if (effectiveLinkUrl != null) {
                // Section Divider for Image
                val imgSectionDivider = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1).coerceAtLeast(1)
                    ).apply {
                        topMargin = dp(6)
                        bottomMargin = dp(6)
                    }
                    setBackgroundColor(taskbarBorderColor)
                }
                actionsContainer.addView(imgSectionDivider)
            }

            // Open image in new tab
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_open_in_new,
                title = context.getString(R.string.open_image_in_new_tab),
                iconColor = primaryColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onOpenImageInNewTab?.invoke(effectiveImageUrl) }
            })

            // Download image
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_downloads,
                title = context.getString(R.string.download_image),
                iconColor = onSurfaceVariantColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onDownloadImage?.invoke(effectiveImageUrl) }
            })

            // Copy image address
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_image,
                title = context.getString(R.string.copy_image_address),
                iconColor = onSurfaceVariantColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onCopyImageAddress?.invoke(effectiveImageUrl) }
            })

            // Share image
            actionsContainer.addView(createActionRow(
                iconRes = R.drawable.ic_share,
                title = context.getString(R.string.share_image),
                iconColor = onSurfaceVariantColor,
                textColor = onSurfaceColor
            ) {
                dismissWithAnimation { onShareImage?.invoke(effectiveImageUrl) }
            })
        }

        scrollView.addView(actionsContainer)
        cardView.addView(scrollView)

        // Root Dialog Container
        // Root Dialog Container
        val rootLayout = object : FrameLayout(context) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                if (ev.action == MotionEvent.ACTION_DOWN) {
                    val rect = android.graphics.Rect()
                    cardView.getGlobalVisibleRect(rect)
                    if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                        dismissWithAnimation()
                        return true
                    }
                }
                return super.dispatchTouchEvent(ev)
            }
        }.apply {
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dismissWithAnimation()
            }
        }

        val cardLayoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
            marginStart = sidePad
            marginEnd = sidePad
            bottomMargin = baseBottomOffset
        }

        rootLayout.addView(cardView, cardLayoutParams)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime())
            val lp = cardView.layoutParams as? FrameLayout.LayoutParams
            if (lp != null) {
                lp.bottomMargin = baseBottomOffset + navInsets.bottom
                cardView.layoutParams = lp
            }
            insets
        }

        setContentView(rootLayout)
        setCanceledOnTouchOutside(true)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0f)
        }

        cardView.post {
            animateEntrance()
        }
    }

    private fun createActionRow(
        iconRes: Int,
        title: String,
        iconColor: Int,
        textColor: Int,
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
            )
            val colorScheme = ThemeManager.getColorScheme(context)
            background = createItemRippleDrawable(colorScheme.surfaceContainerHighest.toArgb())
            isClickable = true
            isFocusable = true
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { onClick() }

            val iconView = ImageView(context).apply {
                val size = dp(20)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(14)
                }
                setImageResource(iconRes)
                setColorFilter(iconColor)
            }
            addView(iconView)

            val textView = TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(textColor)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            addView(textView)
        }
    }

    private fun createItemRippleDrawable(highlightColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            setColor(Color.TRANSPARENT)
        }
    }

    private fun animateEntrance() {
        cardView.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator(1.6f))
            .start()
    }

    fun dismissWithAnimation(onEnd: (() -> Unit)? = null) {
        if (isDismissing) return
        isDismissing = true
        cardView.animate()
            .alpha(0f)
            .translationY(dp(24).toFloat())
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(160)
            .setInterpolator(AccelerateInterpolator(1.4f))
            .withEndAction {
                dismiss()
                onEnd?.invoke()
            }
            .start()
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
