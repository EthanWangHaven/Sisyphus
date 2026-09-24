package cn.wangce.lumi.ui.notes

// 备忘录和待办页：顶部分类 chips + 两列瀑布流笔记卡片 / 待办（快速新增 + 筛选 + 列表）
// 底部居中「笔记/待办」分段切换；黄色 FAB 在笔记 tab 新建笔记

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.image.ImageStore
import cn.wangce.lumi.data.local.NoteEntity
import cn.wangce.lumi.data.local.NoteImageEntity
import cn.wangce.lumi.data.local.TodoEntity
import cn.wangce.lumi.ui.components.AppFab
import cn.wangce.lumi.ui.components.AppTextField
import cn.wangce.lumi.ui.components.DangerButton
import cn.wangce.lumi.ui.components.EmptyState
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.LumiDialog
import cn.wangce.lumi.ui.components.LumiDialogButtons
import cn.wangce.lumi.ui.components.PrimaryPillButton
import cn.wangce.lumi.ui.components.SecondaryButton
import cn.wangce.lumi.ui.components.SelectableChip
import cn.wangce.lumi.ui.components.SwipeToDeleteRow
import cn.wangce.lumi.ui.components.bottomNavSpace
import cn.wangce.lumi.ui.moments.PathImage
import cn.wangce.lumi.ui.tasks.FilterTabs
import cn.wangce.lumi.ui.tasks.TodoDeleteDialog
import cn.wangce.lumi.ui.tasks.TodoEditSheet
import cn.wangce.lumi.ui.tasks.TodoFilter
import cn.wangce.lumi.ui.tasks.TodoRow
import cn.wangce.lumi.ui.tasks.TasksViewModel
import cn.wangce.lumi.ui.theme.AccentInk
import cn.wangce.lumi.ui.theme.AccentPaper
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight

// 底部分段切换的页面类型（顺序即左右：先在左边，笔记在右边）
private enum class NotesTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    TODOS(R.string.sb60ec8, Icons.Outlined.TaskAlt),
    NOTES(R.string.s7051dc, Icons.AutoMirrored.Filled.Notes),
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
    val todoQuery by tasksViewModel.query.collectAsStateWithLifecycle()

    // 进入页面默认先展示待办；rememberSaveable 保证旋转/重建后 tab 与内容一致
    var tab by rememberSaveable { mutableStateOf(NotesTab.TODOS) }
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
            // 搜索框按 tab 绑定各自的查询：待办 tab 过滤待办、笔记 tab 过滤笔记
            val onNotes = tab == NotesTab.NOTES
            SearchField(
                query = if (onNotes) query else todoQuery,
                onQueryChange = if (onNotes) notesViewModel::setQuery else tasksViewModel::setQuery,
                hint = stringResource(if (onNotes) R.string.sbcd47c else R.string.s5b27c9),
            )
            Spacer(Modifier.height(12.dp))

            // 内容区随 tab 显式切换；Crossfade 带淡入淡出，杜绝状态不同步
            Crossfade(
                targetState = tab,
                label = "NotesTabContent",
                modifier = Modifier.weight(1f),
            ) { current ->
                when (current) {
                    NotesTab.NOTES -> NotesTabContent(
                        notes = notes,
                        tags = tags,
                        selectedTag = selectedTag,
                        query = query,
                        noteImages = noteImages,
                        imageStore = notesViewModel.imageStore,
                        onSelectTag = notesViewModel::selectTag,
                        onOpenNote = onOpenNote,
                        onDeleteNote = { notesViewModel.deleteNote(it.id) },
                        onRequestDeleteNote = { pendingDeleteNote = it },
                    )
                    NotesTab.TODOS -> TodosTabContent(
                        todos = todos,
                        todoFilter = todoFilter,
                        onSetFilter = tasksViewModel::setFilter,
                        onToggle = { tasksViewModel.toggleTodo(it) },
                        onEdit = { editingTodo = it },
                        onRequestDeleteTodo = { pendingDeleteTodo = it },
                        onDeleteTodo = { tasksViewModel.deleteTodo(it.id) },
                    )
                }
            }
        }

        // 新建主按钮：56dp 品牌蓝圆钮（笔记 tab 新建笔记，待办 tab 弹出添加弹窗，需求新3/新6）
        AppFab(
            onClick = { if (tab == NotesTab.NOTES) onNewNote() else showAddTodo = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = bottomNavSpace()),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.s3147cb),
                tint = LocalContentColor.current,
            )
        }

    }

    pendingDeleteNote?.let { note ->
        LumiDialog(
            onDismissRequest = { pendingDeleteNote = null },
            title = stringResource(R.string.s847dcf),
            actions = {
                LumiDialogButtons {
                    // 取消：白底 hairline 胶囊
                    SecondaryButton(
                        text = stringResource(R.string.s625fb2),
                        onClick = { pendingDeleteNote = null },
                        modifier = Modifier.weight(1f),
                    )
                    // 删除：破坏性操作，橙红底胶囊
                    DangerButton(
                        text = stringResource(R.string.s2f4aad),
                        onClick = {
                            notesViewModel.deleteNote(note.id)
                            pendingDeleteNote = null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        ) {
            Text(stringResource(R.string.del_confirm, note.title.ifBlank { stringResource(R.string.s44a77d) }))
        }
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

// 笔记 tab 内容：分类 chips + 两列瀑布流卡片
@Composable
private fun NotesTabContent(
    notes: List<NoteEntity>,
    tags: List<String>,
    selectedTag: String,
    query: String,
    noteImages: Map<Long, List<NoteImageEntity>>,
    imageStore: ImageStore,
    onSelectTag: (String) -> Unit,
    onOpenNote: (Long) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onRequestDeleteNote: (NoteEntity) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 分类标签 chips：全部 + 已有标签
        TagChipsRow(
            tags = tags,
            selected = selectedTag,
            onSelect = onSelectTag,
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
                icon = if (selectedTag.isEmpty() && query.isBlank()) {
                    Icons.AutoMirrored.Outlined.Notes
                } else {
                    Icons.Outlined.SearchOff
                },
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    SwipeToDeleteRow(onDelete = { onDeleteNote(note) }, cornerRadius = 16) {
                        NoteCard(
                            note = note,
                            coverPath = noteImages[note.id]?.firstOrNull()?.imagePath,
                            imageStore = imageStore,
                            onClick = { onOpenNote(note.id) },
                            onLongClick = { onRequestDeleteNote(note) },
                        )
                    }
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    Spacer(Modifier.height(bottomNavSpace()))
                }
            }
        }
    }
}

// 待办 tab 内容：筛选条 + 待办列表
@Composable
private fun TodosTabContent(
    todos: List<TodoEntity>,
    todoFilter: TodoFilter,
    onSetFilter: (TodoFilter) -> Unit,
    onToggle: (TodoEntity) -> Unit,
    onEdit: (TodoEntity) -> Unit,
    onRequestDeleteTodo: (TodoEntity) -> Unit,
    onDeleteTodo: (TodoEntity) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FilterTabs(current = todoFilter, onSelect = onSetFilter)
        Spacer(Modifier.height(12.dp))

        if (todos.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.s48b826),
                description = stringResource(R.string.s714675),
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.TaskAlt,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(todos, key = { it.id }) { todo ->
                    SwipeToDeleteRow(onDelete = { onDeleteTodo(todo) }) {
                        TodoRow(
                            todo = todo,
                            onToggle = { onToggle(todo) },
                            onEdit = { onEdit(todo) },
                            onRequestDelete = { onRequestDeleteTodo(todo) },
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

// 添加待办弹窗：中央圆角卡片 + 输入框 + 取消/保存胶囊（风格对齐瞬间页删除弹窗）
@Composable
private fun AddTodoDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val submit = { if (text.isNotBlank()) onAdd(text.trim()) }
    LumiDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.add_todo_title),
        actions = {
            LumiDialogButtons {
                // 取消：白底 hairline 胶囊
                SecondaryButton(
                    text = stringResource(R.string.s625fb2),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                // 保存：主操作品牌蓝胶囊，内容为空时禁用
                PrimaryPillButton(
                    text = stringResource(R.string.sbe5fbb),
                    onClick = { onAdd(text.trim()) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        AppTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
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
        AppTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            hint = hint,
            bare = true,
            leading = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            },
            trailing = if (query.isNotEmpty()) {
                {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.s9d2f1a),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onQueryChange("") },
                    )
                }
            } else {
                null
            },
        )
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
            SelectableChip(
                text = stringResource(R.string.sa8b0c2),
                selected = selected.isEmpty(),
                onClick = { onSelect("") },
            )
        }
        items(tags, key = { it }) { tag ->
            SelectableChip(text = tag, selected = selected == tag, onClick = { onSelect(tag) })
        }
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
    GlassCard(modifier = modifier, cornerRadius = 24, elevation = 4) {
        Row(
            modifier = Modifier.padding(if (compact) 4.dp else 6.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
        ) {
            NotesTab.entries.forEach { tabItem ->
                val selected = tabItem == current
                // 分段切换选中语言与主筛选一致：墨黑填充白字 / 未选中透明底灰字（黑即强调）
                val dark = LocalDarkTheme.current
                val bg = if (selected) {
                    if (dark) AccentPaper else AccentInk
                } else {
                    Color.Transparent
                }
                val fg = if (selected) {
                    if (dark) AccentInk else Color.White
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
