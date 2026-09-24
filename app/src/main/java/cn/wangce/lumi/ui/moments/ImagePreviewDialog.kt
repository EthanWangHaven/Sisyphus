package cn.wangce.lumi.ui.moments

import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.image.ImageStore
import cn.wangce.lumi.ui.theme.LocalAuxText
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 回忆详情（参考「回忆」App 设计）：大标题 + 日期星期 + 地点 + 碎碎念 + 双列图网格。
// 配色跟随主题：深色模式沉浸黑底白字，浅色模式浅底墨字。
// 点图进入全屏横滑查看器（双击/双指缩放保留，看图场景恒定黑底）；采用导航路由而非 Dialog
//（模拟器上 Dialog 窗口会被测量出超屏高度）。
@Composable
fun MomentDetailScreen(
    momentId: Long,
    initialIndex: Int,
    onBack: () -> Unit,
    viewModel: MomentsViewModel = hiltViewModel(),
) {
    // 主题跟随：深色沉浸黑，浅色透出根层天空渐变
    val dark = LocalDarkTheme.current
    val aux = LocalAuxText.current
    val fg = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    val fgSecondary = if (dark) Color.White.copy(alpha = 0.85f) else aux.primary
    val fgTertiary = if (dark) Color.White.copy(alpha = 0.45f) else aux.secondary
    val pageBg = if (dark) Color(0xFF111111) else Color.Transparent

    val moments by viewModel.moments.collectAsStateWithLifecycle()
    val images by viewModel.images.collectAsStateWithLifecycle()
    val imageStore = viewModel.imageStore
    val moment = moments.firstOrNull { it.id == momentId }
    val paths = images[momentId].orEmpty().map { it.imagePath }

    // 兜底：1s 内数据流仍未给出该瞬间（被删除等异常），返回列表页
    LaunchedEffect(Unit) {
        withTimeoutOrNull(1000) {
            viewModel.moments.first { list -> list.any { it.id == momentId } }
        } ?: onBack()
    }

    // 全屏查看器页码（null = 详情网格视图）；查看器内系统返回先退查看器
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    BackHandler(enabled = viewerIndex != null) { viewerIndex = null }

    if (moment == null) {
        // 数据流首帧未到达：先给等待帧（跟随主题）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (dark) Color(0xFF111111) else Color.Transparent),
        )
        return
    }

    // 标题 = 正文首行；其余行 = 碎碎念（与列表页拆分逻辑一致）
    val lines = moment.content.lines()
    val title = lines.firstOrNull().orEmpty().trim()
    val body = lines.drop(1).joinToString("\n").trim()

    // 本地化完整日期 + 星期（zh：2026年8月24日 星期一 / en：Sunday, August 24, 2026）
    val dateText = remember(moment.createdAt) {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yMMMMEEEEd")
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(moment.createdAt))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 浅色模式透出根层天空渐变；深色模式铺沉浸黑
            .background(pageBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            // 返回按钮
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = if (dark) Color.White.copy(alpha = 0.85f) else fg,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) Color.White.copy(alpha = 0.95f) else fg,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = fgTertiary,
            )
            if (moment.location.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = fgTertiary,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = moment.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = fgTertiary,
                    )
                }
            }
            if (body.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = fgSecondary,
                )
            }
            if (paths.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                // 双列图网格：每行 2 张，奇数张时末行右侧留白
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    paths.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { path ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.92f),
                                ) {
                                    PathImage(
                                        path = path,
                                        imageStore = imageStore,
                                        modifier = Modifier.fillMaxSize(),
                                        onClick = { viewerIndex = paths.indexOf(path) },
                                    )
                                }
                            }
                            if (row.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // 全屏查看器：大图横滑 + 页码 + 关闭
        viewerIndex?.let { start ->
            val pagerState = rememberPagerState(
                initialPage = start.coerceAtLeast(0),
            ) { paths.size }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    ZoomableImage(path = paths[page], imageStore = imageStore)
                }
                Text(
                    text = "${pagerState.currentPage + 1} / ${paths.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .statusBarsPadding()
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                )
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.s2f3a89),
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .statusBarsPadding()
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 20.dp)
                        .clickable { viewerIndex = null },
                )
            }
        }
    }
}

// 单页可缩放图：双击 1x/2.5x 切换，双指捏合 1-4 倍
@Composable
private fun ZoomableImage(path: String, imageStore: ImageStore) {
    val bitmap by produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { imageStore.loadBitmap(path, 2048) }
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(path) {
                // 自定义手势分发：双指捏合缩放 / 放大后单指平移时才消费事件，
                // 未放大的单指滑动不消费——让 HorizontalPager 正常横滑翻页
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        if (pressedCount > 1 || scale > 1f) {
                            if (zoomChange != 1f) {
                                scale = (scale * zoomChange).coerceIn(1f, 4f)
                            }
                            if (scale > 1f) {
                                offset += panChange
                            } else {
                                offset = Offset.Zero
                            }
                            event.changes.forEach { if (it.pressed) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(path) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            },
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
    }
}
