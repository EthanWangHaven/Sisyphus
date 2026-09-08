package cn.wangce.lumi.ui.notes

// 备忘录编辑二级页：停止输入 1 秒自动保存；返回/完成前强制保存
// 支持分类标签（自定义输入 + 已有标签点选）与图片（最多 9 张，选图后自动保存入库）

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.image.ImageStore
import cn.wangce.lumi.ui.moments.PathImage
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.PillBgDark
import cn.wangce.lumi.ui.theme.PillBgLight
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val MAX_NOTE_IMAGES = 9

// 备忘录编辑二级页
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Composable
fun NoteEditScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var draftId by remember { mutableStateOf(noteId) }
    var createdAt by remember { mutableStateOf(0L) }
    var loaded by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }
    var lastSavedAt by remember { mutableStateOf(0L) }

    // 图片：已入库保留的 id + 新选未入库的 uri
    var keepIds by remember { mutableStateOf(emptySet<Long>()) }
    var initialImageIds by remember { mutableStateOf(emptySet<Long>()) }
    var pickedUris by remember { mutableStateOf(emptyList<Uri>()) }
    var existingTags by remember { mutableStateOf(emptyList<String>()) }

    val scope = rememberCoroutineScope()
    val imageStore = viewModel.imageStore
    val dark = LocalDarkTheme.current
    val imagesMap by viewModel.images.collectAsStateWithLifecycle()

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_NOTE_IMAGES),
    ) { uris ->
        if (uris.isNotEmpty()) {
            pickedUris = (pickedUris + uris).distinct().take(MAX_NOTE_IMAGES)
        }
    }

    // 串行化保存：退出保存需等待自动保存完成并拿到最新 draftId
    val saveMutex = remember { Mutex() }
    suspend fun persist(): Long = saveMutex.withLock {
        // 有图片（新选或已保留）时空文本也建条目；移除过图片也强制保存以落库删除
        val force = pickedUris.isNotEmpty() || keepIds != initialImageIds
        val savedId = viewModel.saveNote(title, content, tag, draftId, createdAt, force)
        if (savedId > 0) {
            draftId = savedId
            // 图片落库：删除未保留 + 压缩入库新图；回填 keepIds、清空待选防重复插入
            val imgs = viewModel.saveNoteImages(savedId, keepIds, pickedUris)
            keepIds = imgs.map { it.id }.toSet()
            pickedUris = emptyList()
            lastSavedAt = System.currentTimeMillis()
        }
        savedId
    }

    fun exitNow() {
        if (exiting) return
        exiting = true
        scope.launch {
            persist()
            onBack()
        }
    }

    // 加载既有笔记（含图片与已有标签）；新建（noteId=0）无需加载
    LaunchedEffect(noteId) {
        if (noteId > 0) {
            viewModel.getNote(noteId)?.let { note ->
                title = note.title
                content = note.content
                tag = note.tag
                draftId = note.id
                createdAt = note.createdAt
            }
            val imgs = viewModel.getImagesByNote(noteId)
            keepIds = imgs.map { it.id }.toSet()
            initialImageIds = keepIds
        }
        existingTags = viewModel.existingTags()
        loaded = true
    }

    // 自动保存：标题/正文/标签变化停止 1 秒后写入；跳过初始值的首次发射
    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect
        snapshotFlow { Triple(title, content, tag) }
            .drop(1)
            .debounce(1_000)
            .collectLatest {
                persist()
            }
    }

    // 图片变化（新选/移除）800ms 后自动落库
    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect
        snapshotFlow { Pair(pickedUris, keepIds) }
            .drop(1)
            .debounce(800)
            .collectLatest {
                persist()
            }
    }

    BackHandler { exitNow() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // safeDrawing 排除 ime 后处理状态栏/导航条；ime 单独 padding，避免键盘顶起时与导航条留白叠加
            .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime))
            .imePadding() // 键盘弹出时顶起整页内容，正文不被输入法遮挡（用户要求修复）
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { exitNow() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.s5f4112),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (lastSavedAt > 0) {
                    stringResource(
                        R.string.saved_at_fmt,
                        formatRelativeTime(LocalContext.current, lastSavedAt),
                    )
                } else {
                    ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            TextButton(onClick = { exitNow() }) {
                Text(
                    text = stringResource(R.string.s769d88),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            // 提前捕获视口最大高度（嵌套作用域内无法直接访问 BoxWithConstraintsScope.maxHeight）
            val viewportMaxHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(8.dp))

                // 标题
                Box {
                    if (title.isEmpty()) {
                        Text(
                            text = stringResource(R.string.s32c65d),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 标签行：悬浮半透明胶囊（用户要求），Label 图标 + 自定义输入；下方已有标签建议 chips（点选切换）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = if (dark) ShadowDark else ShadowLight,
                            spotColor = if (dark) ShadowDark else ShadowLight,
                        )
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(50),
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (tag.isEmpty()) {
                            Text(
                                text = stringResource(R.string.saae763),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        BasicTextField(
                            value = tag,
                            onValueChange = { tag = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                        )
                    }
                }
                if (existingTags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(existingTags, key = { it }) { suggestion ->
                            val active = tag == suggestion
                            val chipBg = if (active) {
                                if (dark) PillBgDark else PillBgLight
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                            val chipFg = if (active) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .clickable { tag = if (active) "" else suggestion }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = chipFg,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 正文：最小高度占满剩余视口（BoxWithConstraints 提供 maxHeight），内容超出时页面整体滚动
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (content.isEmpty()) {
                        Text(
                            text = stringResource(R.string.s9abf6a),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = viewportMaxHeight - 220.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 0.8f, // 行距减半，光标≈字高（用户要求）
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    )
                }

                // 图片行：仅有图时显示（无图时不留空白图层，用户要求）
                if (keepIds.isNotEmpty() || pickedUris.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(keepIds.toList(), key = { "old_$it" }) { imageId ->
                            val path = imagesMap[draftId]?.firstOrNull { it.id == imageId }?.imagePath
                            NoteRemovableTile(onRemove = { keepIds = keepIds - imageId }) {
                                if (path != null) {
                                    PathImage(path = path, imageStore = imageStore, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                        itemsIndexed(pickedUris, key = { _, uri -> "new_$uri" }) { _, uri ->
                            NoteRemovableTile(onRemove = { pickedUris = pickedUris - uri }) {
                                NoteUriImage(uri = uri, imageStore = imageStore, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
            // 添加图片悬浮小按钮：固定悬浮于右上角，不随内容滚动
            NoteAddImageButton(
                enabled = keepIds.size + pickedUris.size < MAX_NOTE_IMAGES,
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = {
                    pickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        }
    }
}

// 可移除图块：右上角半透明小圆叉
@Composable
private fun NoteRemovableTile(
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
private fun NoteUriImage(uri: Uri, imageStore: ImageStore, modifier: Modifier = Modifier) {
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

// 添加图片悬浮小按钮：40dp 半透明圆钮，悬浮于正文图层上方（达到 9 张上限后禁用）
@Composable
private fun NoteAddImageButton(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val dark = LocalDarkTheme.current
    val shadowColor = if (dark) ShadowDark else ShadowLight
    Box(
        modifier = modifier
            .padding(top = 2.dp, end = 2.dp)
            .size(48.dp)
            .shadow(3.dp, CircleShape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = stringResource(R.string.sb89fb3),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}
