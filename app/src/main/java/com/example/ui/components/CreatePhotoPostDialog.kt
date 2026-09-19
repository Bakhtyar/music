package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.data.MediaModel
import com.example.ui.MediaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * وضع حجم وملاءمة الصورة في المنشور
 */
enum class PhotoScaleMode(val title: String, val description: String, val scale: ContentScale) {
    NATURAL_FIT("الوضع الطبيعي", "ملاءمة كاملة بدون قص", ContentScale.Fit),
    FILL_CROP("ملء الشاشة", "قص وتعبئة كامل الشاشة", ContentScale.Crop),
    INSIDE("توسيط", "الحجم الأصلي في المنتصف", ContentScale.Inside)
}

/**
 * شاشة ونموذج إنشاء منشور صورة تفاعلي على نمط تيك توك / إنستغرام
 * يعرض نموذجاً حياً مباشراً لكيفية ظهور المنشور في صفحة الفيديوهات (Swipe Video Feed)
 * - التعديل وإضافة الصور وحذفها عبر شريط الصور المصغرة
 * - اختيار حجم الصورة (الوضع الطبيعي الأصلي بدون لمسه، أو ملء الشاشة)
 * - الاستماع للأغنية ومعاينتها قبل الاختيار
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePhotoPostDialog(
    viewModel: MediaViewModel,
    onDismiss: () -> Unit,
    onPostCreated: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedAudio by remember { mutableStateOf<MediaModel?>(audios.firstOrNull()) }
    var postTitle by remember { mutableStateOf("") }
    var scaleMode by remember { mutableStateOf(PhotoScaleMode.NATURAL_FIT) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAudioPickerSheet by remember { mutableStateOf(false) }
    var showTitleEditDialog by remember { mutableStateOf(false) }
    var isPreviewAudioPlaying by remember { mutableStateOf(false) }
    var feedbackToast by remember { mutableStateOf<String?>(null) }

    // مشغل الصوت المدمج لمعاينة الأغنية مع عرض الصور داخل النموذج
    val editorPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(editorPlayer) {
        onDispose {
            editorPlayer.stop()
            editorPlayer.release()
        }
    }

    // مزامنة تشغيل الصوت في المعاينة عند تغير الأغنية
    LaunchedEffect(selectedAudio, isPreviewAudioPlaying) {
        if (isPreviewAudioPlaying && selectedAudio != null) {
            try {
                editorPlayer.stop()
                editorPlayer.clearMediaItems()
                val audioUri = if (selectedAudio!!.filePath.startsWith("content://")) {
                    Uri.parse(selectedAudio!!.filePath)
                } else {
                    Uri.fromFile(File(selectedAudio!!.filePath))
                }
                editorPlayer.setMediaItem(MediaItem.fromUri(audioUri))
                editorPlayer.prepare()
                editorPlayer.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            editorPlayer.pause()
        }
    }

    // منتقي الصور المتعددة
    val multiplePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 35)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val combined = (selectedImageUris + uris).distinct()
            selectedImageUris = combined
            errorMessage = null
        }
    }

    // فتح منتقي الصور تلقائياً في المرة الأولى إذا لم تكن هناك صور محددة
    var hasAutoOpenedPicker by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasAutoOpenedPicker && selectedImageUris.isEmpty()) {
            hasAutoOpenedPicker = true
            multiplePhotoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    // إخفاء التنبيه التوضيحي تلقائياً
    LaunchedEffect(feedbackToast) {
        if (feedbackToast != null) {
            delay(2000)
            feedbackToast = null
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) {
                editorPlayer.stop()
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (selectedImageUris.isEmpty()) {
                // حالة فارغة إذا ألغى المستخدم الاختيار الأول
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(90.dp),
                        shape = CircleShape,
                        color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Collections,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "إنشاء منشور صور مع موسيقى",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "اختر صورتين أو أكثر لعرض نموذج حي كما يظهر في صفحة الفيديوهات",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = {
                            multiplePhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار الصور من المعرض", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            editorPlayer.stop()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                    ) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            } else {
                // ============================================================
                // النموذج الحي (LIVE PREVIEW MODEL) - مثل لقطة الشاشة المرفقة
                // ============================================================
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { selectedImageUris.size }
                )

                // 1. معاينة الصور التفاعلية بملء الشاشة مع التمرير الأفقي
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val uri = selectedImageUris.getOrNull(page)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0D14)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uri != null) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "معاينة الصورة ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = scaleMode.scale
                            )
                        }
                    }
                }

                // تدرج ظل سفلي لضمان وضوح النصوص والأزرار
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                // تدرج ظل علوي لضمان وضوح الشريط العلوي والموسيقى
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 2. الشريط العلوي (أيقونة الإغلاق + كبسولة الموسيقى + زر التالي)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // زر الإغلاق / الرجوع
                    IconButton(
                        onClick = {
                            editorPlayer.stop()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "إلغاء", tint = Color.White)
                    }

                    // كبسولة الموسيقى في المنتصف (مثل لقطة الشاشة: FUNK UNIVER... 🎵)
                    Surface(
                        modifier = Modifier
                            .clickable { showAudioPickerSheet = true }
                            .height(38.dp),
                        shape = RoundedCornerShape(19.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = if (selectedAudio != null) Color(0xFF38BDF8) else Color(0xFFFF2A6D),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedAudio?.title ?: "اختر أغنية للمنشور",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 160.dp)
                            )
                            if (selectedAudio != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // زر "التالي" السريع في الزاوية العلوية
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                saveAndCreatePost(
                                    context = context,
                                    selectedImageUris = selectedImageUris,
                                    selectedAudio = selectedAudio,
                                    postTitle = postTitle,
                                    viewModel = viewModel,
                                    onSavingChange = { isSaving = it },
                                    onError = { errorMessage = it },
                                    onSuccess = { postId ->
                                        editorPlayer.stop()
                                        onPostCreated(postId)
                                        onDismiss()
                                    }
                                )
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF2A6D))
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "التالي",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // عداد الصور في الزاوية العلوية اليسرى
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 58.dp, start = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${selectedImageUris.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // 3. الشريط الجانبي للأدوات (Side Tool Rail) - مثل لقطة الشاشة
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // أداة حجم الصورة (تختار حجم الصورة أو تركها بالوضع الطبيعي)
                    ToolIconItem(
                        icon = Icons.Filled.Crop,
                        label = scaleMode.title,
                        isActive = scaleMode == PhotoScaleMode.NATURAL_FIT,
                        onClick = {
                            scaleMode = when (scaleMode) {
                                PhotoScaleMode.NATURAL_FIT -> PhotoScaleMode.FILL_CROP
                                PhotoScaleMode.FILL_CROP -> PhotoScaleMode.INSIDE
                                PhotoScaleMode.INSIDE -> PhotoScaleMode.NATURAL_FIT
                            }
                            feedbackToast = "${scaleMode.title}: ${scaleMode.description}"
                        }
                    )

                    // أداة النص والكتابة (Aa)
                    ToolIconItem(
                        icon = Icons.Filled.TextFields,
                        label = if (postTitle.isBlank()) "نص" else "عنوان",
                        isActive = postTitle.isNotBlank(),
                        onClick = { showTitleEditDialog = true }
                    )

                    // أداة الموسيقى (فتح قائمة الأغاني للاستماع والاختيار)
                    ToolIconItem(
                        icon = Icons.Filled.MusicNote,
                        label = "أغنية",
                        isActive = selectedAudio != null,
                        onClick = { showAudioPickerSheet = true }
                    )

                    // أداة سماع الموسيقى أثناء المعاينة
                    ToolIconItem(
                        icon = if (isPreviewAudioPlaying) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        label = if (isPreviewAudioPlaying) "استماع" else "صامت",
                        isActive = isPreviewAudioPlaying,
                        onClick = {
                            if (selectedAudio == null) {
                                showAudioPickerSheet = true
                            } else {
                                isPreviewAudioPlaying = !isPreviewAudioPlaying
                            }
                        }
                    )

                    // زر إضافة المزيد من الصور
                    ToolIconItem(
                        icon = Icons.Filled.AddPhotoAlternate,
                        label = "إضافة",
                        isActive = false,
                        onClick = {
                            multiplePhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }

                // تنبيه عائم لتوضيح نمط الحجم عند التبديل
                if (feedbackToast != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Text(
                            text = feedbackToast ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }

                // 4. تذييل المعاينة (العنوان + شريط الصور المصغرة + زر التالي الرئيسي)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp)
                ) {
                    // عنوان المنشور فوق شريط الصور
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                            .clickable { showTitleEditDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (postTitle.isNotBlank()) postTitle else "اضغط هنا لكتابة عنوان أو وصف للمنشور (اختياري)...",
                            color = if (postTitle.isNotBlank()) Color.White else Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (postTitle.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // شريط الصور المصغرة (Thumbnail Carousel) مع زر إضافة المزيد
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // زر إضافة صور إضافية على يسار الشريط (+)
                        item {
                            Surface(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clickable {
                                        multiplePhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E2638),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "إضافة صور",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        // عرض الصور المصغرة مع تحديد الصورة النشطة
                        itemsIndexed(selectedImageUris) { index, uri ->
                            val isCurrentPage = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isCurrentPage) 2.5.dp else 1.dp,
                                        color = if (isCurrentPage) Color.White else Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "صورة ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // زر حذف الصورة من المنشور
                                if (selectedImageUris.size > 1) {
                                    IconButton(
                                        onClick = {
                                            selectedImageUris = selectedImageUris.toMutableList().also {
                                                it.removeAt(index)
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(22.dp)
                                            .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "حذف",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                // رقم الصورة المصغرة
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(3.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        "${index + 1}",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // رسالة خطأ إن وجدت
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }

                    // 5. زر النشر والتالي في الأسفل (مثل لقطة الشاشة: زر التالي الأحمر والنمط الطبيعي)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // زر التالي / النشر الأساسي (وردي / أحمر مثل لقطة الشاشة)
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    saveAndCreatePost(
                                        context = context,
                                        selectedImageUris = selectedImageUris,
                                        selectedAudio = selectedAudio,
                                        postTitle = postTitle,
                                        viewModel = viewModel,
                                        onSavingChange = { isSaving = it },
                                        onError = { errorMessage = it },
                                        onSuccess = { postId ->
                                            editorPlayer.stop()
                                            onPostCreated(postId)
                                            onDismiss()
                                        }
                                    )
                                }
                            },
                            enabled = !isSaving && selectedImageUris.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF2A6D),
                                contentColor = Color.White
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري إنشاء المنشور...", fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    "التالي • نشر في الفيديوهات",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // شارة الوضع الحالي (الوضع الطبيعي الافتراضي بدون لمسه)
                        Surface(
                            shape = RoundedCornerShape(25.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .height(50.dp)
                                .clickable {
                                    scaleMode = when (scaleMode) {
                                        PhotoScaleMode.NATURAL_FIT -> PhotoScaleMode.FILL_CROP
                                        PhotoScaleMode.FILL_CROP -> PhotoScaleMode.INSIDE
                                        PhotoScaleMode.INSIDE -> PhotoScaleMode.NATURAL_FIT
                                    }
                                    feedbackToast = "${scaleMode.title}: ${scaleMode.description}"
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.AspectRatio,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    scaleMode.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة إدخال عنوان المنشور (Aa)
    if (showTitleEditDialog) {
        AlertDialog(
            onDismissRequest = { showTitleEditDialog = false },
            title = { Text("عنوان أو وصف المنشور", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = postTitle,
                    onValueChange = { postTitle = it },
                    placeholder = { Text("اكتب عنواناً أو وصفاً يظهر مع المنشور...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color(0xFF10162A),
                        unfocusedContainerColor = Color(0xFF10162A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showTitleEditDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("تم", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTitleEditDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            },
            containerColor = Color(0xFF10162A)
        )
    }

    // قائمة اختيار الأغنية مع ميزة الاستماع والمعاينة قبل الاختيار
    if (showAudioPickerSheet) {
        AudioPickerBottomSheet(
            audios = audios,
            selectedAudio = selectedAudio,
            onSelect = {
                selectedAudio = it
                showAudioPickerSheet = false
                // تشغيل الأغنية المختارة في المعاينة فوراً
                isPreviewAudioPlaying = true
            },
            onDismiss = { showAudioPickerSheet = false }
        )
    }
}

/**
 * أيقونة أداة في الشريط الجانبي (مثل إنستغرام وتيك توك)
 */
@Composable
private fun ToolIconItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = if (isActive) Color(0xFF38BDF8).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.5f),
            border = if (isActive) BorderStroke(1.dp, Color(0xFF38BDF8)) else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (isActive) Color(0xFF38BDF8) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp
        )
    }
}

/**
 * قائمة سفلية لاختيار الأغنية مع إمكانية الاستماع للأغنية (Audio Preview) قبل الاختيار
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPickerBottomSheet(
    audios: List<MediaModel>,
    selectedAudio: MediaModel?,
    onSelect: (MediaModel) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var previewingAudioPath by remember { mutableStateOf<String?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    // مشغل استماع مخصص للمعاينة داخل نافذة الأغاني
    val previewPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isPreviewPlaying = isPlaying
                }
            })
        }
    }

    DisposableEffect(previewPlayer) {
        onDispose {
            previewPlayer.stop()
            previewPlayer.release()
        }
    }

    // منتقي ملف صوتي مخصص من ذاكرة الجهاز (MP3 / WAV) في حال رغب المستخدم في إضافة صوت خارجي
    val customAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri) ?: "أغنية مخصصة"
            val customModel = MediaModel(
                id = System.currentTimeMillis(),
                uri = uri,
                filePath = uri.toString(),
                title = fileName,
                duration = 0L,
                type = "AUDIO",
                artist = "ملف مخصص"
            )
            onSelect(customModel)
        }
    }

    val filteredAudios = remember(audios, searchQuery) {
        if (searchQuery.isBlank()) audios
        else audios.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            previewPlayer.stop()
            onDismiss()
        },
        containerColor = Color(0xFF10162A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // العنوان وملاحظة الاستماع
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "اختر أغنية للمنشور",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "اضغط زر التشغيل ▶ للاستماع للأغنية قبل اختيارها",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF38BDF8)
                    )
                }

                // زر اختيار ملف صوتي من الجهاز
                TextButton(
                    onClick = { customAudioPicker.launch("audio/*") },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8))
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("من الجهاز", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // حقل البحث في الأغاني
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث في الأغاني...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF172038),
                    unfocusedContainerColor = Color(0xFF172038)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredAudios.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد ملفات صوتية متطابقة", color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { customAudioPicker.launch("audio/*") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("اختيار ملف صوتي من ذاكرة الهاتف", color = Color(0xFF38BDF8))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAudios) { audio ->
                        val isCurrentSelected = selectedAudio?.filePath == audio.filePath
                        val isThisAudioPreviewing = previewingAudioPath == audio.filePath && isPreviewPlaying

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isThisAudioPreviewing -> Color(0xFF38BDF8).copy(alpha = 0.22f)
                                isCurrentSelected -> Color(0xFF38BDF8).copy(alpha = 0.12f)
                                else -> Color(0xFF172038)
                            },
                            border = BorderStroke(
                                width = if (isThisAudioPreviewing || isCurrentSelected) 1.5.dp else 0.5.dp,
                                color = if (isThisAudioPreviewing || isCurrentSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. زر الاستماع والمعاينة قبل الاختيار (Play / Pause Preview Button)
                                IconButton(
                                    onClick = {
                                        if (previewingAudioPath == audio.filePath && isPreviewPlaying) {
                                            previewPlayer.pause()
                                            isPreviewPlaying = false
                                        } else {
                                            try {
                                                previewPlayer.stop()
                                                previewPlayer.clearMediaItems()
                                                val audioUri = if (audio.filePath.startsWith("content://")) {
                                                    Uri.parse(audio.filePath)
                                                } else {
                                                    Uri.fromFile(File(audio.filePath))
                                                }
                                                previewPlayer.setMediaItem(MediaItem.fromUri(audioUri))
                                                previewPlayer.prepare()
                                                previewPlayer.play()
                                                previewingAudioPath = audio.filePath
                                                isPreviewPlaying = true
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isThisAudioPreviewing) Color(0xFF38BDF8) else Color(0xFFFF2A6D).copy(alpha = 0.25f)
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isThisAudioPreviewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isThisAudioPreviewing) "إيقاف مؤقت" else "استماع للأغنية",
                                        tint = if (isThisAudioPreviewing) Color.Black else Color(0xFFFF2A6D),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // 2. بيانات الأغنية (العنوان والفنان والمدة)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            previewPlayer.stop()
                                            onSelect(audio)
                                        }
                                ) {
                                    Text(
                                        text = audio.title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrentSelected || isThisAudioPreviewing) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val durationSec = audio.duration / 1000
                                    val durationText = if (durationSec > 0) "${durationSec / 60}:${String.format("%02d", durationSec % 60)}" else "جاهز"
                                    val artistInfo = if (audio.artist.isNotBlank()) "${audio.artist} • $durationText" else durationText
                                    Text(
                                        text = if (isThisAudioPreviewing) "جاري الاستماع الآن... ♫" else artistInfo,
                                        color = if (isThisAudioPreviewing) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // 3. زر اختيار الأغنية
                                Button(
                                    onClick = {
                                        previewPlayer.stop()
                                        onSelect(audio)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCurrentSelected) Color(0xFF38BDF8) else Color(0xFF222F4C),
                                        contentColor = if (isCurrentSelected) Color.Black else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (isCurrentSelected) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isCurrentSelected) "مختارة" else "اختيار",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * تنفيذ عملية حفظ الصور وإنشاء المنشور
 */
private suspend fun saveAndCreatePost(
    context: Context,
    selectedImageUris: List<Uri>,
    selectedAudio: MediaModel?,
    postTitle: String,
    viewModel: MediaViewModel,
    onSavingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onSuccess: (Long) -> Unit
) {
    if (selectedImageUris.isEmpty()) {
        onError("يرجى اختيار صورة واحدة على الأقل للمنشور")
        return
    }
    if (selectedAudio == null) {
        onError("يرجى اختيار أغنية للمنشور من زر الموسيقى")
        return
    }

    onSavingChange(true)
    val savedPaths = withContext(Dispatchers.IO) {
        copyUrisToAppInternalStorage(context, selectedImageUris)
    }

    if (savedPaths.isEmpty()) {
        onSavingChange(false)
        onError("فشل في حفظ الصور المحددة")
        return
    }

    val finalTitle = postTitle.ifBlank {
        selectedAudio.title.ifBlank { "منشور صور" }
    }

    viewModel.createPhotoPost(
        title = finalTitle,
        photoPaths = savedPaths,
        audioFilePath = selectedAudio.filePath,
        audioTitle = selectedAudio.title,
        duration = selectedAudio.duration,
        onSuccess = { postId ->
            onSavingChange(false)
            onSuccess(postId)
        }
    )
}

/**
 * استخراج اسم الملف من Uri
 */
private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                cursor.getString(nameIndex)
            } else null
        } ?: uri.lastPathSegment
    } catch (e: Exception) {
        uri.lastPathSegment
    }
}

/**
 * نسخ الصور المختارة إلى التخزين الداخلي للتطبيق لحفظها بشكل دائم وموثوق
 */
fun copyUrisToAppInternalStorage(context: Context, uris: List<Uri>): List<String> {
    val dir = File(context.filesDir, "photo_posts")
    if (!dir.exists()) dir.mkdirs()
    val paths = mutableListOf<String>()
    val timestamp = System.currentTimeMillis()

    uris.forEachIndexed { idx, uri ->
        try {
            val destFile = File(dir, "photo_${timestamp}_${idx}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                paths.add(destFile.absolutePath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return paths
}
