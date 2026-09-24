package cn.wangce.lumi.ui.moments

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.R
import cn.wangce.lumi.data.image.ImageStore
import cn.wangce.lumi.data.local.MomentEntity
import cn.wangce.lumi.data.local.MomentImageEntity
import cn.wangce.lumi.ui.components.DangerButton
import cn.wangce.lumi.ui.components.LumiDialog
import cn.wangce.lumi.ui.components.LumiDialogButtons
import cn.wangce.lumi.ui.components.LumiSheet
import cn.wangce.lumi.ui.components.SecondaryButton
import cn.wangce.lumi.ui.components.scaleOnPress
import cn.wangce.lumi.ui.theme.AccentPaper
import cn.wangce.lumi.ui.theme.LocalAuxText
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.ScrimOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 编辑页黑底（对齐「回忆」App 编辑设计）：深黑面板，非纯黑以与列表底区分。
// 仅深色模式使用；浅色模式改走 LumiSheet 默认浅底。
private val SheetBlack = Color(0xFF111111)

// 发布 / 编辑弹层：标题 + 碎碎念裸输入 + 横向图片条带（× 移除）+ 地点行 + 保存胶囊。
// 配色跟随主题：深色模式为沉浸黑底白字，浅色模式为浅底墨字。
// content 首行 = 标题、其余行 = 碎碎念，与列表/详情页的拆分逻辑一致。
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentComposeSheet(
    editing: MomentEntity?,
    editingImages: List<MomentImageEntity>,
    imageStore: ImageStore,
    saving: Boolean,
    onSave: (content: String, location: String, keepImageIds: Set<Long>, newUris: List<Uri>) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    // 主题跟随
    val dark = LocalDarkTheme.current
    val aux = LocalAuxText.current
    val fg = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    val fgSecondary = if (dark) Color.White.copy(alpha = 0.85f) else aux.primary
    val fgTertiary = if (dark) Color.White.copy(alpha = 0.4f) else aux.secondary
    // 输入占位（比辅助灰再淡一档）
    val hint = if (dark) Color.White.copy(alpha = 0.26f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val hairline = if (dark) Color(0x14FFFFFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val tileBg = if (dark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val cursor = if (dark) AccentPaper else MaterialTheme.colorScheme.primary

    val initialLines = editing?.content?.lines().orEmpty()
    var title by remember(editing?.id) { mutableStateOf(initialLines.firstOrNull()?.trim().orEmpty()) }
    var body by remember(editing?.id) { mutableStateOf(initialLines.drop(1).joinToString("\n").trim()) }
    var location by remember(editing?.id) { mutableStateOf(editing?.location ?: "") }
    var keepIds by remember(editing?.id) { mutableStateOf(editingImages.map { it.id }.toSet()) }
    var pickedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val totalCount = keepIds.size + pickedUris.size

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9),
    ) { uris ->
        if (uris.isNotEmpty()) {
            pickedUris = (pickedUris + uris).distinct().take(9)
        }
    }

    LumiSheet(
        onDismissRequest = onDismiss,
        // 深色模式才用沉浸黑面板；浅色模式走统一浅底
        containerColor = if (dark) SheetBlack else null,
        scrollable = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            // 头部：页面标题 + 删除（仅编辑时）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (editing == null) {
                        stringResource(R.string.s2295dc)
                    } else {
                        stringResource(R.string.s202ae3)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = fg,
                )
                Spacer(Modifier.weight(1f))
                if (editing != null && onDelete != null) {
                    val deleteInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 34.dp)
                            .scaleOnPress(deleteInteraction)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tileBg)
                            .clickable(
                                interactionSource = deleteInteraction,
                                indication = null,
                            ) { showDeleteConfirm = true }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.s2f4aad),
                            tint = if (dark) Color.White.copy(alpha = 0.7f) else aux.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // 图片条带：已有图（可移除）+ 新选图 + 添加入口
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(editingImages.filter { it.id in keepIds }, key = { "old_${it.id}" }) { img ->
                    RemovableTile(onRemove = { keepIds = keepIds - img.id }) {
                        PathImage(path = img.imagePath, imageStore = imageStore, modifier = Modifier.fillMaxSize())
                    }
                }
                itemsIndexed(pickedUris, key = { _, uri -> "new_$uri" }) { _, uri ->
                    RemovableTile(onRemove = { pickedUris = pickedUris - uri }) {
                        UriImage(uri = uri, imageStore = imageStore, modifier = Modifier.fillMaxSize())
                    }
                }
                item(key = "add") {
                    AddImageTile(
                        enabled = totalCount < 9 && !saving,
                        dark = dark,
                        onClick = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                }
            }
            if (totalCount > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$totalCount / 9",
                    style = MaterialTheme.typography.labelSmall,
                    color = fgTertiary,
                )
            }

            // 标题（裸输入，大号 = 面板视觉重心）
            Spacer(Modifier.height(18.dp))
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(cursor),
                singleLine = true,
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                text = stringResource(R.string.moment_title_hint),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = hint,
                            )
                        }
                        inner()
                    }
                },
            )

            // 碎碎念（裸输入多行）
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 104.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = fgSecondary,
                ),
                cursorBrush = SolidColor(cursor),
                decorationBox = { inner ->
                    Box {
                        if (body.isEmpty()) {
                            Text(
                                text = stringResource(R.string.s77932d),
                                style = MaterialTheme.typography.bodyLarge,
                                color = hint,
                            )
                        }
                        inner()
                    }
                },
            )

            // 设置行区（hairline 分隔，与参考图的分组/时间/地点三行同构）
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(hairline),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = fgTertiary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = fg,
                    ),
                    cursorBrush = SolidColor(cursor),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (location.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.sd3521e),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = hint,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(hairline),
            )

            // 保存：全宽胶囊（空内容禁用，发布中转 loading）
            Spacer(Modifier.height(24.dp))
            SaveCapsule(
                text = if (editing == null) {
                    stringResource(R.string.s83611a)
                } else {
                    stringResource(R.string.sbe5fbb)
                },
                enabled = (title.isNotBlank() || body.isNotBlank()) && !saving,
                loading = saving,
                dark = dark,
                onClick = {
                    val content = listOf(title.trim(), body.trim())
                        .filter { it.isNotBlank() }
                        .joinToString("\n")
                    onSave(content, location.trim(), keepIds, pickedUris)
                },
            )
        }
    }

    // 删除确认弹窗
    if (showDeleteConfirm) {
        LumiDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = stringResource(R.string.sf3d3df),
            actions = {
                LumiDialogButtons {
                    SecondaryButton(
                        text = stringResource(R.string.s625fb2),
                        onClick = { showDeleteConfirm = false },
                        modifier = Modifier.weight(1f),
                    )
                    DangerButton(
                        text = stringResource(R.string.s2f4aad),
                        onClick = {
                            showDeleteConfirm = false
                            onDelete?.invoke()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        ) {
            Text(
                text = stringResource(R.string.sa695e5),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// 保存胶囊：深色模式=纸白胶囊黑字（黑底对照），浅色模式=墨黑胶囊白字（与 FAB 同语言）
@Composable
private fun SaveCapsule(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
) {
    val capsuleBg = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    val onCapsule = if (dark) Color.Black else Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(capsuleBg.copy(alpha = if (enabled) 1f else 0.22f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = onCapsule,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = onCapsule,
            )
        }
    }
}

// 可移除图块：右上角半透明小圆叉
@Composable
private fun RemovableTile(
    onRemove: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.size(76.dp)) {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(ScrimOverlay)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.s833152),
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

// 相册 uri 缩略图（图片尚未入库，按 uri 子采样加载）
@Composable
private fun UriImage(uri: Uri, imageStore: ImageStore, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { imageStore.loadBitmapFromUri(uri, 512) }
    }
    val dark = LocalDarkTheme.current
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
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
                    if (dark) Color.White.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                ),
        )
    }
}

// 添加图片入口：淡色圆角方块（达到 9 张上限后禁用）
@Composable
private fun AddImageTile(enabled: Boolean, dark: Boolean, onClick: () -> Unit) {
    val tileBg = if (dark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    Box(
        modifier = Modifier
            .width(76.dp)
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tileBg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = stringResource(R.string.sb89fb3),
            tint = if (dark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}
