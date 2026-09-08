package cn.wangce.lumi.ui.moments

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.image.ImageStore
import cn.wangce.lumi.data.local.MomentEntity
import cn.wangce.lumi.data.local.MomentImageEntity
import cn.wangce.lumi.ui.components.EmptyState
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.SwipeToDeleteRow
import cn.wangce.lumi.ui.components.bottomNavSpace
import cn.wangce.lumi.ui.components.pressScale
import cn.wangce.lumi.ui.notes.formatRelativeTime
import cn.wangce.lumi.ui.theme.DurState
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.PillBgDark
import cn.wangce.lumi.ui.theme.PillBgLight
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontWeight

// App 内强制深色只作用于 Compose 主题层（不改 Activity uiMode），
// 以 LumiTheme 提供的 LocalDarkTheme 为准
@Composable
internal fun isDarkTheme(): Boolean = LocalDarkTheme.current

// 瞬间页：小红书式两列瀑布流（单图封面卡）+ FAB 发布 + 左滑删除 + 点开横滑看全部图片
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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCompose by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MomentEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<MomentEntity?>(null) }

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
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.s8e5e86),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))

            if (moments.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.s0cd71b),
                    description = stringResource(R.string.s33930e),
                    modifier = Modifier.weight(1f),
                )
            } else {
                // 两列瀑布流：新的在前，卡片高度随内容自然错落
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    verticalItemSpacing = 12.dp,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(moments, key = { it.id }) { moment ->
                        SwipeToDeleteRow(onDelete = { requestDelete(moment) }) {
                            val paths = images[moment.id].orEmpty().map { it.imagePath }
                            MomentCard(
                                moment = moment,
                                momentImages = images[moment.id].orEmpty(),
                                imageStore = imageStore,
                                onClick = {
                                    if (paths.isNotEmpty()) {
                                        // 点进详情：上方大图横滑 + 下方地点/文字（小红书式，独立导航页）
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
                    item(span = StaggeredGridItemSpan.FullLine) {
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

        // 发布按钮：56dp 圆形 + 轻抬升暖阴影 + spring 按压缩放 0.98
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                // Snackbar 出现时上移避让，防止遮住「撤销」按钮
                .offset(y = if (snackbarHostState.currentSnackbarData != null) -(64).dp else 0.dp)
                .padding(end = 20.dp, bottom = bottomNavSpace())
                .shadow(
                    10.dp,
                    CircleShape,
                    ambientColor = if (LocalDarkTheme.current) ShadowDark else ShadowLight,
                    spotColor = if (LocalDarkTheme.current) ShadowDark else ShadowLight,
                )
                .clip(CircleShape)
                // 主操作黑胶囊：浅色黑底 / 深色浅底
                .background(if (LocalDarkTheme.current) PillBgDark else PillBgLight)
                .size(56.dp)
                .pressScale(onPress = {
                    editing = null
                    showCompose = true
                }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.s2295dc),
                tint = MaterialTheme.colorScheme.surface,
            )
        }
    }

    // 发布 / 编辑弹层
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

    // 长按删除确认：半透明磨砂弹窗 + 豆沙红填充胶囊（低饱和不刺眼）
    pendingDelete?.let { moment ->
        Dialog(onDismissRequest = { pendingDelete = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(24.dp),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.sf3d3df),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sa695e5),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 取消：纯文字按钮
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { pendingDelete = null }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.s625fb2),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        // 删除：语义红填充胶囊，轻抬升
                        Box(
                            modifier = Modifier
                                .shadow(
                                    4.dp,
                                    RoundedCornerShape(12.dp),
                                    ambientColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error)
                                .clickable {
                                    pendingDelete = null
                                    requestDelete(moment)
                                }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.s2f4aad),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                }
            }
        }
    }

}

// 瀑布流卡片（小红书式）：单图封面（首张，3:4）→ 文字摘要 → meta 行（地点 + 相对时间）
// 点击看图/编辑，长按编辑，删除走左滑
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MomentCard(
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
        label = "momentScale",
    )
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        cornerRadius = 16,
        elevation = 4,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            momentImages.firstOrNull()?.let { cover ->
                Box {
                    PathImage(
                        path = cover.imagePath,
                        imageStore = imageStore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f),
                    )
                    // 多图角标：提示点开可横滑查看其余图片
                    if (momentImages.size > 1) {
                        Text(
                            text = stringResource(R.string.photo_count_fmt, momentImages.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x66000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (moment.content.isNotBlank()) {
                    Text(
                        text = moment.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                // meta 行：地点 + 相对时间（小字弱化）
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (moment.location.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            modifier = Modifier.size(11.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = moment.location,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = formatRelativeTime(LocalContext.current, moment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}
