package com.tkno.ren.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

object ThemeManager {
    private const val PREFS_NAME = "links_prefs"
    const val KEY_DARK_THEME = "dark_theme"
    const val KEY_DYNAMIC_COLOR = "dynamic_color"

    // 0: System, 1: Dark, 2: Light
    var darkThemeMode by mutableIntStateOf(0)
        private set

    var dynamicColor by mutableStateOf(true)
        private set

    var isIncognitoActive by mutableStateOf(false)

    fun setIncognito(active: Boolean) {
        isIncognitoActive = active
    }

    private var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var isInitialized = false

    fun init(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        darkThemeMode = prefs.getInt(KEY_DARK_THEME, 0)
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)

        applyNightMode(darkThemeMode)

        if (!isInitialized) {
            prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
                when (key) {
                    KEY_DARK_THEME -> {
                        val mode = sp.getInt(KEY_DARK_THEME, 0)
                        if (darkThemeMode != mode) {
                            darkThemeMode = mode
                            applyNightMode(mode)
                        }
                    }
                    KEY_DYNAMIC_COLOR -> {
                        dynamicColor = sp.getBoolean(KEY_DYNAMIC_COLOR, true)
                    }
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(prefsListener)
            isInitialized = true
        }
    }

    fun setDarkThemeMode(context: Context, mode: Int) {
        darkThemeMode = mode
        applyNightMode(mode)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DARK_THEME, mode).apply()
    }

    fun setDynamicColor(context: Context, enabled: Boolean) {
        dynamicColor = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun isDark(systemInDark: Boolean): Boolean {
        if (isIncognitoActive) return true
        return when (darkThemeMode) {
            1 -> true
            2 -> false
            else -> systemInDark
        }
    }

    fun getColorScheme(context: Context): ColorScheme {
        if (isIncognitoActive) {
            return IncognitoDarkColorScheme
        }
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val darkThemePref = prefs.getInt(KEY_DARK_THEME, 0)
        val isDynamic = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val isDark = when (darkThemePref) {
            1 -> true
            2 -> false
            else -> isSystemDark
        }
        return when {
            isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (isDark) dynamicDarkColorScheme(appContext)
                else dynamicLightColorScheme(appContext)
            }
            isDark -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    private fun applyNightMode(mode: Int) {
        val nightMode = when (mode) {
            1 -> AppCompatDelegate.MODE_NIGHT_YES
            2 -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
}

val IncognitoDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF040707),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFFA8C7FA),
    onSecondary = Color(0xFF040707),
    secondaryContainer = Color(0xFF2B2D30),
    onSecondaryContainer = Color(0xFFE3E3E3),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFC4C7C5),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerLow = Color(0xFF0D0D0D),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF282828),
    outline = Color(0xFF8E918F),
    outlineVariant = Color(0xFF3C4043),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0C100D),
    primaryContainer = Color(0xFF1B263B),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFF8AB4F8),
    onSecondary = Color(0xFF0C100D),
    secondaryContainer = Color(0xFF203248),
    onSecondaryContainer = Color(0xFFD2E3FC),
    background = Color(0xFF0C100D),
    onBackground = Color(0xFFE2E3E0),
    surface = Color(0xFF0C100D),
    onSurface = Color(0xFFE2E3E0),
    surfaceVariant = Color(0xFF1E2621),
    onSurfaceVariant = Color(0xFFC2C9C2),
    surfaceContainer = Color(0xFF131815),
    surfaceContainerLow = Color(0xFF181F1B),
    surfaceContainerHigh = Color(0xFF1D2520),
    surfaceContainerHighest = Color(0xFF232B25),
    outline = Color(0xFF8C938C),
    outlineVariant = Color(0xFF202A36),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF690005)
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF1A73E8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F0FE),
    onSecondaryContainer = Color(0xFF001D36),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E3E1),
    onSurfaceVariant = Color(0xFF444746),
    surfaceContainer = Color(0xFFF1F4F1),
    surfaceContainerLow = Color(0xFFF6F9F6),
    surfaceContainerHigh = Color(0xFFEBEFEB),
    surfaceContainerHighest = Color(0xFFE3E8E3),
    outline = Color(0xFF747775),
    outlineVariant = Color(0xFFC4C7C5),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun RenTheme(
    content: @Composable () -> Unit
) {
    val incognito = ThemeManager.isIncognitoActive
    RenTheme(
        darkTheme = if (incognito) true else ThemeManager.isDark(isSystemInDarkTheme()),
        dynamicColor = if (incognito) false else ThemeManager.dynamicColor,
        isIncognito = incognito,
        content = content
    )
}

@Composable
fun RenTheme(
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val incognito = ThemeManager.isIncognitoActive
    RenTheme(
        darkTheme = if (incognito) true else ThemeManager.isDark(isSystemInDarkTheme()),
        dynamicColor = if (incognito) false else dynamicColor,
        isIncognito = incognito,
        content = content
    )
}

@Composable
fun RenTheme(
    darkTheme: Boolean = if (ThemeManager.isIncognitoActive) true else ThemeManager.isDark(isSystemInDarkTheme()),
    dynamicColor: Boolean = if (ThemeManager.isIncognitoActive) false else ThemeManager.dynamicColor,
    isIncognito: Boolean = ThemeManager.isIncognitoActive,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = when {
        isIncognito -> IncognitoDarkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(window, view).apply {
                    val isEffectiveDark = isIncognito || darkTheme
                    isAppearanceLightStatusBars = !isEffectiveDark
                    isAppearanceLightNavigationBars = !isEffectiveDark
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun LinksTheme(
    content: @Composable () -> Unit
) = RenTheme(content = content)

@Composable
fun LinksTheme(
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) = RenTheme(dynamicColor = dynamicColor, content = content)

@Composable
fun LinksTheme(
    darkTheme: Boolean = if (ThemeManager.isIncognitoActive) true else ThemeManager.isDark(isSystemInDarkTheme()),
    dynamicColor: Boolean = if (ThemeManager.isIncognitoActive) false else ThemeManager.dynamicColor,
    content: @Composable () -> Unit,
) = RenTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

