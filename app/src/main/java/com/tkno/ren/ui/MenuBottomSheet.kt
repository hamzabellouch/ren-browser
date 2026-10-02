package com.tkno.ren.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.content.res.Configuration
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.tkno.ren.R
import com.tkno.ren.model.BrowserMenuItem
import com.tkno.ren.ui.theme.ThemeManager
import androidx.compose.ui.graphics.toArgb
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.tkno.ren.util.AdBlockManager
import com.tkno.ren.util.SandboxManager
import com.tkno.ren.util.ScriptManager
import com.tkno.ren.util.TorManager
import com.tkno.ren.util.TranslationManager
import com.tkno.ren.util.UserAgentManager

class MenuBottomSheet(
    context: Context,
    private val onOpenSettings: (() -> Unit)? = null,
    private val onOpenMenu: (() -> Unit)? = null,
    private val onReload: (() -> Unit)? = null,
    private val onExit: () -> Unit,
    private val onClearData: () -> Unit = {},
    private val onOpenHistory: (() -> Unit)? = null,
    private val onToggleUserAgent: ((Boolean) -> Unit)? = null,
    private val onOpenUserAgentSettings: (() -> Unit)? = null,
    private val onOpenAdBlockingSettings: (() -> Unit)? = null,
    private val onOpenTorSettings: (() -> Unit)? = null,
    private val onToggleTor: ((Boolean) -> Unit)? = null,
    private val isTorActive: Boolean = TorManager.isTorEnabled(context),
    private val onToggleIncognito: (() -> Unit)? = null,
    private val isIncognitoActive: Boolean = false,
    private val isDesktopSiteActive: Boolean = false,
    private val onToggleDesktopSite: ((Boolean) -> Unit)? = null,
    private val onToggleNightMode: ((Boolean) -> Unit)? = null,
    private val isOrientationActive: Boolean = false,
    private val onToggleOrientation: ((Boolean) -> Unit)? = null,
    private val onShare: (() -> Unit)? = null,
    private val onToggleSandbox: ((Boolean) -> Unit)? = null,
    private val onOpenSandboxSettings: (() -> Unit)? = null,
    private val isSandboxActive: Boolean = SandboxManager.isSandboxEnabled(context),
    private val onOpenDownloads: (() -> Unit)? = null,
    private val onFindInPage: (() -> Unit)? = null,
    private val onSavePage: (() -> Unit)? = null,
    private val onPrint: (() -> Unit)? = null,
    private val isReaderModeActive: Boolean = false,
    private val onToggleReaderMode: (() -> Unit)? = null,
    private val onOpenSiteConfig: (() -> Unit)? = null,
    private val onOpenScriptsSettings: (() -> Unit)? = null,
    private val onToggleScripts: ((Boolean) -> Unit)? = null,
    private val onOpenQrCode: (() -> Unit)? = null,
    private val onTextSizeChanged: ((Int) -> Unit)? = null,
    private val onTranslatePage: ((String) -> Unit)? = null,
    private val onRestoreOriginalPage: (() -> Unit)? = null
) : Dialog(context) {

    private lateinit var cardView: LinearLayout
    private var isDismissing = false
    private var selectedSwapIndex = -1

    private val isUaActive = UserAgentManager.isUserAgentEnabled(context) && 
            !UserAgentManager.getSelectedTitle(context).equals("Default", ignoreCase = true)
    private val isAdBlockActive = AdBlockManager.isAdBlockEnabled(context)
    private val isScriptsActive = ScriptManager.isScriptsEnabled(context)

    private val isNightModeActive: Boolean
        get() {
            val prefs = context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE)
            val darkThemePref = prefs.getInt("dark_theme", 0)
            val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            return when (darkThemePref) {
                1 -> true
                2 -> false
                else -> isSystemDark
            }
        }

    companion object {
        private const val MENU_PREFS = "links_prefs"
        private const val KEY_MENU_ORDER = "menu_custom_items_order"

        private val DEFAULT_MENU_ORDER = listOf(
            "incognito", "menu", "ad_blocking", "tor_network", "extensions", "sandbox",
            "clear_data", "exit", "share", "scripts", "add_favorite", "downloads",
            "user_agent", "find_in_page", "translate", "desktop_site", "night_mode",
            "save", "qr_code", "add_to_home", "orientation", "text_size",
            "customize_menu", "site_config", "reader_mode", "print", "open_with"
        )

        fun getSavedMenuOrder(context: Context): List<String> {
            val prefs = context.getSharedPreferences(MENU_PREFS, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_MENU_ORDER, null) ?: return DEFAULT_MENU_ORDER
            return try {
                val array = org.json.JSONArray(saved)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                for (item in DEFAULT_MENU_ORDER) {
                    if (!list.contains(item)) {
                        list.add(item)
                    }
                }
                list
            } catch (e: Exception) {
                DEFAULT_MENU_ORDER
            }
        }

        fun saveMenuOrder(context: Context, order: List<String>) {
            val prefs = context.getSharedPreferences(MENU_PREFS, Context.MODE_PRIVATE)
            val array = org.json.JSONArray(order)
            prefs.edit().putString(KEY_MENU_ORDER, array.toString()).apply()
        }
    }

    private fun getSortedMenuItems(useDefaultOrder: Boolean = false): MutableList<BrowserMenuItem> {
        val baseItems = mapOf(
            "incognito" to BrowserMenuItem("incognito", "Incognito\nmode", R.drawable.ic_incognito_glasses, isSelected = isIncognitoActive),
            "menu" to BrowserMenuItem("menu", "Menu", R.drawable.ic_menu, isTogglable = false),
            "ad_blocking" to BrowserMenuItem("ad_blocking", "Ad blocking", R.drawable.ic_ad_blocking, isSelected = isAdBlockActive),
            "tor_network" to BrowserMenuItem("tor_network", "Tor network", R.drawable.ic_tor_network, isSelected = isTorActive),
            "extensions" to BrowserMenuItem("extensions", "Extensions", R.drawable.ic_extension, isTogglable = false, isEnabled = false),
            "sandbox" to BrowserMenuItem("sandbox", "Sandbox", R.drawable.ic_sandbox, isSelected = isSandboxActive),
            "clear_data" to BrowserMenuItem("clear_data", "Clear data", R.drawable.ic_clear_data),
            "exit" to BrowserMenuItem("exit", "Exit", R.drawable.ic_power, isTogglable = false),
            "share" to BrowserMenuItem("share", "Share", R.drawable.ic_share),
            "scripts" to BrowserMenuItem("scripts", "Scripts", R.drawable.ic_scripts, isSelected = isScriptsActive),
            "add_favorite" to BrowserMenuItem("add_favorite", "Favorite", R.drawable.ic_add_favorite),
            "downloads" to BrowserMenuItem("downloads", "Downloads", R.drawable.ic_downloads),
            "user_agent" to BrowserMenuItem("user_agent", "User-agent", R.drawable.ic_user_agent, isSelected = isUaActive),
            "find_in_page" to BrowserMenuItem("find_in_page", "Find in\npage", R.drawable.ic_find_in_page),
            "translate" to BrowserMenuItem("translate", "Translate", R.drawable.ic_translate),
            "desktop_site" to BrowserMenuItem("desktop_site", "Desktop\nsite", R.drawable.ic_desktop_site, isSelected = isDesktopSiteActive),
            "night_mode" to BrowserMenuItem("night_mode", "Night mode", R.drawable.ic_night_mode, isSelected = isNightModeActive),
            "save" to BrowserMenuItem("save", "Save", R.drawable.ic_save),
            "qr_code" to BrowserMenuItem("qr_code", "QR Code", R.drawable.ic_scan_qr_code),
            "add_to_home" to BrowserMenuItem("add_to_home", "Add to\nhome scre...", R.drawable.ic_add_to_home),
            "orientation" to BrowserMenuItem("orientation", "Orientation", R.drawable.ic_orientation, isSelected = isOrientationActive),
            "text_size" to BrowserMenuItem("text_size", "Text size", R.drawable.ic_text_size),
            "customize_menu" to BrowserMenuItem("customize_menu", "Customize\noptions", R.drawable.ic_customize_menu),
            "site_config" to BrowserMenuItem("site_config", "Site configu\nration", R.drawable.ic_site_config),
            "reader_mode" to BrowserMenuItem("reader_mode", "Reader\nmode", R.drawable.ic_reader_mode, isSelected = isReaderModeActive),
            "print" to BrowserMenuItem("print", "Print/PDF", R.drawable.ic_print),
            "open_with" to BrowserMenuItem("open_with", "Open with", R.drawable.ic_open_with)
        )

        val order = if (useDefaultOrder) DEFAULT_MENU_ORDER else getSavedMenuOrder(context)
        val result = mutableListOf<BrowserMenuItem>()
        for (id in order) {
            baseItems[id]?.let { result.add(it) }
        }
        for ((id, item) in baseItems) {
            if (result.none { it.id == id }) {
                result.add(item)
            }
        }
        return result
    }

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

        // Floating Card positioned directly above the taskbar matching Taskbar's theme
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
            setPadding(0, dp(16), 0, dp(4))
            setOnClickListener {
                // Consume click so tapping inside the card does not dismiss
            }
            // Initial animation state: emerge right from above the taskbar smoothly
            alpha = 0f
            translationY = dp(24).toFloat()
            scaleX = 0.94f
            scaleY = 0.94f
        }

        showNormalMenuView()

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

        // Trigger entrance animation starting smoothly from the taskbar
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

    private fun showNormalMenuView() {
        cardView.removeAllViews()
        cardView.setPadding(0, dp(16), 0, dp(4))

        val sortedItems = getSortedMenuItems()
        val pagesData = sortedItems.chunked(8)

        // ViewPager for pages
        val viewPager = ViewPager2(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(190)
            )
            adapter = MenuPagerAdapter(
                pages = pagesData,
                onItemClick = { item -> handleItemClick(item) },
                onItemLongClick = { item ->
                    if (item.id == "user_agent") {
                        dismiss()
                        onOpenUserAgentSettings?.invoke()
                    } else if (item.id == "ad_blocking") {
                        dismiss()
                        onOpenAdBlockingSettings?.invoke()
                    } else if (item.id == "tor_network") {
                        dismiss()
                        onOpenTorSettings?.invoke()
                    } else if (item.id == "sandbox") {
                        dismiss()
                        onOpenSandboxSettings?.invoke()
                    } else if (item.id == "scripts") {
                        dismiss()
                        onOpenScriptsSettings?.invoke()
                    }
                }
            )
        }
        cardView.addView(viewPager)

        // Indicator dots layout
        val dotsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(14))
        }

        val dotViews = mutableListOf<View>()
        for (i in pagesData.indices) {
            val dot = View(context).apply {
                val size = dp(6)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                background = createDotDrawable(i == 0)
            }
            dotViews.add(dot)
            dotsLayout.addView(dot)
        }
        cardView.addView(dotsLayout)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                for (i in dotViews.indices) {
                    dotViews[i].background = createDotDrawable(i == position)
                }
            }
        })
    }

    private fun showCustomizeMenuView() {
        cardView.removeAllViews()
        cardView.setPadding(dp(16), dp(16), dp(16), dp(16))

        val colorScheme = ThemeManager.getColorScheme(context)
        val onSurfaceColor = colorScheme.onSurface.toArgb()
        val onSurfaceVariantColor = colorScheme.onSurfaceVariant.toArgb()
        val primaryColor = colorScheme.primary.toArgb()
        val onPrimaryColor = colorScheme.onPrimary.toArgb()
        val surfaceContainerHighColor = colorScheme.surfaceContainerHigh.toArgb()
        val outlineVariantColor = colorScheme.outlineVariant.copy(alpha = 0.45f).toArgb()

        val currentCustomItems = getSortedMenuItems().toMutableList()

        // Header
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val titleText = TextView(context).apply {
            text = "Customize options"
            textSize = 17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(onSurfaceColor)
        }
        headerLayout.addView(titleText)

        val subtitleText = TextView(context).apply {
            text = "Press and hold to drag and rearrange options"
            textSize = 12f
            setTextColor(onSurfaceVariantColor)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(2)
            }
            layoutParams = lp
        }
        headerLayout.addView(subtitleText)

        cardView.addView(headerLayout)

        // Scrollable Grid of All Options using RecyclerView with ItemTouchHelper
        val displayMetrics = context.resources.displayMetrics
        val maxScrollHeight = (displayMetrics.heightPixels * 0.46f).toInt().coerceAtMost(dp(360))

        val recyclerView = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                maxScrollHeight
            )
            layoutManager = GridLayoutManager(context, 4)
            isVerticalScrollBarEnabled = true
            setPadding(0, 0, 0, dp(4))
            clipToPadding = false
        }

        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemView = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    setPadding(dp(2), dp(6), dp(2), dp(6))
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(82)
                    )
                    val outValue = TypedValue()
                    context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
                    background = ContextCompat.getDrawable(context, outValue.resourceId)
                }

                val icon = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
                }
                itemView.addView(icon)

                val text = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(6)
                    }
                    textSize = 11.5f
                    gravity = Gravity.CENTER
                    maxLines = 2
                }
                itemView.addView(text)

                return object : RecyclerView.ViewHolder(itemView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = currentCustomItems[position]
                val itemLayout = holder.itemView as LinearLayout
                val icon = itemLayout.getChildAt(0) as ImageView
                val text = itemLayout.getChildAt(1) as TextView

                val iconColor = if (item.isSelected) {
                    ContextCompat.getColor(context, R.color.accent_blue)
                } else {
                    ContextCompat.getColor(context, R.color.icon_inactive)
                }

                icon.setImageResource(item.iconRes)
                icon.setColorFilter(iconColor)

                text.text = item.label
                val textColor = if (item.isSelected) iconColor else onSurfaceVariantColor
                text.setTextColor(textColor)
            }

            override fun getItemCount(): Int = currentCustomItems.size
        }

        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                if (fromPos != RecyclerView.NO_POSITION && toPos != RecyclerView.NO_POSITION && fromPos != toPos) {
                    java.util.Collections.swap(currentCustomItems, fromPos, toPos)
                    adapter.notifyItemMoved(fromPos, toPos)
                    return true
                }
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.apply {
                        animate()
                            .scaleX(1.1f)
                            .scaleY(1.1f)
                            .alpha(0.92f)
                            .setDuration(120)
                            .start()
                        elevation = dp(8).toFloat()
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dp(14).toFloat()
                            setColor(surfaceContainerHighColor)
                            setStroke(dp(1.5f.toInt().coerceAtLeast(1)), primaryColor)
                        }
                    }
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.apply {
                    animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(120)
                        .start()
                    elevation = 0f
                    val outValue = TypedValue()
                    context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
                    background = ContextCompat.getDrawable(context, outValue.resourceId)
                }
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        cardView.addView(recyclerView)

        // Bottom Action Capsules Layout (Reset & Done, Aligned to the far right)
        val bottomBarLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

        val resetButton = TextView(context).apply {
            text = "Reset"
            textSize = 14.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(onSurfaceColor)
            setPadding(dp(22), dp(10), dp(22), dp(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setColor(surfaceContainerHighColor)
                setStroke(dp(1), outlineVariantColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(42)
            ).apply {
                marginEnd = dp(10)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                currentCustomItems.clear()
                currentCustomItems.addAll(getSortedMenuItems(useDefaultOrder = true))
                adapter.notifyDataSetChanged()
                Toast.makeText(context, "Order reset to default", Toast.LENGTH_SHORT).show()
            }
        }
        bottomBarLayout.addView(resetButton)

        val confirmButton = TextView(context).apply {
            text = "Done"
            textSize = 14.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(onPrimaryColor)
            setPadding(dp(26), dp(10), dp(26), dp(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setColor(primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(42)
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val order = currentCustomItems.map { it.id }
                saveMenuOrder(context, order)
                Toast.makeText(context, "Options order updated", Toast.LENGTH_SHORT).show()
                showNormalMenuView()
            }
        }
        bottomBarLayout.addView(confirmButton)
        cardView.addView(bottomBarLayout)
    }

    private fun showTextSizeView() {
        cardView.removeAllViews()
        cardView.setPadding(dp(18), dp(18), dp(18), dp(18))

        val colorScheme = ThemeManager.getColorScheme(context)
        val onSurfaceColor = colorScheme.onSurface.toArgb()
        val onSurfaceVariantColor = colorScheme.onSurfaceVariant.toArgb()
        val primaryColor = colorScheme.primary.toArgb()
        val onPrimaryColor = colorScheme.onPrimary.toArgb()
        val primaryContainerColor = colorScheme.primaryContainer.toArgb()
        val onPrimaryContainerColor = colorScheme.onPrimaryContainer.toArgb()
        val surfaceContainerHighestColor = colorScheme.surfaceContainerHighest.toArgb()

        val prefs = context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE)
        var currentZoom = prefs.getInt("web_text_zoom", 100).coerceIn(50, 200)

        // 1. Header with Back Button, Title, and Badge
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(20)
            }
        }

        val backBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            setImageResource(R.drawable.ic_chevron_left)
            setColorFilter(onSurfaceColor)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                showNormalMenuView()
            }
        }
        headerRow.addView(backBtn)

        val titleView = TextView(context).apply {
            text = "Text size"
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(onSurfaceColor)
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(8)
            }
        }
        headerRow.addView(titleView)

        val badgeView = TextView(context).apply {
            text = "$currentZoom%"
            textSize = 13.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(onPrimaryContainerColor)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(primaryContainerColor)
            }
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }
        headerRow.addView(badgeView)
        cardView.addView(headerRow)

        // 2. Slider / SeekBar Row
        val sliderRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        }

        val smallAText = TextView(context).apply {
            text = "A"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(onSurfaceVariantColor)
            setPadding(dp(4), 0, dp(8), 0)
        }
        sliderRow.addView(smallAText)

        val seekBar = android.widget.SeekBar(context).apply {
            max = 150 // 0 = 50%, 150 = 200%
            progress = currentZoom - 50
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            val thumbColorStateList = android.content.res.ColorStateList.valueOf(primaryColor)
            thumbTintList = thumbColorStateList
            progressTintList = thumbColorStateList
        }
        sliderRow.addView(seekBar)

        val largeAText = TextView(context).apply {
            text = "A"
            textSize = 22f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(onSurfaceVariantColor)
            setPadding(dp(8), 0, dp(4), 0)
        }
        sliderRow.addView(largeAText)
        cardView.addView(sliderRow)

        fun updateZoom(zoom: Int) {
            currentZoom = zoom.coerceIn(50, 200)
            badgeView.text = "$currentZoom%"
            seekBar.progress = currentZoom - 50
            prefs.edit().putInt("web_text_zoom", currentZoom).apply()
            onTextSizeChanged?.invoke(currentZoom)
        }

        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val z = progress + 50
                    currentZoom = z
                    badgeView.text = "$currentZoom%"
                    prefs.edit().putInt("web_text_zoom", currentZoom).apply()
                    onTextSizeChanged?.invoke(currentZoom)
                }
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        // 3. Quick Step / Preset Buttons Row
        val buttonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(4)
            }
        }

        fun createChipButton(label: String, onClick: () -> Unit): TextView {
            return TextView(context).apply {
                text = label
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(onSurfaceColor)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(16).toFloat()
                    setColor(surfaceContainerHighestColor)
                }
                setPadding(dp(16), dp(8), dp(16), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        }

        buttonsRow.addView(createChipButton("-10%") {
            updateZoom(currentZoom - 10)
        })
        buttonsRow.addView(createChipButton("100%") {
            updateZoom(100)
        })
        buttonsRow.addView(createChipButton("+10%") {
            updateZoom(currentZoom + 10)
        })
        cardView.addView(buttonsRow)
    }

    private fun showTranslateView() {
        cardView.removeAllViews()
        cardView.setPadding(dp(20), dp(16), dp(20), dp(16))

        val colorScheme = ThemeManager.getColorScheme(context)
        val onSurfaceColor = colorScheme.onSurface.toArgb()
        val onSurfaceVariantColor = colorScheme.onSurfaceVariant.toArgb()
        val primaryColor = colorScheme.primary.toArgb()
        val onPrimaryColor = colorScheme.onPrimary.toArgb()
        val primaryContainerColor = colorScheme.primaryContainer.toArgb()
        val onPrimaryContainerColor = colorScheme.onPrimaryContainer.toArgb()
        val surfaceContainerHighestColor = colorScheme.surfaceContainerHighest.toArgb()
        val surfaceContainerColor = colorScheme.surfaceContainer.toArgb()
        val outlineColor = colorScheme.outlineVariant.copy(alpha = 0.5f).toArgb()

        var selectedLang: String? = null

        // 1. Header (Back button, Title, Show Original Button)
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val backBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            setImageResource(R.drawable.ic_chevron_left)
            setColorFilter(onSurfaceColor)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                showNormalMenuView()
            }
        }
        headerRow.addView(backBtn)

        val titleView = TextView(context).apply {
            text = "Translate"
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(onSurfaceColor)
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(8)
            }
        }
        headerRow.addView(titleView)

        val originalBtn = TextView(context).apply {
            text = "Original ↺"
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(onSurfaceColor)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(surfaceContainerHighestColor)
                setStroke(dp(1), outlineColor)
            }
            setPadding(dp(12), dp(6), dp(12), dp(6))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                selectedLang = null
                onRestoreOriginalPage?.invoke()
                Toast.makeText(context, "Original page restored", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
        headerRow.addView(originalBtn)
        cardView.addView(headerRow)

        // 2. Language Capsules Grid in a ScrollView
        val languages = TranslationManager.SUPPORTED_LANGUAGES
        val scrollView = android.widget.ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(240)
            )
            isVerticalScrollBarEnabled = false
        }

        val gridLayout = GridLayout(context).apply {
            columnCount = 2
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        fun populateGrid() {
            gridLayout.removeAllViews()
            for (lang in languages) {
                val isSelected = selectedLang != null && lang.code.equals(selectedLang, ignoreCase = true)
                val capsule = TextView(context).apply {
                    text = "${lang.flag}  ${lang.nativeName}"
                    textSize = 13.5f
                    typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                    gravity = Gravity.CENTER
                    setTextColor(if (isSelected) onPrimaryContainerColor else onSurfaceColor)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(20).toFloat()
                        setColor(if (isSelected) primaryContainerColor else surfaceContainerColor)
                        if (isSelected) {
                            setStroke(dp(1.5f.toInt()).coerceAtLeast(2), primaryColor)
                        } else {
                            setStroke(dp(1), outlineColor)
                        }
                    }
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        selectedLang = lang.code
                        populateGrid()
                        onTranslatePage?.invoke(lang.code)
                        val rtlNote = if (lang.isRtl) " (RTL)" else ""
                        Toast.makeText(context, "Translating to ${lang.nativeName}$rtlNote...", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }

                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(44)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                }
                gridLayout.addView(capsule, params)
            }
        }

        populateGrid()
        scrollView.addView(gridLayout)
        cardView.addView(scrollView)
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

    private fun createDotDrawable(isActive: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            val color = if (isActive) ContextCompat.getColor(context, R.color.dot_active)
                        else ContextCompat.getColor(context, R.color.dot_inactive)
            setColor(color)
        }
    }

    private fun handleItemClick(item: BrowserMenuItem) {
        when (item.id) {
            "menu" -> {
                dismiss()
                onOpenMenu?.invoke()
            }
            "settings" -> {
                dismiss()
                onOpenSettings?.invoke()
            }
            "incognito" -> {
                dismiss()
                onToggleIncognito?.invoke()
            }
            "clear_data" -> {
                dismiss()
                onClearData()
            }
            "exit" -> {
                dismiss()
                onExit()
            }
            "share" -> {
                dismiss()
                onShare?.invoke()
            }
            "scripts" -> {
                val newEnabled = !item.isSelected
                item.isSelected = newEnabled
                ScriptManager.setScriptsEnabled(context, newEnabled)
                val msg = if (newEnabled) "Scripts enabled" else "Scripts disabled"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onToggleScripts?.invoke(newEnabled)
            }
            "sandbox" -> {
                val newEnabled = !item.isSelected
                item.isSelected = newEnabled
                SandboxManager.setSandboxEnabled(context, newEnabled) {
                    val msg = if (newEnabled) {
                        context.getString(R.string.sandbox_enabled_msg)
                    } else {
                        context.getString(R.string.sandbox_disabled_msg)
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onToggleSandbox?.invoke(newEnabled)
                }
            }
            "downloads" -> {
                dismiss()
                onOpenDownloads?.invoke()
            }
            "find_in_page" -> {
                dismiss()
                onFindInPage?.invoke()
            }
            "save" -> {
                dismiss()
                onSavePage?.invoke()
            }
            "print" -> {
                dismiss()
                onPrint?.invoke()
            }
            "reader_mode" -> {
                dismiss()
                onToggleReaderMode?.invoke()
            }
            "tor_network" -> {
                val willEnable = !item.isSelected
                item.isSelected = willEnable
                if (willEnable) {
                    Toast.makeText(context, context.getString(R.string.tor_connecting), Toast.LENGTH_SHORT).show()
                }
                TorManager.toggleTor(
                    context = context,
                    onProgress = { _, _ -> },
                    onComplete = { enabled, success ->
                        val msg = if (enabled) {
                            if (success) context.getString(R.string.tor_network_enabled) else "Tor proxy locked (Routing via 127.0.0.1:9050)"
                        } else {
                            context.getString(R.string.tor_network_disabled)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        onToggleTor?.invoke(enabled)
                    }
                )
            }
            "ad_blocking" -> {
                val newEnabled = !item.isSelected
                item.isSelected = newEnabled
                AdBlockManager.setAdBlockEnabled(context, newEnabled)
                val msg = if (newEnabled) "Ad blocking enabled" else "Ad blocking disabled"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            "user_agent" -> {
                val newEnabled = !item.isSelected
                item.isSelected = newEnabled
                UserAgentManager.setUserAgentEnabled(context, newEnabled)
                val selectedTitle = UserAgentManager.getSelectedTitle(context)
                if (newEnabled && selectedTitle.equals("Default", ignoreCase = true)) {
                    UserAgentManager.setSelectedTitle(context, "Android (Phone)")
                }
                onToggleUserAgent?.invoke(newEnabled)
                val currentTitle = UserAgentManager.getSelectedTitle(context)
                val msg = if (newEnabled) "User-agent enabled: $currentTitle" else "User-agent disabled (Default)"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            "desktop_site" -> {
                val newDesktop = !item.isSelected
                item.isSelected = newDesktop
                onToggleDesktopSite?.invoke(newDesktop)
                val msg = if (newDesktop) "Desktop site enabled" else "Desktop site disabled"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            "night_mode" -> {
                val currentlyDark = isNightModeActive
                val newDark = !currentlyDark
                item.isSelected = newDark
                val newMode = if (newDark) 1 else 2 // 1: Dark, 2: Light
                ThemeManager.setDarkThemeMode(context, newMode)
                onToggleNightMode?.invoke(newDark)
                val msg = if (newDark) "Night mode enabled" else "Night mode disabled"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            "site_config" -> {
                dismiss()
                onOpenSiteConfig?.invoke()
            }
            "orientation" -> {
                val newOrientation = !item.isSelected
                item.isSelected = newOrientation
                onToggleOrientation?.invoke(newOrientation)
                val msg = if (newOrientation) "Landscape orientation enabled" else "Portrait orientation restored"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            "qr_code", "scan_qr_code" -> {
                dismiss()
                if (::cardView.isInitialized) {
                    cardView.post {
                        onOpenQrCode?.invoke()
                    }
                } else {
                    onOpenQrCode?.invoke()
                }
            }
            "customize_menu" -> {
                showCustomizeMenuView()
            }
            "text_size" -> {
                showTextSizeView()
            }
            "translate" -> {
                showTranslateView()
            }
            else -> {
                if (item.isTogglable) {
                    item.isSelected = !item.isSelected
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

    private class MenuPagerAdapter(
        private val pages: List<List<BrowserMenuItem>>,
        private val onItemClick: (BrowserMenuItem) -> Unit,
        private val onItemLongClick: ((BrowserMenuItem) -> Unit)? = null
    ) : RecyclerView.Adapter<MenuPagerAdapter.PageViewHolder>() {

        class PageViewHolder(val gridLayout: GridLayout) : RecyclerView.ViewHolder(gridLayout)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val grid = GridLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                columnCount = 4
                rowCount = 2
                setPadding(dp(8, parent.context), 0, dp(8, parent.context), 0)
            }
            return PageViewHolder(grid)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val items = pages[position]
            holder.gridLayout.removeAllViews()

            for (item in items) {
                val itemView = createItemView(holder.gridLayout.context, item) {
                    if (item.isEnabled) {
                        onItemClick(item)
                        notifyItemChanged(position)
                    }
                }
                if (onItemLongClick != null && item.isEnabled) {
                    itemView.setOnLongClickListener {
                        onItemLongClick.invoke(item)
                        true
                    }
                }
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(84, holder.gridLayout.context)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                holder.gridLayout.addView(itemView, params)
            }
        }

        override fun getItemCount(): Int = pages.size

        private fun createItemView(context: Context, item: BrowserMenuItem, onClick: () -> Unit): View {
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = item.isEnabled
                isFocusable = item.isEnabled
                if (item.isEnabled) {
                    val outValue = TypedValue()
                    context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
                    setBackgroundResource(outValue.resourceId)
                    setOnClickListener { onClick() }
                } else {
                    alpha = 0.38f
                }
                setPadding(dp(2, context), dp(4, context), dp(2, context), dp(4, context))
            }

            val iconColor = if (item.isSelected) {
                ContextCompat.getColor(context, R.color.accent_blue)
            } else {
                ContextCompat.getColor(context, R.color.icon_inactive)
            }

            val icon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(26, context), dp(26, context))
                setImageResource(item.iconRes)
                setColorFilter(iconColor)
            }
            layout.addView(icon)

            val text = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(6, context)
                }
                this.text = item.label
                textSize = 11.5f
                gravity = Gravity.CENTER
                maxLines = 2
                setTextColor(iconColor)
            }
            layout.addView(text)

            return layout
        }

        private fun dp(value: Int, context: Context): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        }
    }
}
