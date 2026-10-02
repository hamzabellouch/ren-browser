package com.tkno.ren.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.tkno.ren.R
import com.tkno.ren.model.WebTab
import com.tkno.ren.ui.theme.RenTheme
import com.tkno.ren.util.FaviconManager
import kotlinx.coroutines.launch
import kotlin.math.abs

import androidx.compose.runtime.mutableIntStateOf

@Composable
fun TabsGridView(
    tabs: List<WebTab>,
    activeTabId: String,
    onSelectTab: (WebTab) -> Unit,
    onCloseTab: (WebTab) -> Unit,
    onNewTab: (isIncognito: Boolean) -> Unit,
    onCloseAllIncognito: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeTab = tabs.find { it.id == activeTabId }
    var selectedMode by remember(activeTab?.isIncognito) {
        mutableIntStateOf(if (activeTab?.isIncognito == true) 1 else 0) // 0: Normal, 1: Incognito
    }

    val normalTabs = tabs.filter { !it.isIncognito }
    val incognitoTabs = tabs.filter { it.isIncognito }
    val currentTabs = if (selectedMode == 1) incognitoTabs else normalTabs

    RenTheme(isIncognito = selectedMode == 1) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Segmented Switcher & Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Switcher Container (Normal vs Incognito)
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Normal Tab Button
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = if (selectedMode == 0) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .clickable { selectedMode = 0 }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Md3TabOutlined,
                                        contentDescription = "Normal tabs",
                                        tint = if (selectedMode == 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${normalTabs.size}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedMode == 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Incognito Tab Button
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = if (selectedMode == 1) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .clickable { selectedMode = 1 }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_incognito_glasses),
                                        contentDescription = "Incognito tabs",
                                        tint = if (selectedMode == 1) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${incognitoTabs.size}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedMode == 1) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons (New Tab & Close All Incognito)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedMode == 1 && incognitoTabs.isNotEmpty() && onCloseAllIncognito != null) {
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .clickable { onCloseAllIncognito() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close all incognito",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Close all",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Plus (+) Button to Add New Tab in Current Mode
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .clickable { onNewTab(selectedMode == 1) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New tab",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (selectedMode == 1) "New incognito" else "New tab",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (currentTabs.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            // Icon Badge
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (selectedMode == 1) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_incognito_glasses),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Md3TabOutlined,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (selectedMode == 1) "No incognito tabs" else "No open tabs",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (selectedMode == 1) "Open an incognito tab to browse privately" else "Open a new tab to start browsing",
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else {
                    // 2-Columns Grid of Tabs
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 14.dp,
                            end = 14.dp,
                            top = 6.dp,
                            bottom = 110.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(currentTabs, key = { it.id }) { tab ->
                            val isActive = tab.id == activeTabId
                            TabGridCard(
                                tab = tab,
                                isActive = isActive,
                                onSelect = { onSelectTab(tab) },
                                onClose = { onCloseTab(tab) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabGridCard(
    tab: WebTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(1f) }
    val density = LocalDensity.current
    var isDismissing by remember { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(20.dp)
    val headerShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    val previewShape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)

    val activeHeaderBg = MaterialTheme.colorScheme.primaryContainer
    val inactiveHeaderBg = MaterialTheme.colorScheme.surfaceContainerHighest

    val activeCardBorder = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    val inactiveCardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    val headerTextColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val headerIconTint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val closeButtonTint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    val dismissThresholdPx = with(density) { 65.dp.toPx() }
    val dismissTargetPx = with(density) { 260.dp.toPx() }

    val context = LocalContext.current
    var tabFavicon by remember(tab.id, tab.url, tab.favicon) {
        mutableStateOf(tab.favicon ?: FaviconManager.getFavicon(context, tab.url))
    }
    val knownIconRes = remember(tab.url) {
        FaviconManager.getKnownIconRes(tab.url)
    }

    LaunchedEffect(tab.id, tab.url) {
        if (tabFavicon == null && knownIconRes == null && tab.url.isNotBlank() && tab.url != "about:blank") {
            FaviconManager.loadFaviconAsync(context, tab.url) { loadedBitmap ->
                tabFavicon = loadedBitmap
            }
        }
    }

    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (isActive) activeCardBorder else inactiveCardBorder,
        shadowElevation = if (isActive) 6.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(235.dp)
            .graphicsLayer {
                translationX = offsetX.value
                val dragProgress = (abs(offsetX.value) / (dismissThresholdPx * 2.2f)).coerceIn(0f, 0.7f)
                alpha = cardAlpha.value * (1f - dragProgress)
                rotationZ = (offsetX.value / dismissThresholdPx) * 5f
            }
            .clip(cardShape)
            .pointerInput(tab.id) {
                detectHorizontalDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        if (isDismissing) return@detectHorizontalDragGestures
                        val currentOffset = offsetX.value
                        if (abs(currentOffset) > dismissThresholdPx) {
                            isDismissing = true
                            val targetX = if (currentOffset > 0) dismissTargetPx else -dismissTargetPx
                            coroutineScope.launch {
                                launch {
                                    cardAlpha.animateTo(0f, animationSpec = tween(150))
                                }
                                offsetX.animateTo(
                                    targetValue = targetX,
                                    animationSpec = tween(150)
                                )
                                onClose()
                            }
                        } else {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        if (isDismissing) return@detectHorizontalDragGestures
                        coroutineScope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (isDismissing) return@detectHorizontalDragGestures
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                        }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isDismissing && abs(offsetX.value) < 10f) {
                    onSelect()
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row (Site Icon + Title + Close X)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(if (isActive) activeHeaderBg else inactiveHeaderBg, headerShape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Favicon / Site Icon
                if (tab.isIncognito) {
                    Icon(
                        painter = painterResource(R.drawable.ic_incognito_glasses),
                        contentDescription = null,
                        tint = headerIconTint,
                        modifier = Modifier.size(17.dp)
                    )
                } else if (knownIconRes != null) {
                    Image(
                        painter = painterResource(knownIconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(17.dp)
                            .clip(CircleShape)
                    )
                } else if (tabFavicon != null) {
                    Image(
                        bitmap = tabFavicon!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(17.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_globe),
                        contentDescription = null,
                        tint = headerIconTint,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Title
                Text(
                    text = tab.title.ifBlank {
                        if (tab.isIncognito) "Incognito" else "Blank page"
                    },
                    color = headerTextColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Close Button (X)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = closeButtonTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Body Preview Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, previewShape)
                    .clip(previewShape)
            ) {
                if (tab.snapshot != null) {
                    Image(
                        bitmap = tab.snapshot!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Modern Placeholder skeleton bars matching the design in user image
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                    }
                }
            }
        }
    }
}
