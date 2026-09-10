package anhiutangerine.prettiembee.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemeMode(val title: String) {
    LIGHT("Trắng Material"),
    DARK("Giao diện tối"),
    SYSTEM("Theo hệ thống")
}

enum class AppThemeAccent(
    val title: String,
    val previewColor: Color,
    val lightPrimary: Color,
    val darkPrimary: Color,
    val secondary: Color
) {
    SAKURA(
        title = "Hồng Sakura",
        previewColor = SakuraPink,
        lightPrimary = SakuraPinkDark,
        darkPrimary = SakuraPink,
        secondary = SakuraAccent
    ),
    OCEAN(
        title = "Xanh Biển",
        previewColor = OceanBlueLight,
        lightPrimary = OceanBlue,
        darkPrimary = OceanBlueLight,
        secondary = Color(0xFF42A5F5)
    ),
    MINT(
        title = "Xanh Bạc Hà",
        previewColor = MintGreenLight,
        lightPrimary = MintGreen,
        darkPrimary = MintGreenLight,
        secondary = Color(0xFF66BB6A)
    ),
    LAVENDER(
        title = "Tím Oải Hương",
        previewColor = LavenderPurpleLight,
        lightPrimary = LavenderPurple,
        darkPrimary = LavenderPurpleLight,
        secondary = Color(0xFFAB47BC)
    ),
    AMBER(
        title = "Cam Mật Ong",
        previewColor = AmberOrangeLight,
        lightPrimary = AmberOrange,
        darkPrimary = AmberOrangeLight,
        secondary = Color(0xFFFFA726)
    )
}

object ThemeConfig {
    private const val PREFS_NAME = "prettiembee_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_THEME_ACCENT = "theme_accent"

    var themeMode by mutableStateOf(AppThemeMode.LIGHT)
    var themeAccent by mutableStateOf(AppThemeAccent.SAKURA)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_THEME_MODE, AppThemeMode.LIGHT.name)
        val accentStr = prefs.getString(KEY_THEME_ACCENT, AppThemeAccent.SAKURA.name)
        themeMode = try {
            AppThemeMode.valueOf(modeStr ?: AppThemeMode.LIGHT.name)
        } catch (_: Exception) {
            AppThemeMode.LIGHT
        }
        themeAccent = try {
            AppThemeAccent.valueOf(accentStr ?: AppThemeAccent.SAKURA.name)
        } catch (_: Exception) {
            AppThemeAccent.SAKURA
        }
    }

    fun saveThemeMode(context: Context, mode: AppThemeMode) {
        themeMode = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    fun saveThemeAccent(context: Context, accent: AppThemeAccent) {
        themeAccent = accent
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_THEME_ACCENT, accent.name)
            .apply()
    }
}

@Composable
fun PrettieMBeeTheme(
    content: @Composable () -> Unit
) {
    val isDark = when (ThemeConfig.themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val accent = ThemeConfig.themeAccent

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accent.darkPrimary,
            onPrimary = Color(0xFF1E060D),
            primaryContainer = accent.darkPrimary.copy(alpha = 0.22f),
            onPrimaryContainer = Color.White,
            secondary = accent.secondary,
            onSecondary = Color(0xFF1E060D),
            secondaryContainer = DarkSurfaceContainerHigh,
            onSecondaryContainer = DarkOnSurface,
            tertiary = accent.previewColor,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            surfaceContainerLowest = DarkSurfaceContainerLowest,
            surfaceContainerLow = DarkSurfaceContainerLow,
            surfaceContainer = DarkSurfaceContainer,
            surfaceContainerHigh = DarkSurfaceContainerHigh,
            surfaceContainerHighest = DarkSurfaceContainerHighest,
            outline = DarkOutline,
            outlineVariant = DarkOutlineVariant,
            onBackground = DarkOnSurface,
            onSurface = DarkOnSurface,
            onSurfaceVariant = DarkOnSurfaceVariant
        )
    } else {
        lightColorScheme(
            primary = accent.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = accent.previewColor.copy(alpha = 0.25f),
            onPrimaryContainer = Color(0xFF1E060D),
            secondary = accent.secondary,
            onSecondary = Color.White,
            secondaryContainer = LightSurfaceContainerHigh,
            onSecondaryContainer = LightOnSurface,
            tertiary = accent.previewColor,
            background = LightBackground,
            surface = LightSurface,
            surfaceVariant = LightSurfaceVariant,
            surfaceContainerLowest = LightSurfaceContainerLowest,
            surfaceContainerLow = LightSurfaceContainerLow,
            surfaceContainer = LightSurfaceContainer,
            surfaceContainerHigh = LightSurfaceContainerHigh,
            surfaceContainerHighest = LightSurfaceContainerHighest,
            outline = LightOutline,
            outlineVariant = LightOutlineVariant,
            onBackground = LightOnSurface,
            onSurface = LightOnSurface,
            onSurfaceVariant = LightOnSurfaceVariant
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
