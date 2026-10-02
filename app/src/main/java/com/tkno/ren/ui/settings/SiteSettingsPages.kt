package com.tkno.ren.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdUnits
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tkno.ren.R
import com.tkno.ren.util.SiteConfigManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteConfigurationPage(
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onClearData: () -> Unit
) {
    val context = LocalContext.current

    var isDntOn by remember { mutableStateOf(SiteConfigManager.isDoNotTrackEnabled(context)) }
    var isV8OptimizerOn by remember { mutableStateOf(SiteConfigManager.isV8OptimizerEnabled(context)) }
    var isJsOn by remember { mutableStateOf(SiteConfigManager.isJavaScriptEnabled(context)) }
    var isPopupsBlocked by remember { mutableStateOf(SiteConfigManager.isPopupsBlocked(context)) }
    var isSoundOn by remember { mutableStateOf(SiteConfigManager.isSoundEnabled(context)) }
    var isIntrusiveAdsBlocked by remember { mutableStateOf(SiteConfigManager.isIntrusiveAdsBlocked(context)) }
    var isProtectedContentOn by remember { mutableStateOf(SiteConfigManager.isProtectedContentEnabled(context)) }
    var isInsecureContentBlocked by remember { mutableStateOf(SiteConfigManager.isInsecureContentBlocked(context)) }

    val dnsSummary = remember { SiteConfigManager.getEffectiveDnsSummary(context) }
    val cookiesSummary = remember {
        when (SiteConfigManager.getCookiesMode(context)) {
            "allow_all" -> "Allow all cookies"
            "block_third_party" -> "Block third-party cookies (Recommended)"
            "block_all" -> "Block all cookies"
            else -> "Block third-party cookies"
        }
    }

    BasePreferencePage(
        title = "Site configuration",
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            // Section 1: Security & DNS
            item {
                PreferenceSubtitle(text = "Security & Network")
            }
            item {
                PreferenceItem(
                    title = "Use secure DNS",
                    description = dnsSummary,
                    icon = Icons.Outlined.Dns,
                    onClick = { onNavigateTo("secure_dns") }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Send a 'Do Not Track' request",
                    description = "Request websites and ad networks not to track your browsing (DNT & Sec-GPC)",
                    icon = Icons.Outlined.Shield,
                    isChecked = isDntOn,
                    onClick = {
                        val nv = !isDntOn
                        isDntOn = nv
                        SiteConfigManager.setDoNotTrackEnabled(context, nv)
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = "JavaScript V8 Optimization",
                    description = if (isV8OptimizerOn) {
                        "V8 JIT compilation enabled for maximum web performance"
                    } else {
                        "JITless anti-exploit mode active for hardened security"
                    },
                    icon = Icons.Outlined.Speed,
                    isChecked = isV8OptimizerOn,
                    onClick = {
                        val nv = !isV8OptimizerOn
                        isV8OptimizerOn = nv
                        SiteConfigManager.setV8OptimizerEnabled(context, nv)
                        Toast.makeText(
                            context,
                            if (nv) "V8 JIT Optimizer enabled" else "Hardened anti-exploit mode enabled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            // Section 2: Cookies & Data
            item {
                PreferenceSubtitle(text = "Cookies & Site Data")
            }
            item {
                PreferenceItem(
                    title = "Cookies",
                    description = cookiesSummary,
                    icon = Icons.Outlined.Cookie,
                    onClick = { onNavigateTo("cookies_settings") }
                )
            }
            item {
                PreferenceItem(
                    title = "Clear browsing data",
                    description = "Clear cache, cookies, history and local storage",
                    icon = Icons.Outlined.Delete,
                    onClick = onClearData
                )
            }

            // Section 3: Hardware & Device Permissions
            item {
                PreferenceSubtitle(text = "Permissions Manager")
            }
            item {
                PreferenceItem(
                    title = "Manage site permissions",
                    description = "Location, camera, microphone, motion, USB, NFC, Serial, VR/AR, Local network",
                    icon = Icons.Outlined.Sensors,
                    onClick = { onNavigateTo("site_permissions") }
                )
            }

            // Section 4: Content & Media Controls
            item {
                PreferenceSubtitle(text = "Content & Media")
            }
            item {
                PreferenceSwitch(
                    title = "JavaScript",
                    description = if (isJsOn) "Sites can run JavaScript" else "JavaScript is blocked for all sites",
                    icon = Icons.Outlined.Code,
                    isChecked = isJsOn,
                    onClick = {
                        val nv = !isJsOn
                        isJsOn = nv
                        SiteConfigManager.setJavaScriptEnabled(context, nv)
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Pop-ups and redirects",
                    description = if (isPopupsBlocked) "Blocked (Recommended)" else "Allowed",
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    isChecked = isPopupsBlocked,
                    onClick = {
                        val nv = !isPopupsBlocked
                        isPopupsBlocked = nv
                        SiteConfigManager.setPopupsBlocked(context, nv)
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Sound & Media Autoplay",
                    description = if (isSoundOn) "Allow sites to play sound and media" else "Mute sound and require gesture to play",
                    icon = Icons.Outlined.Audiotrack,
                    isChecked = isSoundOn,
                    onClick = {
                        val nv = !isSoundOn
                        isSoundOn = nv
                        SiteConfigManager.setSoundEnabled(context, nv)
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Intrusive ads",
                    description = if (isIntrusiveAdsBlocked) "Block ads on sites that show intrusive or misleading ads" else "Allowed",
                    icon = Icons.Outlined.AdUnits,
                    isChecked = isIntrusiveAdsBlocked,
                    onClick = {
                        val nv = !isIntrusiveAdsBlocked
                        isIntrusiveAdsBlocked = nv
                        SiteConfigManager.setIntrusiveAdsBlocked(context, nv)
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Protected content (DRM)",
                    description = if (isProtectedContentOn) "Allow sites to play protected content (music, movies)" else "Blocked",
                    icon = Icons.Outlined.VpnKey,
                    isChecked = isProtectedContentOn,
                    onClick = {
                        val nv = !isProtectedContentOn
                        isProtectedContentOn = nv
                        SiteConfigManager.setProtectedContentEnabled(context, nv)
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Insecure / Embedded content",
                    description = if (isInsecureContentBlocked) "Block insecure HTTP elements on HTTPS pages (Recommended)" else "Allow mixed content",
                    icon = Icons.Outlined.WarningAmber,
                    isChecked = isInsecureContentBlocked,
                    onClick = {
                        val nv = !isInsecureContentBlocked
                        isInsecureContentBlocked = nv
                        SiteConfigManager.setInsecureContentBlocked(context, nv)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureDnsSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current

    var isSecureDnsOn by remember { mutableStateOf(SiteConfigManager.isSecureDnsEnabled(context)) }
    var selectedMode by remember { mutableStateOf(SiteConfigManager.getSecureDnsMode(context)) }
    var selectedProviderName by remember { mutableStateOf(SiteConfigManager.getSecureDnsProviderName(context)) }
    var customUrl by remember { mutableStateOf(SiteConfigManager.getCustomDnsUrl(context)) }
    var showCustomDialog by remember { mutableStateOf(false) }

    BasePreferencePage(
        title = "Secure DNS",
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                PreferenceSwitch(
                    title = "Use secure DNS",
                    description = "Determines how to look up the IP addresses for sites using DNS over HTTPS (DoH)",
                    icon = Icons.Outlined.Dns,
                    isChecked = isSecureDnsOn,
                    onClick = {
                        val nv = !isSecureDnsOn
                        isSecureDnsOn = nv
                        SiteConfigManager.setSecureDnsEnabled(context, nv)
                    }
                )
            }

            if (isSecureDnsOn) {
                item {
                    PreferenceSubtitle(text = "DNS Resolution Mode")
                }

                // Option 1: Use current service provider (System)
                item {
                    PreferenceRadio(
                        title = "Use current service provider",
                        description = "Use your network's default DNS provider with encryption when available",
                        isSelected = selectedMode == "system",
                        onClick = {
                            selectedMode = "system"
                            SiteConfigManager.setSecureDnsMode(context, "system")
                        }
                    )
                }

                // Option 2: Choose provider
                item {
                    PreferenceRadio(
                        title = "Choose another provider",
                        description = "Select a trusted, high-performance secure DNS provider",
                        isSelected = selectedMode == "provider",
                        onClick = {
                            selectedMode = "provider"
                            SiteConfigManager.setSecureDnsMode(context, "provider")
                        }
                    )
                }

                if (selectedMode == "provider") {
                    item {
                        PreferenceSubtitle(text = "Available DNS Providers")
                    }
                    items(SiteConfigManager.POPULAR_DNS_PROVIDERS) { provider ->
                        PreferenceRadio(
                            title = provider.name,
                            description = "${provider.description}\nDoH: ${provider.dohUrl}",
                            isSelected = provider.name == selectedProviderName,
                            onClick = {
                                selectedProviderName = provider.name
                                SiteConfigManager.setSecureDnsProviderName(context, provider.name)
                                Toast.makeText(context, "Selected: ${provider.name}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // Option 3: Custom provider
                item {
                    PreferenceSubtitle(text = "Custom Provider")
                }
                item {
                    val customDesc = if (customUrl.isNotBlank() && customUrl != "https://") {
                        customUrl
                    } else {
                        "Enter custom DoH URL (e.g. https://example.com/dns-query)"
                    }
                    PreferenceRadio(
                        title = "Custom",
                        description = customDesc,
                        isSelected = selectedMode == "custom",
                        onClick = {
                            selectedMode = "custom"
                            SiteConfigManager.setSecureDnsMode(context, "custom")
                            showCustomDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showCustomDialog) {
        var tempUrl by remember { mutableStateOf(customUrl) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Secure DNS URL") },
            text = {
                Column {
                    Text(
                        text = "Enter a valid DNS-over-HTTPS (DoH) endpoint URL:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("DoH URL or IP") },
                        placeholder = { Text("https://dns.example.com/dns-query") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalUrl = tempUrl.trim()
                        if (finalUrl.isNotBlank() && (finalUrl.startsWith("https://") || finalUrl.startsWith("http://"))) {
                            customUrl = finalUrl
                            SiteConfigManager.setCustomDnsUrl(context, finalUrl)
                            showCustomDialog = false
                            Toast.makeText(context, "Custom DNS saved", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a valid DoH URL (https://...)", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookiesSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var cookiesMode by remember { mutableStateOf(SiteConfigManager.getCookiesMode(context)) }

    BasePreferencePage(
        title = "Cookies",
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                PreferenceSubtitle(text = "General Settings")
            }
            item {
                PreferenceRadio(
                    title = "Allow third-party cookies",
                    description = "Sites can use cookies to remember your preferences and keep you signed in across sites.",
                    isSelected = cookiesMode == "allow_all",
                    onClick = {
                        cookiesMode = "allow_all"
                        SiteConfigManager.setCookiesMode(context, "allow_all")
                    }
                )
            }
            item {
                PreferenceRadio(
                    title = "Block third-party cookies (Recommended)",
                    description = "Sites can't use third-party cookies to track you across the web. Features on some sites may break.",
                    isSelected = cookiesMode == "block_third_party",
                    onClick = {
                        cookiesMode = "block_third_party"
                        SiteConfigManager.setCookiesMode(context, "block_third_party")
                    }
                )
            }
            item {
                PreferenceRadio(
                    title = "Block all cookies (Not recommended)",
                    description = "Websites will not save any cookies. You won't stay signed in and shopping carts won't work.",
                    isSelected = cookiesMode == "block_all",
                    onClick = {
                        cookiesMode = "block_all"
                        SiteConfigManager.setCookiesMode(context, "block_all")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitePermissionsPage(onBack: () -> Unit) {
    val context = LocalContext.current

    var locationPerm by remember { mutableStateOf(SiteConfigManager.getLocationPermission(context)) }
    var cameraPerm by remember { mutableStateOf(SiteConfigManager.getCameraPermission(context)) }
    var micPerm by remember { mutableStateOf(SiteConfigManager.getMicrophonePermission(context)) }
    var motionSensorsOn by remember { mutableStateOf(SiteConfigManager.isMotionSensorsEnabled(context)) }
    var nfcPerm by remember { mutableStateOf(SiteConfigManager.getNfcPermission(context)) }
    var usbPerm by remember { mutableStateOf(SiteConfigManager.getUsbPermission(context)) }
    var serialPerm by remember { mutableStateOf(SiteConfigManager.getSerialPermission(context)) }
    var fileEditingPerm by remember { mutableStateOf(SiteConfigManager.getFileEditingPermission(context)) }
    var keyboardLockAllowed by remember { mutableStateOf(SiteConfigManager.isKeyboardLockAllowed(context)) }
    var vrPerm by remember { mutableStateOf(SiteConfigManager.getVrPermission(context)) }
    var arPerm by remember { mutableStateOf(SiteConfigManager.getArPermission(context)) }
    var localNetworkAllowed by remember { mutableStateOf(SiteConfigManager.isLocalNetworkAccessAllowed(context)) }
    var autoDownloadsPerm by remember { mutableStateOf(SiteConfigManager.getAutomaticDownloadsPermission(context)) }

    var activeDialogPermission by remember { mutableStateOf<String?>(null) }

    BasePreferencePage(
        title = "Permissions Manager",
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                PreferenceSubtitle(text = "Core Hardware & Sensors")
            }
            item {
                PreferenceItem(
                    title = "Location",
                    description = getPermissionDisplaySubtitle(locationPerm, "Ask before accessing location", "Location allowed", "Location blocked"),
                    icon = Icons.Outlined.LocationOn,
                    onClick = { activeDialogPermission = "location" }
                )
            }
            item {
                PreferenceItem(
                    title = "Camera",
                    description = getPermissionDisplaySubtitle(cameraPerm, "Ask before accessing camera", "Camera allowed", "Camera blocked"),
                    icon = Icons.Outlined.CameraAlt,
                    onClick = { activeDialogPermission = "camera" }
                )
            }
            item {
                PreferenceItem(
                    title = "Microphone",
                    description = getPermissionDisplaySubtitle(micPerm, "Ask before accessing microphone", "Microphone allowed", "Microphone blocked"),
                    icon = Icons.Outlined.Mic,
                    onClick = { activeDialogPermission = "mic" }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Motion sensors",
                    description = if (motionSensorsOn) "Allow sites to use motion and orientation sensors" else "Blocked for all sites",
                    icon = Icons.Outlined.MotionPhotosOn,
                    isChecked = motionSensorsOn,
                    onClick = {
                        val nv = !motionSensorsOn
                        motionSensorsOn = nv
                        SiteConfigManager.setMotionSensorsEnabled(context, nv)
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Peripherals & Connectivity")
            }
            item {
                PreferenceItem(
                    title = "NFC devices (WebNFC)",
                    description = getPermissionDisplaySubtitle(nfcPerm, "Ask before connecting to NFC devices", "NFC allowed", "NFC blocked"),
                    icon = Icons.Outlined.Nfc,
                    onClick = { activeDialogPermission = "nfc" }
                )
            }
            item {
                PreferenceItem(
                    title = "USB devices (WebUSB)",
                    description = getPermissionDisplaySubtitle(usbPerm, "Ask before connecting to USB devices", "USB allowed", "USB blocked"),
                    icon = Icons.Outlined.Usb,
                    onClick = { activeDialogPermission = "usb" }
                )
            }
            item {
                PreferenceItem(
                    title = "Serial ports (WebSerial)",
                    description = getPermissionDisplaySubtitle(serialPerm, "Ask before connecting to serial ports", "Serial allowed", "Serial blocked"),
                    icon = Icons.Outlined.Devices,
                    onClick = { activeDialogPermission = "serial" }
                )
            }
            item {
                PreferenceItem(
                    title = "File editing & File System",
                    description = getPermissionDisplaySubtitle(fileEditingPerm, "Ask before editing original files on device", "File editing allowed", "File editing blocked"),
                    icon = Icons.Outlined.EditNote,
                    onClick = { activeDialogPermission = "file_editing" }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Keyboard lock",
                    description = if (keyboardLockAllowed) "Sites can capture full keyboard inputs and shortcuts" else "Blocked (Protects navigation shortcuts)",
                    icon = Icons.Outlined.Keyboard,
                    isChecked = keyboardLockAllowed,
                    onClick = {
                        val nv = !keyboardLockAllowed
                        keyboardLockAllowed = nv
                        SiteConfigManager.setKeyboardLockAllowed(context, nv)
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Extended Reality & Local Network")
            }
            item {
                PreferenceItem(
                    title = "Virtual Reality (VR / WebXR)",
                    description = getPermissionDisplaySubtitle(vrPerm, "Ask before entering immersive VR session", "VR allowed", "VR blocked"),
                    icon = Icons.Outlined.ViewInAr,
                    onClick = { activeDialogPermission = "vr" }
                )
            }
            item {
                PreferenceItem(
                    title = "Augmented Reality (AR / WebXR)",
                    description = getPermissionDisplaySubtitle(arPerm, "Ask before entering AR session", "AR allowed", "AR blocked"),
                    icon = Icons.Outlined.ViewInAr,
                    onClick = { activeDialogPermission = "ar" }
                )
            }
            item {
                PreferenceSwitch(
                    title = "Local network & apps on device",
                    description = if (localNetworkAllowed) "Allow sites to communicate with local network (LAN / 127.0.0.1)" else "Block requests to local network to protect intranet security",
                    icon = Icons.Outlined.Lan,
                    isChecked = localNetworkAllowed,
                    onClick = {
                        val nv = !localNetworkAllowed
                        localNetworkAllowed = nv
                        SiteConfigManager.setLocalNetworkAccessAllowed(context, nv)
                    }
                )
            }
            item {
                PreferenceItem(
                    title = "Automatic downloads",
                    description = getPermissionDisplaySubtitle(autoDownloadsPerm, "Ask before downloading multiple files", "Automatic downloads allowed", "Automatic downloads blocked"),
                    icon = Icons.Outlined.Download,
                    onClick = { activeDialogPermission = "auto_downloads" }
                )
            }
        }
    }

    if (activeDialogPermission != null) {
        val permKey = activeDialogPermission!!
        val title = when (permKey) {
            "location" -> "Location permission"
            "camera" -> "Camera permission"
            "mic" -> "Microphone permission"
            "nfc" -> "NFC permission"
            "usb" -> "USB devices permission"
            "serial" -> "Serial ports permission"
            "file_editing" -> "File editing permission"
            "vr" -> "Virtual Reality permission"
            "ar" -> "Augmented Reality permission"
            "auto_downloads" -> "Automatic downloads"
            else -> "Permission"
        }
        val currentVal = when (permKey) {
            "location" -> locationPerm
            "camera" -> cameraPerm
            "mic" -> micPerm
            "nfc" -> nfcPerm
            "usb" -> usbPerm
            "serial" -> serialPerm
            "file_editing" -> fileEditingPerm
            "vr" -> vrPerm
            "ar" -> arPerm
            "auto_downloads" -> autoDownloadsPerm
            else -> "ask"
        }

        PermissionChooserDialog(
            title = title,
            currentValue = currentVal,
            onDismiss = { activeDialogPermission = null },
            onSelect = { selected ->
                when (permKey) {
                    "location" -> {
                        locationPerm = selected
                        SiteConfigManager.setLocationPermission(context, selected)
                    }
                    "camera" -> {
                        cameraPerm = selected
                        SiteConfigManager.setCameraPermission(context, selected)
                    }
                    "mic" -> {
                        micPerm = selected
                        SiteConfigManager.setMicrophonePermission(context, selected)
                    }
                    "nfc" -> {
                        nfcPerm = selected
                        SiteConfigManager.setNfcPermission(context, selected)
                    }
                    "usb" -> {
                        usbPerm = selected
                        SiteConfigManager.setUsbPermission(context, selected)
                    }
                    "serial" -> {
                        serialPerm = selected
                        SiteConfigManager.setSerialPermission(context, selected)
                    }
                    "file_editing" -> {
                        fileEditingPerm = selected
                        SiteConfigManager.setFileEditingPermission(context, selected)
                    }
                    "vr" -> {
                        vrPerm = selected
                        SiteConfigManager.setVrPermission(context, selected)
                    }
                    "ar" -> {
                        arPerm = selected
                        SiteConfigManager.setArPermission(context, selected)
                    }
                    "auto_downloads" -> {
                        autoDownloadsPerm = selected
                        SiteConfigManager.setAutomaticDownloadsPermission(context, selected)
                    }
                }
                activeDialogPermission = null
            }
        )
    }
}

private fun getPermissionDisplaySubtitle(value: String, askText: String, allowText: String, blockText: String): String {
    return when (value) {
        "allow" -> "Allowed ($allowText)"
        "block" -> "Blocked ($blockText)"
        else -> "Ask first ($askText)"
    }
}

@Composable
fun PermissionChooserDialog(
    title: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "ask" to "Ask first (Recommended)",
        "allow" to "Allow all sites",
        "block" to "Block all sites"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { (key, label) ->
                    PreferenceRadio(
                        title = label,
                        isSelected = currentValue == key,
                        onClick = { onSelect(key) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
