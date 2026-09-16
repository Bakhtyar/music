package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

enum class AppUIStyle(val title: String, val subtitle: String) {
    NIGHT_VIBES("نايت فايبز (Night Vibes)", "المظهر الأنمي النيون (8 شاشات تفاعلية)"),
    LARK_CLASSIC("Lark Player الكلاسيكي", "المظهر القياسي مع مبدل الكثافات والفيديوهات")
}

enum class AppThemeMode(val title: String) {
    SYSTEM("النظام"),
    LIGHT("نهاري"),
    DARK("ليلي")
}

enum class LayoutDensity(val title: String) {
    LARGE("كبير"),
    MEDIUM("وسط"),
    SMALL("صغير")
}

enum class ThemePreset(
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val isWallpaperTheme: Boolean = false
) {
    PINK(
        title = "وردي عصري",
        primaryColor = Color(0xFFE91E63),
        secondaryColor = Color(0xFFFF4081)
    ),
    GREEN(
        title = "أخضر نيون",
        primaryColor = Color(0xFF00E676),
        secondaryColor = Color(0xFF69F0AE)
    ),
    BLUE(
        title = "أزرق لارك",
        primaryColor = Color(0xFF0091EA),
        secondaryColor = Color(0xFF40C4FF)
    ),
    OBSIDIAN(
        title = "داكن فاحم",
        primaryColor = Color(0xFFEEEEEE),
        secondaryColor = Color(0xFF757575)
    ),
    PURPLE(
        title = "بنفسجي ملكي",
        primaryColor = Color(0xFFAB47BC),
        secondaryColor = Color(0xFFCE93D8)
    ),
    WALLPAPER(
        title = "خلفية فنية سحرية",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF818CF8),
        isWallpaperTheme = true
    )
}

data class CustomArtState(
    val heroImagePath: String? = null,
    val nightVibesCoverPath: String? = null,
    val moonlightCoverPath: String? = null,
    val playlistCovers: Map<Long, String> = emptyMap()
)

data class AppThemeState(
    val mode: AppThemeMode = AppThemeMode.DARK,
    val preset: ThemePreset = ThemePreset.BLUE,
    val density: LayoutDensity = LayoutDensity.MEDIUM,
    val style: AppUIStyle = AppUIStyle.NIGHT_VIBES
)

class ThemePreferences(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    private val _themeState = MutableStateFlow(loadThemeState())
    val themeState: StateFlow<AppThemeState> = _themeState

    private val _customArtState = MutableStateFlow(loadCustomArtState())
    val customArtState: StateFlow<CustomArtState> = _customArtState

    private fun loadThemeState(): AppThemeState {
        val modeStr = prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val presetStr = prefs.getString("theme_preset", ThemePreset.BLUE.name) ?: ThemePreset.BLUE.name
        val densityStr = prefs.getString("layout_density", LayoutDensity.MEDIUM.name) ?: LayoutDensity.MEDIUM.name
        val styleStr = prefs.getString("app_ui_style", AppUIStyle.NIGHT_VIBES.name) ?: AppUIStyle.NIGHT_VIBES.name

        val mode = runCatching { AppThemeMode.valueOf(modeStr) }.getOrDefault(AppThemeMode.DARK)
        val preset = runCatching { ThemePreset.valueOf(presetStr) }.getOrDefault(ThemePreset.BLUE)
        val density = runCatching { LayoutDensity.valueOf(densityStr) }.getOrDefault(LayoutDensity.MEDIUM)
        val style = runCatching { AppUIStyle.valueOf(styleStr) }.getOrDefault(AppUIStyle.NIGHT_VIBES)

        return AppThemeState(mode = mode, preset = preset, density = density, style = style)
    }

    private fun loadCustomArtState(): CustomArtState {
        val hero = prefs.getString("custom_hero_path", null)?.takeIf { File(it).exists() }
        val night = prefs.getString("custom_night_path", null)?.takeIf { File(it).exists() }
        val moonlight = prefs.getString("custom_moonlight_path", null)?.takeIf { File(it).exists() }
        val playlistMap = mutableMapOf<Long, String>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("custom_playlist_") && key.endsWith("_path") && value is String) {
                val idStr = key.removePrefix("custom_playlist_").removeSuffix("_path")
                val id = idStr.toLongOrNull()
                if (id != null && File(value).exists()) {
                    playlistMap[id] = value
                }
            }
        }
        return CustomArtState(
            heroImagePath = hero,
            nightVibesCoverPath = night,
            moonlightCoverPath = moonlight,
            playlistCovers = playlistMap
        )
    }

    fun saveCustomImage(type: String, uri: Uri): String? {
        return try {
            val fileName = when (type) {
                "hero" -> "custom_hero_banner.jpg"
                "night" -> "custom_night_cover.jpg"
                "moonlight" -> "custom_moonlight_cover.jpg"
                else -> "custom_art_${System.currentTimeMillis()}.jpg"
            }
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val path = file.absolutePath
            val key = when (type) {
                "hero" -> "custom_hero_path"
                "night" -> "custom_night_path"
                "moonlight" -> "custom_moonlight_path"
                else -> "custom_art_path"
            }
            prefs.edit().putString(key, path).apply()
            _customArtState.value = loadCustomArtState()
            path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resetCustomImage(type: String) {
        try {
            val key = when (type) {
                "hero" -> "custom_hero_path"
                "night" -> "custom_night_path"
                "moonlight" -> "custom_moonlight_path"
                else -> return
            }
            val path = prefs.getString(key, null)
            if (path != null) {
                File(path).delete()
            }
            prefs.edit().remove(key).apply()
            _customArtState.value = loadCustomArtState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun savePlaylistCover(playlistId: Long, uri: Uri): String? {
        return try {
            val fileName = "custom_playlist_${playlistId}.jpg"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val path = file.absolutePath
            prefs.edit().putString("custom_playlist_${playlistId}_path", path).apply()
            _customArtState.value = loadCustomArtState()
            path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resetPlaylistCover(playlistId: Long) {
        try {
            val key = "custom_playlist_${playlistId}_path"
            val path = prefs.getString(key, null)
            if (path != null) {
                File(path).delete()
            }
            prefs.edit().remove(key).apply()
            _customArtState.value = loadCustomArtState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeState.value = _themeState.value.copy(mode = mode)
    }

    fun setThemePreset(preset: ThemePreset) {
        prefs.edit().putString("theme_preset", preset.name).apply()
        _themeState.value = _themeState.value.copy(preset = preset)
    }

    fun setLayoutDensity(density: LayoutDensity) {
        prefs.edit().putString("layout_density", density.name).apply()
        _themeState.value = _themeState.value.copy(density = density)
    }

    fun setAppUIStyle(style: AppUIStyle) {
        prefs.edit().putString("app_ui_style", style.name).apply()
        _themeState.value = _themeState.value.copy(style = style)
    }
}

val LocalAppThemeState = compositionLocalOf { AppThemeState() }
