package com.tkno.ren.model

import android.graphics.Bitmap
import android.webkit.WebView

data class WebTab(
    val id: String,
    var title: String = "Blank page",
    var url: String = "about:blank",
    var webView: WebView,
    val isIncognito: Boolean = false,
    var isDesktopSite: Boolean = false,
    var isReaderMode: Boolean = false,
    var pendingReaderMode: Boolean = false,
    var originalUrlBeforeReader: String? = null,
    var snapshot: Bitmap? = null,
    var favicon: Bitmap? = null
)

data class BrowserMenuItem(
    val id: String,
    val label: String,
    val iconRes: Int,
    var isSelected: Boolean = false,
    val isTogglable: Boolean = true,
    val isEnabled: Boolean = true
)

enum class SettingType {
    NAVIGATION,
    SWITCH,
    RADIO,
    HEADER
}

data class SettingItem(
    val title: String,
    val subtitle: String = "",
    val type: SettingType = SettingType.NAVIGATION,
    var isChecked: Boolean = false,
    val onClick: (() -> Unit)? = null,
    val onToggle: ((Boolean) -> Unit)? = null
)
