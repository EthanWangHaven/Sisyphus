package cn.wangce.lumi.ui.moments

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.R
import cn.wangce.lumi.data.image.ImageStore
import cn.wangce.lumi.data.local.MomentEntity
import cn.wangce.lumi.data.local.MomentImageEntity
import cn.wangce.lumi.ui.theme.DarkSheetBg
import cn.wangce.lumi.ui.theme.LightSheetBg
import cn.wangce.lumi.ui.theme.PillBgDark
import cn.wangce.lumi.ui.theme.PillBgLight
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 发布 / 编辑弹层：多行随笔 + 地点（可选） + 图片选择（最多 9 张）+ 空内容禁发布
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
    var content by remember(editing?.id) { mutableStateOf(editing?.content ?: "") }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme()) DarkSheetBg else LightSheetBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
        ) {
            // 标题 + 发布/保存
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (editing == null) {
                        stringResource(R.string.s2295dc)
                    } else {
                        stringResource(R.string.s202ae3)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                // 删除按钮：仅编辑时显示，位于保存按钮左侧
                if (editing != null && onDelete != null) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .clickable { showDeleteConfirm = true }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.s2f4aad),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                // 主操作黑胶囊（浅色黑底白字 / 深色浅底黑字），禁用时降透明度
                val pillBase = if (isDarkTheme()) PillBgDark else PillBgLight
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 34.dp)
                        // 胶囊发布按钮：轻微抬升阴影（暖阴影 token）
                        .shadow(
                            6.dp,
                            RoundedCornerShape(12.dp),
                            ambientColor = if (isDarkTheme()) ShadowDark else ShadowLight,
                            spotColor = if (isDarkTheme()) ShadowDark else ShadowLight,
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (content.isNotBlank() && !saving) {
                                pillBase
                            } else {
                                pillBase.copy(alpha = 0.35f)
                            },
                        )
                        .clickable(enabled = content.isNotBlank() && !saving) {
                            onSave(content.trim(), location.trim(), keepIds, pickedUris)
                        }
                        .padding(horizontal = 20.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                        )
                    } else {
                        Text(
                            text = if (editing == null) {
                                stringResource(R.string.s83611a)
                            } else {
                                stringResource(R.string.sbe5fbb)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                }
            }

            // 多行随笔输入
            Spacer(Modifier.height(16.dp))
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    val dark = isDarkTheme()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (dark) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                            )
                            // 主题描边勾勒轮廓（浅色暖墨淡边 / 深色暖白淡边）
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        if (content.isEmpty()) {
                            Text(
                                text = stringResource(R.string.s77932d),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )

            // 地点输入（可选）：LocationOn 图标 + 单行输入，样式与正文输入框一致
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDarkTheme()) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (location.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.sd3521e),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
            }

            // 图片行：已有图（可移除）+ 新选图 + 添加入口
            Spacer(Modifier.height(14.dp))
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // 删除确认弹窗
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.sf3d3df),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.sa695e5),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete?.invoke()
                }) {
                    Text(
                        text = stringResource(R.string.s2f4aad),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = stringResource(R.string.s625fb2))
                }
            },
        )
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
                .background(Color(0x99000000))
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

// 添加图片入口：虚线感浅底方块（达到 9 张上限后禁用）
@Composable
private fun AddImageTile(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(76.dp)
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = stringResource(R.string.sb89fb3),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}
