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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tkno.ren.R
import com.tkno.ren.model.WebTab
import com.tkno.ren.util.FaviconManager

class TabsBottomSheet(
    context: Context,
    private val tabs: List<WebTab>,
    private val activeTabId: String,
    private val isIncognito: Boolean = false,
    private val onSelectTab: (WebTab) -> Unit,
    private val onCloseTab: (WebTab) -> Unit,
    private val onNewTab: (isIncognito: Boolean) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_bottom_sheet)
            setPadding(dp(20), dp(20), dp(20), dp(24))
        }

        // Tabs container
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isFillViewport = true
        }

        val tabsListLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        for (tab in tabs) {
            val isActive = tab.id == activeTabId
            val tabRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(48)
                )
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    onSelectTab(tab)
                    dismiss()
                }
            }

            // Globe or incognito or site icon
            val globeIcon = ImageView(context).apply {
                val size = dp(22)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(14)
                }
                if (tab.isIncognito) {
                    setImageResource(R.drawable.ic_incognito_glasses)
                    setColorFilter(
                        if (isActive) ContextCompat.getColor(context, R.color.accent_blue)
                        else ContextCompat.getColor(context, R.color.icon_inactive)
                    )
                } else if (tab.favicon != null) {
                    setImageBitmap(tab.favicon)
                } else {
                    FaviconManager.loadFavicon(context, this, tab.url)
                }
            }
            tabRow.addView(globeIcon)

            // Title
            val titleText = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = tab.title.ifBlank { "Blank page" }
                textSize = 15.5f
                setTextColor(
                    if (isActive) ContextCompat.getColor(context, R.color.accent_blue)
                    else ContextCompat.getColor(context, R.color.text_primary)
                )
                maxLines = 1
            }
            tabRow.addView(titleText)

            // Close button (X)
            val closeBtn = ImageView(context).apply {
                val size = dp(32)
                layoutParams = LinearLayout.LayoutParams(size, size)
                val p = dp(6)
                setPadding(p, p, p, p)
                setImageResource(R.drawable.ic_close)
                setColorFilter(ContextCompat.getColor(context, R.color.icon_inactive))
                setOnClickListener {
                    onCloseTab(tab)
                    dismiss()
                }
            }
            tabRow.addView(closeBtn)

            tabsListLayout.addView(tabRow)
        }

        scrollView.addView(tabsListLayout)
        root.addView(scrollView)

        // Center Plus (+) Button to Add Tab
        val plusBtnLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(8)
            }
        }

        val plusIcon = ImageView(context).apply {
            val size = dp(44)
            layoutParams = LinearLayout.LayoutParams(size, size)
            val p = dp(8)
            setPadding(p, p, p, p)
            setImageResource(R.drawable.ic_add)
            setColorFilter(ContextCompat.getColor(context, R.color.icon_inactive))
            setOnClickListener {
                onNewTab(isIncognito)
                dismiss()
            }
        }
        plusBtnLayout.addView(plusIcon)
        root.addView(plusBtnLayout)

        setContentView(root)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setWindowAnimations(R.style.BottomSheetAnimation)
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
