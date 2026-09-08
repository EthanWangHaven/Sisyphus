package cn.wangce.lumi.ui.notes

// 备忘录和待办页：顶部分类 chips + 两列瀑布流笔记卡片 / 待办（快速新增 + 筛选 + 列表）
// 底部居中「笔记/待办」分段切换；黄色 FAB 在笔记 tab 新建笔记

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.image.ImageStore
import cn.wangce.lumi.data.local.NoteEntity
import cn.wangce.lumi.data.local.TodoEntity
import cn.wangce.lumi.ui.components.EmptyState
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.SwipeToDeleteRow
import cn.wangce.lumi.ui.components.bottomNavSpace
import cn.wangce.lumi.ui.components.pressScale
import cn.wangce.lumi.ui.moments.PathImage
import cn.wangce.lumi.ui.tasks.FilterTabs
import cn.wangce.lumi.ui.tasks.TodoDeleteDialog
import cn.wangce.lumi.ui.tasks.TodoEditSheet
import cn.wangce.lumi.ui.tasks.TodoRow
import cn.wangce.lumi.ui.tasks.TasksViewModel
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.MorandiPink
import cn.wangce.lumi.ui.theme.PillBgDark
import cn.wangce.lumi.ui.theme.PillBgLight
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight

// 底部分段切换的页面类型
private enum class NotesTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    NOTES(R.string.s7051dc, Icons.AutoMirrored.Filled.Notes),
    TODOS(R.string.sb60ec8, Icons.Outlined.TaskAlt),
}

// 备忘录和待办页
@Composable
fun NotesScreen(
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    notesViewModel: NotesViewModel = hiltViewModel(),
    tasksViewModel: TasksViewModel = hiltViewModel(),
) {
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()
    val query by notesViewModel.query.collectAsStateWithLifecycle()
    val selectedTag by notesViewModel.selectedTag.collectAsStateWithLifecycle()
    val tags by notesViewModel.tags.collectAsStateWithLifecycle()
    val noteImages by notesViewModel.images.collectAsStateWithLifecycle()

    val todos by tasksViewModel.todos.collectAsStateWithLifecycle()
    val todoFilter by tasksViewModel.filter.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(NotesTab.NOTES) }
    var pendingDeleteNote by remember { mutableStateOf<NoteEntity?>(null) }
    var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }
    var pendingDeleteTodo by remember { mutableStateOf<TodoEntity?>(null) }
    var showAddTodo by remember { mutableStateOf(false) }

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
            NotesTabSwitcher(
                current = tab,
                onSelect = { tab = it },
                compact = true,
            )
            Spacer(Modifier.height(12.dp))
            SearchField(
                query = query,
                onQueryChange = notesViewModel::setQuery,
                // 待办 tab 显示"搜索待办"，笔记 tab 显示"搜索笔记"
                hint = stringResource(if (tab == NotesTab.TODOS) R.string.s5b27c9 else R.string.sbcd47c),
            )
            Spacer(Modifier.height(12.dp))

            if (tab == NotesTab.NOTES) {
                // 分类标签 chips：全部 + 已有标签
                TagChipsRow(
                    tags = tags,
                    selected = selectedTag,
                    onSelect = notesViewModel::selectTag,
                )
                Spacer(Modifier.height(12.dp))

                if (notes.isEmpty()) {
                    EmptyState(
                        title = if (selectedTag.isEmpty() && query.isBlank()) {
                            stringResource(R.string.s7bda38)
                        } else {
                            stringResource(R.string.s7c5b9b)
                        },
                        description = if (selectedTag.isEmpty() && query.isBlank()) {
                            stringResource(R.string.s9930eb)
                        } else {
                            stringResource(R.string.sb202c5)
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(notes, key = { it.id }) { note ->
                            SwipeToDeleteRow(onDelete = { notesViewModel.deleteNote(note.id) }) {
                                NoteCard(
                                    note = note,
                                    coverPath = noteImages[note.id]?.firstOrNull()?.imagePath,
                                    imageStore = notesViewModel.imageStore,
                                    onClick = { onOpenNote(note.id) },
                                    onLongClick = { pendingDeleteNote = note },
                                )
                            }
                        }
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Spacer(Modifier.height(bottomNavSpace()))
                        }
                    }
                }
            } else {
                // 待办 tab：筛选 + 列表（添加走右下角加号弹窗）
                FilterTabs(current = todoFilter, onSelect = tasksViewModel::setFilter)
                Spacer(Modifier.height(12.dp))

                if (todos.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.s48b826),
                        description = stringResource(R.string.s714675),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(todos, key = { it.id }) { todo ->
                            SwipeToDeleteRow(onDelete = { tasksViewModel.deleteTodo(todo.id) }) {
                                TodoRow(
                                    todo = todo,
                                    onToggle = { tasksViewModel.toggleTodo(todo) },
                                    onEdit = { editingTodo = todo },
                                    onRequestDelete = { pendingDeleteTodo = todo },
                                )
                            }
                        }
                        item {
                            Spacer(Modifier.height(bottomNavSpace()))
                        }
                    }
                }
            }
        }

        // 新建主按钮：56dp 正圆黑白胶囊，两 tab 共用（笔记 tab 新建笔记，待办 tab 弹出添加弹窗，需求新3/新6）
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = bottomNavSpace())
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = if (LocalDarkTheme.current) ShadowDark else ShadowLight,
                    spotColor = if (LocalDarkTheme.current) ShadowDark else ShadowLight,
                )
                .clip(CircleShape)
                .background(if (LocalDarkTheme.current) PillBgDark else PillBgLight)
                .size(56.dp)
                .pressScale(onPress = {
                    if (tab == NotesTab.NOTES) onNewNote() else showAddTodo = true
                }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.s3147cb),
                tint = MaterialTheme.colorScheme.surface,
            )
        }

    }

    pendingDeleteNote?.let { note ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDeleteNote = null },
            title = { Text(stringResource(R.string.s847dcf)) },
            text = { Text(stringResource(R.string.del_confirm, note.title.ifBlank { stringResource(R.string.s44a77d) })) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    notesViewModel.deleteNote(note.id)
                    pendingDeleteNote = null
                }) {
                    Text(stringResource(R.string.s2f4aad), color = MorandiPink)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDeleteNote = null }) {
                    Text(stringResource(R.string.s625fb2))
                }
            },
        )
    }

    editingTodo?.let { todo ->
        TodoEditSheet(
            todo = todo,
            onSave = { newTitle ->
                tasksViewModel.updateTodo(todo, newTitle)
                editingTodo = null
            },
            onDismiss = { editingTodo = null },
        )
    }

    pendingDeleteTodo?.let { todo ->
        TodoDeleteDialog(
            todo = todo,
            onConfirm = {
                tasksViewModel.deleteTodo(todo.id)
                pendingDeleteTodo = null
            },
            onDismiss = { pendingDeleteTodo = null },
        )
    }

    // 添加待办弹窗：屏幕中央弹出（需求新6）
    if (showAddTodo) {
        AddTodoDialog(
            onAdd = { title ->
                tasksViewModel.addTodo(title)
                showAddTodo = false
            },
            onDismiss = { showAddTodo = false },
        )
    }
}

// 添加待办弹窗：中央圆角卡片 + 输入框 + 取消/保存胶囊（风格对齐瞬间页删除弹窗）
@Composable
private fun AddTodoDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val submit = { if (text.isNotBlank()) onAdd(text.trim()) }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(24.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.add_todo_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
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
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.s625fb2),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // 保存：主操作黑胶囊
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (LocalDarkTheme.current) PillBgDark else PillBgLight)
                            .clickable(onClick = submit)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.sbe5fbb),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                }
            }
        }
    }
}

// 搜索框：毛玻璃卡片，输入实时搜索（VM 内 300ms 防抖，仅作用于笔记）
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.s9d2f1a),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onQueryChange("") },
                )
            }
        }
    }
}

// 分类标签 chips 行：全部 + 已有标签（横向滚动）
@Composable
private fun TagChipsRow(
    tags: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            TagChip(
                label = stringResource(R.string.sa8b0c2),
                selected = selected.isEmpty(),
                onClick = { onSelect("") },
            )
        }
        items(tags, key = { it }) { tag ->
            TagChip(label = tag, selected = selected == tag, onClick = { onSelect(tag) })
        }
    }
}

// 单个分类 chip：选中黑胶囊白字 / 未选中浅底
@Composable
private fun TagChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dark = LocalDarkTheme.current
    val bg = if (selected) {
        if (dark) PillBgDark else PillBgLight
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
        )
    }
}

// 笔记卡片：封面（4:3，可选）+ 标题 + 摘要 3 行 + 日期与标签，点击打开、长按删除
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: NoteEntity,
    coverPath: String?,
    imageStore: ImageStore,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        cornerRadius = 16,
        elevation = 3,
    ) {
        Column {
            if (coverPath != null) {
                PathImage(
                    path = coverPath,
                    imageStore = imageStore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.s44a77d) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = noteDayLabel(LocalContext.current, note.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    if (note.tag.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = note.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

// 「笔记/待办」分段切换：玻璃容器 + 选中黑胶囊（compact 用于标题行：无图标、更小内边距）
@Composable
private fun NotesTabSwitcher(
    current: NotesTab,
    onSelect: (NotesTab) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val dark = LocalDarkTheme.current
    GlassCard(modifier = modifier, cornerRadius = 24, elevation = 4) {
        Row(
            modifier = Modifier.padding(if (compact) 4.dp else 6.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
        ) {
            NotesTab.entries.forEach { tabItem ->
                val selected = tabItem == current
                val bg = if (selected) {
                    if (dark) PillBgDark else PillBgLight
                } else {
                    Color.Transparent
                }
                val fg = if (selected) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .clickable { onSelect(tabItem) }
                        .padding(
                            horizontal = if (compact) 12.dp else 16.dp,
                            vertical = if (compact) 6.dp else 9.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!compact) {
                        Icon(
                            imageVector = tabItem.icon,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = stringResource(tabItem.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = fg,
                    )
                }
            }
        }
    }
}

// 笔记卡片日期：M月d日 / MMM d
private fun noteDayLabel(context: android.content.Context, createdAt: Long): String =
    SimpleDateFormat(context.getString(R.string.date_md), Locale.getDefault()).format(Date(createdAt))

// 相对时间文案：刚刚 / N 分钟前 / N 小时前 / 昨天 / N 天前 / 具体日期
internal fun formatRelativeTime(context: android.content.Context, time: Long): String {
    if (time <= 0) return ""
    val now = System.currentTimeMillis()
    val diff = now - time
    val minute = 60_000L
    val hour = 3_600_000L
    val day = 86_400_000L
    return when {
        diff < minute -> context.getString(R.string.s4181f7)
        diff < hour -> context.getString(R.string.rel_minutes_ago, diff / minute)
        diff < day -> context.getString(R.string.rel_hours_ago, diff / hour)
        diff < day * 2 -> context.getString(R.string.s2f8d6f)
        diff < day * 7 -> context.getString(R.string.rel_days_ago, diff / day)
        else -> SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(time))
    }
}
