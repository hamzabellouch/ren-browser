package com.tkno.ren.ui.menu

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkno.ren.R
import com.tkno.ren.ui.settings.BackButton
import com.tkno.ren.ui.settings.PreferenceSubtitle
import com.tkno.ren.ui.settings.PreferenceSwitchWithContainer
import com.tkno.ren.util.UpdateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePage(onNavigateBack: () -> Unit, triggerUpdate: Boolean = false) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("links_prefs", android.content.Context.MODE_PRIVATE) }

    var autoUpdate by remember { mutableStateOf(prefs.getBoolean("auto_update_enabled", false)) }
    var updateChannel by remember { mutableIntStateOf(prefs.getInt("update_channel", 0)) } // 0: STABLE, 1: PRE_RELEASE
    var bellEnabled by remember { mutableStateOf(prefs.getBoolean("update_bell_enabled", true)) }

    val defaultDownloadPath = remember {
        android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        ).absolutePath
    }

    var customDir by remember {
        mutableStateOf(prefs.getString("app_update_directory", defaultDownloadPath) ?: defaultDownloadPath)
    }

    val folderPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                }
                val docDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
                val folderName = docDir?.name ?: uri.lastPathSegment ?: uri.toString()
                val savedPath = uri.toString()

                prefs.edit()
                    .putString("app_update_directory_uri", savedPath)
                    .putString("app_update_directory", folderName)
                    .apply()

                customDir = folderName
                android.widget.Toast.makeText(context, "Update directory: $folderName", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { _ -> }

    val hasInstallPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    var autoInstallEnabled by remember {
        mutableStateOf(prefs.getBoolean("auto_install_apk", false) && hasInstallPermission)
    }

    val installPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else true
            autoInstallEnabled = isGranted
            prefs.edit().putBoolean("auto_install_apk", isGranted).apply()
            if (isGranted) {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.install_permission_granted),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

    var release by remember { mutableStateOf(UpdateUtil.Release()) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(triggerUpdate) {
        if (triggerUpdate && !isLoading) {
            scope.launch {
                isLoading = true
                runCatching {
                    val includePrerelease = updateChannel == 1
                    val foundRelease = withContext(Dispatchers.IO) {
                        UpdateUtil.checkForUpdate(context, includePrerelease = includePrerelease)
                    }
                    if (foundRelease != null) {
                        release = foundRelease
                        showUpdateDialog = true
                    } else {
                        android.widget.Toast.makeText(context, context.getString(R.string.app_up_to_date), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }.onFailure {
                    it.printStackTrace()
                    android.widget.Toast.makeText(context, context.getString(R.string.app_update_failed), android.widget.Toast.LENGTH_SHORT).show()
                }
                isLoading = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            var menuExpanded by remember { mutableStateOf(false) }
            LargeTopAppBar(
                title = {
                    Text(modifier = Modifier, text = stringResource(id = R.string.auto_update))
                },
                navigationIcon = { BackButton { onNavigateBack() } },
                actions = {
                    IconButton(
                        enabled = !autoUpdate,
                        onClick = {
                            val newBellState = !bellEnabled
                            bellEnabled = newBellState
                            prefs.edit().putBoolean("update_bell_enabled", newBellState).apply()
                            if (newBellState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (autoUpdate || bellEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                            contentDescription = "Toggle Update Notifications",
                            modifier = Modifier.alpha(if (autoUpdate) 0.38f else 1f),
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More actions",
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.InstallMobile,
                                        contentDescription = null,
                                    )
                                },
                                trailingIcon = {
                                    Checkbox(
                                        checked = autoInstallEnabled,
                                        onCheckedChange = null,
                                    )
                                },
                                text = { Text(stringResource(id = R.string.auto_install_update)) },
                                onClick = {
                                    if (!autoInstallEnabled) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            installPermissionLauncher.launch(intent)
                                        } else {
                                            autoInstallEnabled = true
                                            prefs.edit().putBoolean("auto_install_apk", true).apply()
                                        }
                                    } else {
                                        autoInstallEnabled = false
                                        prefs.edit().putBoolean("auto_install_apk", false).apply()
                                    }
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                    )
                                },
                                text = { Text(stringResource(id = R.string.update_directory)) },
                                onClick = {
                                    menuExpanded = false
                                    folderPickerLauncher.launch(null)
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { paddings ->
            LazyColumn(modifier = Modifier.padding(paddings)) {
                item {
                    PreferenceSwitchWithContainer(
                        title = stringResource(id = R.string.enable_auto_update),
                        icon = null,
                        isChecked = autoUpdate,
                    ) {
                        autoUpdate = !autoUpdate
                        prefs.edit().putBoolean("auto_update_enabled", autoUpdate).apply()
                    }
                }
                item {
                    PreferenceSubtitle(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(id = R.string.update_channel),
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Stable
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    updateChannel = 0
                                    prefs.edit().putInt("update_channel", 0).apply()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = updateChannel == 0,
                                onClick = {
                                    updateChannel = 0
                                    prefs.edit().putInt("update_channel", 0).apply()
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.stable),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(id = R.string.official_release),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Beta (early access)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    updateChannel = 1
                                    prefs.edit().putInt("update_channel", 1).apply()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = updateChannel == 1,
                                onClick = {
                                    updateChannel = 1
                                    prefs.edit().putInt("update_channel", 1).apply()
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.beta),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(id = R.string.early_access),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ProgressIndicatorButton(
                            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 6.dp).padding(bottom = 12.dp),
                            text = stringResource(id = R.string.check_for_updates),
                            icon = Icons.Outlined.Update,
                            isLoading = isLoading,
                        ) {
                            if (!isLoading) {
                                scope.launch {
                                    isLoading = true
                                    runCatching {
                                        val includePrerelease = updateChannel == 1
                                        val foundRelease = withContext(Dispatchers.IO) {
                                            UpdateUtil.checkForUpdate(context, includePrerelease = includePrerelease)
                                        }
                                        if (foundRelease != null) {
                                            release = foundRelease
                                            showUpdateDialog = true
                                        } else {
                                            android.widget.Toast.makeText(context, context.getString(R.string.app_up_to_date), android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure {
                                        it.printStackTrace()
                                        android.widget.Toast.makeText(context, context.getString(R.string.app_update_failed), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    isLoading = false
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
                item {
                    PreferenceInfo(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(id = R.string.pre_release_desc),
                    )
                }
            }
        },
    )

    if (showUpdateDialog) {
        UpdateDialog(onDismissRequest = { showUpdateDialog = false }, release = release)
    }
}

@Composable
fun ProgressIndicatorButton(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        if (isLoading) {
            Box(modifier = Modifier.size(18.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp).align(Alignment.Center),
                    strokeWidth = 2.dp,
                    strokeCap = StrokeCap.Round,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                )
            }
        } else {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(text = text, modifier = Modifier.padding(start = 8.dp))
    }
}
