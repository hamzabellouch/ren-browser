package com.tkno.ren

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import android.widget.EditText
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.tkno.ren.model.WebTab
import com.tkno.ren.ui.ClearDataDialog
import com.tkno.ren.ui.DownloadsScreen
import com.tkno.ren.ui.FindInPageBar
import com.tkno.ren.ui.FloatingBottomBar
import com.tkno.ren.ui.HistoryBottomSheet
import com.tkno.ren.ui.HistoryScreen
import com.tkno.ren.ui.HistoryView
import com.tkno.ren.ui.HomeScreenView
import com.tkno.ren.ui.LinkContextMenuBottomSheet
import com.tkno.ren.ui.MaterialLoadingIndicatorOverlay
import com.tkno.ren.ui.MenuBottomSheet
import com.tkno.ren.ui.PagePreviewBottomSheet
import com.tkno.ren.ui.PullRefreshLayout
import com.tkno.ren.ui.QrCodeBottomSheet
import com.tkno.ren.ui.TabsBottomSheet
import com.tkno.ren.ui.TabsGridView
import com.tkno.ren.ui.TextSelectionActionModeCallback
import android.content.ClipData
import android.content.ClipboardManager
import com.tkno.ren.ui.menu.MenuHost
import com.tkno.ren.ui.settings.SettingsHost
import com.tkno.ren.ui.theme.RenTheme
import com.tkno.ren.ui.theme.ThemeManager
import com.tkno.ren.util.AdBlockManager
import com.tkno.ren.util.AntiFingerprintManager
import com.tkno.ren.util.DownloadsManager
import com.tkno.ren.util.FaviconManager
import com.tkno.ren.util.HistoryManager
import com.tkno.ren.util.ReaderModeManager
import com.tkno.ren.util.SandboxManager
import com.tkno.ren.util.ScriptManager
import com.tkno.ren.util.SearchEngineManager
import com.tkno.ren.util.SiteConfigManager
import com.tkno.ren.util.TorManager
import com.tkno.ren.util.TranslationManager
import com.tkno.ren.util.UserAgentManager
import com.tkno.ren.util.WebRtcManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import com.tkno.ren.util.IncognitoNotificationManager
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val tabs = mutableListOf<WebTab>()
    private val tabsListState = mutableStateListOf<WebTab>()
    private var activeTabId: String = ""
    private val activeTabIdState = mutableStateOf("")

    private lateinit var rootContainer: FrameLayout
    private lateinit var browserLayout: FrameLayout
    private lateinit var fullscreenContainer: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private lateinit var webContainer: PullRefreshLayout
    private lateinit var pullRefreshIndicatorContainer: View
    private lateinit var homeScreenOverlay: FrameLayout
    private lateinit var homeScreenContainer: ComposeView
    private lateinit var historyContainer: ComposeView
    private lateinit var tabsGridContainer: ComposeView
    private lateinit var contentOverlayContainer: FrameLayout
    private lateinit var findInPageContainer: ComposeView
    private lateinit var taskBarView: View
    private val pullRefreshProgressState = mutableFloatStateOf(0f)
    private val pullRefreshOffsetPxState = mutableFloatStateOf(0f)
    private val isPullRefreshingState = mutableStateOf(false)
    private var isHistoryVisible = false
    private var isTabsGridVisible = false
    private val currentBottomNavTab = mutableIntStateOf(0)
    private val isCurrentTabIncognito = mutableStateOf(false)
    private val isSandboxActiveState = mutableStateOf(false)
    private val isBrowsingState = mutableStateOf(false)
    private val currentUrlState = mutableStateOf("")
    private val isFloatingBubbleState = mutableStateOf(false)
    private val isPageLoadingState = mutableStateOf(false)
    private val loadingProgressState = mutableFloatStateOf(0f)
    private val isFindInPageVisibleState = mutableStateOf(false)
    private val findInPageQueryState = mutableStateOf("")
    private val findInPageMatchIndexState = mutableIntStateOf(0)
    private val findInPageMatchCountState = mutableIntStateOf(0)
    private val isTaskbarVisibleState = mutableStateOf(true)
    private val canGoBackState = mutableStateOf(false)
    private val canGoForwardState = mutableStateOf(false)
    private var bubbleCenterX: Float = 0f
    private var bubbleCenterY: Float = 0f
    private var bubbleRadiusPx: Float = 0f
    private var isBubbleTouchActive: Boolean = false
    private val searchBarBoundsInWindow = android.graphics.RectF()
    private var isSearchBarTouchActive: Boolean = false
    private var statusBarHeightPx: Int = 0
    private var lastHomePressTime: Long = 0L

    private fun updateNavigationHistoryState() {
        val activeWv = getActiveTab()?.webView
        canGoBackState.value = activeWv?.canGoBack() == true
        canGoForwardState.value = activeWv?.canGoForward() == true
    }

    private var isSettingsOpen = false
    private var isMenuOpen = false
    private var isDownloadsOpen = false
    private var isLandscapeForced = false
    private var currentMenuSheet: MenuBottomSheet? = null
    private var currentQrCodeSheet: QrCodeBottomSheet? = null
    private var themePrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            updateIncognitoNotification()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateIncognitoNotification() {
        val incognitoCount = tabs.count { it.isIncognito }
        if (incognitoCount > 0) {
            IncognitoNotificationManager.showNotification(this, incognitoCount)
        } else {
            IncognitoNotificationManager.cancelNotification(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == IncognitoNotificationManager.ACTION_CLOSE_INCOGNITO) {
            closeAllIncognitoTabs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ThemeManager.init(this)
        TorManager.initAtStartup(this)
        IncognitoNotificationManager.initChannel(this)
        DownloadsManager.ensureNotificationChannel(this)
        checkNotificationPermission()

        isSandboxActiveState.value = SandboxManager.isSandboxEnabled(this)
        buildBrowserUI()

        val themePrefs = getSharedPreferences("links_prefs", Context.MODE_PRIVATE)
        themePrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "dark_theme") {
                updateAllTabsNightMode(isNightModeActive())
            }
        }
        themePrefs.registerOnSharedPreferenceChangeListener(themePrefsListener)

        if (intent?.action == IncognitoNotificationManager.ACTION_CLOSE_INCOGNITO) {
            closeAllIncognitoTabs()
        } else {
            // Open initial tab
            createNewTab("about:blank", "Blank page")
        }
    }

    override fun onResume() {
        super.onResume()
        isSandboxActiveState.value = SandboxManager.isSandboxEnabled(this)
    }

    private fun buildBrowserUI() {
        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        browserLayout = FrameLayout(this).apply {
            background = null
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Web Container fills screen with Pull-to-Refresh support
        webContainer = PullRefreshLayout(this).apply {
            background = null
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            onRefresh = {
                isPullRefreshingState.value = true
                isPageLoadingState.value = true
                loadingProgressState.floatValue = 0.15f
                getActiveTab()?.webView?.reload()
            }
            onPullProgress = { progress, offsetPx ->
                pullRefreshProgressState.floatValue = progress
                pullRefreshOffsetPxState.floatValue = offsetPx
            }
        }
        browserLayout.addView(webContainer)

        // Pull to Refresh Material 3 Expressive Loading Indicator Overlay
        pullRefreshIndicatorContainer = createPullRefreshIndicatorComposeView()
        browserLayout.addView(pullRefreshIndicatorContainer)

        // Home & Animated Search Bar Overlay (Custom FrameLayout with intelligent touch pass-through)
        homeScreenOverlay = createHomeScreenOverlay()
        browserLayout.addView(homeScreenOverlay)

        // History Container fills screen (hidden by default)
        historyContainer = createHistoryComposeView()
        historyContainer.visibility = View.GONE
        browserLayout.addView(historyContainer)

        // Tabs Grid Container (hidden by default)
        tabsGridContainer = createTabsGridComposeView()
        tabsGridContainer.visibility = View.GONE
        browserLayout.addView(tabsGridContainer)

        // Content Overlay Container for Menu, Settings, Downloads (hidden by default)
        contentOverlayContainer = FrameLayout(this).apply {
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        browserLayout.addView(contentOverlayContainer)

        // Find in Page Container (hidden by default)
        findInPageContainer = createFindInPageComposeView()
        findInPageContainer.visibility = View.GONE
        browserLayout.addView(findInPageContainer)

        // Floating Bottom TaskBar (Jetpack Compose Pill Dock)
        taskBarView = createComposeTaskBar()
        browserLayout.addView(taskBarView)

        // Window insets listener to handle status bar and bottom navigation insets cleanly
        ViewCompat.setOnApplyWindowInsetsListener(browserLayout) { _, insets ->
            val statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime())
            statusBarHeightPx = statusInsets.top

            // Web container extends from status bar to bottom under floating taskbar
            val webParams = webContainer.layoutParams as? FrameLayout.LayoutParams
            webParams?.let {
                it.topMargin = statusInsets.top
                it.bottomMargin = 0
                webContainer.layoutParams = it
            }

            // Home screen overlay extends to bottom under floating taskbar (handles its own statusBarsPadding)
            val homeParams = homeScreenOverlay.layoutParams as? FrameLayout.LayoutParams
            homeParams?.let {
                it.topMargin = 0
                it.bottomMargin = 0
                homeScreenOverlay.layoutParams = it
            }

            // History container extends to bottom under floating taskbar (handles its own statusBarsPadding)
            val historyParams = historyContainer.layoutParams as? FrameLayout.LayoutParams
            historyParams?.let {
                it.topMargin = 0
                it.bottomMargin = 0
                historyContainer.layoutParams = it
            }

            // Tabs grid container extends to bottom under floating taskbar (handles its own statusBarsPadding)
            val tabsParams = tabsGridContainer.layoutParams as? FrameLayout.LayoutParams
            tabsParams?.let {
                it.topMargin = 0
                it.bottomMargin = 0
                tabsGridContainer.layoutParams = it
            }

            // Find in page container extends from top
            val findParams = findInPageContainer.layoutParams as? FrameLayout.LayoutParams
            findParams?.let {
                it.topMargin = 0
                it.bottomMargin = 0
                findInPageContainer.layoutParams = it
            }

            insets
        }

        fullscreenContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        rootContainer.addView(browserLayout)
        rootContainer.addView(fullscreenContainer)
        setContentView(rootContainer)
    }

    private fun showCustomView(
        view: View?,
        requestedOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        callback: WebChromeClient.CustomViewCallback?
    ) {
        if (customView != null) {
            hideCustomView()
            return
        }
        if (view == null) return

        customView = view
        customViewCallback = callback
        originalOrientation = this.requestedOrientation

        if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            this.requestedOrientation = requestedOrientation
        }

        browserLayout.visibility = View.GONE
        fullscreenContainer.removeAllViews()
        fullscreenContainer.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )
        fullscreenContainer.visibility = View.VISIBLE

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun hideCustomView() {
        if (customView == null) return

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())

        fullscreenContainer.removeAllViews()
        fullscreenContainer.visibility = View.GONE
        browserLayout.visibility = View.VISIBLE

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        this.requestedOrientation = originalOrientation

        try {
            customViewCallback?.onCustomViewHidden()
        } catch (_: Throwable) {}

        customView = null
        customViewCallback = null
    }

    private fun createHomeScreenOverlay(): FrameLayout {
        val overlay = object : FrameLayout(this) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                if (isBrowsingState.value && !isHistoryVisible && !isTabsGridVisible) {
                    if (isFloatingBubbleState.value) {
                        val touchPadding = dp(20).toFloat()
                        val baseRadius = if (bubbleRadiusPx > 0f) bubbleRadiusPx else dp(26).toFloat()
                        val effectiveRadius = baseRadius + touchPadding
                        val cx = if (bubbleCenterX > 0f) bubbleCenterX else searchBarBoundsInWindow.centerX()
                        val cy = if (bubbleCenterY > 0f) bubbleCenterY else searchBarBoundsInWindow.centerY()
                        val dx = ev.x - cx
                        val dy = ev.y - cy
                        val isInsideBubble = (dx * dx + dy * dy) <= (effectiveRadius * effectiveRadius)

                        if (ev.action == MotionEvent.ACTION_DOWN) {
                            isBubbleTouchActive = isInsideBubble
                        }

                        if (isBubbleTouchActive) {
                            val res = super.dispatchTouchEvent(ev)
                            if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
                                isBubbleTouchActive = false
                            }
                            return res
                        } else {
                            return false
                        }
                    } else {
                        val touchPadding = dp(10).toFloat()
                        val isInsideSearchBar = (
                            ev.x >= searchBarBoundsInWindow.left - touchPadding &&
                            ev.x <= searchBarBoundsInWindow.right + touchPadding &&
                            ev.y >= searchBarBoundsInWindow.top - touchPadding &&
                            ev.y <= searchBarBoundsInWindow.bottom + touchPadding
                        )

                        if (ev.action == MotionEvent.ACTION_DOWN) {
                            isSearchBarTouchActive = isInsideSearchBar
                        }

                        if (isSearchBarTouchActive) {
                            val res = super.dispatchTouchEvent(ev)
                            if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
                                isSearchBarTouchActive = false
                            }
                            return res
                        } else {
                            return false
                        }
                    }
                }
                return super.dispatchTouchEvent(ev)
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        homeScreenContainer = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                RenTheme {
                    val isBrowsing by isBrowsingState
                    val currentUrl by currentUrlState
                    val incognito by isCurrentTabIncognito
                    val isSandbox by isSandboxActiveState
                    val isFloatingBubble by isFloatingBubbleState
                    val isPageLoading by isPageLoadingState
                    val loadingProgress by loadingProgressState
                    val canGoBack by canGoBackState
                    val canGoForward by canGoForwardState
                    val isTaskbarVisible by isTaskbarVisibleState
                    HomeScreenView(
                        isBrowsing = isBrowsing,
                        currentUrl = currentUrl,
                        isPageLoading = isPageLoading,
                        loadingProgress = loadingProgress,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                        onGoBack = {
                            val activeTab = getActiveTab()
                            if (activeTab != null && activeTab.webView.canGoBack()) {
                                activeTab.webView.goBack()
                            } else {
                                showHomeScreen()
                            }
                            updateNavigationHistoryState()
                        },
                        onGoForward = {
                            val activeTab = getActiveTab()
                            if (activeTab != null && activeTab.webView.canGoForward()) {
                                activeTab.webView.goForward()
                            }
                            updateNavigationHistoryState()
                        },
                        isTaskbarVisible = isTaskbarVisible,
                        onToggleTaskbar = {
                            isTaskbarVisibleState.value = !isTaskbarVisibleState.value
                        },
                        onSearch = { query ->
                            performSearchOrNavigate(query)
                        },
                        onOpenUrl = { url ->
                            navigateToUrl(url)
                        },
                        onReload = {
                            getActiveTab()?.webView?.reload()
                        },
                        onStopLoading = {
                            getActiveTab()?.webView?.stopLoading()
                            isPageLoadingState.value = false
                            loadingProgressState.floatValue = 0f
                            isPullRefreshingState.value = false
                            webContainer.setRefreshing(false)
                        },
                        onIncognitoClick = {
                            toggleIncognitoMode()
                        },
                        isIncognitoActive = incognito,
                        onSandboxClick = {
                            toggleSandboxMode()
                        },
                        isSandboxActive = isSandbox,
                        isFloatingBubbleMode = isFloatingBubble,
                        onToggleFloatingBubble = {
                            isFloatingBubbleState.value = !isFloatingBubbleState.value
                        },
                        onBubblePositionChanged = { cx, cy, rad ->
                            bubbleCenterX = cx
                            bubbleCenterY = cy
                            bubbleRadiusPx = rad
                        },
                        onSearchBarBoundsChanged = { left, top, right, bottom ->
                            searchBarBoundsInWindow.set(left, top, right, bottom)
                        }
                    )
                }
            }
        }
        overlay.addView(homeScreenContainer)
        return overlay
    }

    private fun createTabsGridComposeView(): ComposeView {
        return ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                val activeId by activeTabIdState
                TabsGridView(
                    tabs = tabsListState,
                    activeTabId = activeId,
                    onSelectTab = { selectedTab ->
                        switchTab(selectedTab)
                        if (selectedTab.url == "about:blank" || selectedTab.url.isEmpty()) {
                            showHomeScreen()
                        } else {
                            showBrowsingScreen()
                        }
                    },
                    onCloseTab = { closedTab ->
                        closeTab(closedTab)
                    },
                    onNewTab = { isIncog ->
                        val newTab = createNewTab(
                            url = "about:blank",
                            title = if (isIncog) "Incognito" else "Blank page",
                            isIncognito = isIncog
                        )
                        switchTab(newTab)
                        showHomeScreen()
                    },
                    onCloseAllIncognito = {
                        closeAllIncognitoTabs()
                    }
                )
            }
        }
    }

    private fun createHistoryComposeView(): ComposeView {
        return ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                val incognito by isCurrentTabIncognito
                RenTheme(isIncognito = incognito) {
                    HistoryScreen(
                        onOpenUrl = { selectedUrl ->
                            navigateToUrl(selectedUrl)
                        },
                        isIncognito = incognito
                    )
                }
            }
        }
    }

    private fun createComposeTaskBar(): View {
        return ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
            setContent {
                RenTheme {
                    val selectedTab by currentBottomNavTab
                    val isFloatingBubble by isFloatingBubbleState
                    val isIncognito by isCurrentTabIncognito
                    val isTaskbarVisible by isTaskbarVisibleState
                    val isBrowsing by isBrowsingState
                    FloatingBottomBar(
                        selectedTab = selectedTab,
                        isVisible = !isFloatingBubble && (isTaskbarVisible || !isBrowsing),
                        isIncognito = isIncognito,
                        onTabSelect = { index ->
                            when (index) {
                                0 -> {
                                    val isAnyOverlayOpen = isHistoryVisible || isTabsGridVisible || isSettingsOpen || isMenuOpen || isDownloadsOpen || currentMenuSheet?.isShowing == true || currentQrCodeSheet?.isShowing == true
                                    if (isAnyOverlayOpen) {
                                        closeAllOverlays()
                                        val activeTab = getActiveTab()
                                        if (activeTab != null && activeTab.url != "about:blank" && activeTab.url.isNotEmpty()) {
                                            showBrowsingScreen()
                                        } else {
                                            showHomeScreen()
                                        }
                                    } else {
                                        val activeTab = getActiveTab()
                                        val isActuallyBrowsing = isBrowsingState.value && activeTab != null && activeTab.url != "about:blank" && activeTab.url.isNotEmpty()
                                        if (isActuallyBrowsing) {
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - lastHomePressTime < 2000) {
                                                lastHomePressTime = 0L
                                                activeTab.url = "about:blank"
                                                activeTab.title = if (activeTab.isIncognito) "Incognito" else "Blank page"
                                                activeTab.webView.loadUrl("about:blank")
                                                showHomeScreen()
                                            } else {
                                                lastHomePressTime = currentTime
                                                Toast.makeText(this@MainActivity, R.string.press_again_to_go_home, Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            showHomeScreen()
                                        }
                                    }
                                }
                                1 -> {
                                    closeAllOverlays()
                                    showTabsGridScreen()
                                }
                                2 -> {
                                    closeAllOverlays()
                                    showHistoryScreen()
                                }
                                3 -> {
                                    showMenuSheet()
                                }
                            }
                        },
                    onTabLongClick = { index ->
                        if (index == 1) {
                            showTabsSheet()
                        }
                    }
                )
            }
        }
    }
}

    private fun createFindInPageComposeView(): ComposeView {
        return ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            )
            setContent {
                RenTheme {
                    val isVisible by isFindInPageVisibleState
                    val query by findInPageQueryState
                    val matchIndex by findInPageMatchIndexState
                    val matchCount by findInPageMatchCountState

                    FindInPageBar(
                        isVisible = isVisible,
                        query = query,
                        onQueryChange = { newQuery ->
                            onFindInPageQueryChanged(newQuery)
                        },
                        matchIndex = matchIndex,
                        matchCount = matchCount,
                        onPrevious = {
                            findInPagePrevious()
                        },
                        onNext = {
                            findInPageNext()
                        },
                        onClose = {
                            closeFindInPage()
                        }
                    )
                }
            }
        }
    }

    private fun createPullRefreshIndicatorComposeView(): View {
        val overlay = object : FrameLayout(this) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                return false
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val composeView = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                RenTheme {
                    val isRefreshing by isPullRefreshingState
                    val pullProgress by pullRefreshProgressState
                    val pullOffsetPx by pullRefreshOffsetPxState
                    MaterialLoadingIndicatorOverlay(
                        isRefreshing = isRefreshing,
                        pullProgress = pullProgress,
                        pullOffsetPx = pullOffsetPx,
                        statusBarHeightPx = statusBarHeightPx
                    )
                }
            }
        }
        overlay.addView(composeView)
        return overlay
    }

    private fun setupWebViewFindListener(webView: WebView) {
        webView.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            if (numberOfMatches > 0) {
                findInPageMatchIndexState.intValue = activeMatchOrdinal + 1
                findInPageMatchCountState.intValue = numberOfMatches
            } else {
                findInPageMatchIndexState.intValue = 0
                findInPageMatchCountState.intValue = 0
            }
        }
    }

    private fun openFindInPage() {
        closeAllOverlays()
        isFindInPageVisibleState.value = true
        findInPageContainer.visibility = View.VISIBLE
        findInPageContainer.bringToFront()
        val activeWebView = getActiveTab()?.webView
        if (activeWebView != null) {
            setupWebViewFindListener(activeWebView)
            val currentQuery = findInPageQueryState.value
            if (currentQuery.isNotEmpty()) {
                activeWebView.findAllAsync(currentQuery)
            }
        }
    }

    private fun closeFindInPage() {
        isFindInPageVisibleState.value = false
        findInPageContainer.visibility = View.GONE
        findInPageQueryState.value = ""
        findInPageMatchIndexState.intValue = 0
        findInPageMatchCountState.intValue = 0
        val activeWebView = getActiveTab()?.webView
        activeWebView?.clearMatches()
        hideKeyboard()
    }

    private fun onFindInPageQueryChanged(newQuery: String) {
        findInPageQueryState.value = newQuery
        val activeWebView = getActiveTab()?.webView ?: return
        if (newQuery.isBlank()) {
            activeWebView.clearMatches()
            findInPageMatchIndexState.intValue = 0
            findInPageMatchCountState.intValue = 0
        } else {
            activeWebView.findAllAsync(newQuery)
        }
    }

    private fun findInPageNext() {
        val activeWebView = getActiveTab()?.webView ?: return
        activeWebView.findNext(true)
    }

    private fun findInPagePrevious() {
        val activeWebView = getActiveTab()?.webView ?: return
        activeWebView.findNext(false)
    }

    private fun navigateToUrl(url: String) {
        hideKeyboard()
        val activeTab = getActiveTab() ?: createNewTab()
        activeTab.url = url
        UserAgentManager.applyToWebView(this@MainActivity, activeTab.webView, isDesktopSite = activeTab.isDesktopSite)
        activeTab.webView.loadUrl(url)
        currentUrlState.value = url
        showBrowsingScreen(resetTaskbar = true)
    }

    private fun performSearchOrNavigate(rawQuery: String? = null) {
        val raw = (rawQuery ?: currentUrlState.value).trim()
        hideKeyboard()

        if (raw.isEmpty()) return

        val url = if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file://") || raw.startsWith("about:")) {
            raw
        } else if (raw.contains(".") && !raw.contains(" ")) {
            "https://$raw"
        } else {
            SearchEngineManager.buildSearchUrl(this@MainActivity, raw)
        }

        navigateToUrl(url)
    }

    private fun toggleIncognitoMode() {
        val activeTab = getActiveTab()
        if (activeTab != null && activeTab.isIncognito) {
            val normalTab = tabs.firstOrNull { !it.isIncognito }
            if (normalTab != null) {
                switchTab(normalTab)
            } else {
                val newTab = createNewTab(
                    url = "about:blank",
                    title = "Blank page",
                    isIncognito = false
                )
                switchTab(newTab)
                showHomeScreen()
            }
        } else {
            val incognitoTab = tabs.firstOrNull { it.isIncognito }
            if (incognitoTab != null) {
                switchTab(incognitoTab)
            } else {
                val newTab = createNewTab(
                    url = "about:blank",
                    title = "Incognito",
                    isIncognito = true
                )
                switchTab(newTab)
                showHomeScreen()
            }
        }
    }

    private fun resetSessionForSandbox(newEnabled: Boolean) {
        // Destroy all existing tabs and clear WebView engine state
        for (tab in tabs) {
            try {
                tab.webView.stopLoading()
                tab.webView.clearCache(true)
                tab.webView.clearFormData()
                tab.webView.clearHistory()
                tab.webView.clearSslPreferences()
                tab.webView.clearMatches()
                tab.webView.loadUrl("about:blank")
                tab.webView.destroy()
            } catch (_: Exception) {}
        }
        tabs.clear()
        tabsListState.clear()
        webContainer.removeAllViews()

        currentUrlState.value = ""
        isBrowsingState.value = false
        canGoBackState.value = false
        canGoForwardState.value = false

        val freshTab = createNewTab("about:blank", "Blank page", isIncognito = false)
        switchTab(freshTab)

        updateAllTabsPrivacySettings()
        showHomeScreen()
        updateTabCount()
        updateNavigationHistoryState()
    }

    private fun toggleSandboxMode() {
        val newEnabled = !SandboxManager.isSandboxEnabled(this)
        val allWebViews = tabs.map { it.webView }
        SandboxManager.setSandboxEnabled(this, newEnabled, allWebViews) {
            val msg = if (newEnabled) {
                getString(R.string.sandbox_enabled_msg)
            } else {
                getString(R.string.sandbox_disabled_msg)
            }
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            isSandboxActiveState.value = newEnabled
            resetSessionForSandbox(newEnabled)
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun createNewTab(
        url: String = "about:blank",
        title: String = "Blank page",
        isIncognito: Boolean = false,
        isDesktopSite: Boolean = false,
        switchToTab: Boolean = true,
        pendingReaderMode: Boolean = false
    ): WebTab {
        val tabId = UUID.randomUUID().toString()
        val webView = object : WebView(this@MainActivity) {
            override fun startActionMode(callback: android.view.ActionMode.Callback?, type: Int): android.view.ActionMode? {
                val wrappedCallback = TextSelectionActionModeCallback(
                    context = this@MainActivity,
                    webView = this,
                    originalCallback = callback,
                    onWebSearch = { searchUrl ->
                        val activeIsIncog = getActiveTab()?.isIncognito == true
                        createNewTab(url = searchUrl, isIncognito = activeIsIncog, switchToTab = true)
                        showBrowsingScreen()
                    }
                )
                return super.startActionMode(wrappedCallback, type)
            }

            override fun startActionMode(callback: android.view.ActionMode.Callback?): android.view.ActionMode? {
                val wrappedCallback = TextSelectionActionModeCallback(
                    context = this@MainActivity,
                    webView = this,
                    originalCallback = callback,
                    onWebSearch = { searchUrl ->
                        val activeIsIncog = getActiveTab()?.isIncognito == true
                        createNewTab(url = searchUrl, isIncognito = activeIsIncog, switchToTab = true)
                        showBrowsingScreen()
                    }
                )
                return super.startActionMode(wrappedCallback)
            }
        }.apply {
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.bg_black))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            val isSandbox = SandboxManager.isSandboxEnabled(this@MainActivity)
            if (isIncognito && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                try {
                    val profileStore = ProfileStore.getInstance()
                    profileStore.getOrCreateProfile("incognito_profile")
                    WebViewCompat.setProfile(this, "incognito_profile")
                } catch (e: Exception) {
                    // Fallback
                }
            }

            val savedTextZoom = getSharedPreferences("links_prefs", Context.MODE_PRIVATE).getInt("web_text_zoom", 100)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = !isIncognito && !isSandbox
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                textZoom = savedTextZoom
                cacheMode = if (isIncognito || isSandbox) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                setGeolocationEnabled(!TorManager.isTorEnabled(this@MainActivity) && !isSandbox && !isIncognito)
                allowFileAccess = !isSandbox && !isIncognito
                allowContentAccess = !isIncognito
                databaseEnabled = !isIncognito && !isSandbox
                mediaPlaybackRequiresUserGesture = false
            }
            UserAgentManager.applyToWebView(this@MainActivity, this, isDesktopSite)
            SiteConfigManager.applyToWebView(this@MainActivity, this)
            applyNightModeToWebView(this, if (isIncognito) true else isNightModeActive())
            setupWebViewFindListener(this)

            var lastTouchX = 0
            var lastTouchY = 0
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    lastTouchX = event.x.toInt()
                    lastTouchY = event.y.toInt()
                }
                false
            }

            setOnLongClickListener { v ->
                val wv = v as? WebView ?: return@setOnLongClickListener false
                val hitResult = wv.hitTestResult
                val hitType = hitResult.type
                val hitExtra = hitResult.extra

                val isLinkOrImage = (
                    hitType == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                    hitType == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE ||
                    hitType == WebView.HitTestResult.IMAGE_TYPE ||
                    hitType == WebView.HitTestResult.EMAIL_TYPE ||
                    hitType == WebView.HitTestResult.PHONE_TYPE ||
                    hitType == WebView.HitTestResult.GEO_TYPE
                )

                val jsDensity = resources.displayMetrics.density
                val cssX = (lastTouchX / jsDensity).toInt()
                val cssY = (lastTouchY / jsDensity).toInt()
                val extractScript = """
                    (function() {
                        try {
                            var elem = document.elementFromPoint($cssX, $cssY);
                            var a = elem ? elem.closest('a') : null;
                            var img = elem ? (elem.tagName === 'IMG' ? elem : elem.querySelector('img')) : null;
                            return JSON.stringify({
                                href: a ? (a.href || '') : '',
                                text: a ? (a.innerText || a.textContent || '').trim() : '',
                                title: a ? (a.title || '') : '',
                                imgSrc: img ? (img.src || '') : ''
                            });
                        } catch(e) {
                            return JSON.stringify({ href: '', text: '', title: '', imgSrc: '' });
                        }
                    })();
                """.trimIndent()

                wv.evaluateJavascript(extractScript) { resultJson ->
                    var extractedHref = ""
                    var extractedText = ""
                    var extractedTitle = ""
                    var extractedImgSrc = ""

                    try {
                        if (!resultJson.isNullOrBlank() && resultJson != "null") {
                            val cleanJson = if (resultJson.startsWith("\"") && resultJson.endsWith("\"")) {
                                org.json.JSONTokener(resultJson).nextValue() as? String ?: resultJson
                            } else resultJson
                            val json = org.json.JSONObject(cleanJson)
                            extractedHref = json.optString("href", "")
                            extractedText = json.optString("text", "")
                            extractedTitle = json.optString("title", "")
                            extractedImgSrc = json.optString("imgSrc", "")
                        }
                    } catch (_: Exception) {}

                    val finalTargetUrl = when {
                        extractedHref.isNotBlank() -> extractedHref
                        hitType == WebView.HitTestResult.SRC_ANCHOR_TYPE -> hitExtra
                        hitType == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> hitExtra
                        hitType == WebView.HitTestResult.EMAIL_TYPE -> hitExtra
                        hitType == WebView.HitTestResult.PHONE_TYPE -> hitExtra
                        hitType == WebView.HitTestResult.GEO_TYPE -> hitExtra
                        else -> null
                    }

                    val finalImageUrl = when {
                        extractedImgSrc.isNotBlank() -> extractedImgSrc
                        hitType == WebView.HitTestResult.IMAGE_TYPE -> hitExtra
                        hitType == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> hitExtra
                        else -> null
                    }

                    val finalLinkText = when {
                        extractedText.isNotBlank() -> extractedText
                        extractedTitle.isNotBlank() -> extractedTitle
                        else -> finalTargetUrl
                    }

                    if (!finalTargetUrl.isNullOrBlank() || !finalImageUrl.isNullOrBlank()) {
                        showLinkContextMenu(
                            targetUrl = finalTargetUrl,
                            linkText = finalLinkText,
                            imageUrl = finalImageUrl
                        )
                    }
                }

                if (isLinkOrImage) {
                    true
                } else {
                    false
                }
            }

            setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                val guessName = android.webkit.URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
                val pageUrl = this.url
                val item = DownloadsManager.addDownload(
                    context = this@MainActivity,
                    url = downloadUrl,
                    customFileName = guessName,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimetype,
                    referer = pageUrl
                )
                if (item != null) {
                    Toast.makeText(this@MainActivity, "Download started: ${item.fileName}", Toast.LENGTH_SHORT).show()
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val targetUri = request.url ?: return false
                    val targetUrl = targetUri.toString()
                    val scheme = targetUri.scheme?.lowercase() ?: ""

                    val currentTab = tabs.find { it.id == tabId }
                    if (targetUrl == ReaderModeManager.EXIT_READER_URL) {
                        exitReaderMode(currentTab)
                        return true
                    }
                    if (currentTab?.isReaderMode == true) {
                        currentTab.isReaderMode = false
                    }

                    val isSandboxActive = SandboxManager.isSandboxEnabled(this@MainActivity)

                    // Standard web navigation schemes handled natively by WebView engine
                    if (scheme == "http" || scheme == "https") {
                        if (DownloadsManager.isDownloadableUrl(targetUrl)) {
                            val guessName = android.webkit.URLUtil.guessFileName(targetUrl, null, null)
                            val pageUrl = view.url
                            val item = DownloadsManager.addDownload(
                                context = this@MainActivity,
                                url = targetUrl,
                                customFileName = guessName,
                                userAgent = view.settings.userAgentString,
                                referer = pageUrl
                            )
                            if (item != null) {
                                Toast.makeText(this@MainActivity, "Download started: ${item.fileName}", Toast.LENGTH_SHORT).show()
                            }
                            return true
                        }
                        return false
                    }
                    if (scheme == "about" || scheme == "data" || scheme == "blob" || scheme == "javascript") {
                        return false
                    }

                    // For external schemes (intent://, market://, mailto:, tel:, etc.)
                    if (isSandboxActive && SandboxManager.isBlockIntents(this@MainActivity)) {
                        // Block opening external app intents while in sandbox
                        return true
                    }

                    return try {
                        val intent = Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME)
                        if (intent != null) {
                            intent.addCategory(Intent.CATEGORY_BROWSABLE)
                            intent.component = null
                            intent.selector = null
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                                true
                            } else {
                                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                if (!fallbackUrl.isNullOrBlank() && (fallbackUrl.startsWith("http://") || fallbackUrl.startsWith("https://"))) {
                                    view.loadUrl(fallbackUrl)
                                    true
                                } else {
                                    true
                                }
                            }
                        } else {
                            true
                        }
                    } catch (e: Exception) {
                        true
                    }
                }

                override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                    try {
                        val tab = tabs.find { it.id == tabId }
                        if (tab != null && tab.id == activeTabId) {
                            webContainer.removeAllViews()
                            val newWebView = WebView(this@MainActivity)
                            tab.webView.destroy()
                            tab.webView = newWebView
                            webContainer.addView(newWebView)
                            if (tab.url.isNotBlank() && tab.url != "about:blank") {
                                newWebView.loadUrl(tab.url)
                            } else {
                                showHomeScreen()
                            }
                        }
                    } catch (_: Throwable) {
                        // Protect against secondary failure
                    }
                    return true
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): android.webkit.WebResourceResponse? {
                    try {
                        val reqUrl = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                        val isSandboxActive = SandboxManager.isSandboxEnabled(this@MainActivity)

                        if (!SiteConfigManager.isLocalNetworkAccessAllowed(this@MainActivity) && SiteConfigManager.isLocalNetworkTarget(reqUrl)) {
                            return android.webkit.WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                java.io.ByteArrayInputStream(ByteArray(0))
                            )
                        }

                        if (isSandboxActive && SandboxManager.isBlockTrackers(this@MainActivity)) {
                            if (SandboxManager.shouldBlockRequest(reqUrl)) {
                                return android.webkit.WebResourceResponse(
                                    "text/plain",
                                    "UTF-8",
                                    java.io.ByteArrayInputStream(ByteArray(0))
                                )
                            }
                        }
                        if (AdBlockManager.shouldBlock(this@MainActivity, reqUrl)) {
                            if (AdBlockManager.isAntiAdblockEnabled(this@MainActivity) && AdBlockManager.isBaitScriptRequest(reqUrl)) {
                                val baitCode = AdBlockManager.getBaitScriptResponse()
                                return android.webkit.WebResourceResponse(
                                    "application/javascript",
                                    "UTF-8",
                                    java.io.ByteArrayInputStream(baitCode.toByteArray(Charsets.UTF_8))
                                )
                            }
                            return android.webkit.WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                java.io.ByteArrayInputStream(ByteArray(0))
                            )
                        }
                    } catch (e: Exception) {
                        // Protect WebView worker thread from crashing
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    val currentTab = tabs.find { it.id == tabId }
                    val isDesktop = (currentTab?.isDesktopSite == true)

                    if (isDesktop) {
                        val desktopPreHook = UserAgentManager.getDesktopModePreHookScript(this@MainActivity, isDesktopSite = true)
                        (view ?: this@apply).evaluateJavascript(desktopPreHook, null)
                    }

                    val polyfill = SiteConfigManager.getPrivacyPolyfillScript(this@MainActivity)
                    (view ?: this@apply).evaluateJavascript(polyfill, null)

                    if (AntiFingerprintManager.isAntiFingerprintEnabled(this@MainActivity)) {
                        val antiFpScript = AntiFingerprintManager.getAntiFingerprintScript(this@MainActivity, isDesktopSite = isDesktop)
                        if (antiFpScript.isNotBlank()) {
                            (view ?: this@apply).evaluateJavascript(antiFpScript, null)
                        }
                    }

                    if (AdBlockManager.isAdBlockEnabled(this@MainActivity) && AdBlockManager.isAntiAdblockEnabled(this@MainActivity)) {
                        val antiAdblockScript = AdBlockManager.getAntiAdblockScript()
                        (view ?: this@apply).evaluateJavascript(antiAdblockScript, null)
                    }

                    if (WebRtcManager.isWebRtcBlockEnabled(this@MainActivity)) {
                        val webRtcScript = WebRtcManager.getWebRtcBlockScript(this@MainActivity)
                        if (webRtcScript.isNotBlank()) {
                            (view ?: this@apply).evaluateJavascript(webRtcScript, null)
                        }
                    }

                    if (ScriptManager.isScriptsEnabled(this@MainActivity) && !url.isNullOrBlank()) {
                        val startScripts = ScriptManager.getMatchingScripts(this@MainActivity, url, "document-start")
                        for (script in startScripts) {
                            val code = script.getCode(this@MainActivity)
                            if (code.isNotBlank()) {
                                (view ?: this@apply).evaluateJavascript(code, null)
                            }
                        }
                    }

                    if (tabId == activeTabId) {
                        updateNavigationHistoryState()
                        if (!url.isNullOrBlank() && url != "about:blank") {
                            currentUrlState.value = url
                            showBrowsingScreen(resetTaskbar = false)
                            isPageLoadingState.value = true
                            loadingProgressState.floatValue = 0.15f
                        }
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val currentTab = tabs.find { it.id == tabId }
                    val isDesktop = (currentTab?.isDesktopSite == true)

                    if (isDesktop) {
                        val desktopPostHook = UserAgentManager.getDesktopModePostHookScript()
                        (view ?: this@apply).evaluateJavascript(desktopPostHook, null)
                    }

                    if (AdBlockManager.isAdBlockEnabled(this@MainActivity) && AdBlockManager.isAntiAdblockEnabled(this@MainActivity)) {
                        val antiAdblockScript = AdBlockManager.getAntiAdblockScript()
                        (view ?: this@apply).evaluateJavascript(antiAdblockScript, null)
                    }

                    if (ScriptManager.isScriptsEnabled(this@MainActivity) && !url.isNullOrBlank()) {
                        val endScripts = ScriptManager.getMatchingScripts(this@MainActivity, url, "document-end")
                        for (script in endScripts) {
                            val code = script.getCode(this@MainActivity)
                            if (code.isNotBlank()) {
                                (view ?: this@apply).evaluateJavascript(code, null)
                            }
                        }
                    }
                    if (tabId == activeTabId) {
                        updateNavigationHistoryState()
                        isPageLoadingState.value = false
                        loadingProgressState.floatValue = 0f
                        if (isPullRefreshingState.value) {
                            isPullRefreshingState.value = false
                            webContainer.setRefreshing(false)
                        }
                        val pageTitle = view?.title ?: ""
                        val tab = tabs.find { it.id == tabId } ?: getActiveTab()
                        if (tab != null) {
                            tab.title = if (pageTitle.isNotBlank()) pageTitle else if (tab.isIncognito) "Incognito" else "Blank page"
                            tab.url = url ?: ""
                            val idx = tabsListState.indexOfFirst { it.id == tabId }
                            if (idx != -1) {
                                tabsListState[idx] = tabsListState[idx].copy(title = tab.title, url = url ?: "")
                            }
                            if (url == "about:blank") {
                                showHomeScreen()
                            } else {
                                currentUrlState.value = url ?: ""
                            }
                            if (tab.pendingReaderMode && !url.isNullOrBlank() && url != "about:blank") {
                                tab.pendingReaderMode = false
                                enterReaderMode(tab)
                            }
                        }
                        val isSandboxActive = SandboxManager.isSandboxEnabled(this@MainActivity)
                        if (isIncognito && !url.isNullOrBlank() && url != "about:blank") {
                            HistoryManager.addIncognitoHistory(pageTitle, url)
                        } else if (!isIncognito && !isSandboxActive && !url.isNullOrBlank() && url != "about:blank") {
                            HistoryManager.addHistory(this@MainActivity, pageTitle, url)
                            val favBmp = view?.favicon
                            if (favBmp != null) {
                                FaviconManager.saveFavicon(this@MainActivity, url, favBmp)
                                if (tab != null && tab.favicon == null) {
                                    tab.favicon = favBmp
                                    val idx2 = tabsListState.indexOfFirst { it.id == tabId }
                                    if (idx2 != -1) {
                                        tabsListState[idx2] = tabsListState[idx2].copy(favicon = favBmp)
                                    }
                                }
                            } else {
                                FaviconManager.loadFaviconAsync(this@MainActivity, url) { netFav ->
                                    if (tab != null && tab.favicon == null) {
                                        tab.favicon = netFav
                                        val idx2 = tabsListState.indexOfFirst { it.id == tabId }
                                        if (idx2 != -1) {
                                            tabsListState[idx2] = tabsListState[idx2].copy(favicon = netFav)
                                        }
                                    }
                                }
                            }
                        }
                        view?.postDelayed({
                            captureActiveTabSnapshot()
                        }, 400)
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message?
                ): Boolean {
                    if (resultMsg == null) return false

                    val isPopupsBlocked = SiteConfigManager.isPopupsBlocked(this@MainActivity)
                    if (isPopupsBlocked && !isUserGesture) {
                        return false
                    }

                    val currentTab = tabs.find { it.id == tabId }
                    val isIncog = currentTab?.isIncognito ?: (getActiveTab()?.isIncognito ?: false)
                    val isDesktop = currentTab?.isDesktopSite ?: false

                    val newTab = createNewTab(
                        url = "about:blank",
                        title = "Loading...",
                        isIncognito = isIncog,
                        isDesktopSite = isDesktop,
                        switchToTab = true
                    )
                    showBrowsingScreen(resetTaskbar = false)

                    val transport = resultMsg.obj as? WebView.WebViewTransport
                    if (transport != null) {
                        transport.webView = newTab.webView
                        resultMsg.sendToTarget()
                        return true
                    }
                    return false
                }

                override fun onCloseWindow(window: WebView?) {
                    super.onCloseWindow(window)
                    val tabToClose = tabs.find { it.webView == window || it.id == tabId }
                    if (tabToClose != null) {
                        closeTab(tabToClose)
                    }
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (tabId == activeTabId) {
                        val prog = (newProgress / 100f).coerceIn(0f, 1f)
                        loadingProgressState.floatValue = prog
                        isPageLoadingState.value = newProgress < 100
                        if (newProgress >= 100) {
                            view?.postDelayed({
                                if (tabId == activeTabId && loadingProgressState.floatValue >= 1f) {
                                    isPageLoadingState.value = false
                                    loadingProgressState.floatValue = 0f
                                    if (isPullRefreshingState.value) {
                                        isPullRefreshingState.value = false
                                        webContainer.setRefreshing(false)
                                    }
                                }
                            }, 250)
                        }
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    val tab = tabs.find { it.id == tabId }
                    if (tab != null && !title.isNullOrBlank()) {
                        tab.title = title
                        val idx = tabsListState.indexOfFirst { it.id == tabId }
                        if (idx != -1) {
                            tabsListState[idx] = tabsListState[idx].copy(title = title)
                        }
                    }
                }

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    val tab = tabs.find { it.id == tabId }
                    if (tab != null && icon != null) {
                        tab.favicon = icon
                        val idx = tabsListState.indexOfFirst { it.id == tabId }
                        if (idx != -1) {
                            tabsListState[idx] = tabsListState[idx].copy(favicon = icon)
                        }
                        val currentUrl = view?.url ?: tab.url
                        FaviconManager.saveFavicon(this@MainActivity, currentUrl, icon)
                    }
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: android.webkit.GeolocationPermissions.Callback?
                ) {
                    val isSandboxActive = SandboxManager.isSandboxEnabled(this@MainActivity)
                    val locPerm = SiteConfigManager.getLocationPermission(this@MainActivity)
                    if (TorManager.isTorEnabled(this@MainActivity) || (isSandboxActive && SandboxManager.isBlockHardware(this@MainActivity)) || locPerm == "block") {
                        callback?.invoke(origin, false, false)
                    } else if (locPerm == "allow") {
                        callback?.invoke(origin, true, false)
                    } else {
                        super.onGeolocationPermissionsShowPrompt(origin, callback)
                    }
                }

                override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                    val isSandboxActive = SandboxManager.isSandboxEnabled(this@MainActivity)
                    if (isSandboxActive && SandboxManager.isBlockHardware(this@MainActivity)) {
                        request?.deny()
                        return
                    }
                    val resources = request?.resources ?: emptyArray()
                    val camPerm = SiteConfigManager.getCameraPermission(this@MainActivity)
                    val micPerm = SiteConfigManager.getMicrophonePermission(this@MainActivity)
                    val protectedPerm = SiteConfigManager.isProtectedContentEnabled(this@MainActivity)

                    val allowed = resources.filter { res ->
                        when (res) {
                            android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE -> camPerm != "block"
                            android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE -> micPerm != "block"
                            android.webkit.PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> protectedPerm
                            else -> true
                        }
                    }.toTypedArray()

                    if (allowed.isNotEmpty()) {
                        request?.grant(allowed)
                    } else {
                        request?.deny()
                    }
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    showCustomView(view, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, callback)
                }

                override fun onShowCustomView(
                    view: View?,
                    requestedOrientation: Int,
                    callback: CustomViewCallback?
                ) {
                    showCustomView(view, requestedOrientation, callback)
                }

                override fun onHideCustomView() {
                    hideCustomView()
                }

                override fun getDefaultVideoPoster(): Bitmap? {
                    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                }
            }
        }

        val tab = WebTab(
            id = tabId,
            title = title,
            url = url,
            webView = webView,
            isIncognito = isIncognito,
            isDesktopSite = isDesktopSite,
            pendingReaderMode = pendingReaderMode
        )

        if (isIncognito) {
            checkNotificationPermission()
        }

        tabs.add(tab)
        tabsListState.add(tab)
        if (switchToTab) {
            switchTab(tab)
        }
        updateTabCount()
        updateIncognitoNotification()

        if (url != "about:blank") {
            webView.loadUrl(url)
        }

        return tab
    }

    private fun captureActiveTabSnapshot() {
        val activeTab = getActiveTab() ?: return
        try {
            val wv = activeTab.webView
            if (wv.isAttachedToWindow && wv.width > 0 && wv.height > 0 && wv.visibility == View.VISIBLE) {
                val scale = 0.4f
                val w = (wv.width * scale).toInt().coerceIn(1, 480)
                val h = (wv.height * scale).toInt().coerceIn(1, 800)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
                val canvas = android.graphics.Canvas(bmp)
                canvas.scale(scale, scale)
                wv.draw(canvas)
                activeTab.snapshot = bmp
                val idx = tabsListState.indexOfFirst { it.id == activeTab.id }
                if (idx != -1) {
                    tabsListState[idx] = activeTab.copy(snapshot = bmp)
                }
            }
        } catch (t: Throwable) {
            // Protect against any native Skia/HWUI draw crash or OOM
        }
    }

    private fun switchTab(tab: WebTab) {
        if (customView != null) {
            hideCustomView()
        }
        isFloatingBubbleState.value = false
        isPullRefreshingState.value = false
        webContainer.setRefreshing(false)
        captureActiveTabSnapshot()
        activeTabId = tab.id
        activeTabIdState.value = tab.id
        webContainer.removeAllViews()
        webContainer.addView(tab.webView)
        isCurrentTabIncognito.value = tab.isIncognito
        ThemeManager.setIncognito(tab.isIncognito)
        applyNightModeToWebView(tab.webView, if (tab.isIncognito) true else isNightModeActive())

        val prog = tab.webView.progress
        loadingProgressState.floatValue = (prog / 100f).coerceIn(0f, 1f)
        isPageLoadingState.value = prog in 1..99
        updateNavigationHistoryState()

        if (tab.url == "about:blank" || tab.url.isEmpty()) {
            showHomeScreen()
        } else {
            showBrowsingScreen()
        }

        if (isFindInPageVisibleState.value) {
            setupWebViewFindListener(tab.webView)
            val query = findInPageQueryState.value
            if (query.isNotEmpty()) {
                tab.webView.findAllAsync(query)
            }
        }
    }

    private fun closeTab(tab: WebTab) {
        if (customView != null) {
            hideCustomView()
        }
        val index = tabs.indexOf(tab)
        if (index == -1) return

        val isTabIncognito = tab.isIncognito
        val isSandboxActive = SandboxManager.isSandboxEnabled(this)
        if (isSandboxActive && SandboxManager.isForgetOnSiteCloseEnabled(this)) {
            SandboxManager.clearSiteData(this, tab.webView)
        }
        tab.webView.destroy()
        tabs.removeAt(index)
        tabsListState.removeIf { it.id == tab.id }

        if (isTabIncognito && tabs.none { it.isIncognito }) {
            cleanupIncognitoProfileData()
            HistoryManager.clearIncognitoHistory()
        }
        updateIncognitoNotification()

        val wasActive = (tab.id == activeTabId)

        if (tabs.isEmpty()) {
            activeTabId = ""
            activeTabIdState.value = ""
            isCurrentTabIncognito.value = false
            ThemeManager.setIncognito(false)
            webContainer.removeAllViews()
            if (!isTabsGridVisible) {
                val newTab = createNewTab()
                switchTab(newTab)
            }
        } else if (wasActive) {
            val newIndex = if (index >= tabs.size) tabs.size - 1 else index
            val newActiveTab = tabs[newIndex]
            activeTabId = newActiveTab.id
            activeTabIdState.value = newActiveTab.id
            webContainer.removeAllViews()
            webContainer.addView(newActiveTab.webView)
            isCurrentTabIncognito.value = newActiveTab.isIncognito
            ThemeManager.setIncognito(newActiveTab.isIncognito)
            applyNightModeToWebView(newActiveTab.webView, if (newActiveTab.isIncognito) true else isNightModeActive())

            if (!isTabsGridVisible) {
                if (newActiveTab.url == "about:blank" || newActiveTab.url.isEmpty()) {
                    showHomeScreen()
                } else {
                    showBrowsingScreen()
                }
            }
        }
        updateTabCount()
        updateNavigationHistoryState()
    }

    private fun closeAllIncognitoTabs() {
        if (customView != null) {
            hideCustomView()
        }
        val incognitoTabsList = tabs.filter { it.isIncognito }
        if (incognitoTabsList.isEmpty()) return

        cleanupIncognitoProfileData()
        HistoryManager.clearIncognitoHistory()

        for (tab in incognitoTabsList) {
            tab.webView.destroy()
            tabs.remove(tab)
            tabsListState.removeIf { it.id == tab.id }
        }
        updateIncognitoNotification()

        val remainingNormalTab = tabs.firstOrNull { !it.isIncognito }
        if (remainingNormalTab != null) {
            switchTab(remainingNormalTab)
        } else {
            val newTab = createNewTab("about:blank", "Blank page", isIncognito = false)
            switchTab(newTab)
            showHomeScreen()
        }
        updateTabCount()
        updateNavigationHistoryState()
    }

    private fun cleanupIncognitoProfileData() {
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                val profileStore = ProfileStore.getInstance()
                val incognitoProfile = profileStore.getProfile("incognito_profile")
                if (incognitoProfile != null) {
                    incognitoProfile.cookieManager.removeAllCookies(null)
                    incognitoProfile.cookieManager.flush()
                    incognitoProfile.webStorage.deleteAllData()
                    incognitoProfile.geolocationPermissions.clearAll()
                    profileStore.deleteProfile("incognito_profile")
                }
            }
        } catch (e: Exception) {
            // Ignore if profile cleanup fails
        }
    }

    private fun updateTabCount() {
        // Tab count updated
    }

    private fun getActiveTab(): WebTab? {
        return tabs.find { it.id == activeTabId }
    }

    private fun showHomeScreen() {
        closeAllOverlays()
        isFloatingBubbleState.value = false
        isHistoryVisible = false
        isTabsGridVisible = false
        isPullRefreshingState.value = false
        webContainer.setRefreshing(false)
        webContainer.isPullToRefreshEnabled = false
        currentBottomNavTab.intValue = 0
        if (tabs.isEmpty()) {
            val newTab = createNewTab()
            switchTab(newTab)
            return
        }
        val activeTab = getActiveTab()
        val isIncog = activeTab?.isIncognito ?: false
        isCurrentTabIncognito.value = isIncog
        ThemeManager.setIncognito(isIncog)
        isBrowsingState.value = false
        isTaskbarVisibleState.value = true
        currentUrlState.value = ""
        updateNavigationHistoryState()
        homeScreenOverlay.visibility = View.VISIBLE
        webContainer.visibility = View.GONE
        historyContainer.visibility = View.GONE
        tabsGridContainer.visibility = View.GONE
    }

    private fun showBrowsingScreen(resetTaskbar: Boolean = false) {
        closeAllOverlays()
        isHistoryVisible = false
        isTabsGridVisible = false
        webContainer.isPullToRefreshEnabled = true
        currentBottomNavTab.intValue = 0
        val activeTab = getActiveTab()
        val isIncog = activeTab?.isIncognito ?: false
        isCurrentTabIncognito.value = isIncog
        ThemeManager.setIncognito(isIncog)
        val wasBrowsing = isBrowsingState.value
        isBrowsingState.value = true
        if (resetTaskbar || !wasBrowsing) {
            isTaskbarVisibleState.value = false
        }
        currentUrlState.value = activeTab?.url ?: ""
        updateNavigationHistoryState()
        homeScreenOverlay.visibility = View.VISIBLE
        webContainer.visibility = View.VISIBLE
        historyContainer.visibility = View.GONE
        tabsGridContainer.visibility = View.GONE
    }

    private fun showHistoryScreen() {
        closeAllOverlays()
        isFloatingBubbleState.value = false
        isPullRefreshingState.value = false
        webContainer.setRefreshing(false)
        webContainer.isPullToRefreshEnabled = false
        captureActiveTabSnapshot()
        isHistoryVisible = true
        isTabsGridVisible = false
        currentBottomNavTab.intValue = 2
        homeScreenOverlay.visibility = View.GONE
        webContainer.visibility = View.GONE
        historyContainer.visibility = View.VISIBLE
        tabsGridContainer.visibility = View.GONE
        hideKeyboard()
    }

    private fun showTabsGridScreen() {
        closeAllOverlays()
        isFloatingBubbleState.value = false
        isPullRefreshingState.value = false
        webContainer.setRefreshing(false)
        webContainer.isPullToRefreshEnabled = false
        captureActiveTabSnapshot()
        isTabsGridVisible = true
        isHistoryVisible = false
        currentBottomNavTab.intValue = 1
        homeScreenOverlay.visibility = View.GONE
        webContainer.visibility = View.GONE
        historyContainer.visibility = View.GONE
        tabsGridContainer.visibility = View.VISIBLE
        hideKeyboard()
    }

    private fun showTabsSheet() {
        val activeTab = getActiveTab()
        val isIncog = activeTab?.isIncognito == true
        val filteredTabs = if (isIncog) tabs.filter { it.isIncognito } else tabs.filter { !it.isIncognito }
        val sheet = TabsBottomSheet(
            context = this,
            tabs = filteredTabs,
            activeTabId = activeTabId,
            isIncognito = isIncog,
            onSelectTab = { selectedTab ->
                switchTab(selectedTab)
            },
            onCloseTab = { closedTab ->
                closeTab(closedTab)
            },
            onNewTab = { newIsIncog ->
                val newTab = createNewTab(
                    url = "about:blank",
                    title = if (newIsIncog) "Incognito" else "Blank page",
                    isIncognito = newIsIncog
                )
                switchTab(newTab)
                showHomeScreen()
            }
        )
        sheet.show()
    }

    private fun showMenuSheet() {
        if (currentMenuSheet?.isShowing == true) {
            currentMenuSheet?.dismiss()
            currentMenuSheet = null
            return
        }
        val activeTab = getActiveTab()
        currentBottomNavTab.intValue = 3
        val sheet = MenuBottomSheet(
            context = this,
            onOpenSettings = {
                openSettings()
            },
            onOpenMenu = {
                openMenu()
            },
            onReload = {
                if (activeTab?.url == "about:blank") {
                    showHomeScreen()
                } else {
                    showBrowsingScreen()
                }
            },
            onExit = {
                finish()
            },
            onClearData = {
                showClearDataDialog()
            },
            onOpenHistory = {
                showHistoryScreen()
            },
            onToggleUserAgent = { enabled ->
                updateAllTabsUserAgent()
                getActiveTab()?.webView?.reload()
            },
            onOpenUserAgentSettings = {
                openUserAgentSettings()
            },
            onOpenAdBlockingSettings = {
                openAdBlockingSettings()
            },
            onOpenTorSettings = {
                openTorSettings()
            },
            onToggleTor = { enabled ->
                updateAllTabsPrivacySettings()
                getActiveTab()?.webView?.reload()
            },
            isTorActive = TorManager.isTorEnabled(this),
            onToggleIncognito = {
                toggleIncognitoMode()
            },
            isIncognitoActive = (activeTab?.isIncognito == true),
            isDesktopSiteActive = (activeTab?.isDesktopSite == true),
            onToggleDesktopSite = { enabled ->
                val tab = getActiveTab()
                if (tab != null) {
                    tab.isDesktopSite = enabled
                    val idx = tabsListState.indexOfFirst { it.id == tab.id }
                    if (idx != -1) {
                        tabsListState[idx] = tabsListState[idx].copy(isDesktopSite = enabled)
                    }
                    UserAgentManager.applyToWebView(this, tab.webView, isDesktopSite = enabled)
                    if (tab.url != "about:blank" && tab.url.isNotEmpty()) {
                        tab.webView.reload()
                    }
                }
            },
            onToggleNightMode = { isDark ->
                updateAllTabsNightMode(isDark)
            },
            isOrientationActive = isLandscapeForced || (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE),
            onToggleOrientation = { enableLandscape ->
                toggleOrientation(enableLandscape)
            },
            onShare = {
                shareCurrentUrl()
            },
            onToggleSandbox = { enabled ->
                isSandboxActiveState.value = enabled
                resetSessionForSandbox(enabled)
            },
            onOpenSandboxSettings = {
                openSettings("sandbox")
            },
            isSandboxActive = SandboxManager.isSandboxEnabled(this),
            onOpenDownloads = {
                openDownloads()
            },
            onFindInPage = {
                openFindInPage()
            },
            onSavePage = {
                saveCurrentPage()
            },
            onPrint = {
                printCurrentPage()
            },
            isReaderModeActive = (activeTab?.isReaderMode == true),
            onToggleReaderMode = {
                toggleReaderMode()
            },
            onOpenSiteConfig = {
                openSettings("site_config")
            },
            onOpenScriptsSettings = {
                openSettings("scripts")
            },
            onOpenQrCode = {
                showQrCodeSheet()
            },
            onTextSizeChanged = { zoom ->
                getActiveTab()?.webView?.settings?.textZoom = zoom
            },
            onTranslatePage = { targetLang ->
                getActiveTab()?.webView?.let { webView ->
                    TranslationManager.translatePage(webView, targetLang)
                }
            },
            onRestoreOriginalPage = {
                getActiveTab()?.webView?.let { webView ->
                    TranslationManager.restoreOriginal(webView)
                }
            }
        )
        sheet.setOnDismissListener {
            if (currentMenuSheet == sheet) {
                currentMenuSheet = null
            }
            restoreNavTabAfterOverlay()
        }
        currentMenuSheet = sheet
        sheet.show()
    }

    private fun showQrCodeSheet() {
        if (currentQrCodeSheet?.isShowing == true) {
            currentQrCodeSheet?.dismiss()
            currentQrCodeSheet = null
            return
        }
        closeAllOverlays()
        val activeTab = getActiveTab()
        val rawUrl = activeTab?.url?.takeIf { it.isNotBlank() && it != "about:blank" }
            ?: currentUrlState.value.takeIf { it.isNotBlank() && it != "about:blank" }
            ?: ""

        currentBottomNavTab.intValue = 3
        val sheet = QrCodeBottomSheet(
            context = this,
            url = rawUrl,
            pageTitle = activeTab?.title,
            onShare = {
                shareCurrentUrl()
            }
        )
        sheet.setOnDismissListener {
            if (currentQrCodeSheet == sheet) {
                currentQrCodeSheet = null
            }
            restoreNavTabAfterOverlay()
        }
        currentQrCodeSheet = sheet
        sheet.show()
    }

    private fun toggleReaderMode() {
        val tab = getActiveTab() ?: return
        if (tab.isReaderMode) {
            exitReaderMode(tab)
        } else {
            enterReaderMode(tab)
        }
    }

    private fun enterReaderMode(tab: WebTab) {
        val currentUrl = tab.url.takeIf { it.isNotBlank() && it != "about:blank" } ?: currentUrlState.value
        if (currentUrl.isBlank() || currentUrl == "about:blank") {
            Toast.makeText(this, "No article to display in Reader Mode", Toast.LENGTH_SHORT).show()
            return
        }

        tab.originalUrlBeforeReader = currentUrl
        Toast.makeText(this, "Loading Reader Mode...", Toast.LENGTH_SHORT).show()

        val isDark = isNightModeActive()
        tab.webView.evaluateJavascript(ReaderModeManager.getExtractionScript()) { resultJson ->
            val article = ReaderModeManager.parseExtractionResult(resultJson ?: "")
            if (article != null && article.contentHtml.isNotBlank()) {
                tab.isReaderMode = true
                val readerHtml = ReaderModeManager.generateReaderHtml(article, isDark)
                tab.webView.loadDataWithBaseURL(
                    tab.originalUrlBeforeReader,
                    readerHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            } else {
                Toast.makeText(this, "Could not extract article for Reader Mode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exitReaderMode(tab: WebTab?) {
        if (tab == null) return
        tab.isReaderMode = false
        val originalUrl = tab.originalUrlBeforeReader
        tab.originalUrlBeforeReader = null
        if (!originalUrl.isNullOrBlank() && originalUrl != "about:blank") {
            tab.webView.loadUrl(originalUrl)
        } else {
            tab.webView.reload()
        }
    }

    private fun printCurrentPage() {
        val activeTab = getActiveTab()
        val url = activeTab?.url ?: currentUrlState.value
        if (activeTab == null || url.isBlank() || url == "about:blank") {
            Toast.makeText(this, "No web page to print", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val printManager = getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
            if (printManager == null) {
                Toast.makeText(this, "Print service unavailable", Toast.LENGTH_SHORT).show()
                return
            }

            val pageTitle = activeTab.title.takeIf { it.isNotBlank() && it != "Blank page" && it != "Incognito" }
                ?: Uri.parse(url).host?.takeIf { !it.isNullOrBlank() }
                ?: "Document"

            val jobName = "${getString(R.string.app_name)} - $pageTitle"
            val printAdapter = activeTab.webView.createPrintDocumentAdapter(jobName)
            val printAttributes = android.print.PrintAttributes.Builder()
                .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                .build()

            printManager.print(jobName, printAdapter, printAttributes)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to print: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCurrentPage() {
        val activeTab = getActiveTab()
        val url = activeTab?.url ?: currentUrlState.value
        if (activeTab == null || url.isBlank() || url == "about:blank") {
            Toast.makeText(this, "No web page to save", Toast.LENGTH_SHORT).show()
            return
        }

        val pageTitle = activeTab.title.takeIf { it.isNotBlank() && it != "Blank page" && it != "Incognito" }
            ?: Uri.parse(url).host?.takeIf { !it.isNullOrBlank() }
            ?: "page"

        val sanitizedTitle = pageTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(50)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val baseFileName = "${sanitizedTitle}_$timeStamp.mht"

        val savedDir = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir, "SavedPages").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(savedDir, baseFileName)

        Toast.makeText(this, "Saving page...", Toast.LENGTH_SHORT).show()

        activeTab.webView.saveWebArchive(targetFile.absolutePath, false) { savedPath ->
            if (savedPath != null) {
                val file = File(savedPath)
                val size = if (file.exists()) file.length() else 0L
                DownloadsManager.addSavedPageDownload(
                    context = this@MainActivity,
                    title = sanitizedTitle,
                    originalUrl = url,
                    filePath = savedPath,
                    fileSize = size
                )
                Toast.makeText(this@MainActivity, "Page saved: ${file.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to save page", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareCurrentUrl() {
        val activeTab = getActiveTab()
        val url = activeTab?.url?.takeIf { it.isNotBlank() && it != "about:blank" }
            ?: currentUrlState.value.takeIf { it.isNotBlank() && it != "about:blank" }

        if (url.isNullOrBlank()) {
            Toast.makeText(this, "No URL to share", Toast.LENGTH_SHORT).show()
            return
        }

        val pageTitle = activeTab?.title?.takeIf { it.isNotBlank() && it != "Blank page" && it != "Incognito" } ?: url
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_SUBJECT, pageTitle)
            putExtra(Intent.EXTRA_TITLE, pageTitle)
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share link")
        startActivity(shareIntent)
    }

    private fun showLinkContextMenu(
        targetUrl: String?,
        linkText: String?,
        imageUrl: String?
    ) {
        val activeTab = getActiveTab()
        val currentIsIncog = activeTab?.isIncognito == true
        val sheet = LinkContextMenuBottomSheet(
            context = this,
            targetUrl = targetUrl,
            linkText = linkText,
            imageUrl = imageUrl,
            onOpenInNewTab = { url ->
                createNewTab(url = url, isIncognito = currentIsIncog, switchToTab = true)
                showBrowsingScreen()
            },
            onOpenInIncognito = { url ->
                createNewTab(url = url, isIncognito = true, switchToTab = true)
                showBrowsingScreen()
            },
            onOpenInBackground = { url ->
                createNewTab(url = url, isIncognito = currentIsIncog, switchToTab = false)
                Toast.makeText(this, R.string.tab_opened_in_background, Toast.LENGTH_SHORT).show()
            },
            onPreviewPage = { url ->
                showPagePreview(url)
            },
            onCopyLinkAddress = { url ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Link Address", url))
                Toast.makeText(this, R.string.link_copied_toast, Toast.LENGTH_SHORT).show()
            },
            onCopyLinkText = { text ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Link Text", text))
                Toast.makeText(this, R.string.link_text_copied_toast, Toast.LENGTH_SHORT).show()
            },
            onDownloadLink = { url ->
                val currentTab = tabs.find { it.id == activeTabId }
                val userAgent = currentTab?.webView?.settings?.userAgentString
                val currentUrl = currentTab?.webView?.url
                val guessName = android.webkit.URLUtil.guessFileName(url, null, null)
                val item = DownloadsManager.addDownload(
                    context = this,
                    url = url,
                    customFileName = guessName,
                    userAgent = userAgent,
                    referer = currentUrl
                )
                if (item != null) {
                    Toast.makeText(this, "Download started: ${item.fileName}", Toast.LENGTH_SHORT).show()
                }
            },
            onShareLink = { url ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                    if (!linkText.isNullOrBlank()) {
                        putExtra(Intent.EXTRA_SUBJECT, linkText)
                    }
                }
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_link)))
            },
            onOpenInReadingMode = { url ->
                createNewTab(url = url, isIncognito = currentIsIncog, switchToTab = true, pendingReaderMode = true)
                showBrowsingScreen()
            },
            onOpenImageInNewTab = { imgUrl ->
                createNewTab(url = imgUrl, isIncognito = currentIsIncog, switchToTab = true)
                showBrowsingScreen()
            },
            onDownloadImage = { imgUrl ->
                val currentTab = tabs.find { it.id == activeTabId }
                val userAgent = currentTab?.webView?.settings?.userAgentString
                val currentUrl = currentTab?.webView?.url
                val guessName = android.webkit.URLUtil.guessFileName(imgUrl, null, "image/*")
                val item = DownloadsManager.addDownload(
                    context = this,
                    url = imgUrl,
                    customFileName = guessName,
                    userAgent = userAgent,
                    mimeType = "image/*",
                    referer = currentUrl
                )
                if (item != null && !imgUrl.startsWith("data:", ignoreCase = true)) {
                    Toast.makeText(this, "Download started: ${item.fileName}", Toast.LENGTH_SHORT).show()
                }
            },
            onCopyImageAddress = { imgUrl ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Image Address", imgUrl))
                Toast.makeText(this, R.string.image_link_copied_toast, Toast.LENGTH_SHORT).show()
            },
            onShareImage = { imgUrl ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, imgUrl)
                }
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_image)))
            }
        )
        sheet.show()
    }

    private fun showPagePreview(url: String) {
        val activeTab = getActiveTab()
        val isIncog = activeTab?.isIncognito == true
        val isDark = isNightModeActive()
        val previewSheet = PagePreviewBottomSheet(
            context = this,
            previewUrl = url,
            isIncognito = isIncog,
            isDarkTheme = isDark,
            onOpenInTab = { finalUrl ->
                createNewTab(url = finalUrl, isIncognito = isIncog, switchToTab = true)
                showBrowsingScreen()
            },
            onShare = { shareUrl ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareUrl)
                }
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share)))
            }
        )
        previewSheet.show()
    }

    private fun toggleOrientation(enableLandscape: Boolean) {
        isLandscapeForced = enableLandscape
        requestedOrientation = if (enableLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun isNightModeActive(): Boolean {
        val activeTab = getActiveTab()
        if (activeTab?.isIncognito == true || ThemeManager.isIncognitoActive) {
            return true
        }
        val isSystemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return ThemeManager.isDark(isSystemDark)
    }

    private fun applyNightModeToWebView(webView: WebView, isDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            webView.settings.isAlgorithmicDarkeningAllowed = isDark
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            webView.settings.forceDark = if (isDark) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
        }
    }

    private fun updateAllTabsNightMode(isDark: Boolean = isNightModeActive()) {
        for (tab in tabs) {
            val tabDark = if (tab.isIncognito) true else isDark
            applyNightModeToWebView(tab.webView, tabDark)
            if (tab.isReaderMode) {
                tab.webView.evaluateJavascript(
                    "document.body.className = '${if (tabDark) "dark" else "light"}';",
                    null
                )
            }
        }
    }

    private fun showClearDataDialog() {
        ClearDataDialog(this) { selectedOptions ->
            performClearData(selectedOptions)
        }.show()
    }

    private fun performClearData(selectedOptions: Set<String>, showToast: Boolean = true) {
        if (selectedOptions.contains("cache")) {
            for (tab in tabs) {
                tab.webView.clearCache(true)
            }
            try {
                cacheDir?.deleteRecursively()
            } catch (e: Exception) {
                // ignore
            }
        }

        if (selectedOptions.contains("form_data")) {
            for (tab in tabs) {
                tab.webView.clearFormData()
            }
        }

        if (selectedOptions.contains("history")) {
            for (tab in tabs) {
                tab.webView.clearHistory()
            }
            HistoryManager.clearHistory(this)
        }

        if (selectedOptions.contains("closed_tabs")) {
            // Closed tabs history reset
        }

        if (selectedOptions.contains("web_storage")) {
            android.webkit.WebStorage.getInstance().deleteAllData()
            android.webkit.GeolocationPermissions.getInstance().clearAll()
        }

        if (selectedOptions.contains("cookies")) {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
        }

        if (selectedOptions.contains("app_cache")) {
            try {
                codeCacheDir?.deleteRecursively()
            } catch (e: Exception) {
                // ignore
            }
        }

        if (showToast) {
            android.widget.Toast.makeText(this, "Browser data cleared", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAllTabsUserAgent() {
        for (tab in tabs) {
            UserAgentManager.applyToWebView(this, tab.webView, tab.isDesktopSite)
        }
    }

    private fun updateAllTabsPrivacySettings() {
        val torEnabled = TorManager.isTorEnabled(this)
        val sandboxEnabled = SandboxManager.isSandboxEnabled(this)
        for (tab in tabs) {
            SiteConfigManager.applyToWebView(this, tab.webView)
            tab.webView.settings.apply {
                domStorageEnabled = !tab.isIncognito && !sandboxEnabled
                cacheMode = if (tab.isIncognito || sandboxEnabled) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                setGeolocationEnabled(!torEnabled && !sandboxEnabled && SiteConfigManager.getLocationPermission(this@MainActivity) != "block")
                allowFileAccess = !sandboxEnabled
            }
        }
    }

    private fun createSettingsComposeView(initialScreen: String = "main"): View {
        return ComposeView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                RenTheme {
                    SettingsHost(
                        initialScreen = initialScreen,
                        onClose = {
                            val newSandbox = SandboxManager.isSandboxEnabled(this@MainActivity)
                            if (isSandboxActiveState.value != newSandbox) {
                                isSandboxActiveState.value = newSandbox
                                resetSessionForSandbox(newSandbox)
                            } else {
                                updateAllTabsPrivacySettings()
                            }
                            closeSettings()
                        },
                        onClearData = { showClearDataDialog() },
                        onUserAgentChanged = { updateAllTabsUserAgent() }
                    )
                }
            }
        }
    }

    private fun openSettings(initialScreen: String = "main") {
        closeAllOverlays()
        webContainer.isPullToRefreshEnabled = false
        isSettingsOpen = true
        currentBottomNavTab.intValue = 3
        val settingsView = createSettingsComposeView(initialScreen)
        contentOverlayContainer.removeAllViews()
        contentOverlayContainer.addView(settingsView)
        contentOverlayContainer.visibility = View.VISIBLE
        taskBarView.bringToFront()
    }

    private fun openUserAgentSettings() {
        openSettings("user_agent")
    }

    private fun openAdBlockingSettings() {
        openSettings("ad_blocking")
    }

    private fun openTorSettings() {
        openSettings("tor")
    }

    private fun createMenuComposeView(): View {
        return ComposeView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                RenTheme {
                    MenuHost(
                        onClose = { closeMenu() },
                        onClearData = { showClearDataDialog() },
                        onUserAgentChanged = { updateAllTabsUserAgent() }
                    )
                }
            }
        }
    }

    private fun openMenu() {
        closeAllOverlays()
        webContainer.isPullToRefreshEnabled = false
        isMenuOpen = true
        currentBottomNavTab.intValue = 3
        val menuView = createMenuComposeView()
        contentOverlayContainer.removeAllViews()
        contentOverlayContainer.addView(menuView)
        contentOverlayContainer.visibility = View.VISIBLE
        taskBarView.bringToFront()
    }

    private fun closeMenu() {
        isMenuOpen = false
        contentOverlayContainer.removeAllViews()
        contentOverlayContainer.visibility = View.GONE
        restoreNavTabAfterOverlay()
    }

    private fun createDownloadsComposeView(): View {
        return ComposeView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                RenTheme {
                    DownloadsScreen(
                        onClose = { closeDownloads() },
                        onOpenItem = { item ->
                            if (item.filePath.isNotBlank() && (item.fileName.endsWith(".mht", true) || item.fileName.endsWith(".mhtml", true) || item.fileName.endsWith(".html", true))) {
                                closeDownloads()
                                navigateToUrl("file://${item.filePath}")
                            } else {
                                DownloadsManager.openDownloadedFile(this@MainActivity, item)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun openDownloads() {
        closeAllOverlays()
        webContainer.isPullToRefreshEnabled = false
        isDownloadsOpen = true
        currentBottomNavTab.intValue = 3
        val downloadsView = createDownloadsComposeView()
        contentOverlayContainer.removeAllViews()
        contentOverlayContainer.addView(downloadsView)
        contentOverlayContainer.visibility = View.VISIBLE
        taskBarView.bringToFront()
    }

    private fun closeDownloads() {
        isDownloadsOpen = false
        contentOverlayContainer.removeAllViews()
        contentOverlayContainer.visibility = View.GONE
        restoreNavTabAfterOverlay()
    }

    private fun closeSettings() {
        isSettingsOpen = false
        contentOverlayContainer.removeAllViews()
        contentOverlayContainer.visibility = View.GONE
        restoreNavTabAfterOverlay()
    }

    private fun closeAllOverlays() {
        currentMenuSheet?.dismiss()
        currentMenuSheet = null
        currentQrCodeSheet?.dismiss()
        currentQrCodeSheet = null
        isMenuOpen = false
        isSettingsOpen = false
        isDownloadsOpen = false
        contentOverlayContainer.removeAllViews()
        contentOverlayContainer.visibility = View.GONE
    }

    private fun restoreNavTabAfterOverlay() {
        webContainer.isPullToRefreshEnabled = isBrowsingState.value && !isHistoryVisible && !isTabsGridVisible
        if (isSettingsOpen || isMenuOpen || isDownloadsOpen || currentMenuSheet?.isShowing == true || currentQrCodeSheet?.isShowing == true) {
            currentBottomNavTab.intValue = 3
        } else if (isTabsGridVisible) {
            currentBottomNavTab.intValue = 1
        } else if (isHistoryVisible) {
            currentBottomNavTab.intValue = 2
        } else {
            currentBottomNavTab.intValue = 0
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let {
            imm?.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    @Deprecated("Deprecated in Java", ReplaceWith("onBackPressedDispatcher.onBackPressed()"))
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (customView != null) {
            hideCustomView()
            return
        }

        if (onBackPressedDispatcher.hasEnabledCallbacks()) {
            onBackPressedDispatcher.onBackPressed()
            return
        }

        if (isFindInPageVisibleState.value) {
            closeFindInPage()
            return
        }

        if (isFloatingBubbleState.value) {
            isFloatingBubbleState.value = false
            return
        }

        if (isTabsGridVisible) {
            val activeTab = getActiveTab()
            if (activeTab?.url == "about:blank" || activeTab?.url.isNullOrEmpty()) {
                showHomeScreen()
            } else {
                showBrowsingScreen()
            }
            return
        }

        if (isHistoryVisible) {
            val activeTab = getActiveTab()
            if (activeTab?.url == "about:blank" || activeTab?.url.isNullOrEmpty()) {
                showHomeScreen()
            } else {
                showBrowsingScreen()
            }
            return
        }

        val activeTab = getActiveTab()
        if (activeTab != null && activeTab.url != "about:blank" && activeTab.webView.canGoBack()) {
            activeTab.webView.goBack()
        } else if (activeTab != null && activeTab.url != "about:blank") {
            activeTab.url = "about:blank"
            activeTab.title = if (activeTab.isIncognito) "Incognito" else "Blank page"
            activeTab.webView.loadUrl("about:blank")
            showHomeScreen()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        if (customView != null) {
            hideCustomView()
        }
    }

    override fun onDestroy() {
        if (customView != null) {
            hideCustomView()
        }
        IncognitoNotificationManager.cancelNotification(this)
        cleanupIncognitoProfileData()
        HistoryManager.clearIncognitoHistory()
        val clearOnExitOptions = ClearDataDialog.getClearOnExitOptions(this)
        if (clearOnExitOptions.isNotEmpty()) {
            performClearData(clearOnExitOptions, showToast = false)
        }
        super.onDestroy()
    }
}
