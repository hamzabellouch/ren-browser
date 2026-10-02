package com.tkno.ren.ui

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tkno.ren.R

class ClearDataDialog(
    context: Context,
    private val onConfirm: (Set<String>) -> Unit
) : Dialog(context) {

    data class ClearOption(
        val key: String,
        val label: String
    )

    companion object {
        private const val PREFS_NAME = "ren_browser_prefs"
        private const val KEY_SELECTED_OPTIONS = "clear_data_selected_options"

        val ALL_OPTIONS = listOf(
            ClearOption("cache", "Cache"),
            ClearOption("form_data", "Form data"),
            ClearOption("history", "History"),
            ClearOption("closed_tabs", "Closed tabs"),
            ClearOption("web_storage", "Web storage"),
            ClearOption("cookies", "Cookies (Login status)"),
            ClearOption("app_cache", "App cache")
        )

        private const val KEY_CLEAR_ON_EXIT_OPTIONS = "clear_on_exit_options"

        fun getSavedSelectedOptions(context: Context): Set<String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val defaultSet = ALL_OPTIONS.map { it.key }.toSet()
            return prefs.getStringSet(KEY_SELECTED_OPTIONS, defaultSet) ?: defaultSet
        }

        fun saveSelectedOptions(context: Context, options: Set<String>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet(KEY_SELECTED_OPTIONS, options).apply()
        }

        fun getClearOnExitOptions(context: Context): Set<String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getStringSet(KEY_CLEAR_ON_EXIT_OPTIONS, emptySet()) ?: emptySet()
        }

        fun saveClearOnExitOptions(context: Context, options: Set<String>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet(KEY_CLEAR_ON_EXIT_OPTIONS, options).apply()
        }
    }

    private val selectedKeys = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        selectedKeys.addAll(getSavedSelectedOptions(context))

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dialog_card)
            setPadding(dp(24), dp(24), dp(24), dp(20))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Title: "Clear data"
        val titleView = TextView(context).apply {
            text = "Clear data"
            textSize = 19.5f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, 0, 0, dp(14))
        }
        root.addView(titleView)

        // Options List
        for (option in ALL_OPTIONS) {
            val row = createOptionRow(option)
            root.addView(row)
        }

        // Action Buttons Row (Cancel, OK)
        val buttonBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
        }

        val cancelBtn = TextView(context).apply {
            text = "Cancel"
            textSize = 15.5f
            setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dismiss()
            }
        }
        buttonBar.addView(cancelBtn)

        val okBtn = TextView(context).apply {
            text = "OK"
            textSize = 15.5f
            setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
            setPadding(dp(12), dp(8), dp(4), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                saveSelectedOptions(context, selectedKeys)
                dismiss()
                onConfirm(selectedKeys)
            }
        }
        buttonBar.addView(okBtn)

        root.addView(buttonBar)

        setContentView(root)

        window?.apply {
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
        }
    }

    private fun createOptionRow(option: ClearOption): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dp(11), 0, dp(11))
            isClickable = true
            isFocusable = true

            val isSelected = selectedKeys.contains(option.key)

            val indicator = View(context).apply {
                val size = dp(18)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(16)
                }
                setBackgroundResource(
                    if (isSelected) R.drawable.bg_radio_selected
                    else R.drawable.bg_radio_unselected
                )
            }
            addView(indicator)

            val label = TextView(context).apply {
                text = option.label
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            addView(label)

            setOnClickListener {
                if (selectedKeys.contains(option.key)) {
                    selectedKeys.remove(option.key)
                    indicator.setBackgroundResource(R.drawable.bg_radio_unselected)
                } else {
                    selectedKeys.add(option.key)
                    indicator.setBackgroundResource(R.drawable.bg_radio_selected)
                }
            }
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
