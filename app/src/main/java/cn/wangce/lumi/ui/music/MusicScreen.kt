package cn.wangce.lumi.ui.music

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.music.MusicUiState
import cn.wangce.lumi.music.MusicUploader
import cn.wangce.lumi.music.RepeatMode
import cn.wangce.lumi.music.Track
import cn.wangce.lumi.ui.theme.DarkGradientBottom
import cn.wangce.lumi.ui.theme.DarkGradientMid
import cn.wangce.lumi.ui.theme.DarkGradientTop
import cn.wangce.lumi.ui.theme.DurState
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import cn.wangce.lumi.ui.theme.LightGradientBottom
import cn.wangce.lumi.ui.theme.LightGradientMid
import cn.wangce.lumi.ui.theme.LightGradientTop
import cn.wangce.lumi.ui.theme.PillBgDark
import cn.wangce.lumi.ui.theme.PillBgLight
import kotlinx.coroutines.delay

// 每首固定一档灰阶点缀色（封面兜底渐变 / 高亮，对标黑白参考）
private val accentPalette =
    listOf(Color(0xFF1A1A1A), Color(0xFF4A4A4A), Color(0xFF6E6E6E), Color(0xFF8C8C8C), Color(0xFFADADAD), Color(0xFFC9C9C9))

private fun accentFor(index: Int): Color =
    if (index < 0) Color(0xFF1A1A1A) else accentPalette[index % accentPalette.size]

// 治愈音乐二级页：曲库列表 + 底部迷你播放条 + 全屏播放页（参考双页音乐播放器设计）
@Composable
fun MusicScreen(
    onBack: () -> Unit,
    viewModel: MusicViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dark = LocalDarkTheme.current

    // 通知栏遥控需要通知权限（API 33+ 首次进入时申请一次）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showPlayer by remember { mutableStateOf(false) }
    var sortDescending by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val current = state.tracks.getOrNull(state.currentIndex)

    // 曲库列表内容层：迷你条用它做 backdrop 磨砂模糊（兄弟节点录制，避免 layer 自引用递归）
    val listLayer = rememberGraphicsLayer()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // ── 曲库列表页 ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    listLayer.record(IntSize(size.width.toInt(), size.height.toInt())) {
                        this@drawWithContent.drawContent()
                    }
                    drawContent()
                }
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.s5f4112),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.s95521b),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                // 右上角：同步个人网站歌单（文字按钮，同步中转圈）
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.music_sync),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { viewModel.syncPlaylist() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 歌单头：封面 + 歌单信息
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoverImage(
                    track = current ?: state.tracks.firstOrNull(),
                    accent = accentFor(state.currentIndex),
                    cornerRadius = 16.dp,
                    targetPx = 256,
                    overridePath = (current ?: state.tracks.firstOrNull())?.let { state.coverOverrides[it.url] },
                    modifier = Modifier.size(92.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.playlist_fmt, state.tracks.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.s4e52e0),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.s11ab90),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 操作胶囊行：播放/暂停 + 定时关闭 + 正序/逆序
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (dark) PillBgDark else PillBgLight)
                        .clickable { viewModel.togglePlayPause() }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.isPlaying) stringResource(R.string.s8d63ef) else stringResource(R.string.sb85270),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .clickable {
                            val next = when (state.sleepMinutes) { 0 -> 30; 30 -> 60; else -> 0 }
                            viewModel.setSleepTimer(next)
                        }
                        .then(
                            if (state.sleepMinutes > 0) {
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            } else {
                                Modifier.size(42.dp)
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (state.sleepMinutes > 0) {
                        Arrangement.Start
                    } else {
                        Arrangement.Center
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = stringResource(R.string.s47cab5),
                        tint = if (state.sleepMinutes > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp),
                    )
                    if (state.sleepMinutes > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.sleep_minutes_fmt, state.sleepMinutes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                // 加号胶囊：添加音乐（与定时胶囊同款样式）
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .clickable { showAddDialog = true }
                        .size(42.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.music_add_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                // 最右侧：正序/逆序切换胶囊
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .clickable { sortDescending = !sortDescending }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (sortDescending) {
                            stringResource(R.string.music_sort_desc)
                        } else {
                            stringResource(R.string.music_sort_asc)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 曲目列表：编号 + 封面 + 标题，当前行高亮（正序/逆序切换；逆序时点击映射回真实索引）
            val displayTracks = if (sortDescending) state.tracks.asReversed() else state.tracks
            displayTracks.forEachIndexed { index, track ->
                val playIndex = if (sortDescending) state.tracks.lastIndex - index else index
                val isCurrent = playIndex == state.currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            else Color.Transparent,
                        )
                        .clickable { viewModel.play(playIndex) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverImage(
                        track = track,
                        accent = accentFor(index),
                        cornerRadius = 12.dp,
                        targetPx = 96,
                        overridePath = state.coverOverrides[track.url],
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else null,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (track.artist.isNotEmpty()) {
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // 给迷你播放条留出空间
            Spacer(Modifier.height(104.dp))
        }

        // ── 底部迷你播放条 ─────────────────────────────────────────
        current?.let { c ->
            // 迷你条磨砂玻璃：录制导航内容 backdrop 高斯模糊 + 半透明底 + 细描边（API 31+，同 BottomNavBar）
            var pillBounds by remember { mutableStateOf(Rect.Zero) }
            var pillSize by remember { mutableStateOf(IntSize.Zero) }
            val pillBlurLayer = rememberGraphicsLayer()
            val pillShape = RoundedCornerShape(percent = 50)
            val pillShadow = if (dark) ShadowDark else ShadowLight
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        pillBounds = it.boundsInRoot()
                        pillSize = it.size
                    }
                    .shadow(6.dp, pillShape, ambientColor = pillShadow, spotColor = pillShadow)
                    .clip(pillShape)
                    .drawBehind {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            pillBlurLayer.record(pillSize) {
                                translate(-pillBounds.left, -pillBounds.top) {
                                    drawLayer(listLayer)
                                }
                            }
                            pillBlurLayer.renderEffect = BlurEffect(24f, 24f, TileMode.Decal)
                            drawLayer(pillBlurLayer)
                        }
                    }
                    .background(if (dark) Color(0x991F1F1F) else Color(0x99FFFFFF))
                    .border(1.dp, MaterialTheme.colorScheme.outline, pillShape)
                    .clickable { showPlayer = true }
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverImage(
                    track = c,
                    accent = accentFor(state.currentIndex),
                    cornerRadius = 12.dp,
                    targetPx = 96,
                    overridePath = state.coverOverrides[c.url],
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = c.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = c.artist + " · " + if (state.isPlaying) stringResource(R.string.sacf254) else stringResource(R.string.sa2d930),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.prev() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.s579321),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiary,
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) stringResource(R.string.s8d63ef) else stringResource(R.string.sb85270),
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.next() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.scfd960),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // ── 全屏播放页（自底部滑入） ──────────────────────────────
        AnimatedVisibility(
            visible = showPlayer,
            enter = slideInVertically(tween(DurState)) { it } + fadeIn(tween(DurState)),
            exit = slideOutVertically(tween(DurState)) { it } + fadeOut(tween(DurState)),
            modifier = Modifier.matchParentSize(),
        ) {
            NowPlayingPanel(
                state = state,
                accent = accentFor(state.currentIndex),
                onClose = { showPlayer = false },
                onFetchCover = viewModel::fetchCover,
                onToggle = viewModel::togglePlayPause,
                onPrev = viewModel::prev,
                onNext = viewModel::next,
                onSeek = viewModel::seekTo,
                onToggleRepeat = viewModel::toggleRepeat,
                onSetSleep = viewModel::setSleepTimer,
            )
        }
    }

    // 添加音乐弹窗（双模式，逻辑对标网页端）
    if (showAddDialog) {
        MusicAddDialog(
            viewModel = viewModel,
            onDismiss = {
                showAddDialog = false
                viewModel.clearResolvedAudio()
            },
        )
    }
}

// 全屏播放页：大封面 + 波形进度 + 居中控制
@Composable
private fun NowPlayingPanel(
    state: MusicUiState,
    accent: Color,
    onClose: () -> Unit,
    onFetchCover: (String, String, (Boolean, String) -> Unit) -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Int) -> Unit,
    onToggleRepeat: () -> Unit,
    onSetSleep: (Int) -> Unit,
) {
    val dark = LocalDarkTheme.current
    val current = state.tracks.getOrNull(state.currentIndex)
    BackHandler(onBack = onClose)

    // 波形拖动中用本地比例显示，松手才真正 seek
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    // 长按封面 → 设置网易云封面弹窗
    var showCoverDialog by remember { mutableStateOf(false) }
    val duration = state.durationMs
    val shownFraction = dragFraction
        ?: if (duration > 0) state.positionMs.toFloat() / duration else 0f
    val enabled = duration > 0 && current != null

    val context = LocalContext.current
    // 毛玻璃背景：封面小图放大（双线性插值天然模糊）+ 渐变遮罩，跟随封面变化
    val bgBitmap: ImageBitmap? = current?.let { c ->
        (state.coverOverrides[c.url] ?: c.cover.takeIf { it.isNotEmpty() })?.let { path ->
            remember(path) { decodeImage(context, path, 24) }
        }
    }
    // 毛玻璃遮罩：封面模糊图上的表面色渐变（浅色暖白 / 深色暖炭）
    val scrimTop = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    val scrimBottom = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)

    // 根节点必须可命中：纯 background 的 Box 不参与 hit test，
    // 点击会穿透到下层曲目列表导致误切歌——加空 clickable 拦截指针
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {},
    ) {
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        // API 31+ 再叠一层高斯模糊，消除小图放大的色块边界
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = BlurEffect(60f, 60f, TileMode.Clamp)
                        }
                    },
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(scrimTop, scrimBottom))),
            )
        } else {
            // 无封面兜底：天空渐变（与全局背景一致），完全遮住列表
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (dark) {
                            Brush.verticalGradient(listOf(DarkGradientTop, DarkGradientMid, DarkGradientBottom))
                        } else {
                            Brush.verticalGradient(listOf(LightGradientTop, LightGradientMid, LightGradientBottom))
                        },
                    ),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
        Spacer(Modifier.height(8.dp))
        // 顶栏：收起 + 标题
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.s7c2e9a),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.sacf254),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(48.dp))
        }

        // 弹性空隙：剩余空间小部分给封面上方，让整体重心下沉
        Spacer(Modifier.weight(0.5f))

        // 大封面（参考图样式：85% 宽居中 + 轻投影）
        if (current != null) {
            CoverImage(
                track = current,
                accent = accent,
                cornerRadius = 28.dp,
                targetPx = 1000,
                overridePath = state.coverOverrides[current.url],
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    // 长按封面：输入网易云链接/ID 获取封面图
                    .pointerInput(current.url) {
                        detectTapGestures(onLongPress = { showCoverDialog = true })
                    },
            )
        } else {
            Spacer(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f),
            )
        }

        Spacer(Modifier.height(28.dp))

        // 歌名 + 副题
        Text(
            text = current?.title ?: stringResource(R.string.s42ce5d),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (current != null) {
                "%s · %02d".format(current.artist, state.currentIndex + 1)
            } else {
                stringResource(R.string.s4e52e0)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // 弹性空隙：剩余空间大部分留给中部，波形与控制行贴底
        Spacer(Modifier.weight(1f))

        // 直线进度条（点击 / 横向拖动 seek）
        LineProgress(
            shownFraction = shownFraction,
            barColor = MaterialTheme.colorScheme.onSurface,
            dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            onTap = { frac -> onSeek((frac * duration).toInt()) },
            onDrag = { frac -> if (enabled) dragFraction = frac },
            onDragEnd = {
                dragFraction?.let { onSeek((it * duration).toInt()) }
                dragFraction = null
            },
        )
        Spacer(Modifier.height(4.dp))
        // 时间行
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatTime(if (dragFraction != null) (shownFraction * duration).toInt() else state.positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatTime(state.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))

        // 控制栏：定时 / 上一首 / 播放暂停 / 下一首 / 循环
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                val next = when (state.sleepMinutes) { 0 -> 30; 30 -> 60; else -> 0 }
                onSetSleep(next)
            }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = stringResource(R.string.s47cab5),
                    tint = if (state.sleepMinutes > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onPrev, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.s579321),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp),
                )
            }
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(76.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (dark) PillBgDark else PillBgLight,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.surface,
                    )
                } else {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) stringResource(R.string.s8d63ef) else stringResource(R.string.sb85270),
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.scfd960),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp),
                )
            }
            IconButton(onClick = onToggleRepeat, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatMode.ALL) Icons.Outlined.Repeat else Icons.Outlined.RepeatOne,
                    contentDescription = if (state.repeatMode == RepeatMode.ALL) stringResource(R.string.s700e98) else stringResource(R.string.s7e91d9),
                    tint = if (state.repeatMode == RepeatMode.ONE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        if (state.sleepMinutes > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.sleep_pause_fmt, state.sleepMinutes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(8.dp))
        }
    }

    // 设置封面弹窗
    if (showCoverDialog) {
        current?.let { c ->
            CoverFetchDialog(
                onDismiss = { showCoverDialog = false },
                onSubmit = { input, onResult -> onFetchCover(c.url, input, onResult) },
            )
        }
    }
}

// 直线进度条：细线圆角端点，已播实色 / 未播淡化；点击或横向拖动 seek
@Composable
private fun LineProgress(
    shownFraction: Float,
    barColor: Color,
    dimColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onTap: (Float) -> Unit = {},
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onTap((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onDrag((offset.x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                ) { change, _ ->
                    onDrag((change.position.x / size.width).coerceIn(0f, 1f))
                    change.consume()
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val y = size.height / 2f
            // 未播底线
            drawLine(
                color = dimColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            // 已播进度线
            drawLine(
                color = barColor,
                start = Offset(0f, y),
                end = Offset(size.width * shownFraction.coerceIn(0f, 1f), y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

// 封面：优先用户自定义封面（本地文件），其次 assets 位图（按 targetPx 采样解码），失败兜底灰渐变
@Composable
private fun CoverImage(
    track: Track?,
    accent: Color,
    cornerRadius: Dp,
    targetPx: Int,
    modifier: Modifier = Modifier,
    overridePath: String? = null,
) {
    val context = LocalContext.current
    val bitmap: ImageBitmap? = (overridePath ?: track?.cover?.takeIf { it.isNotEmpty() })?.let { path ->
        remember(path, targetPx) { decodeImage(context, path, targetPx) }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = track?.title,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.12f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

// 图片采样解码，控制内存：/ 开头 = 本地文件（自定义封面），否则 assets 路径
private fun decodeImage(context: Context, path: String, targetPx: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openImage(context, path).use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetPx) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    openImage(context, path).use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
}.getOrNull()

// 按路径类型打开图片流：本地文件用 FileInputStream，其余视为 assets
private fun openImage(context: Context, path: String) = if (path.startsWith("/")) {
    java.io.FileInputStream(path)
} else {
    context.assets.open(path)
}

// 毫秒 → m:ss
private fun formatTime(ms: Int): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    return "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}"
}

// 长按封面弹窗：输入网易云歌曲链接/ID → 获取封面图并持久化
@Composable
private fun CoverFetchDialog(
    onDismiss: () -> Unit,
    onSubmit: (input: String, onResult: (Boolean, String) -> Unit) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var fetching by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { if (!fetching) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(
                    text = stringResource(R.string.music_cover_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; errorText = "" },
                    placeholder = { Text(stringResource(R.string.music_cover_hint)) },
                    singleLine = true,
                    enabled = !fetching,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorText.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss, enabled = !fetching) {
                        Text(stringResource(R.string.s625fb2))
                    }
                    TextButton(
                        onClick = {
                            fetching = true
                            errorText = ""
                            onSubmit(input) { ok, message ->
                                fetching = false
                                if (ok) {
                                    onDismiss()
                                } else {
                                    errorText = message
                                }
                            }
                        },
                        enabled = !fetching && input.isNotBlank(),
                    ) {
                        if (fetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.music_cover_ok))
                        }
                    }
                }
            }
        }
    }
}

// 添加音乐弹窗：双模式（网易云 id 解析 / 上传音源文件），逻辑对标网页端 music-add-dialog
@Composable
private fun MusicAddDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf("id") } // "id" = 链接解析；"file" = 上传音源
    var busy by remember { mutableStateOf(false) }
    var phaseRes by remember { mutableStateOf(0) } // 进行中阶段文案资源 id
    var resolved by remember { mutableStateOf(false) } // id 模式：已解析并预下载音源
    var resolvedNotice by remember { mutableStateOf("") }
    var fileNotice by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    var idInput by remember { mutableStateOf("") }
    var fileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var fileExt by remember { mutableStateOf("mp3") }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }
    var lyricCount by remember { mutableStateOf(0) }

    // 成功后 2.6s 自动关闭（对齐网页端）
    LaunchedEffect(done) {
        if (done) {
            delay(2600)
            onDismiss()
        }
    }

    // SAF 选择音频文件：读 bytes + 60MB 校验 + 取文件名/扩展名
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException(context.getString(R.string.music_add_err_read_file))
            if (bytes.size > MusicUploader.MAX_AUDIO_SIZE) {
                errorText = context.getString(R.string.music_add_err_too_large)
                return@runCatching
            }
            var name = uri.lastPathSegment ?: "audio"
            runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) c.getString(idx)?.let { name = it }
                    }
                }
            }
            fileBytes = bytes
            fileExt = name.substringAfterLast('.', "").lowercase().ifEmpty { "mp3" }
            fileNotice = context.getString(
                R.string.music_add_file_fmt,
                name,
                "%.1f".format(bytes.size / 1024f / 1024f),
            )
            errorText = ""
        }.onFailure {
            if (errorText.isEmpty()) errorText = context.getString(R.string.music_add_err_read_file)
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                // 标题行 + 关闭
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.music_add_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss, enabled = !busy) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.music_add_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // 模式切换：选中 primary 底白字，未选中灰底
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModePill(
                        text = stringResource(R.string.music_add_mode_id),
                        selected = mode == "id",
                        enabled = !busy,
                        onClick = { mode = "id"; errorText = "" },
                    )
                    ModePill(
                        text = stringResource(R.string.music_add_mode_file),
                        selected = mode == "file",
                        enabled = !busy,
                        onClick = { mode = "file"; errorText = "" },
                    )
                }
                Spacer(Modifier.height(14.dp))

                if (mode == "id") {
                    Text(
                        text = stringResource(R.string.music_add_id_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = idInput,
                        onValueChange = { idInput = it; errorText = "" },
                        placeholder = { Text(stringResource(R.string.music_add_id_hint), style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        enabled = !busy && !resolved,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    )
                    if (!resolved) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                busy = true
                                phaseRes = R.string.music_add_phase_resolving
                                errorText = ""
                                viewModel.resolveId(
                                    idInput.trim(),
                                    onPhase = { phaseRes = it },
                                ) { ok, msg, t, a ->
                                    busy = false
                                    phaseRes = 0
                                    if (ok) {
                                        resolved = true
                                        resolvedNotice = msg
                                        if (title.isBlank()) title = t
                                        if (artist.isBlank()) artist = a
                                    } else {
                                        errorText = msg
                                    }
                                }
                            },
                            enabled = !busy && idInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(stringResource(R.string.music_add_resolve))
                            }
                        }
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = resolvedNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    // file 模式：点击选文件
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            .clickable(enabled = !busy) { filePicker.launch(arrayOf("audio/*")) }
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = fileNotice.ifEmpty { stringResource(R.string.music_add_file_pick) },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (fileNotice.isEmpty()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorText = "" },
                    placeholder = { Text(stringResource(R.string.music_add_title_hint), style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    enabled = !busy && !done,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it; errorText = "" },
                    placeholder = { Text(stringResource(R.string.music_add_artist_hint), style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    enabled = !busy && !done,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                )

                if (errorText.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(16.dp))
                if (done) {
                    // 成功态：对勾 + 提示（2.6s 后自动关闭）
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34A853),
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.music_add_done),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (lyricCount > 0) {
                                stringResource(R.string.music_add_lyrics_fmt, lyricCount)
                            } else {
                                stringResource(R.string.music_add_no_lyrics)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.music_add_deploy_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            // 校验顺序对齐网页端：元信息 → 音源
                            val validationError = when {
                                title.isBlank() || artist.isBlank() -> context.getString(R.string.music_add_err_need_meta)
                                mode == "id" && !resolved -> context.getString(R.string.music_add_err_need_resolve)
                                mode == "file" && fileBytes == null -> context.getString(R.string.music_add_err_need_file)
                                else -> ""
                            }
                            if (validationError.isNotEmpty()) {
                                errorText = validationError
                            } else {
                                busy = true
                                phaseRes = R.string.music_add_phase_lyrics
                                errorText = ""
                                viewModel.submitSong(
                                    if (mode == "id") idInput.trim() else null,
                                    fileBytes,
                                    fileExt,
                                    title.trim(),
                                    artist.trim(),
                                    onPhase = { phaseRes = it },
                                ) { ok, count, errMsg ->
                                    busy = false
                                    phaseRes = 0
                                    if (ok) {
                                        done = true
                                        lyricCount = count
                                    } else {
                                        errorText = errMsg
                                    }
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.music_add_submit))
                        }
                    }
                    if (busy && phaseRes != 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(phaseRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.music_add_foot),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

// 模式切换胶囊：选中 primary 底白字 / 未选中灰底（同操作胶囊风格）
@Composable
private fun ModePill(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                },
            )
            .clickable(enabled = enabled && !selected) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
