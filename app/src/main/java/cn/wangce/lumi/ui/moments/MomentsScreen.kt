package cn.wangce.lumi.ui.moments

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.image.ImageStore
import cn.wangce.lumi.data.local.MomentEntity
import cn.wangce.lumi.data.local.MomentImageEntity
import cn.wangce.lumi.ui.components.AppFab
import cn.wangce.lumi.ui.components.EmptyState
import cn.wangce.lumi.ui.components.SwipeToDeleteRow
import cn.wangce.lumi.ui.components.bottomNavSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import cn.wangce.lumi.ui.theme.LocalAuxText
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.DurState

// 回忆页（对标「回忆」App 设计）：时间线列表，左侧大日期数字 + 中缩略图 + 右标题/碎碎念。
// 配色跟随主题：浅色模式为浅底（透出根层天空渐变，与其他浅色页同语言），
// 深色模式才用沉浸黑底（#111111，比全局深底更沉一档）。
private val MomentsBgDark = Color(0xFF111111)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MomentsScreen(
    onOpenDetail: (Long, Int) -> Unit,
    viewModel: MomentsViewModel = hiltViewModel(),
) {
    val moments by viewModel.moments.collectAsStateWithLifecycle()
    val images by viewModel.images.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val imageStore = viewModel.imageStore

    // 主题跟随：浅色模式走浅底（透出根层天空渐变），深色模式才用沉浸黑底
    val dark = LocalDarkTheme.current
    val fg = if (dark) Color.White else MaterialTheme.colorScheme.onSurface

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCompose by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MomentEntity?>(null) }

    // 非组合 lambda（点击回调）里不能调 stringResource，先在组合期取好
    val deletedMsg = stringResource(R.string.sd64fda)
    val undoLabel = stringResource(R.string.sbd9fcf)

    fun requestDelete(moment: MomentEntity) {
        viewModel.softDelete(moment)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = deletedMsg,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 浅色模式不铺底色，让根层天空渐变透上来（与其他浅色页一致）；深色模式才铺沉浸黑
            .then(if (dark) Modifier.background(MomentsBgDark) else Modifier)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            // 页头：大标题 + 计数副标（一页一重心，标题即最大字）
            Text(
                text = stringResource(R.string.s8e5e86),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = fg,
            )
            Spacer(Modifier.height(14.dp))
            // 细分隔线：页头与列表之间的呼吸
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(fg.copy(alpha = 0.08f)),
            )
            Spacer(Modifier.height(6.dp))

            if (moments.isEmpty()) {
                // 空态走全局组件：颜色跟随主题（浅色墨字/深色白字），留白纪律与渐入统一
                EmptyState(
                    title = stringResource(R.string.s0cd71b),
                    description = stringResource(R.string.s33930e),
                    icon = Icons.Outlined.PhotoLibrary,
                    modifier = Modifier.weight(1f),
                )
            } else {
                // 时间线列表：新的在前，每行 = 日期列 + 缩略图 + 标题/碎碎念
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(moments, key = { it.id }) { moment ->
                        SwipeToDeleteRow(onDelete = { requestDelete(moment) }, cornerRadius = 14) {
                            val paths = images[moment.id].orEmpty().map { it.imagePath }
                            MomentRow(
                                moment = moment,
                                momentImages = images[moment.id].orEmpty(),
                                imageStore = imageStore,
                                onClick = {
                                    if (paths.isNotEmpty()) {
                                        onOpenDetail(moment.id, 0)
                                    } else {
                                        editing = moment
                                        showCompose = true
                                    }
                                },
                                onLongClick = {
                                    editing = moment
                                    showCompose = true
                                },
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(bottomNavSpace()))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomNavSpace()),
        ) { data ->
            Snackbar(snackbarData = data)
        }

        // 发布按钮：浅色=墨黑锚点，深色=纸白反色（与底栏中央锚点同语言）
        AppFab(
            onClick = {
                editing = null
                showCompose = true
            },
            darkSurface = dark,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                // Snackbar 出现时上移避让，防止遮住「撤销」按钮
                .padding(end = 20.dp, bottom = bottomNavSpace()),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.s2295dc),
                tint = LocalContentColor.current,
            )
        }
    }

    // 发布 / 编辑弹层（黑底皮肤）
    if (showCompose) {
        MomentComposeSheet(
            editing = editing,
            editingImages = images[editing?.id].orEmpty(),
            imageStore = imageStore,
            saving = saving,
            onSave = { content, location, keepIds, uris ->
                viewModel.saveMoment(editing, content, location, keepIds, uris) {
                    showCompose = false
                    editing = null
                }
            },
            onDelete = editing?.let { moment ->
                {
                    viewModel.softDelete(moment)
                    showCompose = false
                    editing = null
                }
            },
            onDismiss = {
                showCompose = false
                editing = null
            },
        )
    }

}

// 时间线行：左日期列（大号「日」+ 年/月）→ 缩略图（首图 64dp）→ 标题 + 碎碎念 + 地点
// 点击看图/编辑，长按编辑，删除走左滑
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MomentRow(
    moment: MomentEntity,
    momentImages: List<MomentImageEntity>,
    imageStore: ImageStore,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(DurState),
        label = "momentRowScale",
    )

    // 主题跟随：深色沉浸页用白字梯度，浅色页用墨字 + 双档辅助灰
    val dark = LocalDarkTheme.current
    val aux = LocalAuxText.current
    val fg = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    val fgSecondary = if (dark) Color.White.copy(alpha = 0.42f) else aux.primary
    val fgTertiary = if (dark) Color.White.copy(alpha = 0.32f) else aux.secondary
    val tileBg = if (dark) Color.White.copy(alpha = 0.06f) else fg.copy(alpha = 0.05f)

    val calendar = remember(moment.createdAt) {
        Calendar.getInstance().apply { timeInMillis = moment.createdAt }
    }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val monthLabel = "${calendar.get(Calendar.YEAR)}/${calendar.get(Calendar.MONTH) + 1}"

    // 标题 = 正文首行；其余行作碎碎念摘要
    val lines = moment.content.lines()
    val title = lines.firstOrNull().orEmpty().trim()
    val snippet = lines.drop(1).joinToString("\n").trim()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左：日期列（大号「日」= 屏内第二视觉重心）
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(56.dp),
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = fg,
            )
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.labelSmall,
                color = fgTertiary,
            )
        }
        Spacer(Modifier.width(16.dp))
        // 中：缩略图（无图时用淡色占位块）
        if (momentImages.isNotEmpty()) {
            PathImage(
                path = momentImages.first().imagePath,
                imageStore = imageStore,
                modifier = Modifier.size(72.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tileBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = fgTertiary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        // 右：标题 + 碎碎念 + 地点
        Column(modifier = Modifier.weight(1f)) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = fg,
                )
            }
            if (snippet.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = fgSecondary,
                )
            }
            if (moment.location.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = fgTertiary,
                        modifier = Modifier.size(11.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = moment.location,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = fgTertiary,
                    )
                }
            }
        }
    }
}

// 本地图片：子采样异步解码 + 圆角裁剪，可选点击
@Composable
internal fun PathImage(
    path: String,
    imageStore: ImageStore,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { imageStore.loadBitmap(path, 512) }
    }
    // 仅在传入 onClick 时才挂 clickable：enabled=false 的 clickable 仍会拦截
    // 父级 combinedClickable 的 tap（卡片图片区域点不动的问题根因）
    // 占位底色跟随主题：深色（沉浸黑页/全局深色）用淡白，浅色用淡墨
    val dark = LocalDarkTheme.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick?.invoke() }
                } else {
                    Modifier
                },
            ),
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } ?: Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (dark) Color.White.copy(alpha = 0.07f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                ),
        )
    }
}
