package com.tkno.ren.ui

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tkno.ren.R
import com.tkno.ren.model.SettingItem
import com.tkno.ren.model.SettingType
import com.tkno.ren.util.AdBlockManager
import com.tkno.ren.util.FilterSubscription
import com.tkno.ren.util.UserAgentManager

class SettingsView(
    private val context: Context,
    private val onBackToBrowser: () -> Unit,
    private val onClearData: () -> Unit = {},
    private val onUserAgentChanged: () -> Unit = {},
    private val onOpenSiteConfig: (() -> Unit)? = null
) {

    enum class Screen {
        MAIN, GENERAL, USER_AGENT, NEW_USER_AGENT, AD_BLOCKING, CUSTOM_FILTERS, FILTER_SUBSCRIPTIONS, PRIVACY, ADVANCED, SCRIPTS, CUSTOMIZATION, ABOUT
    }

    var currentScreen: Screen = Screen.MAIN
        private set

    fun onBackPressed(): Boolean {
        return when (currentScreen) {
            Screen.NEW_USER_AGENT -> {
                showView(createUserAgentSettingsView())
                true
            }
            Screen.USER_AGENT -> {
                showView(createGeneralSettingsView())
                true
            }
            Screen.CUSTOM_FILTERS, Screen.FILTER_SUBSCRIPTIONS -> {
                showView(createAdBlockingSettingsView())
                true
            }
            Screen.AD_BLOCKING -> {
                showView(createGeneralSettingsView())
                true
            }
            Screen.GENERAL, Screen.PRIVACY, Screen.ADVANCED, Screen.SCRIPTS, Screen.CUSTOMIZATION, Screen.ABOUT -> {
                showView(createMainSettingsView())
                true
            }
            Screen.MAIN -> {
                false
            }
        }
    }

    fun createMainSettingsView(): View {
        currentScreen = Screen.MAIN
        val root = createBaseContainer()
        val header = createHeader("Settings", onBack = onBackToBrowser)
        root.addView(header)

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }

        val categories = listOf("General", "Customization", "Privacy", "Advanced", "Scripts", "About")
        for (category in categories) {
            val row = createSimpleRow(category) {
                when (category) {
                    "General" -> showView(createGeneralSettingsView())
                    "Privacy" -> showView(createPrivacySettingsView())
                    "Advanced" -> showView(createAdvancedSettingsView())
                    "Scripts" -> showView(createScriptsSettingsView())
                    "Customization" -> showView(createCustomizationSettingsView())
                    "About" -> showView(createAboutSettingsView())
                }
            }
            content.addView(row)
        }

        scrollView.addView(content)
        root.addView(scrollView)
        return root
    }

    private fun createGeneralSettingsView(): View {
        currentScreen = Screen.GENERAL
        val root = createBaseContainer()
        root.addView(createHeader("General") { showView(createMainSettingsView()) })

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }

        val selectedUaTitle = UserAgentManager.getSelectedTitle(context)

        val items = listOf(
            SettingItem(
                title = "User-agent",
                subtitle = selectedUaTitle,
                onClick = { showView(createUserAgentSettingsView()) }
            ),
            SettingItem("Clear data", onClick = onClearData),
            SettingItem(
                title = "Ad blocking",
                onClick = { showView(createAdBlockingSettingsView()) }
            ),
            SettingItem(
                title = "Site configuration",
                subtitle = "Manage permissions, cookies, DNS and JavaScript",
                onClick = { onOpenSiteConfig?.invoke() }
            ),
            SettingItem("Password manager"),
            SettingItem("Night mode"),
            SettingItem("Reader mode"),
            SettingItem("Toolbars settings"),
            SettingItem("Customize options"),
            SettingItem("Customize context menu"),
            SettingItem("Language", "English"),
            SettingItem("Homepage", "Default"),
            SettingItem("Search settings"),
            SettingItem("AI settings"),
            SettingItem("Orientation", "System"),
            SettingItem("Download location", "Ren"),
            SettingItem("Download manager", "Built-in download manager"),
            SettingItem("External video player", "System sharing"),
            SettingItem("Clear data on exit"),
            SettingItem("Font"),
            SettingItem("Gestures", "Gestures, shortcuts, function keys"),
            SettingItem("Import/export bookmarks"),
            SettingItem("Import data"),
            SettingItem("Export data", "Export bookmarks, settings, scripts and ad filters"),
            SettingItem("Restore tabs on startup", "Don't restore"),
            SettingItem(
                "Show toast to undo closing tab",
                "The toast will not be shown if incognito mode is on",
                SettingType.SWITCH,
                isChecked = false
            ),
            SettingItem("Set as default browser")
        )

        for (item in items) {
            content.addView(createSettingRow(item))
        }

        scrollView.addView(content)
        root.addView(scrollView)
        return root
    }

    fun createUserAgentSettingsView(): View {
        currentScreen = Screen.USER_AGENT
        val root = createBaseContainer()

        // Header with Back and +
        val header = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            )
            setPadding(dp(16), 0, dp(16), 0)
        }

        val leftContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START
            )
        }

        val backIcon = ImageView(context).apply {
            val size = dp(32)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(16)
            }
            val p = dp(4)
            setPadding(p, p, p, p)
            setImageResource(R.drawable.ic_chevron_left)
            setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
            setOnClickListener { showView(createGeneralSettingsView()) }
        }
        leftContainer.addView(backIcon)

        val titleText = TextView(context).apply {
            text = "User-agent"
            textSize = 21f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
        leftContainer.addView(titleText)
        header.addView(leftContainer)

        val addIcon = ImageView(context).apply {
            val size = dp(36)
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd = dp(4)
            }
            val p = dp(6)
            setPadding(p, p, p, p)
            setImageResource(R.drawable.ic_add)
            setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
            setOnClickListener {
                showView(createNewUserAgentView())
            }
        }
        header.addView(addIcon)

        root.addView(header)

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }

        val allUas = UserAgentManager.getAllUserAgents(context)
        val currentSelected = UserAgentManager.getSelectedTitle(context)

        for (item in allUas) {
            val isSelected = item.title.equals(currentSelected, ignoreCase = true)
            val row = createUserAgentRadioRow(item.title, isSelected) {
                UserAgentManager.setSelectedTitle(context, item.title)
                onUserAgentChanged()
                showView(createUserAgentSettingsView())
            }
            content.addView(row)
        }

        // Section "Advanced"
        val advancedTitle = TextView(context).apply {
            text = "Advanced"
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
            setPadding(dp(24), dp(24), dp(24), dp(10))
        }
        content.addView(advancedTitle)

        // Row: "User-agent in desktop mode"
        val desktopUaTitle = UserAgentManager.getDesktopModeTitle(context)
        val desktopRow = createSettingRow(
            SettingItem(
                title = "User-agent in desktop mode",
                subtitle = desktopUaTitle,
                onClick = {
                    showDesktopUaChooserDialog()
                }
            )
        )
        content.addView(desktopRow)

        // Row: "User-agent reduction"
        val isReduction = UserAgentManager.isReductionEnabled(context)
        val reductionItem = SettingItem(
            title = "User-agent reduction",
            subtitle = "Remove device information and minor version information from User-agent",
            type = SettingType.SWITCH,
            isChecked = isReduction,
            onToggle = { isChecked ->
                UserAgentManager.setReductionEnabled(context, isChecked)
                onUserAgentChanged()
            }
        )
        content.addView(createSettingRow(reductionItem))

        scrollView.addView(content)
        root.addView(scrollView)
        return root
    }

    private fun createUserAgentRadioRow(title: String, isSelected: Boolean, onClick: () -> Unit): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(24), dp(16), dp(24), dp(16))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }

            val radio = View(context).apply {
                val size = dp(18)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(20)
                }
                setBackgroundResource(
                    if (isSelected) R.drawable.bg_radio_selected
                    else R.drawable.bg_radio_unselected
                )
            }
            addView(radio)

            val textView = TextView(context).apply {
                text = title
                textSize = 16.5f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            addView(textView)
        }
    }

    fun createNewUserAgentView(): View {
        currentScreen = Screen.NEW_USER_AGENT
        val root = createBaseContainer()

        // Header with Back and SAVE
        val header = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            )
            setPadding(dp(16), 0, dp(16), 0)
        }

        val leftContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START
            )
        }

        val backIcon = ImageView(context).apply {
            val size = dp(32)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(16)
            }
            val p = dp(4)
            setPadding(p, p, p, p)
            setImageResource(R.drawable.ic_chevron_left)
            setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
            setOnClickListener { showView(createUserAgentSettingsView()) }
        }
        leftContainer.addView(backIcon)

        val titleText = TextView(context).apply {
            text = "New"
            textSize = 21f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
        leftContainer.addView(titleText)
        header.addView(leftContainer)

        val titleInput = EditText(context).apply {
            hint = "Title"
            setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 16.5f
            isSingleLine = true
            background = null
            setPadding(0, dp(14), 0, dp(14))
            imeOptions = EditorInfo.IME_ACTION_NEXT
        }

        val uaInput = EditText(context).apply {
            hint = "User-agent"
            setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 16.5f
            background = null
            setPadding(0, dp(14), 0, dp(14))
            imeOptions = EditorInfo.IME_ACTION_DONE
        }

        val saveBtn = TextView(context).apply {
            text = "SAVE"
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val title = titleInput.text.toString().trim()
                val ua = uaInput.text.toString().trim()

                if (title.isEmpty()) {
                    Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val finalUa = if (ua.isNotEmpty()) ua else title
                UserAgentManager.addCustomUserAgent(context, title, finalUa)
                UserAgentManager.setSelectedTitle(context, title)
                onUserAgentChanged()
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                showView(createUserAgentSettingsView())
            }
        }
        header.addView(saveBtn)

        root.addView(header)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(24), dp(8), dp(24), dp(24))
        }

        content.addView(titleInput)

        val divider1 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
            setBackgroundColor(ContextCompat.getColor(context, R.color.divider_color))
        }
        content.addView(divider1)

        content.addView(uaInput)

        val divider2 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
            setBackgroundColor(ContextCompat.getColor(context, R.color.divider_color))
        }
        content.addView(divider2)

        root.addView(content)
        return root
    }

    private fun showDesktopUaChooserDialog() {
        val options = listOf(
            "Windows (Chrome)",
            "Windows (IE 11)",
            "macOS",
            "Android (Tablet)",
            "iPad"
        )
        val current = UserAgentManager.getDesktopModeTitle(context)

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_bottom_sheet)
            setPadding(0, dp(16), 0, dp(16))
        }

        val dialogTitle = TextView(context).apply {
            text = "User-agent in desktop mode"
            textSize = 17.5f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(dp(24), dp(8), dp(24), dp(16))
        }
        container.addView(dialogTitle)

        for (opt in options) {
            val isSelected = opt.equals(current, ignoreCase = true)
            val row = createUserAgentRadioRow(opt, isSelected) {
                UserAgentManager.setDesktopModeTitle(context, opt)
                onUserAgentChanged()
                dialog.dismiss()
                showView(createUserAgentSettingsView())
            }
            container.addView(row)
        }

        dialog.setContentView(container)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    fun createAdBlockingSettingsView(): View {
        currentScreen = Screen.AD_BLOCKING
        val root = createBaseContainer()
        root.addView(createHeader("Ad blocking") { showView(createGeneralSettingsView()) })

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }

        // 1. Ad blocking switch
        val isAdBlockOn = AdBlockManager.isAdBlockEnabled(context)
        val statsSubtitle = AdBlockManager.getFormattedStatsSubtitle(context)
        val adBlockItem = SettingItem(
            title = "Ad blocking",
            subtitle = statsSubtitle,
            type = SettingType.SWITCH,
            isChecked = isAdBlockOn,
            onToggle = { isChecked ->
                AdBlockManager.setAdBlockEnabled(context, isChecked)
                showView(createAdBlockingSettingsView())
            }
        )
        content.addView(createSettingRow(adBlockItem))

        // 2. Custom filters
        val customFiltersItem = SettingItem(
            title = "Custom filters",
            subtitle = "Including custom filters and marked ads",
            onClick = { showView(createCustomFiltersView()) }
        )
        content.addView(createSettingRow(customFiltersItem))

        // 4. Filter subscriptions
        val filterSubsItem = SettingItem(
            title = "Filter subscriptions",
            subtitle = "Subscribe to Adblock Plus filter lists",
            onClick = { showView(createFilterSubscriptionsView()) }
        )
        content.addView(createSettingRow(filterSubsItem))

        // 5. Anti-adblock switch
        val isAntiAdblockOn = AdBlockManager.isAntiAdblockEnabled(context)
        val antiAdblockItem = SettingItem(
            title = "Anti-adblock",
            subtitle = "Prevent websites from detecting adblock and bypass anti-adblock popups",
            type = SettingType.SWITCH,
            isChecked = isAntiAdblockOn,
            onToggle = { isChecked ->
                AdBlockManager.setAntiAdblockEnabled(context, isChecked)
                showView(createAdBlockingSettingsView())
            }
        )
        content.addView(createSettingRow(antiAdblockItem))

        scrollView.addView(content)
        root.addView(scrollView)
        return root
    }

    fun createCustomFiltersView(): View {
        currentScreen = Screen.CUSTOM_FILTERS
        val root = createBaseContainer()

        // Header with Back, Title, HELP and SAVE
        val header = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            )
            setPadding(dp(16), 0, dp(16), 0)
        }

        val leftContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START
            )
        }

        val backIcon = ImageView(context).apply {
            val size = dp(32)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(16)
            }
            val p = dp(4)
            setPadding(p, p, p, p)
            setImageResource(R.drawable.ic_chevron_left)
            setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
            setOnClickListener { showView(createAdBlockingSettingsView()) }
        }
        leftContainer.addView(backIcon)

        val titleText = TextView(context).apply {
            text = "Custom filters"
            textSize = 21f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
        leftContainer.addView(titleText)
        header.addView(leftContainer)

        val rightContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END
            )
        }

        val helpBtn = TextView(context).apply {
            text = "HELP"
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                showCustomFiltersHelpDialog()
            }
        }
        rightContainer.addView(helpBtn)

        val saveBtn = TextView(context).apply {
            text = "SAVE"
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isClickable = true
            isFocusable = true
        }
        rightContainer.addView(saveBtn)
        header.addView(rightContainer)
        root.addView(header)

        val input = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            hint = "One filter per line."
            setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 15.5f
            gravity = Gravity.TOP or Gravity.START
            background = null
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setText(AdBlockManager.getCustomFilters(context))
        }
        root.addView(input)

        saveBtn.setOnClickListener {
            val text = input.text.toString()
            AdBlockManager.setCustomFilters(context, text)
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    private fun showCustomFiltersHelpDialog() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dialog_card)
            setPadding(dp(24), dp(20), dp(24), dp(20))
        }

        val title = TextView(context).apply {
            text = "Filter Syntax Help"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, 0, 0, dp(12))
        }
        container.addView(title)

        val content = TextView(context).apply {
            text = "Rules are applied line by line:\n\n" +
                    "• ||example.com^ : Blocks domains & subdomains\n" +
                    "• |https://example.com/ad.js| : Exact URL match\n" +
                    "• /ads/banner : URL keyword match\n" +
                    "• ! Comment line"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        container.addView(content)

        val okBtn = TextView(context).apply {
            text = "OK"
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
            setPadding(dp(16), dp(16), 0, 0)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { dialog.dismiss() }
        }
        container.addView(okBtn)

        dialog.setContentView(container)
        dialog.window?.apply {
            val width = (context.resources.displayMetrics.widthPixels * 0.88).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    fun createFilterSubscriptionsView(): View {
        currentScreen = Screen.FILTER_SUBSCRIPTIONS
        val root = createBaseContainer()

        // Header with Back, Title, + (Add), and UPDATE
        val header = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            )
            setPadding(dp(16), 0, dp(16), 0)
        }

        val leftContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START
            )
        }

        val backIcon = ImageView(context).apply {
            val size = dp(32)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(16)
            }
            val p = dp(4)
            setPadding(p, p, p, p)
            setImageResource(R.drawable.ic_chevron_left)
            setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
            setOnClickListener { showView(createAdBlockingSettingsView()) }
        }
        leftContainer.addView(backIcon)

        val titleText = TextView(context).apply {
            text = "Filter subscriptions"
            textSize = 21f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
        leftContainer.addView(titleText)
        header.addView(leftContainer)

        val rightContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END
            )
        }

        val addIcon = ImageView(context).apply {
            val size = dp(36)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(10)
            }
            val p = dp(6)
            setPadding(p, p, p, p)
            setImageResource(R.drawable.ic_add)
            setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
            setOnClickListener {
                showAddFilterSubscriptionDialog()
            }
        }
        rightContainer.addView(addIcon)

        val updateText = TextView(context).apply {
            text = "UPDATE"
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(dp(4), dp(8), dp(4), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                Toast.makeText(context, "Updating filter subscriptions...", Toast.LENGTH_SHORT).show()
                AdBlockManager.updateAllSubscriptions(
                    context,
                    onProgress = { cur, total -> },
                    onComplete = {
                        Toast.makeText(context, "Filter subscriptions updated", Toast.LENGTH_SHORT).show()
                        showView(createFilterSubscriptionsView())
                    }
                )
            }
        }
        rightContainer.addView(updateText)
        header.addView(rightContainer)
        root.addView(header)

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }

        // Update interval Row
        val currentInterval = AdBlockManager.getUpdateInterval(context)
        val intervalItem = SettingItem(
            title = "Update interval",
            subtitle = currentInterval,
            onClick = { showUpdateIntervalDialog() }
        )
        content.addView(createSettingRow(intervalItem))

        // Section Title: Filter subscriptions
        val sectionTitle = TextView(context).apply {
            text = "Filter subscriptions"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
            setPadding(dp(24), dp(18), dp(24), dp(8))
        }
        content.addView(sectionTitle)

        // Subscriptions List
        val subscriptions = AdBlockManager.getSubscriptions(context)
        for (sub in subscriptions) {
            val subItem = SettingItem(
                title = sub.title,
                subtitle = "${sub.filterCount} filters, ${sub.getFormattedUpdate()}",
                type = SettingType.SWITCH,
                isChecked = sub.isEnabled,
                onToggle = { isChecked ->
                    AdBlockManager.toggleSubscription(context, sub.id, isChecked)
                }
            )
            val rowView = createSettingRow(subItem)
            if (sub.isCustom) {
                rowView.setOnLongClickListener {
                    showDeleteSubscriptionDialog(sub)
                    true
                }
            }
            content.addView(rowView)
        }

        scrollView.addView(content)
        root.addView(scrollView)
        return root
    }

    private fun showAddFilterSubscriptionDialog() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dialog_card)
            setPadding(dp(24), dp(20), dp(24), dp(16))
        }

        val title = TextView(context).apply {
            text = "Add filter subscription"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, 0, 0, dp(16))
        }
        container.addView(title)

        val input = EditText(context).apply {
            setText("https://")
            setSelection(text.length)
            setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 15.5f
            isSingleLine = true
            background = null
            setPadding(0, dp(8), 0, dp(8))
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        container.addView(input)

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                bottomMargin = dp(16)
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.accent_blue))
        }
        container.addView(divider)

        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val cancelBtn = TextView(context).apply {
            text = "Cancel"
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { dialog.dismiss() }
        }
        buttonContainer.addView(cancelBtn)

        val okBtn = TextView(context).apply {
            text = "OK"
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
            setPadding(dp(12), dp(8), dp(4), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val url = input.text.toString().trim()
                if (url.isEmpty() || url == "https://" || url == "http://") {
                    Toast.makeText(context, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AdBlockManager.addSubscription(context, url)
                Toast.makeText(context, "Subscription added", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                showView(createFilterSubscriptionsView())
            }
        }
        buttonContainer.addView(okBtn)
        container.addView(buttonContainer)

        dialog.setContentView(container)
        dialog.window?.apply {
            val width = (context.resources.displayMetrics.widthPixels * 0.88).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun showDeleteSubscriptionDialog(subscription: FilterSubscription) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dialog_card)
            setPadding(dp(24), dp(20), dp(24), dp(16))
        }

        val title = TextView(context).apply {
            text = "Delete subscription?"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, 0, 0, dp(12))
        }
        container.addView(title)

        val desc = TextView(context).apply {
            text = subscription.title
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, 0, 0, dp(16))
        }
        container.addView(desc)

        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val cancelBtn = TextView(context).apply {
            text = "Cancel"
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { dialog.dismiss() }
        }
        buttonContainer.addView(cancelBtn)

        val deleteBtn = TextView(context).apply {
            text = "Delete"
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.icon_danger))
            setPadding(dp(12), dp(8), dp(4), dp(8))
            setOnClickListener {
                AdBlockManager.deleteSubscription(context, subscription.id)
                dialog.dismiss()
                showView(createFilterSubscriptionsView())
            }
        }
        buttonContainer.addView(deleteBtn)
        container.addView(buttonContainer)

        dialog.setContentView(container)
        dialog.window?.apply {
            val width = (context.resources.displayMetrics.widthPixels * 0.88).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun showUpdateIntervalDialog() {
        val options = listOf(
            "Never",
            "Every day",
            "Every 3 days",
            "Every week",
            "Every 15 days"
        )
        val current = AdBlockManager.getUpdateInterval(context)

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_bottom_sheet)
            setPadding(0, dp(16), 0, dp(16))
        }

        val dialogTitle = TextView(context).apply {
            text = "Update interval"
            textSize = 17.5f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(dp(24), dp(8), dp(24), dp(16))
        }
        container.addView(dialogTitle)

        for (opt in options) {
            val isSelected = opt.equals(current, ignoreCase = true)
            val row = createUserAgentRadioRow(opt, isSelected) {
                AdBlockManager.setUpdateInterval(context, opt)
                dialog.dismiss()
                showView(createFilterSubscriptionsView())
            }
            container.addView(row)
        }

        dialog.setContentView(container)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun createPrivacySettingsView(): View {
        currentScreen = Screen.PRIVACY
        val root = createBaseContainer()
        root.addView(createHeader("Privacy") { showView(createMainSettingsView()) })

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }

        val items = listOf(
            SettingItem("Do not track", "", SettingType.SWITCH, isChecked = false),
            SettingItem("Disable WebRTC", "", SettingType.SWITCH, isChecked = false),
            SettingItem(
                "Do not sell or share data",
                "Request the website not to sell or share my data to protect privacy (experimental)",
                SettingType.SWITCH,
                isChecked = false
            )
        )

        for (item in items) {
            content.addView(createSettingRow(item))
        }

        scrollView.addView(content)
        root.addView(scrollView)
        return root
    }

    private fun createAdvancedSettingsView(): View {
        currentScreen = Screen.ADVANCED
        val root = createBaseContainer()
        root.addView(createHeader("Advanced") { showView(createMainSettingsView()) })

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }

        val items = listOf(
            SettingItem("Save data", "Enable data-savings mode on compatible websites", SettingType.RADIO, isChecked = false),
            SettingItem("Show sniffer button automatically", "", SettingType.RADIO, isChecked = false),
            SettingItem("Enable webpage debugging", "", SettingType.RADIO, isChecked = false),
            SettingItem(
                "Disable Custom Tabs",
                "Skip redirect page when third-party apps open links via Custom Tabs",
                SettingType.SWITCH,
                isChecked = false
            ),
            SettingItem(
                "Disable safe browsing",
                "Safe browsing allows WebView to protect against malware and phishing attacks by verifying the links",
                SettingType.RADIO,
                isChecked = false
            ),
            SettingItem(
                "Ignore SSL certificate warnings",
                "Be careful, ignoring ssl certificate warnings may make connection to website insecure",
                SettingType.RADIO,
                isChecked = false
            )
        )

        for (item in items) {
            content.addView(createSettingRow(item))
        }

        scrollView.addView(content)
        root.addView(scrollView)
        return root
    }

    private fun createScriptsSettingsView(): View {
        currentScreen = Screen.SCRIPTS
        val root = createBaseContainer()

        // Custom Header for Scripts with + and UPDATE buttons
        val header = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            )
            setPadding(dp(16), 0, dp(16), 0)
        }

        val leftContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START
            )
        }

        val backIcon = ImageView(context).apply {
            val size = dp(32)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(16)
            }
            val p = dp(4)
            setPadding(p, p, p, p)
            setImageResource(R.drawable.ic_chevron_left)
            setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
            setOnClickListener { showView(createMainSettingsView()) }
        }
        leftContainer.addView(backIcon)

        val titleText = TextView(context).apply {
            text = "Scripts"
            textSize = 21f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
        leftContainer.addView(titleText)
        header.addView(leftContainer)

        // Right Action container (+ and UPDATE)
        val rightContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END
            )
        }

        val addIcon = ImageView(context).apply {
            val size = dp(36)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(12)
            }
            val p = dp(6)
            setPadding(p, p, p, p)
            setImageResource(R.drawable.ic_add)
            setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
        }
        rightContainer.addView(addIcon)

        val updateText = TextView(context).apply {
            text = "UPDATE"
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        rightContainer.addView(updateText)
        header.addView(rightContainer)

        root.addView(header)

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }

        // Enable scripts
        content.addView(createSettingRow(SettingItem("Enable scripts", "", SettingType.SWITCH, isChecked = false)))
        // Update interval
        content.addView(createSettingRow(SettingItem("Update interval", "Never")))

        // Scripts Section Title
        val sectionTitle = TextView(context).apply {
            text = "Scripts"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
            setPadding(dp(24), dp(18), dp(24), dp(8))
        }
        content.addView(sectionTitle)

        // Script Items
        val scripts = listOf(
            SettingItem("AdGuard Extra (Beta)", "1.1.38-beta.1", SettingType.SWITCH, isChecked = false),
            SettingItem("AdGuard Assistant (Beta)", "4.4.15-beta.0", SettingType.SWITCH, isChecked = false),
            SettingItem("AdGuard Popup Blocker (Beta)", "2.5.117-beta.1", SettingType.SWITCH, isChecked = false),
            SettingItem("Web of Trust (Beta)", "1.1.37-beta.1", SettingType.SWITCH, isChecked = false)
        )

        for (script in scripts) {
            content.addView(createSettingRow(script))
        }

        scrollView.addView(content)
        root.addView(scrollView)
        return root
    }

    private fun createCustomizationSettingsView(): View {
        currentScreen = Screen.CUSTOMIZATION
        val root = createBaseContainer()
        root.addView(createHeader("Customization") { showView(createMainSettingsView()) })

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }
        val items = listOf(
            SettingItem("Theme", "Pure Black (OLED)"),
            SettingItem("Background wallpaper", "Default"),
            SettingItem("Logo style", "Minimalist"),
            SettingItem("Search box style", "Rounded pill"),
            SettingItem("Toolbar layout", "Top (Default)")
        )
        for (item in items) {
            content.addView(createSettingRow(item))
        }
        scrollView.addView(content)
        root.addView(scrollView)
        return root
    }

    private fun createAboutSettingsView(): View {
        currentScreen = Screen.ABOUT
        val root = createBaseContainer()
        root.addView(createHeader("About") { showView(createMainSettingsView()) })

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }
        val items = listOf(
            SettingItem("Ren Browser", "Version 1.0.0"),
            SettingItem("Kernel", "Chromium WebView"),
            SettingItem("Architecture", "Kotlin Native"),
            SettingItem("Open source licenses")
        )
        for (item in items) {
            content.addView(createSettingRow(item))
        }
        scrollView.addView(content)
        root.addView(scrollView)
        return root
    }

    private fun createBaseContainer(): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.bg_black))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val statusBarSpacer = View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.bg_black))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0
            )
        }
        container.addView(statusBarSpacer, 0)

        ViewCompat.setOnApplyWindowInsetsListener(container) { _, insets ->
            val statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime())

            val spacerParams = statusBarSpacer.layoutParams as? LinearLayout.LayoutParams
            spacerParams?.let {
                it.height = statusInsets.top
                statusBarSpacer.layoutParams = it
            }

            container.setPadding(0, 0, 0, navInsets.bottom)
            insets
        }

        return container
    }

    private fun createHeader(title: String, onBack: () -> Unit): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            )
            setPadding(dp(16), 0, dp(16), 0)

            val backIcon = ImageView(context).apply {
                val size = dp(32)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(16)
                }
                val p = dp(4)
                setPadding(p, p, p, p)
                setImageResource(R.drawable.ic_chevron_left)
                setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
                setOnClickListener { onBack() }
            }
            addView(backIcon)

            val titleText = TextView(context).apply {
                text = title
                textSize = 21f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            addView(titleText)
        }
    }

    private fun createSimpleRow(title: String, onClick: () -> Unit): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(24), dp(18), dp(24), dp(18))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }

            val titleView = TextView(context).apply {
                text = title
                textSize = 17f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            addView(titleView)
        }
    }

    private fun createSettingRow(item: SettingItem): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(24), dp(14), dp(24), dp(14))
            isClickable = true
            isFocusable = true

            val textContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleView = TextView(context).apply {
                text = item.title
                textSize = 16.5f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            textContainer.addView(titleView)

            if (item.subtitle.isNotEmpty()) {
                val subtitleView = TextView(context).apply {
                    text = item.subtitle
                    textSize = 13.5f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    setPadding(0, dp(3), 0, 0)
                }
                textContainer.addView(subtitleView)
            }
            addView(textContainer)

            when (item.type) {
                SettingType.SWITCH -> {
                    val switch = SwitchCompat(context).apply {
                        isChecked = item.isChecked
                        thumbTintList = ColorStateList(
                            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                            intArrayOf(
                                ContextCompat.getColor(context, R.color.accent_blue),
                                ContextCompat.getColor(context, R.color.dot_inactive)
                            )
                        )
                        trackTintList = ColorStateList(
                            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                            intArrayOf(
                                ContextCompat.getColor(context, R.color.accent_blue),
                                ContextCompat.getColor(context, R.color.bg_pill)
                            )
                        )
                        setOnCheckedChangeListener { _, isChecked ->
                            item.isChecked = isChecked
                            item.onToggle?.invoke(isChecked)
                        }
                    }
                    addView(switch)
                    setOnClickListener { switch.toggle() }
                }
                SettingType.RADIO -> {
                    val indicator = View(context).apply {
                        val size = dp(18)
                        layoutParams = LinearLayout.LayoutParams(size, size).apply {
                            marginStart = dp(12)
                        }
                        setBackgroundResource(
                            if (item.isChecked) R.drawable.bg_radio_selected
                            else R.drawable.bg_radio_unselected
                        )
                    }
                    addView(indicator)
                    setOnClickListener {
                        item.isChecked = !item.isChecked
                        indicator.isSelected = item.isChecked
                        item.onToggle?.invoke(item.isChecked)
                    }
                }
                else -> {
                    setOnClickListener { item.onClick?.invoke() }
                }
            }
        }
    }

    private var currentViewSetter: ((View) -> Unit)? = null

    fun setViewChangeListener(setter: (View) -> Unit) {
        currentViewSetter = setter
    }

    private fun showView(view: View) {
        currentViewSetter?.invoke(view)
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
