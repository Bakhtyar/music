package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.MediaViewModel
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppUIStyle
import com.example.ui.theme.LayoutDensity
import com.example.ui.theme.ThemePreset
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    viewModel: MediaViewModel,
    onNavigateBack: () -> Unit
) {
    val themeState by viewModel.themeState.collectAsStateWithLifecycle()
    val customArt by viewModel.customArtState.collectAsStateWithLifecycle()

    var activePickerType by remember { mutableStateOf("hero") }
    val galleryArtPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.saveCustomImage(activePickerType, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "السمة",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Density Switcher at top (Large, Medium, Small)
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DensityTab(
                            title = "كبير",
                            icon = Icons.Filled.GridView,
                            isSelected = themeState.density == LayoutDensity.LARGE,
                            onClick = { viewModel.setLayoutDensity(LayoutDensity.LARGE) }
                        )
                        DensityTab(
                            title = "وسط",
                            icon = Icons.Filled.ViewAgenda,
                            isSelected = themeState.density == LayoutDensity.MEDIUM,
                            onClick = { viewModel.setLayoutDensity(LayoutDensity.MEDIUM) }
                        )
                        DensityTab(
                            title = "صغير",
                            icon = Icons.Filled.DensitySmall,
                            isSelected = themeState.density == LayoutDensity.SMALL,
                            onClick = { viewModel.setLayoutDensity(LayoutDensity.SMALL) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Live Interactive Lark Player Phone Mockup Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                LarkPlayerMockupPreview(
                    themeState = themeState
                )
            }

            // Bottom Controls Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // UI Style Selector: Night Vibes vs Lark Classic
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (themeState.style == AppUIStyle.NIGHT_VIBES) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { viewModel.setAppUIStyle(AppUIStyle.NIGHT_VIBES) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.NightsStay,
                                contentDescription = null,
                                tint = if (themeState.style == AppUIStyle.NIGHT_VIBES) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Night Vibes",
                                color = if (themeState.style == AppUIStyle.NIGHT_VIBES) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (themeState.style == AppUIStyle.LARK_CLASSIC) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { viewModel.setAppUIStyle(AppUIStyle.LARK_CLASSIC) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = if (themeState.style == AppUIStyle.LARK_CLASSIC) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Lark الكلاسيكي",
                                color = if (themeState.style == AppUIStyle.LARK_CLASSIC) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Theme Mode Pill Selector (النظام | نهاري | ليلي)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeModeButton(
                        title = "النظام",
                        icon = Icons.Filled.BrightnessAuto,
                        isSelected = themeState.mode == AppThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) }
                    )
                    ThemeModeButton(
                        title = "نهاري",
                        icon = Icons.Filled.WbSunny,
                        isSelected = themeState.mode == AppThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) }
                    )
                    ThemeModeButton(
                        title = "ليلي",
                        icon = Icons.Filled.DarkMode,
                        isSelected = themeState.mode == AppThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(AppThemeMode.DARK) }
                    )
                }

                // Color Accents & Themes Palette (Lark Player style circle chips)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(ThemePreset.values()) { preset ->
                        ThemeColorChip(
                            preset = preset,
                            isSelected = themeState.preset == preset,
                            onClick = { viewModel.setThemePreset(preset) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Artwork & Anime Frames Customizer
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "تخصيص صور الأنمي والأغلفة من المعرض",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "يمكنك استبدال صور الأنمي وأغلفة الأغاني بصور شخصية من معرض جهازك، ويتم حفظ التغييرات تلقائياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. Hero Anime Banner
                        CustomArtRowItem(
                            title = "صورة الواجهة الرئيسية (البانر)",
                            subtitle = "صورة الأنمي العلوية في الصفحة الرئيسية",
                            customPath = customArt.heroImagePath,
                            defaultResId = R.drawable.img_night_anime_hero,
                            onPick = {
                                activePickerType = "hero"
                                galleryArtPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onReset = { viewModel.resetCustomImage("hero") }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        )

                        // 2. Night Vibes Cover
                        CustomArtRowItem(
                            title = "إطار غلاف Night Drive",
                            subtitle = "الغلاف الافتراضي لمشغل الموسيقى والقوائم",
                            customPath = customArt.nightVibesCoverPath,
                            defaultResId = R.drawable.img_night_vibes_cover,
                            onPick = {
                                activePickerType = "night"
                                galleryArtPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onReset = { viewModel.resetCustomImage("night") }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        )

                        // 3. Moonlight Cover
                        CustomArtRowItem(
                            title = "إطار غلاف Broken Hearts",
                            subtitle = "الغلاف البديل للأغاني الهادئة والحزينة",
                            customPath = customArt.moonlightCoverPath,
                            defaultResId = R.drawable.img_moonlight_cover,
                            onPick = {
                                activePickerType = "moonlight"
                                galleryArtPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onReset = { viewModel.resetCustomImage("moonlight") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomArtRowItem(
    title: String,
    subtitle: String,
    customPath: String?,
    defaultResId: Int,
    onPick: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail preview
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
        ) {
            if (customPath != null) {
                AsyncImage(
                    model = File(customPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = defaultResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (customPath != null) "مخصصة من المعرض" else subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (customPath != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalButton(
                onClick = onPick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تغيير", style = MaterialTheme.typography.labelSmall)
            }

            if (customPath != null) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = "استعادة",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DensityTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(title, color = contentColor, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ThemeModeButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, color = contentColor, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ThemeColorChip(
    preset: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(54.dp)
            .clip(CircleShape)
            .border(2.5.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        if (preset.isWallpaperTheme) {
            // Wallpaper gradient preview circle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF38BDF8),
                                Color(0xFF1E293B)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Brush,
                    contentDescription = preset.title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(preset.primaryColor),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "محدد",
                        tint = if (preset == ThemePreset.OBSIDIAN) Color.Black else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LarkPlayerMockupPreview(
    themeState: com.example.ui.theme.AppThemeState
) {
    val isLight = themeState.mode == AppThemeMode.LIGHT
    val primaryColor = themeState.preset.primaryColor
    val cardBg = if (isLight) Color.White else (if (themeState.preset == ThemePreset.OBSIDIAN) Color(0xFF1E1E1E) else Color(0xFF1F1D2B))
    val phoneCanvasBg = if (isLight) Color(0xFFF1F5F9) else (if (themeState.preset == ThemePreset.OBSIDIAN) Color(0xFF0A0A0A) else Color(0xFF121216))
    val textColor = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textMuted = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)

    // Phone Frame Mockup
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 280.dp)
            .shadow(16.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .border(1.5.dp, if (themeState.style == AppUIStyle.NIGHT_VIBES) Color(0xFF38BDF8) else primaryColor.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
            .background(if (themeState.style == AppUIStyle.NIGHT_VIBES) Color(0xFF0B0F19) else phoneCanvasBg)
            .padding(12.dp)
    ) {
        if (themeState.style == AppUIStyle.NIGHT_VIBES) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Mockup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("مرحباً بك 🌙", color = Color(0xFF38BDF8), fontSize = 10.sp)
                        Text("Night Vibes", color = Color.White, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mini Hero banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_night_anime_hero),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC0B0F19))))
                            .padding(6.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text("استمع لأجواء الليل الهادئة", color = Color.White, fontSize = 9.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4 Categories row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("المفضلة" to Color(0xFFEF4444), "القوائم" to Color(0xFF3B82F6), "الفنانين" to Color(0xFF10B981), "الأنواع" to Color(0xFF8B5CF6)).forEach { (cat, col) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(col.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(col))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(cat, color = Color(0xFF94A3B8), fontSize = 7.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Recommended Cards preview
                Text("مقترح لك", color = Color.White, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_night_vibes_cover),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0x66000000)).padding(4.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text("Night Drive", color = Color.White, fontSize = 8.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_moonlight_cover),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0x66000000)).padding(4.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text("Broken Hearts", color = Color.White, fontSize = 8.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Docked MiniPlayer preview in Night Vibes
                Surface(
                    color = Color(0xFF161F30),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF38BDF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.NightsStay, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Midnight Melodies", color = Color.White, fontSize = 8.sp, maxLines = 1)
                        }
                        Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
            // Header Mockup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lark Player",
                    color = textColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 15.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, tint = textMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.Settings, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chips Row (فيديوهات | أغاني | قوائم)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("الأغاني", color = Color.White, fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("الفيديوهات", color = textMuted, fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("قوائم التشغيل", color = textMuted, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content preview based on Density
            when (themeState.density) {
                LayoutDensity.LARGE -> {
                    // 2-Column Grid Preview (Screenshot 5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MockupGridCard(primaryColor, cardBg, textColor, textMuted, "Heart Skips", Modifier.weight(1f))
                        MockupGridCard(primaryColor, cardBg, textColor, textMuted, "Crazy Love", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MockupGridCard(primaryColor, cardBg, textColor, textMuted, "Beach Day", Modifier.weight(1f))
                        MockupGridCard(primaryColor, cardBg, textColor, textMuted, "Skyline", Modifier.weight(1f))
                    }
                }
                LayoutDensity.MEDIUM -> {
                    // Comfortable List Preview (Screenshot 1 & 2)
                    MockupListRow(primaryColor, cardBg, textColor, textMuted, "Crazy In Love With Smile", "Don Juan", 38.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    MockupListRow(primaryColor, cardBg, textColor, textMuted, "Heart Skips A Beat", "Hariel", 38.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    MockupListRow(primaryColor, cardBg, textColor, textMuted, "Beach Memories", "Nattan", 38.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    MockupListRow(primaryColor, cardBg, textColor, textMuted, "Touch The Sky", "Mc Poze", 38.dp)
                }
                LayoutDensity.SMALL -> {
                    // Dense Compact List Preview (Screenshot 4)
                    MockupListRow(primaryColor, cardBg, textColor, textMuted, "Crazy In Love", "Don Juan", 26.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    MockupListRow(primaryColor, cardBg, textColor, textMuted, "Heart Skips", "Hariel", 26.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    MockupListRow(primaryColor, cardBg, textColor, textMuted, "Beach Memories", "Nattan", 26.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    MockupListRow(primaryColor, cardBg, textColor, textMuted, "Can't Stop", "Juliano", 26.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    MockupListRow(primaryColor, cardBg, textColor, textMuted, "Blue Eyes", "Poze", 26.dp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Mini Player Mockup in Phone
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Spread My Wings", color = textColor, fontSize = 9.sp, maxLines = 1)
                            Text("Zendaya", color = textMuted, fontSize = 8.sp, maxLines = 1)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PlayArrow, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.SkipNext, null, tint = textColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
}

@Composable
fun MockupGridCard(
    primaryColor: Color,
    cardBg: Color,
    textColor: Color,
    textMuted: Color,
    title: String,
    modifier: Modifier
) {
    Surface(
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(primaryColor.copy(alpha = 0.8f), cardBg)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, color = textColor, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
fun MockupListRow(
    primaryColor: Color,
    cardBg: Color,
    textColor: Color,
    textMuted: Color,
    title: String,
    artist: String,
    artSize: androidx.compose.ui.unit.Dp
) {
    Surface(
        color = cardBg,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(6.dp))
                    .background(primaryColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, null, tint = primaryColor, modifier = Modifier.size(artSize * 0.5f))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textColor, fontSize = 10.sp, maxLines = 1)
                Text(artist, color = textMuted, fontSize = 8.sp, maxLines = 1)
            }
            Icon(Icons.Filled.MoreVert, null, tint = textMuted, modifier = Modifier.size(14.dp))
        }
    }
}
