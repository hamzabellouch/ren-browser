package com.tkno.ren.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.tkno.ren.R
import com.tkno.ren.util.HistoryItem
import com.tkno.ren.util.HistoryManager

class HistoryBottomSheet(
    context: Context,
    private val onOpenUrl: (String) -> Unit
) : Dialog(context) {

    private lateinit var contentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_bottom_sheet)
            setPadding(0, dp(16), 0, dp(20))
        }

        // Header: Title, Clear button, Close button
        val header = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
            setPadding(dp(20), 0, dp(16), dp(8))
        }

        // Title on Start
        val titleLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START or Gravity.CENTER_VERTICAL
            )
        }

        val historyIcon = ImageView(context).apply {
            val size = dp(22)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(10)
            }
            setImageResource(R.drawable.ic_history)
            setColorFilter(ContextCompat.getColor(context, R.color.accent_blue))
        }
        titleLayout.addView(historyIcon)

        val titleText = TextView(context).apply {
            text = "History"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
        titleLayout.addView(titleText)
        header.addView(titleLayout)

        // Actions on End (Clear All + Close)
        val actionsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END or Gravity.CENTER_VERTICAL
            )
        }

        val clearAllText = TextView(context).apply {
            text = "CLEAR"
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.icon_danger))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                HistoryManager.clearHistory(context)
                Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                refreshList()
            }
        }
        actionsLayout.addView(clearAllText)

        val closeBtn = ImageView(context).apply {
            val size = dp(32)
            layoutParams = LinearLayout.LayoutParams(size, size)
            val p = dp(6)
            setPadding(p, p, p, p)
            setImageResource(R.drawable.ic_close)
            setColorFilter(ContextCompat.getColor(context, R.color.icon_inactive))
            setOnClickListener {
                dismiss()
            }
        }
        actionsLayout.addView(closeBtn)

        header.addView(actionsLayout)
        root.addView(header)

        // Divider
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
            setBackgroundColor(ContextCompat.getColor(context, R.color.divider_color))
        }
        root.addView(divider)

        // Scrollable Content
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(340)
            )
            isFillViewport = true
        }

        contentContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(12))
        }

        scrollView.addView(contentContainer)
        root.addView(scrollView)

        setContentView(root)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setWindowAnimations(R.style.BottomSheetAnimation)
        }

        refreshList()
    }

    private fun refreshList() {
        contentContainer.removeAllViews()
        val historyList = HistoryManager.getHistory(context)

        if (historyList.isEmpty()) {
            val emptyLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(200)
                )
            }

            val emptyIcon = ImageView(context).apply {
                val size = dp(40)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    bottomMargin = dp(10)
                }
                setImageResource(R.drawable.ic_history)
                setColorFilter(ContextCompat.getColor(context, R.color.icon_dimmed))
            }
            emptyLayout.addView(emptyIcon)

            val emptyText = TextView(context).apply {
                text = "No browsing history"
                textSize = 15f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
            emptyLayout.addView(emptyText)
            contentContainer.addView(emptyLayout)
            return
        }

        for (item in historyList) {
            val row = createHistoryRow(item)
            contentContainer.addView(row)
        }
    }

    private fun createHistoryRow(item: HistoryItem): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
            setPadding(dp(4), dp(8), dp(4), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onOpenUrl(item.url)
                dismiss()
            }
        }

        // Site Favicon Icon Container
        val iconContainer = FrameLayout(context).apply {
            val size = dp(34)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(12)
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(ContextCompat.getColor(context, R.color.bg_surface_card))
            }
        }

        val siteIcon = ImageView(context).apply {
            val size = dp(20)
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        com.tkno.ren.util.FaviconManager.loadFavicon(context, siteIcon, item.url)
        iconContainer.addView(siteIcon)
        row.addView(iconContainer)

        // Title and URL text container
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleText = TextView(context).apply {
            text = item.title
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            maxLines = 1
        }
        textContainer.addView(titleText)

        val urlText = TextView(context).apply {
            text = item.url
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            maxLines = 1
        }
        textContainer.addView(urlText)

        row.addView(textContainer)

        // Delete single item button (X)
        val deleteBtn = ImageView(context).apply {
            val size = dp(28)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = dp(8)
            }
            val p = dp(5)
            setPadding(p, p, p, p)
            setImageResource(R.drawable.ic_close)
            setColorFilter(ContextCompat.getColor(context, R.color.icon_dimmed))
            setOnClickListener {
                HistoryManager.deleteHistoryItem(context, item.id)
                refreshList()
            }
        }
        row.addView(deleteBtn)

        return row
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
