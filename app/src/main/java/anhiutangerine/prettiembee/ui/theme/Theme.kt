package anhiutangerine.prettiembee.ui.theme

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream

enum class AppThemeMode(val title: String) {
    LIGHT("Trắng Material"),
    MATERIAL_DARK("Đen Material"),
    OLED_DARK("Đen OLED")
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

data class CustomAccentColors(
    val previewColor: Color,
    val lightPrimary: Color,
    val darkPrimary: Color,
    val secondary: Color
)

object ThemeConfig {
    private const val PREFS_NAME = "prettiembee_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_THEME_ACCENT = "theme_accent"
    private const val KEY_STATUS_BG_URI = "status_bg_uri"
    private const val KEY_APP_BG_URI = "app_bg_uri"
    private const val KEY_BG_DIM = "bg_dim"
    private const val KEY_CARD_ALPHA = "card_alpha"
    private const val KEY_USE_BG_SEED_COLOR = "use_bg_seed_color"
    private const val KEY_EXTRACTED_SEED_COLOR = "extracted_seed_color"
    private const val KEY_APP_DPI = "app_dpi"

    // Applied Theme tracking
    private const val KEY_APPLIED_NEW_THEME = "applied_new_theme"
    private const val KEY_APPLIED_ORIGINAL_THEME = "applied_original_theme"
    private const val KEY_APPLIED_IS_PRIORITY = "applied_is_priority"
    private const val KEY_PINNED_THEMES = "pinned_theme_ids"

    var themeMode by mutableStateOf(AppThemeMode.LIGHT)
    var themeAccent by mutableStateOf(AppThemeAccent.SAKURA)
    var statusCardBackgroundUri by mutableStateOf<Uri?>(null)
    var appBackgroundUri by mutableStateOf<Uri?>(null)
    var backgroundDim by mutableFloatStateOf(0f)
    var cardAlpha by mutableFloatStateOf(1f)
    var useBackgroundSeedColor by mutableStateOf(false)
    var extractedSeedColor by mutableStateOf<Color?>(null)
    var appDpi by mutableIntStateOf(0)

    // Installed theme state
    var appliedNewThemeName by mutableStateOf<String?>(null)
    var appliedOriginalThemeName by mutableStateOf<String?>(null)
    var appliedIsPriority by mutableStateOf(false)

    // Store pin state (community / custom theme ids)
    var pinnedThemeIds by mutableStateOf<Set<String>>(emptySet())

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_THEME_MODE, AppThemeMode.LIGHT.name)
        val accentStr = prefs.getString(KEY_THEME_ACCENT, AppThemeAccent.SAKURA.name)
        themeMode = try {
            AppThemeMode.valueOf(modeStr ?: AppThemeMode.LIGHT.name)
        } catch (_: Exception) {
            when (modeStr) {
                "DARK" -> AppThemeMode.MATERIAL_DARK
                "SYSTEM" -> AppThemeMode.LIGHT
                else -> AppThemeMode.LIGHT
            }
        }
        themeAccent = try {
            AppThemeAccent.valueOf(accentStr ?: AppThemeAccent.SAKURA.name)
        } catch (_: Exception) {
            AppThemeAccent.SAKURA
        }

        val statusBgStr = prefs.getString(KEY_STATUS_BG_URI, null)
        statusCardBackgroundUri = statusBgStr?.let { Uri.parse(it) }?.takeIf { uri ->
            val file = File(uri.path ?: "")
            file.exists()
        }

        val appBgStr = prefs.getString(KEY_APP_BG_URI, null)
        appBackgroundUri = appBgStr?.let { Uri.parse(it) }?.takeIf { uri ->
            val file = File(uri.path ?: "")
            file.exists()
        }

        backgroundDim = prefs.getFloat(KEY_BG_DIM, 0f).coerceIn(0f, 0.9f)
        cardAlpha = prefs.getFloat(KEY_CARD_ALPHA, 1f).coerceIn(0.1f, 1f)
        useBackgroundSeedColor = prefs.getBoolean(KEY_USE_BG_SEED_COLOR, false)
        val seedColorInt = prefs.getInt(KEY_EXTRACTED_SEED_COLOR, 0)
        extractedSeedColor = if (seedColorInt != 0) Color(seedColorInt) else null
        appDpi = prefs.getInt(KEY_APP_DPI, 0)

        appliedNewThemeName = prefs.getString(KEY_APPLIED_NEW_THEME, null)
        appliedOriginalThemeName = prefs.getString(KEY_APPLIED_ORIGINAL_THEME, null)
        appliedIsPriority = prefs.getBoolean(KEY_APPLIED_IS_PRIORITY, false)

        val pinnedRaw = prefs.getString(KEY_PINNED_THEMES, "") ?: ""
        pinnedThemeIds = pinnedRaw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    fun isPinnedTheme(themeId: String): Boolean = themeId in pinnedThemeIds

    fun togglePinnedTheme(context: Context, themeId: String) {
        pinnedThemeIds = if (themeId in pinnedThemeIds) {
            pinnedThemeIds - themeId
        } else {
            pinnedThemeIds + themeId
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_PINNED_THEMES, pinnedThemeIds.joinToString(","))
            .apply()
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

    fun saveStatusCardBackground(context: Context, uri: Uri?) {
        val finalUri = if (uri != null) {
            context.filesDir.listFiles()?.filter { it.name.startsWith("status_card_bg") }?.forEach { it.delete() }
            val fileName = "status_card_bg_${System.currentTimeMillis()}.jpg"
            copyImageToInternalStorage(context, uri, fileName)
        } else {
            context.filesDir.listFiles()?.filter { it.name.startsWith("status_card_bg") }?.forEach { it.delete() }
            null
        }
        statusCardBackgroundUri = finalUri
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATUS_BG_URI, finalUri?.toString())
            .apply()
    }

    fun saveAppBackground(context: Context, uri: Uri?) {
        val finalUri = if (uri != null) {
            context.filesDir.listFiles()?.filter { it.name.startsWith("app_bg") }?.forEach { it.delete() }
            val fileName = "app_bg_${System.currentTimeMillis()}.jpg"
            val copied = copyImageToInternalStorage(context, uri, fileName)
            if (backgroundDim == 0f) backgroundDim = 0.25f
            if (cardAlpha == 1f) cardAlpha = 0.75f
            copied?.let {
                val seed = extractDominantColor(context, it)
                extractedSeedColor = seed
            }
            copied
        } else {
            context.filesDir.listFiles()?.filter { it.name.startsWith("app_bg") }?.forEach { it.delete() }
            backgroundDim = 0f
            cardAlpha = 1f
            extractedSeedColor = null
            useBackgroundSeedColor = false
            null
        }
        appBackgroundUri = finalUri
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_APP_BG_URI, finalUri?.toString())
            .putFloat(KEY_BG_DIM, backgroundDim)
            .putFloat(KEY_CARD_ALPHA, cardAlpha)
            .putBoolean(KEY_USE_BG_SEED_COLOR, useBackgroundSeedColor)
            .putInt(KEY_EXTRACTED_SEED_COLOR, extractedSeedColor?.toArgb() ?: 0)
            .apply()
    }

    fun saveBackgroundDim(context: Context, dim: Float) {
        val safeDim = dim.coerceIn(0f, 0.9f)
        backgroundDim = safeDim
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_BG_DIM, safeDim)
            .apply()
    }

    fun saveCardAlpha(context: Context, alpha: Float) {
        val safeAlpha = alpha.coerceIn(0.1f, 1f)
        cardAlpha = safeAlpha
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_CARD_ALPHA, safeAlpha)
            .apply()
    }

    fun saveUseBackgroundSeedColor(context: Context, enable: Boolean) {
        useBackgroundSeedColor = enable
        if (enable && extractedSeedColor == null && appBackgroundUri != null) {
            extractedSeedColor = extractDominantColor(context, appBackgroundUri!!)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_USE_BG_SEED_COLOR, enable)
            .putInt(KEY_EXTRACTED_SEED_COLOR, extractedSeedColor?.toArgb() ?: 0)
            .apply()
    }

    fun saveAppDpi(context: Context, dpi: Int) {
        appDpi = dpi
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_APP_DPI, dpi)
            .apply()
    }

    fun saveAppliedTheme(context: Context, newTheme: String?, originalTheme: String?, isPriority: Boolean) {
        appliedNewThemeName = newTheme
        appliedOriginalThemeName = originalTheme
        appliedIsPriority = isPriority
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_APPLIED_NEW_THEME, newTheme)
            .putString(KEY_APPLIED_ORIGINAL_THEME, originalTheme)
            .putBoolean(KEY_APPLIED_IS_PRIORITY, isPriority)
            .apply()
    }

    fun clearAppliedTheme(context: Context) {
        appliedNewThemeName = null
        appliedOriginalThemeName = null
        appliedIsPriority = false
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_APPLIED_NEW_THEME)
            .remove(KEY_APPLIED_ORIGINAL_THEME)
            .remove(KEY_APPLIED_IS_PRIORITY)
            .apply()
    }

    private fun copyImageToInternalStorage(context: Context, uri: Uri, fileName: String): Uri? {
        return try {
            val inputStream = if (uri.scheme == "file") {
                File(uri.path ?: "").inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            } ?: return null
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(8 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
            inputStream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    fun extractDominantColor(context: Context, uri: Uri): Color {
        return try {
            val inputStream = if (uri.scheme == "file") {
                File(uri.path ?: "").inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            } ?: return SakuraPink
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options) ?: return SakuraPink
            inputStream.close()

            val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
            if (scaled != bitmap) {
                bitmap.recycle()
            }

            var maxSaturation = -1f
            var bestColor = SakuraPink.toArgb()

            val width = scaled.width
            val height = scaled.height
            val pixels = IntArray(width * height)
            scaled.getPixels(pixels, 0, width, 0, 0, width, height)
            scaled.recycle()

            for (pixel in pixels) {
                val alpha = (pixel shr 24) and 0xFF
                if (alpha < 128) continue
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(pixel, hsv)
                val saturation = hsv[1]
                val value = hsv[2]
                if (saturation in 0.3f..0.95f && value in 0.35f..0.95f) {
                    if (saturation > maxSaturation) {
                        maxSaturation = saturation
                        bestColor = pixel
                    }
                }
            }
            Color(bestColor)
        } catch (e: Exception) {
            SakuraPink
        }
    }
}

fun deriveThemeAccentFromColor(color: Color): CustomAccentColors {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)

    val lightHsv = floatArrayOf(hsv[0], (hsv[1] * 1.15f).coerceIn(0.45f, 0.95f), (hsv[2] * 0.7f).coerceIn(0.25f, 0.6f))
    val lightPrimary = Color(android.graphics.Color.HSVToColor(lightHsv))

    val darkHsv = floatArrayOf(hsv[0], (hsv[1] * 0.7f).coerceIn(0.2f, 0.7f), (hsv[2] * 1.15f).coerceIn(0.75f, 1f))
    val darkPrimary = Color(android.graphics.Color.HSVToColor(darkHsv))

    val secHsv = floatArrayOf((hsv[0] + 30f) % 360f, (hsv[1] * 0.8f).coerceIn(0.3f, 0.8f), hsv[2].coerceIn(0.5f, 0.85f))
    val secondary = Color(android.graphics.Color.HSVToColor(secHsv))

    return CustomAccentColors(
        previewColor = color,
        lightPrimary = lightPrimary,
        darkPrimary = darkPrimary,
        secondary = secondary
    )
}

@Composable
fun PrettieMBeeTheme(
    content: @Composable () -> Unit
) {
    val isDark = when (ThemeConfig.themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.MATERIAL_DARK -> true
        AppThemeMode.OLED_DARK -> true
    }
    val isOled = ThemeConfig.themeMode == AppThemeMode.OLED_DARK

    val baseBackground = when {
        isOled -> OledBackground
        isDark -> DarkBackground
        else -> LightBackground
    }

    val customAccent = remember(ThemeConfig.useBackgroundSeedColor, ThemeConfig.extractedSeedColor, ThemeConfig.themeAccent) {
        if (ThemeConfig.useBackgroundSeedColor && ThemeConfig.extractedSeedColor != null) {
            deriveThemeAccentFromColor(ThemeConfig.extractedSeedColor!!)
        } else {
            CustomAccentColors(
                previewColor = ThemeConfig.themeAccent.previewColor,
                lightPrimary = ThemeConfig.themeAccent.lightPrimary,
                darkPrimary = ThemeConfig.themeAccent.darkPrimary,
                secondary = ThemeConfig.themeAccent.secondary
            )
        }
    }

    val colorScheme = when {
        isOled -> darkColorScheme(
            primary = customAccent.darkPrimary,
            onPrimary = Color(0xFF1E060D),
            primaryContainer = customAccent.darkPrimary.copy(alpha = 0.22f),
            onPrimaryContainer = Color.White,
            secondary = customAccent.secondary,
            onSecondary = Color(0xFF1E060D),
            secondaryContainer = OledSurfaceContainerHigh,
            onSecondaryContainer = OledOnSurface,
            tertiary = customAccent.previewColor,
            background = if (ThemeConfig.appBackgroundUri != null) Color.Transparent else OledBackground,
            surface = OledSurface,
            surfaceVariant = OledSurfaceVariant,
            surfaceContainerLowest = OledSurfaceContainerLowest,
            surfaceContainerLow = OledSurfaceContainerLow,
            surfaceContainer = OledSurfaceContainer,
            surfaceContainerHigh = OledSurfaceContainerHigh,
            surfaceContainerHighest = OledSurfaceContainerHighest,
            outline = OledOutline,
            outlineVariant = OledOutlineVariant,
            onBackground = OledOnSurface,
            onSurface = OledOnSurface,
            onSurfaceVariant = OledOnSurfaceVariant
        )
        isDark -> darkColorScheme(
            primary = customAccent.darkPrimary,
            onPrimary = Color(0xFF1E060D),
            primaryContainer = customAccent.darkPrimary.copy(alpha = 0.22f),
            onPrimaryContainer = Color.White,
            secondary = customAccent.secondary,
            onSecondary = Color(0xFF1E060D),
            secondaryContainer = DarkSurfaceContainerHigh,
            onSecondaryContainer = DarkOnSurface,
            tertiary = customAccent.previewColor,
            background = if (ThemeConfig.appBackgroundUri != null) Color.Transparent else DarkBackground,
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
        else -> lightColorScheme(
            primary = customAccent.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = customAccent.previewColor.copy(alpha = 0.25f),
            onPrimaryContainer = Color(0xFF1E060D),
            secondary = customAccent.secondary,
            onSecondary = Color.White,
            secondaryContainer = LightSurfaceContainerHigh,
            onSecondaryContainer = LightOnSurface,
            tertiary = customAccent.previewColor,
            background = if (ThemeConfig.appBackgroundUri != null) Color.Transparent else LightBackground,
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
                window.statusBarColor = if (ThemeConfig.appBackgroundUri != null) Color.Transparent.toArgb() else colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    val systemDensity = LocalDensity.current
    val density = remember(systemDensity, ThemeConfig.appDpi) {
        if (ThemeConfig.appDpi <= 0) {
            systemDensity
        } else {
            val targetDensity = ThemeConfig.appDpi / 160f
            Density(density = targetDensity, fontScale = systemDensity.fontScale)
        }
    }

    CompositionLocalProvider(LocalDensity provides density) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(baseBackground)
            ) {
                // Full Screen Background (App Wallpaper)
                if (ThemeConfig.appBackgroundUri != null) {
                    val file = remember(ThemeConfig.appBackgroundUri) {
                        ThemeConfig.appBackgroundUri?.path?.let { File(it) }?.takeIf { it.exists() }
                    }
                    if (file != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(file)
                                .allowHardware(false)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (ThemeConfig.backgroundDim > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = ThemeConfig.backgroundDim))
                            )
                        }
                    }
                }
                content()
            }
        }
    }
}

