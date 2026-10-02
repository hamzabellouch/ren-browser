package com.tkno.ren.ui.menu

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ContactSupport
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.UpdateDisabled
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsApplications
import com.tkno.ren.ui.sandbox.SandboxIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tkno.ren.R
import com.tkno.ren.ui.settings.AdBlockingSettingsPage
import com.tkno.ren.ui.settings.AdvancedSettingsPage
import com.tkno.ren.ui.settings.AppearancePreferences
import com.tkno.ren.ui.settings.AppearanceSettingsPage
import com.tkno.ren.ui.settings.BasePreferencePage
import com.tkno.ren.ui.settings.AntiFingerprintSettingsPage
import com.tkno.ren.ui.settings.WebRtcSettingsPage
import com.tkno.ren.ui.settings.CustomFiltersPage
import com.tkno.ren.ui.settings.DarkThemePreferences
import com.tkno.ren.ui.settings.FilterSubscriptionsPage
import com.tkno.ren.ui.settings.GeneralSettingsPage
import com.tkno.ren.ui.settings.NetworkIdentitySettingsPage
import com.tkno.ren.ui.settings.InterfaceInteractionSettingsPage
import com.tkno.ren.ui.settings.LanguagesPage
import com.tkno.ren.ui.settings.NewUserAgentPage
import com.tkno.ren.ui.settings.PrivacySettingsPage
import com.tkno.ren.ui.settings.RenSettingsDarkColorScheme
import com.tkno.ren.ui.settings.SandboxSettingsPage
import com.tkno.ren.ui.settings.ScriptsSettingsPage
import com.tkno.ren.ui.settings.TorSettingsPage
import com.tkno.ren.ui.settings.UserAgentSettingsPage
import com.tkno.ren.ui.settings.SiteConfigurationPage
import com.tkno.ren.ui.settings.SecureDnsSettingsPage
import com.tkno.ren.ui.settings.CookiesSettingsPage
import com.tkno.ren.ui.settings.SitePermissionsPage
import com.tkno.ren.ui.theme.RenTheme
import com.tkno.ren.ui.theme.ThemeManager

/* ---------------- روابط وروابط خارجية ---------------- */
private const val releaseURL = "https://github.com/hamzabellouch/links/releases"
private const val repoUrl = "https://github.com/hamzabellouch/links/blob/main/README.md"
private const val githubIssueUrl = "https://github.com/hamzabellouch/links/issues"
private const val matrixSpaceUrl = "https://sites.google.com/view/hamzabellouch"
private const val githubSponsor = "https://github.com/sponsors/hamzabellouch"
private const val privacyPolicyUrl = "https://github.com/hamzabellouch/links/blob/main/PRIVACY_POLICY.md"

val LocalFireDepartment: ImageVector = Icons.Outlined.LocalFireDepartment
val Policy: ImageVector = Icons.Outlined.Policy

object DynamicColorImageVectors {
    fun coder(): ImageVector = Icons.Outlined.DeveloperMode
}

public val Dashboard2: ImageVector
    get() {
        if (_dashboard2 != null) {
            return _dashboard2!!
        }
        _dashboard2 = ImageVector.Builder(
            name = "dashboard_2",
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
                moveTo(15f, 20f)
                verticalLineTo(13f)
                horizontalLineToRelative(7f)
                verticalLineToRelative(7f)
                horizontalLineTo(15f)
                close()
                moveTo(11f, 11f)
                verticalLineTo(4f)
                horizontalLineTo(22f)
                verticalLineToRelative(7f)
                horizontalLineTo(11f)
                close()
                moveTo(2f, 20f)
                verticalLineTo(13f)
                horizontalLineTo(13f)
                verticalLineToRelative(7f)
                horizontalLineTo(2f)
                close()
                moveTo(2f, 11f)
                verticalLineTo(4f)
                horizontalLineTo(9f)
                verticalLineToRelative(7f)
                horizontalLineTo(2f)
                close()
                moveTo(13f, 9f)
                horizontalLineToRelative(7f)
                verticalLineTo(6f)
                horizontalLineTo(13f)
                verticalLineTo(9f)
                close()
                moveTo(4f, 18f)
                horizontalLineToRelative(7f)
                verticalLineTo(15f)
                horizontalLineTo(4f)
                verticalLineToRelative(3f)
                close()
            }
        }.build()
        return _dashboard2!!
    }

private var _dashboard2: ImageVector? = null

public val StarIcon: ImageVector
    get() {
        if (_starIcon != null) {
            return _starIcon!!
        }
        _starIcon = ImageVector.Builder(
            name = "star",
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
                moveTo(8.85f, 16.83f)
                lineTo(12f, 14.93f)
                lineToRelative(3.15f, 1.93f)
                lineToRelative(-0.82f, -3.6f)
                lineToRelative(2.78f, -2.4f)
                lineTo(13.45f, 10.52f)
                lineTo(12f, 7.13f)
                lineTo(10.55f, 10.5f)
                lineTo(6.9f, 10.83f)
                lineToRelative(2.78f, 2.43f)
                lineTo(8.85f, 16.83f)
                close()
                moveTo(5.83f, 21f)
                lineTo(7.45f, 13.98f)
                lineTo(2f, 9.25f)
                lineTo(9.2f, 8.63f)
                lineTo(12f, 2f)
                lineToRelative(2.8f, 6.63f)
                lineTo(22f, 9.25f)
                lineToRelative(-5.45f, 4.72f)
                lineTo(18.18f, 21f)
                lineTo(12f, 17.27f)
                lineTo(5.83f, 21f)
                close()
                moveTo(12f, 12.25f)
                close()
            }
        }.build()
        return _starIcon!!
    }

private var _starIcon: ImageVector? = null

enum class MenuSubScreen {
    Main, 
    Settings, 
    Sponsor, 
    Troubleshooting, 
    About,
    Credits
}

@Composable
fun MainMenuList(
    onNavigateTo: (MenuSubScreen) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // شريط العنوان العلوي (Header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 12.dp)
                    .height(48.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.menu), // "Menu"
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            // عناصر وخيارات القائمة
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                // 1. خيار الإعدادات (Settings)
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.settings)) },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    onClick = { onNavigateTo(MenuSubScreen.Settings) },
                    selected = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                // 2. خيار الدعم (Sponsor)
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.sponsor)) },
                    icon = { Icon(Icons.Outlined.VolunteerActivism, contentDescription = null) },
                    onClick = { onNavigateTo(MenuSubScreen.Sponsor) },
                    selected = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                // 3. خيار حل المشاكل (Troubleshooting)
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.trouble_shooting)) },
                    icon = { Icon(Icons.Outlined.BugReport, contentDescription = null) },
                    onClick = { onNavigateTo(MenuSubScreen.Troubleshooting) },
                    selected = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                // 4. خيار حول التطبيق (About)
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.about)) },
                    icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    onClick = { onNavigateTo(MenuSubScreen.About) },
                    selected = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
            }

            Spacer(Modifier.height(84.dp))
        }
    }
}

/* ---------------- SettingsPage ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    onNavigateBack: () -> Unit,
    onNavigateTo: (String) -> Unit = {}
) {
    BasePreferencePage(
        title = stringResource(id = R.string.settings),
        onBack = onNavigateBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(), 
            contentPadding = padding
        ) {
            // 1. الإعدادات العامة (General Settings)
            item {
                SettingItem(
                    title = stringResource(id = R.string.general_settings),
                    description = stringResource(id = R.string.general_settings_desc),
                    icon = Icons.Rounded.SettingsApplications,
                ) {
                    onNavigateTo("general")
                }
            }

            // 2. شبكة وهوية (Network & Identity)
            item {
                SettingItem(
                    title = stringResource(id = R.string.network_identity),
                    description = stringResource(id = R.string.network_identity_desc),
                    icon = Icons.Outlined.Security,
                ) {
                    onNavigateTo("network_identity")
                }
            }

            // 3. بيئة العزل (Sandbox)
            item {
                SettingItem(
                    title = stringResource(id = R.string.sandbox),
                    description = stringResource(id = R.string.sandbox_desc),
                    icon = SandboxIcon,
                ) {
                    onNavigateTo("sandbox")
                }
            }

            // 4. السكربتات (Scripts)
            item {
                SettingItem(
                    title = stringResource(id = R.string.scripts_addons),
                    description = stringResource(id = R.string.scripts_addons_desc),
                    icon = Icons.Outlined.Code,
                ) {
                    onNavigateTo("scripts")
                }
            }

            // 5. المظهر والألوان (Look and feel)
            item {
                SettingItem(
                    title = stringResource(id = R.string.look_and_feel),
                    description = stringResource(id = R.string.display_settings),
                    icon = Icons.Rounded.Palette,
                ) {
                    onNavigateTo("appearance")
                }
            }

            // 6. واجهة المستخدم والتفاعل (Interface & Interaction)
            item {
                SettingItem(
                    title = stringResource(id = R.string.interface_interaction),
                    description = stringResource(id = R.string.interface_interaction_desc),
                    icon = Dashboard2,
                ) {
                    onNavigateTo("interface_interaction")
                }
            }
        }
    }
}

/* ---------------- SettingItem ---------------- */

@Composable
fun SettingItem(
    title: String,
    description: String,
    icon: ImageVector?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (icon == null) 12.dp else 0.dp)
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/* ---------------- فقاعة المحادثة للمطور (Conversation) ---------------- */

@Composable
fun Conversation(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/* ---------------- صفحة الدعم SponsorsPage ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorsPage(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true },
    )
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val typography = MaterialTheme.typography
            val overrideTypography = remember(typography) {
                typography.copy(headlineMedium = typography.displaySmall)
            }

            MaterialTheme(typography = overrideTypography) {
                LargeTopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.sponsors))
                    },
                    navigationIcon = { BackButton { onNavigateBack() } },
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(0.dp),
                )
            }
        },
        content = { values ->
            LazyVerticalGrid(
                modifier = Modifier.padding(horizontal = 12.dp),
                columns = GridCells.Fixed(12),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = values,
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = CardDefaults.shape,
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth()
                        ) {
                            // عنوان الرسالة العلوية
                            Text(
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .align(Alignment.CenterHorizontally),
                                text = stringResource(id = R.string.msg_from_developer),
                                style = MaterialTheme.typography.labelLarge,
                            )

                            // صف يحتوي على صورة المطور + فقاعات الرسائل
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.developer_avatar),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .aspectRatio(1f, true)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Conversation(
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        text = stringResource(id = R.string.sponsor_msg),
                                    )
                                    Conversation(
                                        modifier = Modifier,
                                        text = stringResource(id = R.string.sponsor_msg2),
                                    )
                                }
                            }

                            // زر الدعم (Sponsor Button)
                            Button(
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.sponsor_unavailable),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(ButtonDefaults.IconSize),
                                    imageVector = Icons.Outlined.VolunteerActivism,
                                    contentDescription = null,
                                )

                                Text(text = stringResource(id = R.string.sponsor))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // زر تقييم بنجمة على GitHub (Star Button)
                            Button(
                                onClick = {
                                    uriHandler.openUri("https://github.com/hamzabellouch/links")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107),
                                    contentColor = Color(0xFF212121),
                                ),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(ButtonDefaults.IconSize),
                                    imageVector = StarIcon,
                                    contentDescription = null,
                                )

                                Text(text = stringResource(id = R.string.star))
                            }
                        }
                    }
                }
            }
        },
    )
}

/* ---------------- TroubleShootingPage ---------------- */

@Composable
fun TroubleShootingPage(onNavigateBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    var showContactDialog by remember { mutableStateOf(false) }

    val isDark = ThemeManager.isDark(isSystemInDarkTheme())

    // تكييف ألوان البطاقات حسب المظهر (داكن / فاتح)
    val emailContainer = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val emailContent = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val whatsappContainer = if (isDark) Color(0xFF0A2B1D) else Color(0xFFE8F8F0)
    val whatsappContent = if (isDark) Color(0xFF25D366) else Color(0xFF128C7E)

    val facebookContainer = if (isDark) Color(0xFF0D2646) else Color(0xFFE7F3FF)
    val facebookContent = if (isDark) Color(0xFF4599FF) else Color(0xFF1877F2)

    val instagramContainer = if (isDark) Color(0xFF3D1625) else Color(0xFFFDF0F3)
    val instagramContent = if (isDark) Color(0xFFFF527B) else Color(0xFFD82E62)

    val linkedinContainer = if (isDark) Color(0xFF0E2E4E) else Color(0xFFE8F2FF)
    val linkedinContent = if (isDark) Color(0xFF55A4FC) else Color(0xFF0A66C2)

    val xContainer = if (isDark) Color(0xFF16181C) else Color(0xFFF5F8FA)
    val xContent = if (isDark) Color(0xFFE7E9EA) else Color(0xFF0F1419)

    val youtubeContainer = if (isDark) Color(0xFF3A1115) else Color(0xFFFFEBEE)
    val youtubeContent = if (isDark) Color(0xFFE53935) else Color(0xFFCC0000)

    val tiktokContainer = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF1F1F1)
    val tiktokContent = if (isDark) Color(0xFFFFFFFF) else Color(0xFF010101)

    val redditContainer = if (isDark) Color(0xFF3D1E16) else Color(0xFFFFEBE5)
    val redditContent = if (isDark) Color(0xFFFF5A1F) else Color(0xFFFF4500)

    val blueskyContainer = if (isDark) Color(0xFF0A2E4C) else Color(0xFFE8F8FF)
    val blueskyContent = if (isDark) Color(0xFF3BA1FF) else Color(0xFF0085FF)

    val telegramContainer = if (isDark) Color(0xFF0F2C3D) else Color(0xFFE8F5FA)
    val telegramContent = if (isDark) Color(0xFF52B6E9) else Color(0xFF24A1DE)

    BasePreferencePage(
        title = stringResource(R.string.trouble_shooting),
        onBack = onNavigateBack,
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            // قسم بطاقات التواصل والتواصل الاجتماعي (11 بطاقة أفقية عبر HorizontalPager)
            item {
                val pagerState = rememberPagerState(initialPage = 0) { 11 }
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                    ) { page ->
                        when (page) {
                            0 -> PreferencesHintCard(
                                title = stringResource(R.string.contact),
                                description = stringResource(R.string.contact_desc),
                                icon = Icons.Outlined.Email,
                                containerColor = emailContainer,
                                contentColor = emailContent,
                                textColor = Color.White,
                            ) { showContactDialog = true }

                            1 -> PreferencesHintCard(
                                title = stringResource(id = R.string.whatsapp),
                                icon = painterResource(id = R.drawable.ic_whatsapp),
                                description = stringResource(id = R.string.whatsapp_desc),
                                containerColor = whatsappContainer,
                                contentColor = whatsappContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://whatsapp.com/channel/0029Vb7MArw0LKZMpjjqOk2P") }

                            2 -> PreferencesHintCard(
                                title = stringResource(id = R.string.facebook),
                                icon = painterResource(id = R.drawable.ic_facebook),
                                description = stringResource(id = R.string.facebook_desc),
                                containerColor = facebookContainer,
                                contentColor = facebookContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.facebook.com/hamzabellouch0") }

                            3 -> PreferencesHintCard(
                                title = stringResource(id = R.string.instagram),
                                icon = painterResource(id = R.drawable.ic_instagram),
                                description = stringResource(id = R.string.instagram_desc),
                                containerColor = instagramContainer,
                                contentColor = instagramContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.instagram.com/hamzabellouch0") }

                            4 -> PreferencesHintCard(
                                title = stringResource(id = R.string.linkedin),
                                icon = painterResource(id = R.drawable.ic_linkedin),
                                description = stringResource(id = R.string.linkedin_desc),
                                containerColor = linkedinContainer,
                                contentColor = linkedinContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.linkedin.com/in/hamzabellouch") }

                            5 -> PreferencesHintCard(
                                title = stringResource(id = R.string.x_platform),
                                icon = painterResource(id = R.drawable.ic_x),
                                description = stringResource(id = R.string.x_desc),
                                containerColor = xContainer,
                                contentColor = xContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://x.com/hamzabellouch0") }

                            6 -> PreferencesHintCard(
                                title = stringResource(id = R.string.youtube),
                                icon = painterResource(id = R.drawable.ic_youtube),
                                description = stringResource(id = R.string.youtube_desc),
                                containerColor = youtubeContainer,
                                contentColor = youtubeContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.youtube.com/@hamzabellouch") }

                            7 -> PreferencesHintCard(
                                title = stringResource(id = R.string.tiktok),
                                icon = painterResource(id = R.drawable.ic_tiktok),
                                description = stringResource(id = R.string.tiktok_desc),
                                containerColor = tiktokContainer,
                                contentColor = tiktokContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.tiktok.com/@hamzabellouch0") }

                            8 -> PreferencesHintCard(
                                title = stringResource(id = R.string.reddit),
                                icon = painterResource(id = R.drawable.ic_reddit),
                                description = stringResource(id = R.string.reddit_desc),
                                containerColor = redditContainer,
                                contentColor = redditContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.reddit.com") }

                            9 -> PreferencesHintCard(
                                title = stringResource(id = R.string.bluesky),
                                icon = painterResource(id = R.drawable.ic_bluesky),
                                description = stringResource(id = R.string.bluesky_desc),
                                containerColor = blueskyContainer,
                                contentColor = blueskyContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://bsky.app/profile/hamzabellouch.bsky.social") }

                            10 -> PreferencesHintCard(
                                title = stringResource(id = R.string.telegram_channel),
                                icon = painterResource(id = R.drawable.icons8_telegram_app),
                                description = stringResource(id = R.string.telegram_channel_desc),
                                containerColor = telegramContainer,
                                contentColor = telegramContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://t.me/hamzabellouch") }
                        }
                    }

                    // مؤشرات الصفحات (Dots Indicator)
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(11) { pageIndex ->
                            val isSelected = pagerState.currentPage == pageIndex
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                }
            }

            // قسم تتبع المشاكل على GitHub (Issue Tracker)
            item {
                OutlinedCard(modifier = Modifier.padding(16.dp)) {
                    PreferenceInfo(
                        modifier = Modifier,
                        text = stringResource(R.string.issue_tracker_hint),
                    )
                    PreferenceItem(
                        title = stringResource(R.string.links_issue_tracker),
                        description = null,
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = { uriHandler.openUri("https://github.com/hamzabellouch/links/issues") },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // نافذة تأكيد إرسال بريد إلكتروني للمطور
    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            confirmButton = {
                FilledButtonWithIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    text = stringResource(id = R.string.proceed),
                    onClick = {
                        showContactDialog = false
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:hamzabellouchcontact@gmail.com")
                            setPackage("com.google.android.gm")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:hamzabellouchcontact@gmail.com")
                            }
                            try {
                                context.startActivity(Intent.createChooser(fallbackIntent, "Send Email"))
                            } catch (ex: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
            },
            dismissButton = {
                OutlinedButtonWithIcon(
                    icon = Icons.Outlined.Cancel,
                    text = stringResource(id = R.string.cancel),
                    onClick = { showContactDialog = false },
                )
            },
            title = { Text(text = stringResource(R.string.contact_developer)) },
            text = { Text(text = stringResource(R.string.contact_developer_confirm)) },
        )
    }
}

/* ---------------- AboutPage ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onNavigateBack: () -> Unit,
    onNavigateToCreditsPage: () -> Unit,
    onNavigateToUpdatePage: () -> Unit = {},
    onNavigateToDonatePage: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true },
    )
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val prefs = remember { 
        context.getSharedPreferences("links_prefs", Context.MODE_PRIVATE) 
    }
    var isAutoUpdateEnabled by remember {
        mutableStateOf(prefs.getBoolean("auto_update_enabled", true))
    }

    var showWhatsNewDialog by remember { mutableStateOf(false) }

    val whatsNewDismissedUntil = remember {
        prefs.getLong("whats_new_dismissed_until", 0L)
    }
    var isWhatsNewDismissed by remember {
        mutableStateOf(System.currentTimeMillis() < whatsNewDismissedUntil)
    }

    // فحص التحديثات التلقائية في الخلفية
    AppUpdater(isAutoUpdateEnabled = isAutoUpdateEnabled)

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.2-beta"
    } catch (e: Exception) {
        "0.0.2-beta"
    }
    val info = "App version: $versionName\nPackage name: ${context.packageName}\nDevice: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val uriHandler = LocalUriHandler.current

    fun openUrl(url: String) {
        uriHandler.openUri(url)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val typography = MaterialTheme.typography
            val overrideTypography = remember(typography) { 
                typography.copy(headlineMedium = typography.displaySmall) 
            }

            MaterialTheme(typography = overrideTypography) {
                LargeTopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.about))
                    },
                    navigationIcon = { BackButton { onNavigateBack() } },
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(0.dp),
                    actions = {
                        // كبسولة "What's New / ما الجديد" في أعلى الصفحة
                        if (!isWhatsNewDismissed) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clip(CircleShape)
                            ) {
                                Row(
                                    modifier = Modifier.height(36.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // الجزء الأيسر: الأيقونة والنص لفتح نافذة الجديد
                                    Row(
                                        modifier = Modifier
                                            .clickable(
                                                role = Role.Button,
                                                onClick = { showWhatsNewDialog = true }
                                            )
                                            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = LocalFireDepartment,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.whats_new),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    // الجزء الأيمن: زر 'X' لإخفاء الكبسولة لمدة 24 ساعة
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                role = Role.Button,
                                                onClick = {
                                                    val dismissUntil = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                                                    prefs.edit().putLong("whats_new_dismissed_until", dismissUntil).apply()
                                                    isWhatsNewDismissed = true
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.close),
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        },
        content = { innerPadding ->
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                // 1. README
                item {
                    PreferenceItem(
                        title = stringResource(R.string.readme),
                        description = stringResource(R.string.readme_desc),
                        icon = Icons.Outlined.Description,
                    ) {
                        openUrl(repoUrl)
                    }
                }
                // 2. Latest Release
                item {
                    PreferenceItem(
                        title = stringResource(R.string.release),
                        description = stringResource(R.string.release_desc),
                        icon = Icons.Outlined.NewReleases,
                    ) {
                        openUrl(releaseURL)
                    }
                }
                // 3. GitHub Issue
                item {
                    PreferenceItem(
                        title = stringResource(R.string.github_issue),
                        description = stringResource(R.string.github_issue_desc),
                        icon = Icons.AutoMirrored.Outlined.ContactSupport,
                    ) {
                        openUrl(githubIssueUrl)
                    }
                }
                // 4. GitHub Stars
                item {
                    PreferenceItem(
                        title = stringResource(R.string.github_stars),
                        description = stringResource(R.string.github_stars_desc),
                        icon = Icons.Outlined.StarBorder,
                    ) {
                        openUrl("https://github.com/hamzabellouch/links")
                    }
                }
                // 5. Website
                item {
                    PreferenceItem(
                        title = stringResource(R.string.website),
                        description = matrixSpaceUrl,
                        icon = Icons.Outlined.Language,
                    ) {
                        openUrl(matrixSpaceUrl)
                    }
                }
                // 6. Credits
                item {
                    PreferenceItem(
                        title = stringResource(id = R.string.credits),
                        description = stringResource(id = R.string.credits_desc),
                        icon = Icons.Outlined.AutoAwesome,
                    ) {
                        onNavigateToCreditsPage()
                    }
                }
                // 7. Auto Update Toggle
                item {
                    PreferenceSwitchWithDivider(
                        title = stringResource(R.string.auto_update),
                        description = stringResource(R.string.check_for_updates_desc),
                        icon = if (isAutoUpdateEnabled) Icons.Outlined.Update else Icons.Outlined.UpdateDisabled,
                        isChecked = isAutoUpdateEnabled,
                        isSwitchEnabled = true,
                        onClick = onNavigateToUpdatePage,
                        onChecked = {
                            isAutoUpdateEnabled = !isAutoUpdateEnabled
                            prefs.edit().putBoolean("auto_update_enabled", isAutoUpdateEnabled).apply()
                        },
                    )
                }
                // 8. Privacy Policy
                item {
                    PreferenceItem(
                        title = stringResource(R.string.privacy_policy),
                        description = stringResource(R.string.privacy_policy_desc),
                        icon = Policy,
                    ) {
                        openUrl(privacyPolicyUrl)
                    }
                }
                // 9. Version
                item {
                    PreferenceItem(
                        title = stringResource(R.string.version),
                        description = versionName,
                        icon = Icons.Outlined.Info,
                    ) {
                        clipboardManager.setText(AnnotatedString(info))
                        Toast.makeText(context, context.getString(R.string.info_copied), Toast.LENGTH_SHORT).show()
                    }
                }
                // 10. Package Name
                item {
                    PreferenceItem(
                        title = stringResource(R.string.package_name),
                        description = context.packageName,
                        icon = Icons.Outlined.Code,
                    ) {
                        clipboardManager.setText(AnnotatedString(context.packageName))
                        Toast.makeText(context, context.getString(R.string.info_copied), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    if (showWhatsNewDialog) {
        WhatsNewDialog(onDismissRequest = { showWhatsNewDialog = false })
    }
}

/* ---------------- CreditsPage ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsPage(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val creditsList = remember {
        listOf(
            Triple("Android Jetpack", "Apache License, Version 2.0", "https://github.com/androidx/androidx"),
            Triple("Kotlin", "Apache License, Version 2.0", "https://kotlinlang.org/"),
            Triple("Material Design 3", "Apache License, Version 2.0", "https://m3.material.io/"),
            Triple("Material Icons", "Apache License, Version 2.0", "https://fonts.google.com/icons"),
            Triple("Accompanist", "Apache License, Version 2.0", "https://github.com/google/accompanist"),
            Triple("ZXing", "Apache License, Version 2.0", "https://github.com/zxing/zxing"),
            Triple("App icon by Icons8", "Universal Multimedia Licensing Agreement for Icons8", "https://icons8.com/"),
        )
    }

    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.credits),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = { BackButton { onNavigateBack() } },
            )
        },
        content = { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .clip(MaterialTheme.shapes.large)
                            .clickable {}
                            .clearAndSetSemantics {},
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        val painter = rememberVectorPainter(image = DynamicColorImageVectors.coder())
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 72.dp, vertical = 48.dp),
                        )
                    }
                }
                items(creditsList) { item ->
                    CreditItem(title = item.first, license = item.second) {
                        uriHandler.openUri(item.third)
                    }
                }
            }
        },
    )
}

/* ---------------- المكونات المساعدة ---------------- */

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.back),
        )
    }
}

@Composable
fun PreferenceItem(
    title: String,
    description: String? = null,
    icon: Any? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (icon) {
                is ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp, end = 16.dp).size(24.dp),
                        tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (icon == null) 8.dp else 0.dp)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!description.isNullOrEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CreditItem(
    title: String,
    license: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Surface(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    text = title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                license?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun PreferenceSwitchWithDivider(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    isChecked: Boolean,
    isSwitchEnabled: Boolean = true,
    onClick: () -> Unit = {},
    onChecked: (Boolean) -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 16.dp)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (icon == null) 8.dp else 0.dp)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!description.isNullOrEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Switch(
                checked = isChecked,
                onCheckedChange = { onChecked(it) },
                enabled = isSwitchEnabled,
                modifier = Modifier.padding(start = 4.dp, end = 8.dp)
            )
        }
    }
}

@Composable
fun PreferencesHintCard(
    title: String = "",
    description: String? = null,
    icon: Any? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    textColor: Color = contentColor,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (icon) {
                is ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp, end = 16.dp).size(24.dp),
                        tint = contentColor,
                    )
                }
                is Painter -> {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp, end = 16.dp).size(24.dp),
                        tint = contentColor,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (icon == null) 12.dp else 0.dp, end = 12.dp)
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = textColor,
                )
                if (description != null) {
                    Text(
                        text = description,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun PreferenceInfo(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector = Icons.Outlined.Info,
    applyPaddings: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .run {
                if (applyPaddings) padding(horizontal = 16.dp, vertical = 16.dp) else this
            }
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun FilledButtonWithIcon(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        enabled = enabled,
    ) {
        Icon(modifier = Modifier.size(18.dp), imageVector = icon, contentDescription = null)
        Text(modifier = Modifier.padding(start = 6.dp), text = text)
    }
}

@Composable
fun OutlinedButtonWithIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = Color.Unspecified,
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
    ) {
        Icon(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            imageVector = icon,
            contentDescription = null,
        )
        Text(modifier = Modifier.padding(start = 8.dp), text = text, color = textColor)
    }
}

@Composable
fun AppUpdater(isAutoUpdateEnabled: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(isAutoUpdateEnabled) {
        if (isAutoUpdateEnabled) {
            // Check for updates
        }
    }
}

@Composable
fun WhatsNewDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.close))
            }
        },
        title = {
            Text(stringResource(R.string.whats_new))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Added new Menu screen with Material Design 3")
                Text("• Added About and Credits pages")
                Text("• Added Troubleshooting and Contact page")
                Text("• Added Sponsors page")
                Text("• Added Settings page")
            }
        }
    )
}

@Composable
fun MenuHost(
    onClose: () -> Unit,
    onNavigateSubScreen: ((MenuSubScreen) -> Unit)? = null,
    onClearData: () -> Unit = {},
    onUserAgentChanged: () -> Unit = {}
) {
    val screenStack = remember { mutableStateListOf("main") }
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

    RenTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                "main" -> {
                    MainMenuList(
                        onNavigateTo = { subScreen ->
                            when (subScreen) {
                                MenuSubScreen.Settings -> navigateTo("settings")
                                MenuSubScreen.About -> navigateTo("about")
                                MenuSubScreen.Troubleshooting -> navigateTo("troubleshooting")
                                MenuSubScreen.Sponsor -> navigateTo("sponsor")
                                else -> onNavigateSubScreen?.invoke(subScreen)
                            }
                        }
                    )
                }
                "settings" -> {
                    SettingsPage(
                        onNavigateBack = navigateBack,
                        onNavigateTo = navigateTo
                    )
                }
                "general" -> {
                    GeneralSettingsPage(
                        onBack = navigateBack,
                        onNavigateTo = navigateTo,
                        onClearData = onClearData
                    )
                }
                "network_identity" -> {
                    NetworkIdentitySettingsPage(
                        onBack = navigateBack,
                        onNavigateTo = navigateTo,
                        onClearData = onClearData
                    )
                }
                "anti_fingerprint" -> {
                    AntiFingerprintSettingsPage(
                        onBack = navigateBack
                    )
                }
                "webrtc" -> {
                    WebRtcSettingsPage(
                        onBack = navigateBack
                    )
                }
                "appearance" -> {
                    AppearancePreferences(
                        onNavigateBack = navigateBack,
                        onNavigateTo = navigateTo
                    )
                }
                "dark_theme" -> {
                    DarkThemePreferences(
                        onNavigateBack = navigateBack
                    )
                }
                "languages" -> {
                    LanguagesPage(
                        onNavigateBack = navigateBack
                    )
                }
                "interface_interaction" -> {
                    InterfaceInteractionSettingsPage(
                        onBack = navigateBack
                    )
                }
                "user_agent" -> {
                    UserAgentSettingsPage(
                        onBack = navigateBack,
                        onNavigateTo = navigateTo,
                        onUserAgentChanged = onUserAgentChanged
                    )
                }
                "new_user_agent" -> {
                    NewUserAgentPage(
                        onBack = navigateBack,
                        onUserAgentSaved = onUserAgentChanged
                    )
                }
                "ad_blocking" -> {
                    AdBlockingSettingsPage(
                        onBack = navigateBack,
                        onNavigateTo = navigateTo
                    )
                }
                "custom_filters" -> {
                    CustomFiltersPage(
                        onBack = navigateBack
                    )
                }
                "filter_subscriptions" -> {
                    FilterSubscriptionsPage(
                        onBack = navigateBack
                    )
                }
                "privacy" -> {
                    PrivacySettingsPage(
                        onBack = navigateBack,
                        onNavigateTo = navigateTo,
                        onClearData = onClearData
                    )
                }
                "tor" -> {
                    TorSettingsPage(
                        onBack = navigateBack
                    )
                }
                "advanced" -> {
                    AdvancedSettingsPage(
                        onBack = navigateBack
                    )
                }
                "site_config" -> {
                    SiteConfigurationPage(
                        onBack = navigateBack,
                        onNavigateTo = navigateTo,
                        onClearData = onClearData
                    )
                }
                "secure_dns" -> {
                    SecureDnsSettingsPage(
                        onBack = navigateBack
                    )
                }
                "cookies_settings" -> {
                    CookiesSettingsPage(
                        onBack = navigateBack
                    )
                }
                "site_permissions" -> {
                    SitePermissionsPage(
                        onBack = navigateBack
                    )
                }
                "sandbox" -> {
                    SandboxSettingsPage(
                        onBack = navigateBack
                    )
                }
                "scripts" -> {
                    ScriptsSettingsPage(
                        onBack = navigateBack
                    )
                }
                "sponsor" -> {
                    SponsorsPage(
                        onNavigateBack = navigateBack
                    )
                }
                "troubleshooting" -> {
                    TroubleShootingPage(
                        onNavigateBack = navigateBack
                    )
                }
                "about" -> {
                    AboutPage(
                        onNavigateBack = navigateBack,
                        onNavigateToCreditsPage = { navigateTo("credits") }
                    )
                }
                "credits" -> {
                    CreditsPage(
                        onNavigateBack = navigateBack
                    )
                }
                else -> {
                    MainMenuList(
                        onNavigateTo = { subScreen ->
                            when (subScreen) {
                                MenuSubScreen.Settings -> navigateTo("settings")
                                MenuSubScreen.About -> navigateTo("about")
                                MenuSubScreen.Troubleshooting -> navigateTo("troubleshooting")
                                MenuSubScreen.Sponsor -> navigateTo("sponsor")
                                else -> onNavigateSubScreen?.invoke(subScreen)
                            }
                        }
                    )
                }
            }
        }
    }
}
