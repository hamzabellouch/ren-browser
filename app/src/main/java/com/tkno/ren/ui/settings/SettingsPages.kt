package com.tkno.ren.ui.settings

import android.app.LocaleManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestorePage
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Timer
import com.tkno.ren.util.AntiFingerprintManager
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tkno.ren.R
import com.tkno.ren.ui.ClearDataDialog
import com.tkno.ren.ui.sandbox.SandboxIcon
import com.tkno.ren.ui.theme.ThemeManager
import androidx.compose.material3.RadioButton
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.res.painterResource
import com.tkno.ren.util.AdBlockManager
import com.tkno.ren.util.DownloadsManager
import com.tkno.ren.util.FilterSubscription
import com.tkno.ren.util.SandboxManager
import com.tkno.ren.util.ScriptManager
import com.tkno.ren.util.SearchEngineManager
import com.tkno.ren.util.TorEngine
import com.tkno.ren.util.TorManager
import com.tkno.ren.rentor.BridgeType
import com.tkno.ren.rentor.RenTorEngine
import com.tkno.ren.rentor.RenTorManager
import com.tkno.ren.rentor.RenTorStatus
import androidx.compose.runtime.collectAsState
import com.tkno.ren.util.UserAgentManager
import com.tkno.ren.util.UserScript
import com.tkno.ren.util.WarpManager
import com.tkno.ren.util.WarpSecurityMode
import com.tkno.ren.util.WebRtcManager
import java.util.Locale

@Composable
fun SettingsHost(
    initialScreen: String = "main",
    onClose: () -> Unit,
    onClearData: () -> Unit,
    onUserAgentChanged: () -> Unit
) {
    val screenStack = remember { mutableStateListOf(initialScreen) }
    val currentScreen = screenStack.lastOrNull() ?: "main"

    val navigateTo: (String) -> Unit = { route ->
        screenStack.add(route)
    }

    val navigateBack: () -> Unit = {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
        } else {
            onClose()
        }
    }

    BackHandler(enabled = true) {
        navigateBack()
    }

    when (currentScreen) {
        "main" -> SettingsPage(onNavigateBack = navigateBack, onNavigateTo = navigateTo)
        "general" -> GeneralSettingsPage(onBack = navigateBack, onNavigateTo = navigateTo, onClearData = onClearData)
        "network_identity" -> NetworkIdentitySettingsPage(onBack = navigateBack, onNavigateTo = navigateTo, onClearData = onClearData)
        "anti_fingerprint" -> AntiFingerprintSettingsPage(onBack = navigateBack)
        "webrtc" -> WebRtcSettingsPage(onBack = navigateBack)
        "appearance" -> AppearancePreferences(onNavigateBack = navigateBack, onNavigateTo = navigateTo)
        "dark_theme" -> DarkThemePreferences(onNavigateBack = navigateBack)
        "languages" -> LanguagesPage(onNavigateBack = navigateBack)
        "interface_interaction" -> InterfaceInteractionSettingsPage(onBack = navigateBack)
        "privacy" -> PrivacySettingsPage(onBack = navigateBack, onNavigateTo = navigateTo, onClearData = onClearData)
        "tor" -> TorSettingsPage(onBack = navigateBack)
        "warp" -> WarpSettingsPage(onBack = navigateBack)
        "ad_blocking" -> AdBlockingSettingsPage(onBack = navigateBack, onNavigateTo = navigateTo)
        "custom_filters" -> CustomFiltersPage(onBack = navigateBack)
        "filter_subscriptions" -> FilterSubscriptionsPage(onBack = navigateBack)
        "user_agent" -> UserAgentSettingsPage(onBack = navigateBack, onNavigateTo = navigateTo, onUserAgentChanged = onUserAgentChanged)
        "new_user_agent" -> NewUserAgentPage(onBack = navigateBack, onUserAgentSaved = onUserAgentChanged)
        "advanced" -> AdvancedSettingsPage(onBack = navigateBack)
        "site_config" -> SiteConfigurationPage(onBack = navigateBack, onNavigateTo = navigateTo, onClearData = onClearData)
        "secure_dns" -> SecureDnsSettingsPage(onBack = navigateBack)
        "cookies_settings" -> CookiesSettingsPage(onBack = navigateBack)
        "site_permissions" -> SitePermissionsPage(onBack = navigateBack)
        "sandbox" -> SandboxSettingsPage(onBack = navigateBack, onPurgeData = onClearData)
        "scripts" -> ScriptsSettingsPage(onBack = navigateBack)
        "about" -> AboutSettingsPage(onBack = navigateBack)
        else -> SettingsPage(onNavigateBack = navigateBack, onNavigateTo = navigateTo)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    onNavigateBack: () -> Unit,
    onNavigateTo: (String) -> Unit
) {
    val context = LocalContext.current
    var isSandboxEnabled by remember { mutableStateOf(SandboxManager.isSandboxEnabled(context)) }

    BasePreferencePage(
        title = stringResource(id = R.string.settings),
        onBack = onNavigateBack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            item {
                SettingItem(
                    title = stringResource(id = R.string.general_settings),
                    description = stringResource(id = R.string.general_settings_desc),
                    icon = Icons.Rounded.SettingsApplications,
                    onClick = { onNavigateTo("general") }
                )
            }
            item {
                SettingItem(
                    title = stringResource(id = R.string.network_identity),
                    description = stringResource(id = R.string.network_identity_desc),
                    icon = Icons.Outlined.Security,
                    onClick = { onNavigateTo("network_identity") }
                )
            }
            item {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.sandbox),
                    description = if (isSandboxEnabled) "Sandbox active (Quarantined)" else stringResource(id = R.string.sandbox_desc),
                    icon = SandboxIcon,
                    isChecked = isSandboxEnabled,
                    onCheckedChange = { checked ->
                        isSandboxEnabled = checked
                        SandboxManager.setSandboxEnabled(context, checked) {
                            val msg = if (checked) {
                                context.getString(R.string.sandbox_enabled_msg)
                            } else {
                                context.getString(R.string.sandbox_disabled_msg)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClick = { onNavigateTo("sandbox") }
                )
            }
            item {
                SettingItem(
                    title = stringResource(id = R.string.scripts_addons),
                    description = stringResource(id = R.string.scripts_addons_desc),
                    icon = Icons.Outlined.Code,
                    onClick = { onNavigateTo("scripts") }
                )
            }
            item {
                SettingItem(
                    title = stringResource(id = R.string.privacy_security),
                    description = stringResource(id = R.string.privacy_security_desc),
                    icon = Icons.Outlined.Shield,
                    onClick = { onNavigateTo("privacy") }
                )
            }
            item {
                SettingItem(
                    title = stringResource(id = R.string.ad_blocking),
                    description = stringResource(id = R.string.ad_blocking_desc),
                    icon = Icons.Outlined.Block,
                    onClick = { onNavigateTo("ad_blocking") }
                )
            }
            item {
                SettingItem(
                    title = stringResource(id = R.string.user_agent),
                    description = stringResource(id = R.string.user_agent_desc),
                    icon = Icons.Outlined.Devices,
                    onClick = { onNavigateTo("user_agent") }
                )
            }
            item {
                SettingItem(
                    title = stringResource(id = R.string.look_and_feel),
                    description = stringResource(id = R.string.display_settings),
                    icon = Icons.Outlined.Palette,
                    onClick = { onNavigateTo("appearance") }
                )
            }
            item {
                SettingItem(
                    title = stringResource(id = R.string.interface_interaction),
                    description = stringResource(id = R.string.interface_interaction_desc),
                    icon = Dashboard2,
                    onClick = { onNavigateTo("interface_interaction") }
                )
            }
            item {
                SettingItem(
                    title = stringResource(id = R.string.advanced_settings),
                    description = stringResource(id = R.string.advanced_settings_desc),
                    icon = Icons.Outlined.Build,
                    onClick = { onNavigateTo("advanced") }
                )
            }
            item {
                SettingItem(
                    title = stringResource(id = R.string.about),
                    description = stringResource(id = R.string.about_desc),
                    icon = Icons.Outlined.Info,
                    onClick = { onNavigateTo("about") }
                )
            }
        }
    }
}

fun isAppDefaultBrowser(context: Context): Boolean {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
            }
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName == context.packageName
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}

fun getDefaultBrowserIntent(context: Context): Intent? {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
            }
        }
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        if (intent.resolveActivity(context.packageManager) != null) {
            return intent
        }
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        if (appDetailsIntent.resolveActivity(context.packageManager) != null) {
            return appDetailsIntent
        }
        return Intent(Settings.ACTION_SETTINGS)
    } catch (e: Exception) {
        return null
    }
}

fun openDefaultBrowserSettings(context: Context) {
    try {
        val intent = getDefaultBrowserIntent(context) ?: Intent(Settings.ACTION_SETTINGS)
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(appDetailsIntent)
        } catch (e2: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e3: Exception) {
                Toast.makeText(context, "Cannot open default app settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsPage(
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onClearData: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE) }

    var isDefaultBrowser by remember { mutableStateOf(isAppDefaultBrowser(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultBrowser = isAppDefaultBrowser(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val defaultBrowserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultBrowser = isAppDefaultBrowser(context)
    }

    var isNotificationsEnabled by remember { mutableStateOf(true) }
    var isPrivateModeEnabled by remember { mutableStateOf(false) }
    var isPreviewDisabled by remember { mutableStateOf(false) }
    var restoreTabsOption by remember {
        mutableStateOf(prefs.getString("restore_tabs_on_startup", "Don't restore") ?: "Don't restore")
    }
    var showRestoreTabsDialog by remember { mutableStateOf(false) }

    var selectedSearchEngine by remember {
        mutableStateOf(SearchEngineManager.getSelectedSearchEngine(context))
    }
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showCustomSearchDialog by remember { mutableStateOf(false) }
    var customSearchName by remember { mutableStateOf("") }
    var customSearchUrl by remember { mutableStateOf("") }

    var downloadLocationName by remember {
        mutableStateOf(DownloadsManager.getDownloadLocationDisplayName(context))
    }
    var downloadLocationType by remember {
        mutableStateOf(DownloadsManager.getDownloadLocationType(context))
    }
    var showDownloadLocationDialog by remember { mutableStateOf(false) }

    var downloadEngine by remember {
        mutableStateOf(DownloadsManager.getDownloadEngine(context))
    }
    var showDownloadEngineDialog by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: Exception) {
                // ignore
            }
            val docId = try {
                DocumentsContract.getTreeDocumentId(uri)
            } catch (e: Exception) {
                uri.lastPathSegment ?: "Custom Folder"
            }
            val readableName = if (docId.contains(":")) {
                val parts = docId.split(":", limit = 2)
                if (parts.size > 1 && parts[1].isNotBlank()) parts[1] else docId
            } else {
                docId
            }
            val finalDisplayName = "Custom: $readableName"
            DownloadsManager.setDownloadLocation(
                context = context,
                type = "custom",
                displayName = finalDisplayName,
                uriString = uri.toString(),
                subPath = readableName
            )
            downloadLocationType = "custom"
            downloadLocationName = finalDisplayName
            Toast.makeText(context, "Download location set to $finalDisplayName", Toast.LENGTH_SHORT).show()
        }
    }

    BasePreferencePage(
        title = stringResource(id = R.string.general_settings),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            // Notifications
            item {
                PreferenceSubtitle(text = "Notifications")
            }
            item {
                PreferenceSwitch(
                    title = "Enable notifications",
                    description = "Receive notifications when background operations complete",
                    icon = if (isNotificationsEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                    isChecked = isNotificationsEnabled,
                    onClick = { isNotificationsEnabled = !isNotificationsEnabled }
                )
            }

            // Privacy and Display
            item {
                PreferenceSubtitle(text = "Privacy & Display")
            }
            item {
                PreferenceSwitch(
                    title = "Private Mode",
                    description = "Do not save browsing history or previous sessions",
                    icon = Icons.Outlined.HistoryToggleOff,
                    isChecked = isPrivateModeEnabled,
                    onClick = { isPrivateModeEnabled = !isPrivateModeEnabled }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Disable preview",
                    description = "Hide media previews to improve performance",
                    icon = if (isPreviewDisabled) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    isChecked = isPreviewDisabled,
                    onClick = { isPreviewDisabled = !isPreviewDisabled }
                )
            }

            // Browser Preferences
            item {
                PreferenceSubtitle(text = "Browser Preferences")
            }
            item {
                PreferenceItem(
                    title = "Set as default browser",
                    description = if (isDefaultBrowser) "Ren is currently your default browser" else "Make Ren your default browser",
                    icon = Icons.Outlined.OpenInBrowser,
                    onClick = {
                        if (isDefaultBrowser) {
                            Toast.makeText(context, "Ren is already your default browser", Toast.LENGTH_SHORT).show()
                        } else {
                            val intent = getDefaultBrowserIntent(context)
                            if (intent != null) {
                                try {
                                    defaultBrowserLauncher.launch(intent)
                                } catch (e: Exception) {
                                    openDefaultBrowserSettings(context)
                                }
                            } else {
                                openDefaultBrowserSettings(context)
                            }
                        }
                    }
                )
            }
            item {
                PreferenceItem(
                    title = "Restore tabs on startup",
                    description = restoreTabsOption,
                    icon = Icons.Outlined.RestorePage,
                    onClick = {
                        showRestoreTabsDialog = true
                    }
                )
            }
            item {
                PreferenceItem(
                    title = "Search settings",
                    description = selectedSearchEngine.name,
                    icon = Icons.Outlined.Tune,
                    onClick = {
                        showSearchEngineDialog = true
                    }
                )
            }
            item {
                PreferenceItem(
                    title = "Homepage",
                    description = "Default blank page",
                    onClick = {
                        Toast.makeText(context, "Homepage: Default", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            item {
                PreferenceItem(
                    title = "Site configuration",
                    description = "Manage permissions, cookies, DNS and JavaScript",
                    icon = Icons.Outlined.Public,
                    onClick = {
                        onNavigateTo("site_config")
                    }
                )
            }
            item {
                PreferenceItem(
                    title = "Download location",
                    description = downloadLocationName,
                    icon = Icons.Outlined.Folder,
                    onClick = {
                        showDownloadLocationDialog = true
                    }
                )
            }
            item {
                PreferenceItem(
                    title = "Download method",
                    description = if (downloadEngine == "builtin") "Built-in download manager" else "System download manager",
                    icon = Icons.Outlined.Download,
                    onClick = {
                        showDownloadEngineDialog = true
                    }
                )
            }
        }
    }

    if (showRestoreTabsDialog) {
        val restoreOptions = listOf("Don't restore", "Always restore", "Ask first")
        AlertDialog(
            onDismissRequest = { showRestoreTabsDialog = false },
            title = {
                Text(
                    text = "Restore tabs on startup",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    restoreOptions.forEach { option ->
                        PreferenceRadio(
                            title = option,
                            isSelected = restoreTabsOption == option,
                            onClick = {
                                restoreTabsOption = option
                                prefs.edit().putString("restore_tabs_on_startup", option).apply()
                                showRestoreTabsDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRestoreTabsDialog = false }) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    if (showSearchEngineDialog) {
        val searchEngines = remember(showSearchEngineDialog) {
            SearchEngineManager.getAllSearchEngines(context)
        }
        AlertDialog(
            onDismissRequest = { showSearchEngineDialog = false },
            title = {
                Text(
                    text = "Search Engine",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    items(searchEngines) { engine ->
                        Surface(
                            onClick = {
                                SearchEngineManager.setSelectedSearchEngine(context, engine.id)
                                selectedSearchEngine = engine
                                showSearchEngineDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSearchEngine.id == engine.id,
                                    onClick = {
                                        SearchEngineManager.setSelectedSearchEngine(context, engine.id)
                                        selectedSearchEngine = engine
                                        showSearchEngineDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = engine.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = engine.homeUrl.ifBlank { engine.searchUrl },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                showSearchEngineDialog = false
                                customSearchName = ""
                                customSearchUrl = ""
                                showCustomSearchDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Add Custom Search Engine")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchEngineDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    if (showCustomSearchDialog) {
        AlertDialog(
            onDismissRequest = { showCustomSearchDialog = false },
            title = {
                Text(text = "Custom Search Engine", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter search engine details. Use %s for query parameter (e.g. https://search.example.com/?q=%s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customSearchName,
                        onValueChange = { customSearchName = it },
                        label = { Text("Engine Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customSearchUrl,
                        onValueChange = { customSearchUrl = it },
                        label = { Text("Search URL") },
                        placeholder = { Text("https://example.com/search?q=") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customSearchName.isNotBlank() && customSearchUrl.isNotBlank()) {
                            SearchEngineManager.setCustomSearchEngine(context, customSearchName, customSearchUrl)
                            selectedSearchEngine = SearchEngineManager.getSelectedSearchEngine(context)
                            showCustomSearchDialog = false
                        }
                    },
                    enabled = customSearchName.isNotBlank() && customSearchUrl.isNotBlank()
                ) {
                    Text(text = "Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomSearchDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    if (showDownloadLocationDialog) {
        val downloadOptions = listOf(
            Triple("ren", "Ren / Downloads", "Dedicated Ren folder inside Downloads"),
            Triple("default", "Downloads (Default)", "Standard Android Downloads folder"),
            Triple("custom", "Choose custom folder...", "Select any folder via system file picker")
        )
        AlertDialog(
            onDismissRequest = { showDownloadLocationDialog = false },
            title = {
                Text(
                    text = "Download Location",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    downloadOptions.forEach { (type, title, desc) ->
                        val isSelected = downloadLocationType == type
                        Surface(
                            onClick = {
                                if (type == "custom") {
                                    showDownloadLocationDialog = false
                                    folderPickerLauncher.launch(null)
                                } else {
                                    DownloadsManager.setDownloadLocation(
                                        context = context,
                                        type = type,
                                        displayName = title
                                    )
                                    downloadLocationType = type
                                    downloadLocationName = title
                                    showDownloadLocationDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (type == "custom") {
                                            showDownloadLocationDialog = false
                                            folderPickerLauncher.launch(null)
                                        } else {
                                            DownloadsManager.setDownloadLocation(
                                                context = context,
                                                type = type,
                                                displayName = title
                                            )
                                            downloadLocationType = type
                                            downloadLocationName = title
                                            showDownloadLocationDialog = false
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (type == "custom" && downloadLocationType == "custom") downloadLocationName else title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadLocationDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    if (showDownloadEngineDialog) {
        val engineOptions = listOf(
            Triple(
                "system",
                "System download manager",
                "Uses Android OS download service with background continuation & system notifications"
            ),
            Triple(
                "builtin",
                "Built-in download manager",
                "Uses browser's internal engine to stream and save files directly"
            )
        )
        AlertDialog(
            onDismissRequest = { showDownloadEngineDialog = false },
            title = {
                Text(
                    text = "Download Method",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    engineOptions.forEach { (engineKey, title, desc) ->
                        val isSelected = downloadEngine == engineKey
                        Surface(
                            onClick = {
                                DownloadsManager.setDownloadEngine(context, engineKey)
                                downloadEngine = engineKey
                                showDownloadEngineDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        DownloadsManager.setDownloadEngine(context, engineKey)
                                        downloadEngine = engineKey
                                        showDownloadEngineDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadEngineDialog = false }) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkIdentitySettingsPage(
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onClearData: () -> Unit
) {
    val context = LocalContext.current
    val selectedUaTitle = UserAgentManager.getSelectedTitle(context)
    var isAntiFingerprintEnabled by remember {
        mutableStateOf(AntiFingerprintManager.isAntiFingerprintEnabled(context))
    }
    var isWebRtcBlockEnabled by remember {
        mutableStateOf(WebRtcManager.isWebRtcBlockEnabled(context))
    }
    var isWarpEnabled by remember {
        mutableStateOf(WarpManager.isWarpEnabled(context))
    }

    var showClearBrowsingDataDialog by remember { mutableStateOf(false) }

    var clearOnExitOptions by remember {
        mutableStateOf(ClearDataDialog.getClearOnExitOptions(context))
    }
    var showClearOnExitDialog by remember { mutableStateOf(false) }
    val tempClearOnExitOptions = remember(showClearOnExitDialog) {
        mutableStateListOf<String>().apply {
            if (showClearOnExitDialog) {
                addAll(clearOnExitOptions)
            }
        }
    }

    BasePreferencePage(
        title = stringResource(id = R.string.network_identity),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                PreferenceSubtitle(text = "Network & Identity")
            }
            item {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.cloudflare_warp),
                    description = if (isWarpEnabled)
                        stringResource(id = R.string.cloudflare_warp_enabled)
                    else
                        stringResource(id = R.string.cloudflare_warp_desc),
                    icon = painterResource(id = R.drawable.ic_cloudflare_warp),
                    isChecked = isWarpEnabled,
                    onClick = { onNavigateTo("warp") },
                    onCheckedChange = { checked ->
                        WarpManager.toggleWarp(context) { enabled, success, errorMsg ->
                            isWarpEnabled = enabled
                            val msg = if (enabled) {
                                if (success) context.getString(R.string.cloudflare_warp_enabled) else "Failed to apply Cloudflare WARP proxy"
                            } else {
                                errorMsg ?: context.getString(R.string.cloudflare_warp_disabled)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
            item {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.anti_fingerprint),
                    description = if (isAntiFingerprintEnabled)
                        stringResource(id = R.string.anti_fingerprint_active_desc)
                    else
                        stringResource(id = R.string.anti_fingerprint_disabled_desc),
                    icon = Icons.Outlined.Fingerprint,
                    isChecked = isAntiFingerprintEnabled,
                    onClick = { onNavigateTo("anti_fingerprint") },
                    onCheckedChange = { checked ->
                        isAntiFingerprintEnabled = checked
                        AntiFingerprintManager.setAntiFingerprintEnabled(context, checked)
                    }
                )
            }
            item {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.block_webrtc),
                    description = if (isWebRtcBlockEnabled)
                        stringResource(id = R.string.block_webrtc_active_desc)
                    else
                        stringResource(id = R.string.block_webrtc_disabled_desc),
                    icon = Icons.Outlined.Shield,
                    isChecked = isWebRtcBlockEnabled,
                    onClick = { onNavigateTo("webrtc") },
                    onCheckedChange = { checked ->
                        isWebRtcBlockEnabled = checked
                        WebRtcManager.setWebRtcBlockEnabled(context, checked)
                    }
                )
            }
            item {
                PreferenceItem(
                    title = "User-agent",
                    description = selectedUaTitle,
                    icon = Icons.Outlined.Devices,
                    onClick = { onNavigateTo("user_agent") }
                )
            }
            item {
                PreferenceItem(
                    title = "Ad blocking",
                    description = if (AdBlockManager.isAdBlockEnabled(context)) "Enabled" else "Disabled",
                    icon = Icons.Outlined.Block,
                    onClick = { onNavigateTo("ad_blocking") }
                )
            }
            item {
                PreferenceItem(
                    title = "Clear browsing data",
                    description = "Clear cache, history, cookies and web storage",
                    icon = Icons.Outlined.Delete,
                    onClick = { showClearBrowsingDataDialog = true }
                )
            }
            item {
                val clearOnExitDesc = if (clearOnExitOptions.isEmpty()) {
                    "None"
                } else {
                    ClearDataDialog.ALL_OPTIONS
                        .filter { clearOnExitOptions.contains(it.key) }
                        .joinToString(", ") { it.label }
                }
                PreferenceItem(
                    title = "Clear data on exit",
                    description = clearOnExitDesc,
                    icon = Icons.Outlined.CleaningServices,
                    onClick = { showClearOnExitDialog = true }
                )
            }
        }
    }

    if (showClearBrowsingDataDialog) {
        ClearBrowsingDataDialog(
            onDismissRequest = { showClearBrowsingDataDialog = false },
            onConfirm = { _ ->
                onClearData()
            }
        )
    }

    if (showClearOnExitDialog) {
        AlertDialog(
            onDismissRequest = { showClearOnExitDialog = false },
            title = {
                Text(
                    text = "Clear data on exit",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ClearDataDialog.ALL_OPTIONS.forEach { option ->
                        val isChecked = tempClearOnExitOptions.contains(option.key)
                        Surface(
                            onClick = {
                                if (isChecked) {
                                    tempClearOnExitOptions.remove(option.key)
                                } else {
                                    tempClearOnExitOptions.add(option.key)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            tempClearOnExitOptions.add(option.key)
                                        } else {
                                            tempClearOnExitOptions.remove(option.key)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.padding(start = 12.dp))
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newSet = tempClearOnExitOptions.toSet()
                        clearOnExitOptions = newSet
                        ClearDataDialog.saveClearOnExitOptions(context, newSet)
                        showClearOnExitDialog = false
                    }
                ) {
                    Text(
                        text = "OK",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearOnExitDialog = false }
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePreferences(
    onNavigateBack: () -> Unit,
    onNavigateTo: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE) }

    val dynamicColor = ThemeManager.dynamicColor
    val isDark = ThemeManager.isDark(isSystemInDarkTheme())

    val uriHandler = LocalUriHandler.current

    val currentLanguageName = remember(prefs.getString("app_language", "system")) {
        getSavedLocaleDisplayName(context)
    }

    BasePreferencePage(
        title = stringResource(id = R.string.look_and_feel),
        onBack = onNavigateBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.dynamic_color),
                    description = stringResource(id = R.string.dynamic_color_desc),
                    icon = Icons.Outlined.Colorize,
                    isChecked = dynamicColor,
                    onClick = {
                        ThemeManager.setDynamicColor(context, !dynamicColor)
                    }
                )
            }

            item {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.dark_theme),
                    icon = Icons.Outlined.DarkMode,
                    isChecked = isDark,
                    onCheckedChange = { checked ->
                        val newMode = if (checked) 1 else 2
                        ThemeManager.setDarkThemeMode(context, newMode)
                    },
                    onClick = {
                        onNavigateTo("dark_theme")
                    }
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(id = R.string.language),
                    description = currentLanguageName,
                    icon = Icons.Outlined.Language,
                    onClick = {
                        onNavigateTo("languages")
                    }
                )
            }

            item {
                PreferencesHintCard(
                    title = stringResource(id = R.string.translate),
                    description = stringResource(id = R.string.translate_desc),
                    icon = Icons.Outlined.Translate,
                    onClick = {
                        try {
                            uriHandler.openUri("https://github.com/ren-browser")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AppearanceSettingsPage(
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit = {}
) {
    AppearancePreferences(
        onNavigateBack = onBack,
        onNavigateTo = onNavigateTo
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkThemePreferences(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val selectedTheme = ThemeManager.darkThemeMode

    BasePreferencePage(
        title = stringResource(id = R.string.dark_theme),
        onBack = onNavigateBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                PreferenceSingleChoiceItem(
                    text = stringResource(id = R.string.follow_system),
                    selected = selectedTheme == 0,
                    onClick = {
                        ThemeManager.setDarkThemeMode(context, 0)
                    }
                )
            }
            item {
                PreferenceSingleChoiceItem(
                    text = stringResource(id = R.string.on),
                    selected = selectedTheme == 1,
                    onClick = {
                        ThemeManager.setDarkThemeMode(context, 1)
                    }
                )
            }
            item {
                PreferenceSingleChoiceItem(
                    text = stringResource(id = R.string.off),
                    selected = selectedTheme == 2,
                    onClick = {
                        ThemeManager.setDarkThemeMode(context, 2)
                    }
                )
            }
        }
    }
}

data class LanguageItem(
    val code: String,
    val nativeName: String,
    val englishName: String
)

val supportedLanguages = listOf(
    LanguageItem("ar", "العربية", "Arabic"),
    LanguageItem("bn", "বাংলা", "Bengali"),
    LanguageItem("zh-CN", "中文 (简体)", "Chinese Simplified"),
    LanguageItem("zh-TW", "中文 (繁體)", "Chinese Traditional"),
    LanguageItem("cs", "Čeština", "Czech"),
    LanguageItem("da", "Dansk", "Danish"),
    LanguageItem("nl", "Nederlands", "Dutch"),
    LanguageItem("en", "English", "English"),
    LanguageItem("fi", "Suomi", "Finnish"),
    LanguageItem("fr", "Français", "French"),
    LanguageItem("de", "Deutsch", "German"),
    LanguageItem("el", "Ελληνικά", "Greek"),
    LanguageItem("he", "עברית", "Hebrew"),
    LanguageItem("hi", "हिन्दी", "Hindi"),
    LanguageItem("hu", "Magyar", "Hungarian"),
    LanguageItem("id", "Bahasa Indonesia", "Indonesian"),
    LanguageItem("it", "Italiano", "Italian"),
    LanguageItem("ja", "日本語", "Japanese"),
    LanguageItem("ko", "한국어", "Korean"),
    LanguageItem("ms", "Bahasa Melayu", "Malay"),
    LanguageItem("no", "Norsk", "Norwegian"),
    LanguageItem("fa", "فارسی", "Persian"),
    LanguageItem("pl", "Polski", "Polish"),
    LanguageItem("pt", "Português", "Portuguese"),
    LanguageItem("ro", "Română", "Romanian"),
    LanguageItem("ru", "Русский", "Russian"),
    LanguageItem("es", "Español", "Spanish"),
    LanguageItem("sv", "Svenska", "Swedish"),
    LanguageItem("th", "ไทย", "Thai"),
    LanguageItem("tr", "Türkçe", "Turkish"),
    LanguageItem("uk", "Українська", "Ukrainian"),
    LanguageItem("ur", "اردو", "Urdu"),
    LanguageItem("vi", "Tiếng Việt", "Vietnamese")
)

fun setAppLanguage(context: Context, languageCode: String) {
    val prefs = context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("app_language", languageCode).apply()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
        if (languageCode == "system" || languageCode.isEmpty()) {
            localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
        } else {
            localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
        }
    } else {
        val locale = if (languageCode == "system" || languageCode.isEmpty()) {
            Locale.getDefault()
        } else {
            val parts = languageCode.split("-")
            if (parts.size > 1) Locale(parts[0], parts[1]) else Locale(languageCode)
        }
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}

fun getSavedLocaleDisplayName(context: Context): String {
    val prefs = context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE)
    val savedCode = prefs.getString("app_language", "system") ?: "system"
    if (savedCode == "system") {
        return context.getString(R.string.follow_system)
    }
    val item = supportedLanguages.find { it.code.equals(savedCode, ignoreCase = true) }
    return item?.let { "${it.nativeName} (${it.englishName})" } ?: item?.nativeName ?: savedCode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagesPage(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE) }
    var selectedLanguage by remember {
        mutableStateOf(prefs.getString("app_language", "system") ?: "system")
    }

    val systemLocale = remember { Locale.getDefault() }
    val systemLanguageCode = remember { systemLocale.language }

    val suggestedLanguages = remember {
        val list = mutableListOf<LanguageItem>()
        val currentSys = supportedLanguages.find { it.code.equals(systemLanguageCode, ignoreCase = true) }
        if (currentSys != null) list.add(currentSys)
        val en = supportedLanguages.find { it.code == "en" }
        if (en != null && !list.contains(en)) list.add(en)
        val ar = supportedLanguages.find { it.code == "ar" }
        if (ar != null && !list.contains(ar)) list.add(ar)
        list.take(3)
    }

    BasePreferencePage(
        title = stringResource(id = R.string.language),
        onBack = onNavigateBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                PreferenceSubtitle(text = stringResource(id = R.string.suggested))
            }
            item {
                PreferenceSingleChoiceItem(
                    text = stringResource(id = R.string.follow_system),
                    selected = selectedLanguage == "system",
                    onClick = {
                        selectedLanguage = "system"
                        setAppLanguage(context, "system")
                    }
                )
            }
            items(suggestedLanguages) { lang ->
                PreferenceSingleChoiceItem(
                    text = "${lang.nativeName} (${lang.englishName})",
                    selected = selectedLanguage == lang.code,
                    onClick = {
                        selectedLanguage = lang.code
                        setAppLanguage(context, lang.code)
                    }
                )
            }

            item {
                PreferenceSubtitle(text = stringResource(id = R.string.all_languages))
            }
            items(supportedLanguages) { lang ->
                PreferenceSingleChoiceItem(
                    text = "${lang.nativeName} (${lang.englishName})",
                    selected = selectedLanguage == lang.code,
                    onClick = {
                        selectedLanguage = lang.code
                        setAppLanguage(context, lang.code)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceInteractionSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { 
        context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE) 
    }

    // قراءة الحالات المخزنة في SharedPreferences
    var hideLabels by remember { 
        mutableStateOf(prefs.getBoolean("hide_navigation_labels", false)) 
    }
    var useClassicTaskbar by remember { 
        mutableStateOf(prefs.getBoolean("use_classic_taskbar", false)) 
    }

    // الاستماع للتغييرات الفورية على SharedPreferences
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

    BasePreferencePage(
        title = stringResource(id = R.string.interface_interaction),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // عنوان فرعي لقسم التنقل (Navigation)
            PreferenceSubtitle(text = stringResource(R.string.navigation))

            // 1. خيار إخفاء تسميات شريط التنقل (Hide navigation labels)
            PreferenceSwitch(
                title = stringResource(R.string.hide_navigation_labels),
                description = stringResource(R.string.hide_navigation_labels_desc),
                icon = Icons.Outlined.VisibilityOff,
                isChecked = hideLabels,
                onClick = {
                    val newValue = !hideLabels
                    hideLabels = newValue
                    prefs.edit().putBoolean("hide_navigation_labels", newValue).apply()
                }
            )

            // 2. خيار استخدام شريط المهام الكلاسيكي (Use classic taskbar)
            PreferenceSwitch(
                title = stringResource(R.string.use_classic_taskbar),
                description = stringResource(R.string.use_classic_taskbar_desc),
                icon = Icons.Outlined.Dashboard,
                isChecked = useClassicTaskbar,
                onClick = {
                    val newValue = !useClassicTaskbar
                    useClassicTaskbar = newValue
                    prefs.edit().putBoolean("use_classic_taskbar", newValue).apply()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceAndInteractionPage(onNavigateBack: () -> Unit) = InterfaceInteractionSettingsPage(onBack = onNavigateBack)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsPage(
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit = {},
    onClearData: () -> Unit
) {
    val context = LocalContext.current
    var isTorEnabled by remember { mutableStateOf(TorManager.isTorEnabled(context)) }
    var isWarpEnabled by remember { mutableStateOf(WarpManager.isWarpEnabled(context)) }
    var clearOnExit by remember { mutableStateOf(false) }
    var showClearBrowsingDataDialog by remember { mutableStateOf(false) }

    BasePreferencePage(
        title = stringResource(id = R.string.privacy_security),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
            item {
                PreferenceSubtitle(text = "High-Speed VPN & Cloudflare WARP")
            }
            item {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.cloudflare_warp),
                    description = if (isWarpEnabled)
                        stringResource(id = R.string.cloudflare_warp_enabled)
                    else
                        stringResource(id = R.string.cloudflare_warp_desc),
                    icon = painterResource(id = R.drawable.ic_cloudflare_warp),
                    isChecked = isWarpEnabled,
                    onCheckedChange = { checked ->
                        WarpManager.toggleWarp(context) { enabled, success, errorMsg ->
                            isWarpEnabled = enabled
                            val msg = if (enabled) {
                                if (success) context.getString(R.string.cloudflare_warp_enabled) else "Failed to apply WARP routing"
                            } else {
                                errorMsg ?: context.getString(R.string.cloudflare_warp_disabled)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    onClick = {
                        onNavigateTo("warp")
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Anonymity & Tor Network")
            }
            item {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.tor_network),
                    icon = Icons.Outlined.VpnLock,
                    isChecked = isTorEnabled,
                    onCheckedChange = { checked ->
                        TorManager.toggleTor(context) { enabled, success ->
                            isTorEnabled = enabled
                            val msg = if (enabled) {
                                if (success) context.getString(R.string.tor_network_enabled) else "Failed to apply Tor proxy"
                            } else {
                                context.getString(R.string.tor_network_disabled)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClick = {
                        onNavigateTo("tor")
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Data & Tracking")
            }
            item {
                PreferenceItem(
                    title = "Clear browsing data",
                    description = "Clear cache, cookies, form data and history",
                    icon = Icons.Outlined.Delete,
                    onClick = { showClearBrowsingDataDialog = true }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Clear data on exit",
                    description = "Automatically clear selected data when browser closes",
                    icon = Icons.Outlined.CleaningServices,
                    isChecked = clearOnExit,
                    onClick = { clearOnExit = !clearOnExit }
                )
            }
            item {
                PreferenceItem(
                    title = "Password manager",
                    description = "Save and autofill passwords securely",
                    icon = Icons.Outlined.Password,
                    onClick = {}
                )
            }
            item {
                PreferenceItem(
                    title = "Cookies & site permissions",
                    description = "Manage third-party cookies, camera, mic and location",
                    icon = Icons.Outlined.Cookie,
                    onClick = {
                        onNavigateTo("site_config")
                    }
                )
            }
        }
    }

    if (showClearBrowsingDataDialog) {
        ClearBrowsingDataDialog(
            onDismissRequest = { showClearBrowsingDataDialog = false },
            onConfirm = { _ ->
                onClearData()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorSettingsPage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val engineState by RenTorEngine.state.collectAsState()
    var isTorEnabled by remember { mutableStateOf(RenTorManager.isTorEnabled(context)) }
    val proxyHost = remember { RenTorManager.getProxyHost(context) }
    var proxyPort by remember { mutableIntStateOf(RenTorManager.getProxyPort(context)) }
    var controlPort by remember { mutableIntStateOf(RenTorManager.getControlPort(context)) }
    var blockGeo by remember { mutableStateOf(RenTorManager.isBlockGeolocation(context)) }
    var useBridges by remember { mutableStateOf(RenTorManager.isUseBridgesEnabled(context)) }
    var bridgeType by remember { mutableStateOf(RenTorManager.getBridgeType(context)) }
    var exitCountry by remember { mutableStateOf(RenTorManager.getExitCountry(context)) }

    var showPortDialog by remember { mutableStateOf(false) }
    var tempPortString by remember { mutableStateOf(proxyPort.toString()) }
    var showBridgeDialog by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showCustomBridgeDialog by remember { mutableStateOf(false) }
    var customBridgesText by remember { mutableStateOf(RenTorManager.getCustomBridges(context)) }

    var testStatusText by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var isRenewingIdentity by remember { mutableStateOf(false) }

    val countries = remember {
        listOf(
            null to "Automatic (Any Country)",
            "ch" to "Switzerland 🇨🇭 (High Privacy)",
            "is" to "Iceland 🇮🇸 (High Privacy)",
            "de" to "Germany 🇩🇪",
            "nl" to "Netherlands 🇳🇱",
            "se" to "Sweden 🇸🇪",
            "us" to "United States 🇺🇸",
            "ca" to "Canada 🇨🇦"
        )
    }

    BasePreferencePage(
        title = stringResource(id = R.string.tor_settings),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
            item {
                PreferenceSubtitle(text = "Ren-Tor Privacy Engine")
            }

            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.tor_network),
                    description = if (isTorEnabled) {
                        when (engineState.status) {
                            RenTorStatus.CONNECTED -> "✓ Ren-Tor Connected & Routing (SOCKS:$proxyPort)"
                            RenTorStatus.BOOTSTRAPPING -> "Bootstrapping circuits... (${engineState.bootstrapProgress}%)"
                            RenTorStatus.INITIALIZING -> "Initializing Ren-Tor daemon..."
                            RenTorStatus.REFRESHING_IDENTITY -> "Renewing Tor circuits & identity..."
                            RenTorStatus.ERROR -> "Engine error: ${engineState.lastError ?: "Failed to connect"}"
                            else -> stringResource(id = R.string.tor_network_enabled)
                        }
                    } else {
                        stringResource(id = R.string.tor_network_desc)
                    },
                    icon = Icons.Outlined.VpnLock,
                    isChecked = isTorEnabled,
                    onClick = {
                        RenTorManager.toggleTor(context) { enabled, success ->
                            isTorEnabled = enabled
                            val msg = if (enabled) {
                                if (success) context.getString(R.string.tor_network_enabled) else "Ren-Tor proxy locked (Routing via 127.0.0.1:$proxyPort)"
                            } else {
                                context.getString(R.string.tor_network_disabled)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            if (isTorEnabled) {
                item {
                    PreferenceItem(
                        title = stringResource(id = R.string.ren_tor_renew_identity),
                        description = if (isRenewingIdentity) "Requesting fresh circuit (NEWNYM)..." else stringResource(id = R.string.ren_tor_renew_identity_desc),
                        icon = Icons.Outlined.Refresh,
                        onClick = {
                            if (!isRenewingIdentity) {
                                isRenewingIdentity = true
                                RenTorManager.renewIdentity { success, msg ->
                                    isRenewingIdentity = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            item {
                PreferenceSubtitle(text = "Anti-Censorship & Bridges")
            }

            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.ren_tor_bridges),
                    description = if (useBridges) "Using ${bridgeType.displayName}" else stringResource(id = R.string.ren_tor_bridges_desc),
                    icon = Icons.Outlined.Shield,
                    isChecked = useBridges,
                    onClick = {
                        val next = !useBridges
                        useBridges = next
                        RenTorManager.setUseBridgesEnabled(context, next)
                        if (next && bridgeType == BridgeType.NONE) {
                            bridgeType = BridgeType.SNOWFLAKE
                            RenTorManager.setBridgeType(context, BridgeType.SNOWFLAKE)
                        }
                    }
                )
            }

            if (useBridges) {
                item {
                    PreferenceItem(
                        title = "Bridge Transport Type",
                        description = bridgeType.displayName,
                        icon = Icons.Outlined.Tune,
                        onClick = { showBridgeDialog = true }
                    )
                }

                if (bridgeType == BridgeType.CUSTOM) {
                    item {
                        PreferenceItem(
                            title = "Configure Custom Bridges",
                            description = if (customBridgesText.isBlank()) "Tap to enter bridge lines" else customBridgesText.take(40) + "...",
                            icon = Icons.Outlined.EditNote,
                            onClick = { showCustomBridgeDialog = true }
                        )
                    }
                }
            }

            item {
                PreferenceSubtitle(text = "Geographic Exit Nodes")
            }

            item {
                val currentCountryLabel = countries.find { it.first == exitCountry }?.second ?: (exitCountry?.uppercase() ?: "Automatic (Any Country)")
                PreferenceItem(
                    title = stringResource(id = R.string.ren_tor_exit_country),
                    description = currentCountryLabel,
                    icon = Icons.Outlined.Public,
                    onClick = { showCountryDialog = true }
                )
            }

            item {
                PreferenceSubtitle(text = "Status & Diagnostics")
            }

            item {
                PreferenceItem(
                    title = stringResource(id = R.string.ren_tor_test_connection),
                    description = if (isTestingConnection) "Connecting through Ren-Tor exit node..." else (testStatusText ?: stringResource(id = R.string.ren_tor_test_connection_desc)),
                    icon = Icons.Outlined.Security,
                    onClick = {
                        if (!isTestingConnection) {
                            isTestingConnection = true
                            testStatusText = "Connecting through Ren-Tor to check.torproject.org..."
                            RenTorEngine.verifyConnection(
                                host = proxyHost,
                                port = proxyPort
                            ) { isTor, ip, message ->
                                isTestingConnection = false
                                testStatusText = if (isTor && ip != null) {
                                    "✓ Ren-Tor Verified! Exit IP: $ip"
                                } else if (ip != null) {
                                    "IP: $ip (Not Tor - $message)"
                                } else {
                                    "Connection check: $message"
                                }
                            }
                        }
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Proxy & Port Configuration")
            }

            item {
                PreferenceItem(
                    title = stringResource(id = R.string.tor_proxy_host),
                    description = proxyHost,
                    icon = Icons.Outlined.Computer,
                    onClick = {}
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(id = R.string.tor_proxy_port),
                    description = "SOCKS Port: $proxyPort (Control: $controlPort)",
                    icon = Icons.Outlined.Tune,
                    onClick = {
                        tempPortString = proxyPort.toString()
                        showPortDialog = true
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Privacy & Zero-Leak Protection")
            }

            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.tor_block_geolocation),
                    description = stringResource(id = R.string.tor_block_geolocation_desc),
                    icon = Icons.Outlined.Shield,
                    isChecked = blockGeo,
                    onClick = {
                        val newGeo = !blockGeo
                        blockGeo = newGeo
                        RenTorManager.setBlockGeolocation(context, newGeo)
                    }
                )
            }
        }
    }

    if (showCountryDialog) {
        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            title = { Text(stringResource(id = R.string.ren_tor_exit_country)) },
            text = {
                Column {
                    countries.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    exitCountry = code
                                    RenTorManager.setExitCountry(context, code)
                                    showCountryDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (exitCountry == code),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryDialog = false }) {
                    Text(stringResource(id = R.string.close))
                }
            }
        )
    }

    if (showBridgeDialog) {
        AlertDialog(
            onDismissRequest = { showBridgeDialog = false },
            title = { Text("Select Bridge Type") },
            text = {
                Column {
                    listOf(
                        BridgeType.SNOWFLAKE,
                        BridgeType.OBFS4,
                        BridgeType.MEEK_AZURE,
                        BridgeType.CUSTOM
                    ).forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    bridgeType = type
                                    RenTorManager.setBridgeType(context, type)
                                    showBridgeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (bridgeType == type),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = type.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBridgeDialog = false }) {
                    Text(stringResource(id = R.string.close))
                }
            }
        )
    }

    if (showCustomBridgeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomBridgeDialog = false },
            title = { Text("Custom Bridge Relays") },
            text = {
                OutlinedTextField(
                    value = customBridgesText,
                    onValueChange = { customBridgesText = it },
                    label = { Text("Bridge lines (one per line)") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
            },
            confirmButton = {
                Button(onClick = {
                    RenTorManager.setCustomBridges(context, customBridgesText)
                    showCustomBridgeDialog = false
                }) {
                    Text(stringResource(id = R.string.proceed))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomBridgeDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showPortDialog) {
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            title = { Text(stringResource(id = R.string.tor_proxy_port)) },
            text = {
                OutlinedTextField(
                    value = tempPortString,
                    onValueChange = { tempPortString = it },
                    label = { Text("Port Number") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = tempPortString.toIntOrNull()
                    if (parsed != null && parsed in 1..65535) {
                        proxyPort = parsed
                        RenTorManager.setProxyPort(context, parsed)
                        if (isTorEnabled) {
                            RenTorManager.applyProxyOverride(context)
                        }
                        showPortDialog = false
                    } else {
                        Toast.makeText(context, "Invalid port (1-65535)", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(id = R.string.proceed))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPortDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarpSettingsPage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isWarpEnabled by remember { mutableStateOf(WarpManager.isWarpEnabled(context)) }
    var proxyHost by remember { mutableStateOf(WarpManager.getProxyHost(context)) }
    var proxyPort by remember { mutableIntStateOf(WarpManager.getProxyPort(context)) }
    var securityMode by remember { mutableStateOf(WarpManager.getSecurityMode(context)) }
    var licenseKey by remember { mutableStateOf(WarpManager.getLicenseKey(context)) }
    var isDohEnabled by remember { mutableStateOf(WarpManager.isDohEnabled(context)) }

    var showHostDialog by remember { mutableStateOf(false) }
    var tempHostString by remember { mutableStateOf(proxyHost) }
    var showPortDialog by remember { mutableStateOf(false) }
    var tempPortString by remember { mutableStateOf(proxyPort.toString()) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var tempLicenseString by remember { mutableStateOf(licenseKey) }
    var showSecurityModeDialog by remember { mutableStateOf(false) }

    var testStatusText by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var proxyPingStatus by remember { mutableStateOf<String?>(null) }
    var isPingingProxy by remember { mutableStateOf(false) }

    BasePreferencePage(
        title = stringResource(id = R.string.cloudflare_warp_settings),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How Cloudflare in Ren works",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Cloudflare WARP Proxy: Routes browser traffic through Cloudflare Edge and changes your public IP. Requires an active WARP / SOCKS5 proxy on the configured endpoint below.\n\n• 1.1.1.1 Secure DNS (DoH): Encrypts DNS lookups so internet providers cannot see domains you visit (DNS encryption does not change your public IP address).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.cloudflare_warp),
                    description = if (isWarpEnabled)
                        stringResource(id = R.string.cloudflare_warp_enabled)
                    else
                        stringResource(id = R.string.cloudflare_warp_desc),
                    icon = painterResource(id = R.drawable.ic_cloudflare_warp),
                    isChecked = isWarpEnabled,
                    onClick = {
                        WarpManager.toggleWarp(context) { enabled, success, errorMsg ->
                            isWarpEnabled = enabled
                            val msg = if (enabled) {
                                if (success) context.getString(R.string.cloudflare_warp_enabled) else "WARP proxy applied"
                            } else {
                                errorMsg ?: context.getString(R.string.cloudflare_warp_disabled)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Status & Diagnostics")
            }

            item {
                PreferenceItem(
                    title = "Test Proxy Port Reachability",
                    description = if (isPingingProxy) "Testing connection to $proxyHost:$proxyPort..." else (proxyPingStatus ?: "Check if the local/remote WARP proxy daemon is running on $proxyHost:$proxyPort"),
                    icon = Icons.Outlined.Tune,
                    onClick = {
                        if (!isPingingProxy) {
                            isPingingProxy = true
                            proxyPingStatus = "Pinging proxy port $proxyHost:$proxyPort..."
                            WarpManager.checkProxyHealth(context) { isOpen, msg ->
                                isPingingProxy = false
                                proxyPingStatus = if (isOpen) "✓ $msg" else "✗ $msg"
                            }
                        }
                    }
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(id = R.string.warp_diagnostics),
                    description = if (isTestingConnection) "Checking Cloudflare edge connection..." else (testStatusText ?: stringResource(id = R.string.warp_diagnostics_desc)),
                    icon = Icons.Outlined.Security,
                    onClick = {
                        if (!isTestingConnection) {
                            isTestingConnection = true
                            testStatusText = "Connecting to Cloudflare edge..."
                            WarpManager.verifyWarpConnection(context) { isWarp, ip, loc, colo, message ->
                                isTestingConnection = false
                                testStatusText = if (ip != null) {
                                    if (isWarp) "✓ Active IP: $ip ($colo, $loc) • $message" else "⚠ Real IP Exposed: $ip ($colo, $loc) • $message"
                                } else {
                                    "Status: $message"
                                }
                            }
                        }
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Security & DNS Mode")
            }

            item {
                PreferenceItem(
                    title = stringResource(id = R.string.warp_security_mode),
                    description = securityMode.title,
                    icon = Icons.Outlined.Shield,
                    onClick = {
                        showSecurityModeDialog = true
                    }
                )
            }

            item {
                PreferenceSwitch(
                    title = "DNS over HTTPS (1.1.1.1 DoH)",
                    description = "Encrypt all DNS requests using Cloudflare Anycast resolvers",
                    icon = Icons.Outlined.Lock,
                    isChecked = isDohEnabled,
                    onClick = {
                        val newDoh = !isDohEnabled
                        isDohEnabled = newDoh
                        WarpManager.setDohEnabled(context, newDoh)
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Proxy & Endpoint Configuration")
            }

            item {
                PreferenceItem(
                    title = "WARP Proxy Host",
                    description = "Host: $proxyHost (Default: 127.0.0.1)",
                    icon = Icons.Outlined.Computer,
                    onClick = {
                        tempHostString = proxyHost
                        showHostDialog = true
                    }
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(id = R.string.warp_proxy_port),
                    description = "WARP SOCKS/HTTP Port: $proxyPort (Default: 9060)",
                    icon = Icons.Outlined.Tune,
                    onClick = {
                        tempPortString = proxyPort.toString()
                        showPortDialog = true
                    }
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(id = R.string.warp_license_key),
                    description = if (licenseKey.isNotEmpty()) "Configured (WARP+ Active)" else stringResource(id = R.string.warp_license_key_desc),
                    icon = Icons.Outlined.Key,
                    onClick = {
                        tempLicenseString = licenseKey
                        showLicenseDialog = true
                    }
                )
            }
        }
    }

    if (showHostDialog) {
        AlertDialog(
            onDismissRequest = { showHostDialog = false },
            title = { Text("WARP Proxy Host") },
            text = {
                OutlinedTextField(
                    value = tempHostString,
                    onValueChange = { tempHostString = it },
                    label = { Text("Host Address (e.g. 127.0.0.1)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = tempHostString.trim()
                    if (trimmed.isNotEmpty()) {
                        proxyHost = trimmed
                        WarpManager.setProxyHost(context, trimmed)
                        if (isWarpEnabled) {
                            WarpManager.applyProxyOverride(context)
                        }
                        showHostDialog = false
                    }
                }) {
                    Text(stringResource(id = R.string.proceed))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHostDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showPortDialog) {
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            title = { Text(stringResource(id = R.string.warp_proxy_port)) },
            text = {
                OutlinedTextField(
                    value = tempPortString,
                    onValueChange = { tempPortString = it },
                    label = { Text("Port Number") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = tempPortString.toIntOrNull()
                    if (parsed != null && parsed in 1..65535) {
                        proxyPort = parsed
                        WarpManager.setProxyPort(context, parsed)
                        if (isWarpEnabled) {
                            WarpManager.applyProxyOverride(context)
                        }
                        showPortDialog = false
                    } else {
                        Toast.makeText(context, "Invalid port (1-65535)", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(id = R.string.proceed))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPortDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = { Text(stringResource(id = R.string.warp_license_key)) },
            text = {
                Column {
                    Text(
                        text = "Paste your Cloudflare WARP+ license key from the 1.1.1.1 app or Zero Trust dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = tempLicenseString,
                        onValueChange = { tempLicenseString = it },
                        label = { Text("License Key") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    licenseKey = tempLicenseString.trim()
                    WarpManager.setLicenseKey(context, licenseKey)
                    showLicenseDialog = false
                    Toast.makeText(context, "WARP key saved", Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(id = R.string.proceed))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLicenseDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showSecurityModeDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityModeDialog = false },
            title = { Text(stringResource(id = R.string.warp_security_mode)) },
            text = {
                Column {
                    WarpSecurityMode.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    securityMode = mode
                                    WarpManager.setSecurityMode(context, mode)
                                    showSecurityModeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = (securityMode == mode),
                                onClick = {
                                    securityMode = mode
                                    WarpManager.setSecurityMode(context, mode)
                                    showSecurityModeDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityModeDialog = false }) {
                    Text(stringResource(id = R.string.close))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdBlockingSettingsPage(
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit
) {
    val context = LocalContext.current
    var isAdBlockOn by remember { mutableStateOf(AdBlockManager.isAdBlockEnabled(context)) }
    var isAntiAdblockOn by remember { mutableStateOf(AdBlockManager.isAntiAdblockEnabled(context)) }

    BasePreferencePage(
        title = stringResource(id = R.string.ad_blocking),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
            item {
                PreferenceSubtitle(text = "Filters & Rules")
            }
            item {
                PreferenceSwitch(
                    title = "Ad blocking",
                    description = AdBlockManager.getFormattedStatsSubtitle(context),
                    icon = Icons.Outlined.Shield,
                    isChecked = isAdBlockOn,
                    onClick = {
                        isAdBlockOn = !isAdBlockOn
                        AdBlockManager.setAdBlockEnabled(context, isAdBlockOn)
                    }
                )
            }
            item {
                PreferenceItem(
                    title = "Custom filters",
                    description = "Custom rules and marked ads",
                    icon = Icons.Outlined.EditNote,
                    onClick = { onNavigateTo("custom_filters") }
                )
            }
            item {
                PreferenceItem(
                    title = "Filter subscriptions",
                    description = "Subscribe to EasyList and Adblock Plus filter lists",
                    icon = Icons.Outlined.Subscriptions,
                    onClick = { onNavigateTo("filter_subscriptions") }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Anti-adblock",
                    description = "Prevent websites from detecting adblock and bypass anti-adblock popups",
                    icon = Icons.Outlined.Security,
                    isChecked = isAntiAdblockOn,
                    onClick = {
                        isAntiAdblockOn = !isAntiAdblockOn
                        AdBlockManager.setAntiAdblockEnabled(context, isAntiAdblockOn)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFiltersPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var filterText by remember { mutableStateOf(AdBlockManager.getCustomFilters(context)) }
    var showHelpDialog by remember { mutableStateOf(false) }

    BasePreferencePage(
        title = "Custom filters",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showHelpDialog = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = "Help"
                )
            }
            IconButton(onClick = {
                AdBlockManager.setCustomFilters(context, filterText)
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                label = { Text("Filters (one rule per line)") },
                placeholder = { Text("||example.com^\n/ads/banner") },
                modifier = Modifier.fillMaxSize(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Filter Syntax Help") },
            text = {
                Text(
                    "Rules are applied line by line:\n\n" +
                    "• ||example.com^ : Blocks domains & subdomains\n" +
                    "• |https://example.com/ad.js| : Exact URL match\n" +
                    "• /ads/banner : URL keyword match\n" +
                    "• ! Comment line"
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSubscriptionsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val subscriptions = remember { mutableStateListOf<FilterSubscription>() }
    var isUpdating by remember { mutableStateOf(false) }
    var subToDelete by remember { mutableStateOf<FilterSubscription?>(null) }

    val refreshSubs = {
        subscriptions.clear()
        subscriptions.addAll(AdBlockManager.getSubscriptions(context))
    }

    remember {
        refreshSubs()
        true
    }

    var showAddDialog by remember { mutableStateOf(false) }

    BasePreferencePage(
        title = "Filter subscriptions",
        onBack = onBack,
        actions = {
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(
                    onClick = {
                        isUpdating = true
                        Toast.makeText(context, "Updating filter subscriptions...", Toast.LENGTH_SHORT).show()
                        AdBlockManager.updateAllSubscriptions(
                            context = context,
                            onProgress = { _, _ -> },
                            onComplete = {
                                isUpdating = false
                                refreshSubs()
                                Toast.makeText(context, "Filter subscriptions updated", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Update subscriptions"
                    )
                }
            }
            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
            item {
                PreferenceSubtitle(text = "Active Subscriptions")
            }
            items(subscriptions) { sub ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PreferenceSwitch(
                            title = sub.title,
                            description = "${sub.url}\n${sub.filterCount} rules • ${sub.getFormattedUpdate()}",
                            isChecked = sub.isEnabled,
                            onClick = {
                                AdBlockManager.toggleSubscription(context, sub.id, !sub.isEnabled)
                                refreshSubs()
                            }
                        )
                    }
                    if (sub.isCustom) {
                        IconButton(
                            onClick = { subToDelete = sub },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (subToDelete != null) {
        val target = subToDelete!!
        AlertDialog(
            onDismissRequest = { subToDelete = null },
            title = { Text("Delete Subscription") },
            text = { Text("Are you sure you want to delete \"${target.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        AdBlockManager.deleteSubscription(context, target.id)
                        refreshSubs()
                        subToDelete = null
                        Toast.makeText(context, "Subscription deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { subToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Subscription") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("URL (e.g. https://...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUrl.isNotBlank()) {
                            Toast.makeText(context, "Adding subscription & fetching rules...", Toast.LENGTH_SHORT).show()
                            AdBlockManager.addSubscription(
                                context = context,
                                url = newUrl.trim(),
                                title = newTitle.trim().ifBlank { null }
                            ) {
                                refreshSubs()
                                Toast.makeText(context, "Subscription rules downloaded and active", Toast.LENGTH_SHORT).show()
                            }
                            refreshSubs()
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAgentSettingsPage(
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onUserAgentChanged: () -> Unit
) {
    val context = LocalContext.current
    var selectedTitle by remember { mutableStateOf(UserAgentManager.getSelectedTitle(context)) }
    val allUas = remember { UserAgentManager.getAllUserAgents(context) }
    var isReductionOn by remember { mutableStateOf(UserAgentManager.isReductionEnabled(context)) }

    BasePreferencePage(
        title = stringResource(id = R.string.user_agent),
        onBack = onBack,
        actions = {
            IconButton(onClick = { onNavigateTo("new_user_agent") }) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add custom user agent"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
            item {
                PreferenceSubtitle(text = "Available Identities")
            }
            items(allUas) { uaItem ->
                PreferenceRadio(
                    title = uaItem.title,
                    description = if (!uaItem.title.equals("Default", ignoreCase = true)) uaItem.userAgentString else null,
                    isSelected = uaItem.title.equals(selectedTitle, ignoreCase = true),
                    onClick = {
                        selectedTitle = uaItem.title
                        UserAgentManager.setSelectedTitle(context, uaItem.title)
                        onUserAgentChanged()
                        Toast.makeText(context, "Selected: ${uaItem.title}", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Advanced Options")
            }
            item {
                PreferenceItem(
                    title = "User-agent in desktop mode",
                    description = UserAgentManager.getDesktopModeTitle(context),
                    icon = Icons.Outlined.Computer,
                    onClick = {}
                )
            }
            item {
                PreferenceSwitch(
                    title = "User-agent reduction",
                    description = "Remove device and minor version details from User-agent for privacy",
                    icon = Icons.Outlined.Security,
                    isChecked = isReductionOn,
                    onClick = {
                        isReductionOn = !isReductionOn
                        UserAgentManager.setReductionEnabled(context, isReductionOn)
                        onUserAgentChanged()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewUserAgentPage(
    onBack: () -> Unit,
    onUserAgentSaved: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var uaString by remember { mutableStateOf("") }

    BasePreferencePage(
        title = "New User-agent",
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                if (title.isBlank()) {
                    Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                    return@IconButton
                }
                val finalUa = if (uaString.isNotBlank()) uaString.trim() else title.trim()
                UserAgentManager.addCustomUserAgent(context, title.trim(), finalUa)
                UserAgentManager.setSelectedTitle(context, title.trim())
                onUserAgentSaved()
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                onBack()
            }) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("e.g. My Custom Browser") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uaString,
                onValueChange = { uaString = it },
                label = { Text("User-agent String") },
                placeholder = { Text("Mozilla/5.0 ...") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsPage(onBack: () -> Unit) {
    BasePreferencePage(
        title = stringResource(id = R.string.advanced_settings),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
            item {
                PreferenceSubtitle(text = "Rendering & Network")
            }
            item {
                PreferenceItem(
                    title = "Hardware acceleration",
                    description = "Enabled",
                    icon = Icons.Outlined.Build,
                    onClick = {}
                )
            }
            item {
                PreferenceItem(
                    title = "Developer options",
                    description = "WebView remote debugging and inspection",
                    icon = Icons.Outlined.Code,
                    onClick = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptsSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var isScriptsEnabled by remember { mutableStateOf(ScriptManager.isScriptsEnabled(context)) }
    val scripts = remember { mutableStateListOf<UserScript>() }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var showAddCodeDialog by remember { mutableStateOf(false) }
    var scriptToDelete by remember { mutableStateOf<UserScript?>(null) }
    var viewingScript by remember { mutableStateOf<UserScript?>(null) }
    var isUpdatingScripts by remember { mutableStateOf(false) }

    val refreshScripts = {
        scripts.clear()
        scripts.addAll(ScriptManager.getScripts(context))
    }

    remember {
        refreshScripts()
        true
    }

    val scriptFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val code = stream.bufferedReader().readText()
                    if (code.isNotBlank()) {
                        val added = ScriptManager.addScript(context, code)
                        refreshScripts()
                        Toast.makeText(context, "Script \"${added.name}\" installed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read script file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BasePreferencePage(
        title = stringResource(id = R.string.scripts_addons),
        onBack = onBack,
        actions = {
            IconButton(
                onClick = {
                    if (!isUpdatingScripts) {
                        isUpdatingScripts = true
                        Toast.makeText(context, "Checking for script updates...", Toast.LENGTH_SHORT).show()
                        ScriptManager.updateAllScripts(context) { updatedCount, totalOnlineCount ->
                            isUpdatingScripts = false
                            refreshScripts()
                            if (totalOnlineCount == 0) {
                                Toast.makeText(context, "Scripts reloaded (no URL-based scripts)", Toast.LENGTH_SHORT).show()
                            } else if (updatedCount > 0) {
                                Toast.makeText(context, "Updated $updatedCount of $totalOnlineCount script(s)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "All $totalOnlineCount script(s) are up to date", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isUpdatingScripts
            ) {
                if (isUpdatingScripts) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Update scripts"
                    )
                }
            }
            IconButton(onClick = { showAddUrlDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add script"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
            item {
                PreferenceSubtitle(text = "General")
            }
            item {
                PreferenceSwitch(
                    title = "Enable user scripts",
                    description = if (isScriptsEnabled) "User scripts are active and injected into matching web pages" else "User script execution is disabled",
                    icon = Icons.Outlined.Code,
                    isChecked = isScriptsEnabled,
                    onClick = {
                        val nv = !isScriptsEnabled
                        isScriptsEnabled = nv
                        ScriptManager.setScriptsEnabled(context, nv)
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Install Script")
            }
            item {
                PreferenceItem(
                    title = "Install from URL",
                    description = "Download and install .user.js script from URL",
                    icon = Icons.Outlined.Public,
                    onClick = { showAddUrlDialog = true }
                )
            }
            item {
                PreferenceItem(
                    title = "Install from Storage",
                    description = "Import .user.js or .js file from device",
                    icon = Icons.Outlined.Folder,
                    onClick = { scriptFilePicker.launch("*/*") }
                )
            }
            item {
                PreferenceItem(
                    title = "Create / Paste script",
                    description = "Write or paste custom JavaScript code directly",
                    icon = Icons.Outlined.EditNote,
                    onClick = { showAddCodeDialog = true }
                )
            }

            item {
                PreferenceSubtitle(text = "Installed Scripts (${scripts.size})")
            }
            if (scripts.isEmpty()) {
                item {
                    PreferencesHintCard(
                        title = "No user scripts installed",
                        description = "Add scripts via URL, storage file, or paste custom code. Scripts with matching patterns will automatically execute on web pages.",
                        icon = Icons.Outlined.Info,
                        onClick = {}
                    )
                }
            } else {
                items(scripts) { script ->
                    val matchesStr = if (script.matchPatterns.contains("*") || script.matchPatterns.contains("*://*/*")) {
                        "All sites"
                    } else {
                        script.matchPatterns.joinToString(", ")
                    }
                    val subDesc = buildString {
                        if (script.description.isNotBlank()) append(script.description).append("\n")
                        append("v${script.version} • ${script.runAt} • Match: $matchesStr")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            PreferenceSwitch(
                                title = script.name,
                                description = subDesc,
                                isChecked = script.isEnabled,
                                onClick = {
                                    ScriptManager.toggleScript(context, script.id, !script.isEnabled)
                                    refreshScripts()
                                }
                            )
                        }
                        IconButton(
                            onClick = { viewingScript = script },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = "View script",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { scriptToDelete = script },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddUrlDialog) {
        var scriptUrl by remember { mutableStateOf("") }
        var isDownloading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isDownloading) showAddUrlDialog = false },
            title = { Text("Install from URL") },
            text = {
                Column {
                    Text(
                        text = "Enter direct URL to .user.js script:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scriptUrl,
                        onValueChange = { scriptUrl = it },
                        label = { Text("Script URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (scriptUrl.isNotBlank()) {
                            isDownloading = true
                            Toast.makeText(context, "Downloading script...", Toast.LENGTH_SHORT).show()
                            ScriptManager.downloadScriptFromUrl(context, scriptUrl) { result ->
                                isDownloading = false
                                showAddUrlDialog = false
                                if (result != null) {
                                    refreshScripts()
                                    Toast.makeText(context, "Script \"${result.name}\" installed", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to download or parse script", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isDownloading
                ) {
                    Text(if (isDownloading) "Downloading..." else "Install")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddUrlDialog = false },
                    enabled = !isDownloading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddCodeDialog) {
        var scriptCode by remember { mutableStateOf("// ==UserScript==\n// @name         Custom Script\n// @match        *://*/*\n// @run-at       document-end\n// ==/UserScript==\n\nconsole.log('User script loaded!');\n") }

        AlertDialog(
            onDismissRequest = { showAddCodeDialog = false },
            title = { Text("Create / Paste Script") },
            text = {
                Column {
                    Text(
                        text = "Write JavaScript code with Greasemonkey header:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scriptCode,
                        onValueChange = { scriptCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 320.dp),
                        maxLines = 15
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (scriptCode.isNotBlank()) {
                            val added = ScriptManager.addScript(context, scriptCode)
                            refreshScripts()
                            showAddCodeDialog = false
                            Toast.makeText(context, "Script \"${added.name}\" created & active", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (viewingScript != null) {
        val target = viewingScript!!
        val code = remember(target.id) { target.getCode(context) }

        AlertDialog(
            onDismissRequest = { viewingScript = null },
            title = { Text(target.name) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Version: ${target.version} | Run at: ${target.runAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (target.author.isNotBlank()) {
                        Text(
                            text = "Author: ${target.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Matches: ${target.matchPatterns.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = code.ifBlank { "// Empty script code" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Row {
                    if (!target.sourceUrl.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                Toast.makeText(context, "Updating \"${target.name}\"...", Toast.LENGTH_SHORT).show()
                                ScriptManager.updateScript(context, target) { success, msg ->
                                    refreshScripts()
                                    if (success) {
                                        viewingScript = ScriptManager.getScripts(context).find { it.id == target.id }
                                    }
                                    Toast.makeText(context, msg ?: if (success) "Script updated" else "Update failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Update")
                        }
                    }
                    TextButton(onClick = { viewingScript = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (scriptToDelete != null) {
        val target = scriptToDelete!!
        AlertDialog(
            onDismissRequest = { scriptToDelete = null },
            title = { Text("Delete Script") },
            text = { Text("Are you sure you want to delete script \"${target.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        ScriptManager.deleteScript(context, target.id)
                        refreshScripts()
                        scriptToDelete = null
                        Toast.makeText(context, "Script deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { scriptToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsPage(onBack: () -> Unit) {
    BasePreferencePage(
        title = stringResource(id = R.string.about),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
            item {
                PreferenceSubtitle(text = "Application Info")
            }
            item {
                PreferenceItem(
                    title = "Ren Browser",
                    description = "Version 0.0.2-beta (Material 3 Edition)",
                    icon = Icons.Outlined.Info,
                    onClick = {}
                )
            }
            item {
                PreferenceItem(
                    title = "Privacy Policy",
                    description = "Read our privacy guidelines",
                    icon = Icons.Outlined.Shield,
                    onClick = {}
                )
            }
            item {
                PreferenceItem(
                    title = "Open source licenses",
                    description = "Third-party libraries and licenses",
                    icon = Icons.Outlined.Code,
                    onClick = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxSettingsPage(
    onBack: () -> Unit,
    onPurgeData: () -> Unit = {}
) {
    val context = LocalContext.current
    var isSandboxActive by remember { mutableStateOf(SandboxManager.isSandboxEnabled(context)) }
    var forgetOnSiteClose by remember { mutableStateOf(SandboxManager.isForgetOnSiteCloseEnabled(context)) }
    var blockTrackers by remember { mutableStateOf(SandboxManager.isBlockTrackers(context)) }
    var blockHardware by remember { mutableStateOf(SandboxManager.isBlockHardware(context)) }
    var blockIntents by remember { mutableStateOf(SandboxManager.isBlockIntents(context)) }
    var blockWebRtc by remember { mutableStateOf(SandboxManager.isBlockWebRtc(context)) }

    BasePreferencePage(
        title = stringResource(id = R.string.sandbox_settings),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                PreferenceSubtitle(text = "Sandbox Environment")
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.sandbox_mode_title),
                    description = stringResource(id = R.string.sandbox_mode_desc),
                    icon = SandboxIcon,
                    isChecked = isSandboxActive,
                    onClick = {
                        val newValue = !isSandboxActive
                        isSandboxActive = newValue
                        SandboxManager.setSandboxEnabled(context, newValue) {
                            val msg = if (newValue) {
                                context.getString(R.string.sandbox_enabled_msg)
                            } else {
                                context.getString(R.string.sandbox_disabled_msg)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Quarantine & Protection Rules")
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.sandbox_forget_on_close_title),
                    description = stringResource(id = R.string.sandbox_forget_on_close_desc),
                    icon = Icons.Outlined.Cookie,
                    isChecked = forgetOnSiteClose,
                    onClick = {
                        val nv = !forgetOnSiteClose
                        forgetOnSiteClose = nv
                        SandboxManager.setForgetOnSiteCloseEnabled(context, nv)
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.sandbox_block_trackers),
                    description = stringResource(id = R.string.sandbox_block_trackers_desc),
                    icon = Icons.Outlined.FilterAlt,
                    isChecked = blockTrackers,
                    onClick = {
                        val nv = !blockTrackers
                        blockTrackers = nv
                        SandboxManager.setBlockTrackers(context, nv)
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.sandbox_block_hardware),
                    description = stringResource(id = R.string.sandbox_block_hardware_desc),
                    icon = Icons.Outlined.Devices,
                    isChecked = blockHardware,
                    onClick = {
                        val nv = !blockHardware
                        blockHardware = nv
                        SandboxManager.setBlockHardware(context, nv)
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.sandbox_block_intents),
                    description = stringResource(id = R.string.sandbox_block_intents_desc),
                    icon = Icons.Outlined.OpenInBrowser,
                    isChecked = blockIntents,
                    onClick = {
                        val nv = !blockIntents
                        blockIntents = nv
                        SandboxManager.setBlockIntents(context, nv)
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.sandbox_block_webrtc),
                    description = stringResource(id = R.string.sandbox_block_webrtc_desc),
                    icon = Icons.Outlined.VpnLock,
                    isChecked = blockWebRtc,
                    onClick = {
                        val nv = !blockWebRtc
                        blockWebRtc = nv
                        SandboxManager.setBlockWebRtc(context, nv)
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Data Management")
            }
            item {
                PreferenceItem(
                    title = stringResource(id = R.string.sandbox_purge_now),
                    description = stringResource(id = R.string.sandbox_purge_now_desc),
                    icon = Icons.Outlined.CleaningServices,
                    onClick = {
                        SandboxManager.purgeSandboxData(context) {
                            Toast.makeText(context, context.getString(R.string.sandbox_purged_toast), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiFingerprintSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var isMasterOn by remember { mutableStateOf(AntiFingerprintManager.isAntiFingerprintEnabled(context)) }
    var isCanvasOn by remember { mutableStateOf(AntiFingerprintManager.isCanvasProtectionEnabled(context)) }
    var isWebGlOn by remember { mutableStateOf(AntiFingerprintManager.isWebGlProtectionEnabled(context)) }
    var isAudioOn by remember { mutableStateOf(AntiFingerprintManager.isAudioProtectionEnabled(context)) }
    var isFontOn by remember { mutableStateOf(AntiFingerprintManager.isFontProtectionEnabled(context)) }
    var isDomRectOn by remember { mutableStateOf(AntiFingerprintManager.isDomRectProtectionEnabled(context)) }
    var isHardwareOn by remember { mutableStateOf(AntiFingerprintManager.isHardwareProtectionEnabled(context)) }
    var isBatteryOn by remember { mutableStateOf(AntiFingerprintManager.isBatteryProtectionEnabled(context)) }
    var isMediaOn by remember { mutableStateOf(AntiFingerprintManager.isMediaDevicesProtectionEnabled(context)) }
    var isSpeechOn by remember { mutableStateOf(AntiFingerprintManager.isSpeechProtectionEnabled(context)) }
    var isTimerOn by remember { mutableStateOf(AntiFingerprintManager.isTimerProtectionEnabled(context)) }

    BasePreferencePage(
        title = stringResource(id = R.string.anti_fingerprint),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                PreferenceSubtitle(text = "Master Switch")
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.anti_fingerprint_master_switch),
                    description = if (isMasterOn)
                        "All anti-fingerprinting shields are currently ACTIVE"
                    else
                        "Fingerprinting defenses are DISABLED",
                    icon = Icons.Outlined.Fingerprint,
                    isChecked = isMasterOn,
                    onClick = {
                        val nv = !isMasterOn
                        isMasterOn = nv
                        AntiFingerprintManager.setAntiFingerprintEnabled(context, nv)
                    }
                )
            }
            item {
                PreferenceSubtitle(text = "Protection Vectors")
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.afp_canvas_title),
                    description = stringResource(id = R.string.afp_canvas_desc),
                    icon = Icons.Outlined.Brush,
                    enabled = isMasterOn,
                    isChecked = isCanvasOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isCanvasOn
                            isCanvasOn = nv
                            AntiFingerprintManager.setCanvasProtectionEnabled(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.afp_webgl_title),
                    description = stringResource(id = R.string.afp_webgl_desc),
                    icon = Icons.Outlined.Computer,
                    enabled = isMasterOn,
                    isChecked = isWebGlOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isWebGlOn
                            isWebGlOn = nv
                            AntiFingerprintManager.setWebGlProtectionEnabled(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.afp_audio_title),
                    description = stringResource(id = R.string.afp_audio_desc),
                    icon = Icons.Outlined.GraphicEq,
                    enabled = isMasterOn,
                    isChecked = isAudioOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isAudioOn
                            isAudioOn = nv
                            AntiFingerprintManager.setAudioProtectionEnabled(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.afp_font_title),
                    description = stringResource(id = R.string.afp_font_desc),
                    icon = Icons.Outlined.FontDownload,
                    enabled = isMasterOn,
                    isChecked = isFontOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isFontOn
                            isFontOn = nv
                            AntiFingerprintManager.setFontProtectionEnabled(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.afp_dom_rect_title),
                    description = stringResource(id = R.string.afp_dom_rect_desc),
                    icon = Icons.Outlined.CropFree,
                    enabled = isMasterOn,
                    isChecked = isDomRectOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isDomRectOn
                            isDomRectOn = nv
                            AntiFingerprintManager.setDomRectProtectionEnabled(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.afp_hardware_title),
                    description = stringResource(id = R.string.afp_hardware_desc),
                    icon = Icons.Outlined.Memory,
                    enabled = isMasterOn,
                    isChecked = isHardwareOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isHardwareOn
                            isHardwareOn = nv
                            AntiFingerprintManager.setHardwareProtectionEnabled(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.afp_battery_title),
                    description = stringResource(id = R.string.afp_battery_desc),
                    icon = Icons.Outlined.BatteryStd,
                    enabled = isMasterOn,
                    isChecked = isBatteryOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isBatteryOn
                            isBatteryOn = nv
                            AntiFingerprintManager.setBatteryProtectionEnabled(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.afp_media_devices_title),
                    description = stringResource(id = R.string.afp_media_devices_desc),
                    icon = Icons.Outlined.Mic,
                    enabled = isMasterOn,
                    isChecked = isMediaOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isMediaOn
                            isMediaOn = nv
                            AntiFingerprintManager.setMediaDevicesProtectionEnabled(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.afp_speech_title),
                    description = stringResource(id = R.string.afp_speech_desc),
                    icon = Icons.Outlined.RecordVoiceOver,
                    enabled = isMasterOn,
                    isChecked = isSpeechOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isSpeechOn
                            isSpeechOn = nv
                            AntiFingerprintManager.setSpeechProtectionEnabled(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.afp_timer_title),
                    description = stringResource(id = R.string.afp_timer_desc),
                    icon = Icons.Outlined.Timer,
                    enabled = isMasterOn,
                    isChecked = isTimerOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isTimerOn
                            isTimerOn = nv
                            AntiFingerprintManager.setTimerProtectionEnabled(context, nv)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebRtcSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var isMasterOn by remember { mutableStateOf(WebRtcManager.isWebRtcBlockEnabled(context)) }
    var isPeerConnOn by remember { mutableStateOf(WebRtcManager.isPeerConnectionBlocked(context)) }
    var isIpLeakOn by remember { mutableStateOf(WebRtcManager.isIpLeakProtectionEnabled(context)) }
    var isStunTurnOn by remember { mutableStateOf(WebRtcManager.isStunTurnBlocked(context)) }
    var isDataChannelsOn by remember { mutableStateOf(WebRtcManager.isDataChannelsBlocked(context)) }
    var isMediaRestrictedOn by remember { mutableStateOf(WebRtcManager.isMediaDevicesRestricted(context)) }
    var isTorIsolationOn by remember { mutableStateOf(WebRtcManager.isTorIsolationEnabled(context)) }

    BasePreferencePage(
        title = stringResource(id = R.string.block_webrtc),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                PreferenceSubtitle(text = "Master Switch")
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.webrtc_master_switch),
                    description = if (isMasterOn)
                        "All WebRTC blocks and IP leak shields are currently ACTIVE"
                    else
                        "WebRTC protection is DISABLED",
                    icon = Icons.Outlined.Shield,
                    isChecked = isMasterOn,
                    onClick = {
                        val nv = !isMasterOn
                        isMasterOn = nv
                        WebRtcManager.setWebRtcBlockEnabled(context, nv)
                    }
                )
            }
            item {
                PreferenceSubtitle(text = "Protection Vectors")
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.webrtc_peer_connection_title),
                    description = stringResource(id = R.string.webrtc_peer_connection_desc),
                    icon = Icons.Outlined.Block,
                    enabled = isMasterOn,
                    isChecked = isPeerConnOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isPeerConnOn
                            isPeerConnOn = nv
                            WebRtcManager.setPeerConnectionBlocked(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.webrtc_ip_leak_title),
                    description = stringResource(id = R.string.webrtc_ip_leak_desc),
                    icon = Icons.Outlined.Security,
                    enabled = isMasterOn,
                    isChecked = isIpLeakOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isIpLeakOn
                            isIpLeakOn = nv
                            WebRtcManager.setIpLeakProtectionEnabled(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.webrtc_stun_turn_title),
                    description = stringResource(id = R.string.webrtc_stun_turn_desc),
                    icon = Icons.Outlined.Public,
                    enabled = isMasterOn,
                    isChecked = isStunTurnOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isStunTurnOn
                            isStunTurnOn = nv
                            WebRtcManager.setStunTurnBlocked(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.webrtc_data_channels_title),
                    description = stringResource(id = R.string.webrtc_data_channels_desc),
                    icon = Icons.Outlined.Devices,
                    enabled = isMasterOn,
                    isChecked = isDataChannelsOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isDataChannelsOn
                            isDataChannelsOn = nv
                            WebRtcManager.setDataChannelsBlocked(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.webrtc_media_devices_title),
                    description = stringResource(id = R.string.webrtc_media_devices_desc),
                    icon = Icons.Outlined.Mic,
                    enabled = isMasterOn,
                    isChecked = isMediaRestrictedOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isMediaRestrictedOn
                            isMediaRestrictedOn = nv
                            WebRtcManager.setMediaDevicesRestricted(context, nv)
                        }
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.webrtc_tor_isolation_title),
                    description = stringResource(id = R.string.webrtc_tor_isolation_desc),
                    icon = Icons.Outlined.VpnLock,
                    enabled = isMasterOn,
                    isChecked = isTorIsolationOn && isMasterOn,
                    onClick = {
                        if (isMasterOn) {
                            val nv = !isTorIsolationOn
                            isTorIsolationOn = nv
                            WebRtcManager.setTorIsolationEnabled(context, nv)
                        }
                    }
                )
            }
        }
    }
}




