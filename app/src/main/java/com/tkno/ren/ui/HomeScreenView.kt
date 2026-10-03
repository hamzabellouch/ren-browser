package com.tkno.ren.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.tkno.ren.R
import com.tkno.ren.util.SearchEngine
import com.tkno.ren.util.SearchEngineManager
import com.tkno.ren.util.SearchSuggestionManager
import java.net.URI
import kotlin.math.roundToInt

private val ArrowBackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "arrow_back",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.White),
            fillAlpha = 1f,
            stroke = null,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(20f, 11f)
            horizontalLineTo(7.83f)
            lineTo(13.42f, 5.41f)
            lineTo(12f, 4f)
            lineTo(4f, 12f)
            lineTo(12f, 20f)
            lineTo(13.41f, 18.59f)
            lineTo(7.83f, 13f)
            horizontalLineTo(20f)
            verticalLineTo(11f)
            close()
        }
    }.build()
}

private val ArrowForwardIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "arrow_forward",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.White),
            fillAlpha = 1f,
            stroke = null,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12f, 4f)
            lineTo(10.59f, 5.41f)
            lineTo(16.17f, 11f)
            horizontalLineTo(4f)
            verticalLineTo(13f)
            horizontalLineTo(16.17f)
            lineTo(10.59f, 18.59f)
            lineTo(12f, 20f)
            lineTo(20f, 12f)
            lineTo(12f, 4f)
            close()
        }
    }.build()
}

private val ChevronUpDownIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "keyboard_arrow_down",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.White),
            fillAlpha = 1f,
            stroke = null,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(7.41f, 8.59f)
            lineTo(12f, 13.17f)
            lineTo(16.59f, 8.59f)
            lineTo(18f, 10f)
            lineTo(12f, 16f)
            lineTo(6f, 10f)
            lineTo(7.41f, 8.59f)
            close()
        }
    }.build()
}

data class ShortcutItem(
    val title: String,
    val url: String,
    val iconRes: Int? = null,
    val iconBgColor: Color = Color(0xFF282A30),
    val iconTint: Color? = Color.White,
    val letter: String? = null
)

@Composable
fun HomeScreenView(
    isBrowsing: Boolean,
    currentUrl: String,
    onSearch: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onReload: () -> Unit,
    onStopLoading: () -> Unit = {},
    onIncognitoClick: () -> Unit,
    isIncognitoActive: Boolean = false,
    onSandboxClick: () -> Unit = {},
    isSandboxActive: Boolean = false,
    isFloatingBubbleMode: Boolean = false,
    isPageLoading: Boolean = false,
    loadingProgress: Float = 0f,
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    onGoBack: () -> Unit = {},
    onGoForward: () -> Unit = {},
    isTaskbarVisible: Boolean = true,
    onToggleTaskbar: () -> Unit = {},
    onToggleFloatingBubble: () -> Unit = {},
    onBubblePositionChanged: (centerX: Float, centerY: Float, radius: Float) -> Unit = { _, _, _ -> },
    onSearchBarBoundsChanged: (left: Float, top: Float, right: Float, bottom: Float) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val prefs = remember { context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE) }
    var hideLabels by remember { mutableStateOf(prefs.getBoolean("hide_navigation_labels", false)) }
    var useClassicTaskbar by remember { mutableStateOf(prefs.getBoolean("use_classic_taskbar", false)) }

    val searchPrefs = remember { context.getSharedPreferences(SearchEngineManager.PREFS_NAME, Context.MODE_PRIVATE) }
    var selectedSearchEngine by remember { mutableStateOf(SearchEngineManager.getSelectedSearchEngine(context)) }
    var showSearchEngines by remember { mutableStateOf(false) }
    var allSearchEngines by remember { mutableStateOf(SearchEngineManager.getAllSearchEngines(context)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "hide_navigation_labels") {
                hideLabels = p.getBoolean("hide_navigation_labels", false)
            } else if (key == "use_classic_taskbar") {
                useClassicTaskbar = p.getBoolean("use_classic_taskbar", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    DisposableEffect(searchPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SearchEngineManager.KEY_SELECTED_ENGINE_ID || key == null) {
                selectedSearchEngine = SearchEngineManager.getSelectedSearchEngine(context)
                allSearchEngines = SearchEngineManager.getAllSearchEngines(context)
            }
        }
        searchPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            searchPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    var searchTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var shouldSelectAllOnNextFocus by remember { mutableStateOf(false) }
    var recentShortcuts by remember { mutableStateOf<List<ShortcutItem>>(emptyList()) }
    var showAddShortcutDialog by remember { mutableStateOf(false) }
    var shortcutToDelete by remember { mutableStateOf<ShortcutItem?>(null) }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Screen dimensions
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    val initialBubbleXPx = with(density) { 12.dp.toPx() }

    var bubbleDragX by remember { mutableFloatStateOf(initialBubbleXPx) }
    var bubbleDragY by remember { mutableFloatStateOf(0f) }

    // Query real-time search suggestions when typing
    LaunchedEffect(searchTextFieldValue.text, isSearchFocused, selectedSearchEngine) {
        val query = searchTextFieldValue.text.trim()
        if (isSearchFocused && query.length >= 2 && !query.startsWith("http://", ignoreCase = true) && !query.startsWith("https://", ignoreCase = true)) {
            val suggestions = SearchSuggestionManager.getSuggestions(context, query, selectedSearchEngine.id)
            searchSuggestions = suggestions
        } else {
            searchSuggestions = emptyList()
        }
    }

    // Sync searchTextFieldValue with currentUrl when browsing URL changes
    LaunchedEffect(currentUrl, isBrowsing) {
        if (isBrowsing && currentUrl.isNotBlank() && currentUrl != "about:blank") {
            searchTextFieldValue = TextFieldValue(
                text = currentUrl,
                selection = TextRange.Zero
            )
            showSearchEngines = false
        } else if (!isBrowsing) {
            searchTextFieldValue = TextFieldValue("")
        }
    }

    // Reset search query and suggestions whenever sandbox mode changes
    LaunchedEffect(isSandboxActive) {
        searchTextFieldValue = TextFieldValue("")
        showSearchEngines = false
        focusManager.clearFocus()
    }

    // Load recent history items or default shortcuts
    LaunchedEffect(Unit) {
        recentShortcuts = loadShortcuts(context)
    }

    // Animation progress: 0f = Center Home Screen, 1f = Bottom Bar Browsing
    val transitionProgress by animateFloatAsState(
        targetValue = if (isBrowsing) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "searchBarElevation"
    )

    // Animation progress for collapsing search bar into floating bubble
    val bubbleProgress by animateFloatAsState(
        targetValue = if (isBrowsing && isFloatingBubbleMode) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "bubbleCollapseProgress"
    )

    // Animated loading progress inside search capsule
    val animatedLoadingProgress by animateFloatAsState(
        targetValue = loadingProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "pageLoadingProgress"
    )

    val loadingProgressAlpha by animateFloatAsState(
        targetValue = if (isPageLoading && isBrowsing && animatedLoadingProgress > 0f) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "loadingProgressAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = if (isBrowsing) (1f - transitionProgress) else 1f))
            .statusBarsPadding()
    ) {
        val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val imeBottomDp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val isImeVisible = imeBottomDp > 0.dp

        val taskbarTargetHeight = if (isTaskbarVisible) (if (hideLabels) 54.dp else 64.dp) else 0.dp
        val taskbarTargetMargin = if (isTaskbarVisible) (if (useClassicTaskbar) 0.dp else 16.dp) else 0.dp
        val taskbarTargetGap = if (isTaskbarVisible) 8.dp else 0.dp

        val animatedTaskbarOffset by animateDpAsState(
            targetValue = taskbarTargetHeight + taskbarTargetMargin + taskbarTargetGap,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "taskbarOffsetAnim"
        )

        val bottomOffset = if (isImeVisible) {
            imeBottomDp + 8.dp
        } else {
            animatedTaskbarOffset + navBarBottomDp + (if (!isTaskbarVisible) 10.dp else 0.dp)
        }

        val barHeight = lerp(56.dp, 50.dp, transitionProgress)
        val browsingTargetY = (maxHeight - bottomOffset - barHeight).coerceAtLeast(0.dp)
        val homeTopOffset = 134.dp
        val barTopOffset = lerp(homeTopOffset, browsingTargetY, transitionProgress)
        val barHorizontalPadding = lerp(20.dp, if (useClassicTaskbar) 10.dp else 14.dp, transitionProgress)
        val homeContentAlpha = (1f - transitionProgress * 1.5f).coerceIn(0f, 1f)
        val homeContentTranslationY = lerp(0.dp, (-25).dp, transitionProgress)

        // Reset bubble drag coordinates when browsing is deactivated
        LaunchedEffect(isBrowsing) {
            if (!isBrowsing) {
                bubbleDragX = initialBubbleXPx
                bubbleDragY = with(density) { browsingTargetY.toPx() }
            } else if (bubbleDragY == 0f) {
                bubbleDragY = with(density) { browsingTargetY.toPx() }
            }
        }

        // 1. Home Screen Content ("Ren" title, Action Pills, Shortcuts Card)
        if (homeContentAlpha > 0.01f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(homeContentAlpha)
                    .offset(y = homeContentTranslationY)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 44.dp, bottom = 100.dp)
            ) {
                // Logo: "Ren" with Incognito Icon on the left side when in Incognito mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 26.dp)
                ) {
                    if (isIncognitoActive) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_incognito_glasses),
                            contentDescription = "Incognito Mode",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(38.dp)
                        )
                    }
                    Text(
                        text = "Ren",
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    )
                }

                // Spacer matching the Search Bar height + generous margin for Action Pills
                Spacer(modifier = Modifier.height(84.dp))

                // Search Engines Quick-Switch Capsules (Animated, 3 per row)
                AnimatedVisibility(
                    visible = showSearchEngines,
                    enter = expandVertically(animationSpec = tween(320, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(280)),
                    exit = shrinkVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(200))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allSearchEngines.chunked(3).forEach { rowEngines ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowEngines.forEach { engine ->
                                    val isSelected = selectedSearchEngine.id == engine.id
                                    val engineIcon = SearchEngineManager.getSearchEngineIconRes(engine.id)
                                    val displayName = if (engine.name == "Brave Search") "Brave" else engine.name

                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(percent = 50),
                                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(percent = 50))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                SearchEngineManager.setSelectedSearchEngine(context, engine.id)
                                                selectedSearchEngine = engine
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = engineIcon),
                                                contentDescription = engine.name,
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = displayName,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (rowEngines.size < 3) {
                                    repeat(3 - rowEngines.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Pills: Sandbox (functional) + Incognito (functional)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sandbox Pill
                    Surface(
                        color = if (isSandboxActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onSandboxClick()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_sandbox),
                                contentDescription = "Sandbox",
                                tint = if (isSandboxActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sandbox",
                                color = if (isSandboxActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Incognito Pill
                    Surface(
                        color = if (isIncognitoActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onIncognitoClick()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_incognito_glasses),
                                contentDescription = "Incognito",
                                tint = if (isIncognitoActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Incognito",
                                color = if (isIncognitoActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Shortcuts Card (Horizontally Scrollable)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        recentShortcuts.forEach { item ->
                            ShortcutItemView(
                                item = item,
                                onClick = { onOpenUrl(item.url) },
                                onLongClick = {
                                    shortcutToDelete = item
                                }
                            )
                        }

                        // Add Button (+)
                        AddShortcutButton(
                            onClick = {
                                showAddShortcutDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Add Shortcut Dialog
        if (showAddShortcutDialog) {
            var titleInput by remember { mutableStateOf("") }
            var urlInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddShortcutDialog = false },
                title = {
                    Text(
                        text = "Add Shortcut",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Name") },
                            placeholder = { Text("e.g. Reddit") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("URL") },
                            placeholder = { Text("e.g. reddit.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (urlInput.isNotBlank()) {
                                saveCustomShortcut(context, titleInput.trim(), urlInput.trim())
                                recentShortcuts = loadShortcuts(context)
                                showAddShortcutDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddShortcutDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Shortcut Confirmation Dialog
        if (shortcutToDelete != null) {
            val item = shortcutToDelete!!
            AlertDialog(
                onDismissRequest = { shortcutToDelete = null },
                title = { Text("Remove Shortcut") },
                text = { Text("Are you sure you want to remove \"${item.title}\"?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteCustomShortcut(context, item.url)
                            recentShortcuts = loadShortcuts(context)
                            shortcutToDelete = null
                        }
                    ) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { shortcutToDelete = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 2. Animated Search Bar Capsule, 3 Action Circles, & Floating Draggable Google Bubble
        val fullWidth = maxWidth - (barHorizontalPadding * 2)
        val circleButtonsAlpha = (transitionProgress * 2.5f - 1.5f).coerceIn(0f, 1f) * (1f - bubbleProgress)
        val buttonSpacing = 6.dp
        val circleSize = barHeight

        val animatedButtonSize by animateDpAsState(
            targetValue = if (isBrowsing && !isFloatingBubbleMode) circleSize else 0.dp,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "actionButtonsSize"
        )

        val currentX = lerp(barHorizontalPadding, with(density) { bubbleDragX.toDp() }, bubbleProgress)
        val currentY = lerp(barTopOffset, with(density) { bubbleDragY.toDp() }, bubbleProgress)
        val containerWidth = lerp(fullWidth, 52.dp, bubbleProgress)
        val containerHeight = lerp(barHeight, 52.dp, bubbleProgress)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = with(density) { currentX.toPx() }.roundToInt(),
                        y = with(density) { currentY.toPx() }.roundToInt()
                    )
                }
                .width(containerWidth)
                .height(containerHeight)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    onBubblePositionChanged(bounds.center.x, bounds.center.y, bounds.width / 2f)
                    onSearchBarBoundsChanged(bounds.left, bounds.top, bounds.right, bounds.bottom)
                }
        ) {
            // Main Search Capsule
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(percent = 50),
                border = if (bubbleProgress > 0.3f) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                shadowElevation = if (bubbleProgress > 0.5f) 10.dp else if (isBrowsing) 8.dp else 4.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(isFloatingBubbleMode) {
                        if (!isFloatingBubbleMode) return@pointerInput
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var isDrag = false
                            var totalMovement = 0f

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    if (!isDrag) {
                                        onToggleFloatingBubble()
                                    }
                                    break
                                }

                                val dragX = change.position.x - change.previousPosition.x
                                val dragY = change.position.y - change.previousPosition.y
                                totalMovement += kotlin.math.abs(dragX) + kotlin.math.abs(dragY)

                                if (totalMovement > touchSlop) {
                                    isDrag = true
                                    change.consume()
                                    val minX = with(density) { 8.dp.toPx() }
                                    val maxX = screenWidthPx - with(density) { 60.dp.toPx() }
                                    val minY = with(density) { 4.dp.toPx() }
                                    val maxY = screenHeightPx - with(density) { 100.dp.toPx() }

                                    bubbleDragX = (bubbleDragX + dragX).coerceIn(minX, maxX)
                                    bubbleDragY = (bubbleDragY + dragY).coerceIn(minY, maxY)
                                }
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(percent = 50))
                ) {
                    // Integrated Loading Progress Track & Fill inside the Search Capsule
                    if (loadingProgressAlpha > 0.001f) {
                        val progressFraction = animatedLoadingProgress.coerceIn(0.04f, 1f)

                        // Subtle background progress fill across the capsule
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .alpha(loadingProgressAlpha)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        )
                                    )
                                )
                        )

                        // Top progress indicator running along the top contour of the capsule from left to right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth(progressFraction)
                                .height(3.dp)
                                .alpha(loadingProgressAlpha)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Search Engine / Google / Incognito Icon (Clickable in browsing mode to enter/exit floating bubble mode, or on home screen to toggle search engines)
                        Box(
                            modifier = Modifier
                                .size(if (bubbleProgress > 0.01f) containerHeight else 46.dp)
                                .clip(CircleShape)
                                .clickable(
                                    enabled = !isFloatingBubbleMode,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (isBrowsing) {
                                        onToggleFloatingBubble()
                                    } else {
                                        showSearchEngines = !showSearchEngines
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val activeEngineIcon = if (isIncognitoActive) {
                                R.drawable.ic_incognito_glasses
                            } else {
                                SearchEngineManager.getSearchEngineIconRes(selectedSearchEngine.id)
                            }
                            Icon(
                                painter = painterResource(id = activeEngineIcon),
                                contentDescription = if (isIncognitoActive) "Incognito Mode" else "${selectedSearchEngine.name} Search",
                                tint = if (isIncognitoActive) MaterialTheme.colorScheme.onSurface else Color.Unspecified,
                                modifier = Modifier.size(if (bubbleProgress > 0.5f) 24.dp else 22.dp)
                            )
                        }

                        val textAlpha = (1f - bubbleProgress * 3f).coerceIn(0f, 1f)
                        if (textAlpha > 0.01f) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(textAlpha)
                                    .padding(end = 12.dp)
                            ) {
                                // Editable Search Field
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchTextFieldValue.text.isEmpty() && !isSearchFocused) {
                                        val placeholderText = if (isIncognitoActive) {
                                            "Search or type URL"
                                        } else {
                                            "Search ${selectedSearchEngine.name} or type URL"
                                        }
                                        Text(
                                            text = placeholderText,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    BasicTextField(
                                        value = searchTextFieldValue,
                                        onValueChange = { newValue ->
                                            if (shouldSelectAllOnNextFocus) {
                                                shouldSelectAllOnNextFocus = false
                                                if (newValue.text == searchTextFieldValue.text && newValue.text.isNotEmpty()) {
                                                    searchTextFieldValue = newValue.copy(
                                                        selection = TextRange(0, newValue.text.length)
                                                    )
                                                    return@BasicTextField
                                                }
                                            }
                                            searchTextFieldValue = newValue
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.5.sp
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                if (searchTextFieldValue.text.isNotBlank()) {
                                                    focusManager.clearFocus()
                                                    searchTextFieldValue = searchTextFieldValue.copy(selection = TextRange.Zero)
                                                    onSearch(searchTextFieldValue.text.trim())
                                                }
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    if (!isSearchFocused && searchTextFieldValue.text.isNotEmpty()) {
                                                        shouldSelectAllOnNextFocus = true
                                                        searchTextFieldValue = searchTextFieldValue.copy(
                                                            selection = TextRange(0, searchTextFieldValue.text.length)
                                                        )
                                                    }
                                                } else {
                                                    shouldSelectAllOnNextFocus = false
                                                    searchTextFieldValue = searchTextFieldValue.copy(
                                                        selection = TextRange.Zero
                                                    )
                                                }
                                                isSearchFocused = focusState.isFocused
                                            }
                                    )
                                }

                                // Right action icon: Reload or Stop Loading when browsing
                                if (isBrowsing) {
                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Reload / Stop Loading icon button
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                if (isPageLoading) {
                                                    onStopLoading()
                                                } else {
                                                    onReload()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Crossfade(
                                            targetState = isPageLoading,
                                            animationSpec = tween(durationMillis = 200),
                                            label = "ReloadStopCrossfade"
                                        ) { loading ->
                                            Icon(
                                                painter = painterResource(id = if (loading) R.drawable.ic_close else R.drawable.ic_reload),
                                                contentDescription = if (loading) "Stop Loading" else "Reload",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3 Circular Action Buttons Beside Search Capsule (Back, Forward, Toggle Taskbar)
            if (animatedButtonSize > 1.dp && circleButtonsAlpha > 0.001f) {
                Spacer(modifier = Modifier.width(buttonSpacing))

                // Circle 1: Back Button (⬅️)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape,
                    shadowElevation = if (isBrowsing) 8.dp else 4.dp,
                    modifier = Modifier
                        .size(animatedButtonSize)
                        .alpha(circleButtonsAlpha)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onGoBack()
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ArrowBackIcon,
                            contentDescription = "Back",
                            tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(buttonSpacing))

                // Circle 2: Forward Button (➡️)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape,
                    shadowElevation = if (isBrowsing) 8.dp else 4.dp,
                    modifier = Modifier
                        .size(animatedButtonSize)
                        .alpha(circleButtonsAlpha)
                        .clip(CircleShape)
                        .clickable(
                            enabled = canGoForward,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onGoForward()
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ArrowForwardIcon,
                            contentDescription = "Forward",
                            tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(buttonSpacing))

                // Circle 3: Toggle Taskbar Button (⬇️ / ⬆️)
                val arrowRotation by animateFloatAsState(
                    targetValue = if (isTaskbarVisible) 0f else 180f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "taskbarArrowRotation"
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape,
                    shadowElevation = if (isBrowsing) 8.dp else 4.dp,
                    modifier = Modifier
                        .size(animatedButtonSize)
                        .alpha(circleButtonsAlpha)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onToggleTaskbar()
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ChevronUpDownIcon,
                            contentDescription = if (isTaskbarVisible) "Hide Taskbar" else "Show Taskbar",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(arrowRotation)
                        )
                    }
                }
            }
        }

        // Search Suggestions Overlay Dropdown
        val isShowingSuggestions = isSearchFocused && searchSuggestions.isNotEmpty()
        AnimatedVisibility(
            visible = isShowingSuggestions,
            enter = fadeIn(tween(180)) + expandVertically(tween(220, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset {
                    val yPos = if (isBrowsing) {
                        (browsingTargetY - 8.dp).toPx() - (searchSuggestions.size.coerceAtMost(5) * 44.dp.toPx() + 16.dp.toPx())
                    } else {
                        (homeTopOffset + barHeight + 8.dp).toPx()
                    }
                    IntOffset(0, yPos.roundToInt().coerceAtLeast(0))
                }
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    searchSuggestions.take(6).forEach { suggestion ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus()
                                    searchTextFieldValue = TextFieldValue(suggestion)
                                    onSearch(suggestion)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = suggestion,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = ArrowForwardIcon,
                                contentDescription = "Fill suggestion",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        searchTextFieldValue = TextFieldValue(suggestion, selection = TextRange(suggestion.length))
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutItemView(
    item: ShortcutItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        // Circle Avatar
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(item.iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            if (item.iconRes != null) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.title,
                    tint = item.iconTint ?: Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
            } else if (!item.letter.isNullOrEmpty()) {
                Text(
                    text = item.letter,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_globe),
                    contentDescription = item.title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = item.title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AddShortcutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        // Circle Avatar with + icon
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Add Shortcut",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = "Add",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private const val SHORTCUTS_PREFS = "ren_shortcuts_prefs"
private const val KEY_CUSTOM_SHORTCUTS = "custom_shortcuts_json"
private const val KEY_DELETED_DEFAULTS = "deleted_default_shortcuts_json"

private fun getDeletedDefaultUrls(context: Context): Set<String> {
    val prefs = context.getSharedPreferences(SHORTCUTS_PREFS, Context.MODE_PRIVATE)
    val jsonString = prefs.getString(KEY_DELETED_DEFAULTS, null) ?: return emptySet()
    val set = mutableSetOf<String>()
    try {
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            set.add(array.getString(i).lowercase())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return set
}

private fun markDefaultAsDeleted(context: Context, url: String) {
    val prefs = context.getSharedPreferences(SHORTCUTS_PREFS, Context.MODE_PRIVATE)
    val set = getDeletedDefaultUrls(context).toMutableSet()
    set.add(url.lowercase())
    val array = JSONArray()
    for (u in set) {
        array.put(u)
    }
    prefs.edit().putString(KEY_DELETED_DEFAULTS, array.toString()).apply()
}

private fun unmarkDefaultAsDeleted(context: Context, url: String) {
    val prefs = context.getSharedPreferences(SHORTCUTS_PREFS, Context.MODE_PRIVATE)
    val set = getDeletedDefaultUrls(context).toMutableSet()
    set.remove(url.lowercase())
    val array = JSONArray()
    for (u in set) {
        array.put(u)
    }
    prefs.edit().putString(KEY_DELETED_DEFAULTS, array.toString()).apply()
}

private fun getIconForDomain(domain: String): Pair<Int, Color>? {
    return when {
        domain.contains("github") -> Pair(R.drawable.ic_github, Color(0xFF22242B))
        domain.contains("facebook") -> Pair(R.drawable.ic_facebook, Color(0xFF1877F2))
        domain.contains("instagram") -> Pair(R.drawable.ic_instagram, Color(0xFFE1306C))
        domain.contains("youtube") -> Pair(R.drawable.ic_youtube, Color(0xFF22242B))
        domain.contains("linkedin") -> Pair(R.drawable.ic_linkedin, Color(0xFF0077B5))
        domain.contains("bluesky") || domain.contains("bsky") -> Pair(R.drawable.ic_bluesky, Color(0xFF1185FE))
        else -> null
    }
}

private fun getCustomShortcuts(context: Context): List<ShortcutItem> {
    val prefs = context.getSharedPreferences(SHORTCUTS_PREFS, Context.MODE_PRIVATE)
    val jsonString = prefs.getString(KEY_CUSTOM_SHORTCUTS, null) ?: return emptyList()
    val list = mutableListOf<ShortcutItem>()
    try {
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val title = obj.optString("title", "")
            val url = obj.optString("url", "")
            val domain = extractDomain(url)
            val iconPair = getIconForDomain(domain)
            val letter = if (obj.has("letter") && obj.getString("letter").isNotEmpty()) {
                obj.getString("letter")
            } else {
                domain.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "W"
            }
            list.add(
                ShortcutItem(
                    title = title.ifBlank { domain },
                    url = url,
                    iconRes = iconPair?.first,
                    iconBgColor = iconPair?.second ?: Color(0xFF2A2C34),
                    iconTint = if (iconPair != null && iconPair.first == R.drawable.ic_youtube) Color(0xFFFF0000) else Color.White,
                    letter = if (iconPair == null) letter else null
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun saveCustomShortcut(context: Context, title: String, url: String) {
    val prefs = context.getSharedPreferences(SHORTCUTS_PREFS, Context.MODE_PRIVATE)
    val existing = getCustomShortcuts(context).toMutableList()
    val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
        "https://$url"
    } else {
        url
    }
    val domain = extractDomain(formattedUrl)
    val cleanTitle = title.ifBlank { domain }
    val letter = domain.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "W"

    unmarkDefaultAsDeleted(context, formattedUrl)

    if (existing.none { it.url.equals(formattedUrl, ignoreCase = true) }) {
        existing.add(
            ShortcutItem(
                title = cleanTitle,
                url = formattedUrl,
                iconRes = null,
                iconBgColor = Color(0xFF2A2C34),
                letter = letter
            )
        )
        val array = JSONArray()
        for (item in existing) {
            val obj = JSONObject()
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("letter", item.letter ?: "")
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_SHORTCUTS, array.toString()).apply()
    }
}

private fun deleteCustomShortcut(context: Context, url: String) {
    val prefs = context.getSharedPreferences(SHORTCUTS_PREFS, Context.MODE_PRIVATE)
    val existing = getCustomShortcuts(context).filterNot { it.url.equals(url, ignoreCase = true) }
    val array = JSONArray()
    for (item in existing) {
        val obj = JSONObject()
        obj.put("title", item.title)
        obj.put("url", item.url)
        obj.put("letter", item.letter ?: "")
        array.put(obj)
    }
    prefs.edit().putString(KEY_CUSTOM_SHORTCUTS, array.toString()).apply()
    markDefaultAsDeleted(context, url)
}

private fun loadShortcuts(context: Context): List<ShortcutItem> {
    val defaultList = listOf(
        ShortcutItem(
            title = "GitHub",
            url = "https://github.com",
            iconRes = R.drawable.ic_github,
            iconBgColor = Color(0xFF22242B),
            iconTint = Color.White
        ),
        ShortcutItem(
            title = "Facebook",
            url = "https://www.facebook.com",
            iconRes = R.drawable.ic_facebook,
            iconBgColor = Color(0xFF1877F2),
            iconTint = Color.White
        ),
        ShortcutItem(
            title = "Instagram",
            url = "https://www.instagram.com",
            iconRes = R.drawable.ic_instagram,
            iconBgColor = Color(0xFFE1306C),
            iconTint = Color.White
        ),
        ShortcutItem(
            title = "YouTube",
            url = "https://www.youtube.com",
            iconRes = R.drawable.ic_youtube,
            iconBgColor = Color(0xFF22242B),
            iconTint = Color(0xFFFF0000)
        )
    )

    val deletedDefaults = getDeletedDefaultUrls(context)
    val customList = getCustomShortcuts(context)

    val result = mutableListOf<ShortcutItem>()
    val seenDomains = mutableSetOf<String>()

    // 1. Add default list items (excluding any deleted ones)
    for (defaultItem in defaultList) {
        val domain = extractDomain(defaultItem.url)
        if (!deletedDefaults.contains(defaultItem.url.lowercase()) && domain.isNotEmpty() && seenDomains.add(domain)) {
            result.add(defaultItem)
        }
    }

    // 2. Add custom user shortcuts added via '+' button
    for (customItem in customList) {
        val domain = extractDomain(customItem.url)
        if (domain.isNotEmpty() && seenDomains.add(domain)) {
            result.add(customItem)
        }
    }

    return result
}

private fun extractDomain(url: String): String {
    return try {
        val uri = URI(url)
        val host = uri.host ?: ""
        host.removePrefix("www.").removePrefix("m.")
    } catch (e: Exception) {
        url.replace("https://", "").replace("http://", "").split("/").firstOrNull() ?: url
    }
}

val BubbleChartIcon: ImageVector
    get() {
        if (_bubbleChart != null) {
            return _bubbleChart!!
        }
        _bubbleChart = ImageVector.Builder(
            name = "bubble_chart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(12.38f, 20.13f)
                quadTo(11.5f, 19.25f, 11.5f, 18f)
                reflectiveQuadToRelative(0.88f, -2.13f)
                reflectiveQuadTo(14.5f, 15f)
                reflectiveQuadToRelative(2.13f, 0.88f)
                reflectiveQuadTo(17.5f, 18f)
                reflectiveQuadToRelative(-0.88f, 2.13f)
                reflectiveQuadTo(14.5f, 21f)
                reflectiveQuadTo(12.38f, 20.13f)
                close()
                moveToRelative(2.84f, -1.41f)
                quadTo(15.5f, 18.43f, 15.5f, 18f)
                reflectiveQuadTo(15.21f, 17.29f)
                reflectiveQuadTo(14.5f, 17f)
                reflectiveQuadToRelative(-0.71f, 0.29f)
                reflectiveQuadTo(13.5f, 18f)
                reflectiveQuadToRelative(0.29f, 0.71f)
                reflectiveQuadTo(14.5f, 19f)
                reflectiveQuadToRelative(0.71f, -0.29f)
                close()
                moveTo(12.6f, 12.4f)
                quadTo(11f, 10.8f, 11f, 8.5f)
                reflectiveQuadTo(12.6f, 4.6f)
                reflectiveQuadTo(16.5f, 3f)
                reflectiveQuadToRelative(3.9f, 1.6f)
                reflectiveQuadTo(22f, 8.5f)
                reflectiveQuadToRelative(-1.6f, 3.9f)
                reflectiveQuadTo(16.5f, 14f)
                reflectiveQuadTo(12.6f, 12.4f)
                close()
                moveToRelative(6.39f, -1.41f)
                quadTo(20f, 9.98f, 20f, 8.5f)
                quadTo(20f, 7.02f, 18.99f, 6.01f)
                reflectiveQuadTo(16.5f, 5f)
                quadTo(15.03f, 5f, 14.01f, 6.01f)
                reflectiveQuadTo(13f, 8.5f)
                reflectiveQuadToRelative(1.01f, 2.49f)
                reflectiveQuadTo(16.5f, 12f)
                reflectiveQuadToRelative(2.49f, -1.01f)
                close()
                moveTo(7f, 18f)
                quadTo(5.35f, 18f, 4.18f, 16.83f)
                reflectiveQuadTo(3f, 14f)
                reflectiveQuadTo(4.18f, 11.18f)
                reflectiveQuadTo(7f, 10f)
                reflectiveQuadToRelative(2.83f, 1.17f)
                reflectiveQuadTo(11f, 14f)
                reflectiveQuadTo(9.83f, 16.83f)
                reflectiveQuadTo(7f, 18f)
                close()
                moveTo(8.41f, 15.41f)
                quadTo(9f, 14.83f, 9f, 14f)
                reflectiveQuadTo(8.41f, 12.59f)
                quadTo(7.83f, 12f, 7f, 12f)
                reflectiveQuadTo(5.59f, 12.59f)
                quadTo(5f, 13.18f, 5f, 14f)
                reflectiveQuadToRelative(0.59f, 1.41f)
                reflectiveQuadTo(7f, 16f)
                quadToRelative(0.83f, 0f, 1.41f, -0.59f)
                close()
                moveTo(14.5f, 18f)
                close()
                moveToRelative(2f, -9.5f)
                close()
                moveTo(7f, 14f)
                close()
            }
        }.build()
        return _bubbleChart!!
    }

private var _bubbleChart: ImageVector? = null

