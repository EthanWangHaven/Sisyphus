package cn.wangce.lumi.ui.tasks

// 待办页（首页）：欢迎区 + 周历 + 环形进度/天气卡 + 快捷入口 + 未完成待办列表
// 待办的添加/筛选入口已移至「备忘录和待办」页，首页仅展示未完成待办

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.local.TodoEntity
import cn.wangce.lumi.ui.components.bottomNavSpace
import cn.wangce.lumi.ui.components.EmptyState
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.SwipeToDeleteRow

// 待办页（首页）
@Composable
fun TasksScreen(
    onOpenExpects: () -> Unit = {},
    onOpenMusic: () -> Unit = {},
    onOpenHabits: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val greeting by viewModel.greeting.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.fetchWeather() }

    var editing by remember { mutableStateOf<TodoEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<TodoEntity?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var showGreetingEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        WelcomeHeader(
            greeting = greeting,
            searchOpen = searchOpen,
            onToggleSearch = { searchOpen = !searchOpen },
            onOpenSettings = onOpenSettings,
            onEditGreeting = { showGreetingEditor = true },
        )
        Spacer(Modifier.height(14.dp))
        AnimatedVisibility(
            visible = searchOpen,
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(250)) + fadeOut(tween(250)),
        ) {
            SearchField(query = query, onQueryChange = viewModel::setQuery)
        }
        Spacer(Modifier.height(14.dp))
        WeekBar()
        Spacer(Modifier.height(10.dp))
        ProgressWeatherCard(pending = stats.first, done = stats.second, weather = weather)
        Spacer(Modifier.height(14.dp))
        QuickEntriesRow(
            onOpenExpects = onOpenExpects,
            onOpenMusic = onOpenMusic,
            onOpenHabits = onOpenHabits,
        )
        Spacer(Modifier.height(14.dp))

        if (todos.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.s48b826),
                description = stringResource(R.string.s48c14f),
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(todos, key = { it.id }) { todo ->
                    SwipeToDeleteRow(onDelete = { viewModel.deleteTodo(todo.id) }) {
                        TodoRow(
                            todo = todo,
                            onToggle = { viewModel.toggleTodo(todo) },
                            onEdit = { editing = todo },
                            onRequestDelete = { pendingDelete = todo },
                        )
                    }
                }
                item { Spacer(Modifier.height(bottomNavSpace())) }
            }
        }
    }

    editing?.let { todo ->
        TodoEditSheet(
            todo = todo,
            onSave = { newTitle ->
                viewModel.updateTodo(todo, newTitle)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    pendingDelete?.let { todo ->
        TodoDeleteDialog(
            todo = todo,
            onConfirm = {
                viewModel.deleteTodo(todo.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (showGreetingEditor) {
        GreetingDialog(
            current = greeting,
            onSave = {
                viewModel.saveGreeting(it)
                showGreetingEditor = false
            },
            onDismiss = { showGreetingEditor = false },
        )
    }
}

// 顶部欢迎区：Hi, WangCe（长按可自定义）+ 右侧设置/搜索圆钮
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WelcomeHeader(
    greeting: String?,
    searchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditGreeting: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = greeting ?: DEFAULT_GREETING,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onEditGreeting, // 长按欢迎词：自定义弹窗
                ),
        )
        // 搜索开关（右上角，设置入口右侧为全 App 唯一设置；圆形细描边框）
        GlassCard(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onToggleSearch),
            cornerRadius = 22,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = if (searchOpen) {
                        stringResource(R.string.s3f5a2b)
                    } else {
                        stringResource(R.string.s5b27c9)
                    },
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        // 设置入口（全 App 唯一，首页右上角最右；圆形细描边框）
        GlassCard(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onOpenSettings),
            cornerRadius = 22,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.se366cc),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// 待办搜索框：毛玻璃卡片，实时过滤标题
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
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
                                text = stringResource(R.string.s5b27c9),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

// 默认欢迎词与快捷候选词（首项为默认，点选即恢复默认）
private const val DEFAULT_GREETING = "Hi, WangCe"
private val GREETING_PRESETS = listOf(
    DEFAULT_GREETING,
    "早安，今天也要加油",
    "欢迎回来",
    "你好呀",
    "下午好",
    "晚上好",
)

// 欢迎词自定义弹窗：输入框 + 快捷候选词；保存空文本恢复默认
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GreetingDialog(
    current: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(current ?: DEFAULT_GREETING) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.greeting_edit_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.greeting_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GREETING_PRESETS.forEach { preset ->
                        GreetingPresetChip(
                            label = preset,
                            selected = text == preset,
                            onClick = { text = preset },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.sbe5fbb)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.s625fb2)) }
        },
    )
}

// 快捷候选词胶囊：细描边圆角，选中淡色填充
@Composable
private fun GreetingPresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                } else {
                    Color.Transparent
                },
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
