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
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import cn.wangce.lumi.ui.components.AppTextField
import cn.wangce.lumi.ui.components.LumiDialog
import cn.wangce.lumi.ui.components.LumiDialogButtons
import cn.wangce.lumi.ui.components.PrimaryPillButton
import cn.wangce.lumi.ui.components.SecondaryButton
import cn.wangce.lumi.ui.components.SelectableChip
import cn.wangce.lumi.ui.components.bottomNavSpace
import cn.wangce.lumi.ui.components.pressScale
import cn.wangce.lumi.ui.components.EmptyState
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.SwipeToDeleteRow
import cn.wangce.lumi.ui.theme.AccentInk
import cn.wangce.lumi.ui.theme.AccentPaper
import cn.wangce.lumi.ui.theme.DurEnter
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.PagePadding
import cn.wangce.lumi.ui.theme.SpaceS
import cn.wangce.lumi.ui.theme.SpaceXS

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
            .padding(horizontal = PagePadding),
    ) {
        Spacer(Modifier.height(SpaceXS))
        WelcomeHeader(
            searchOpen = searchOpen,
            onToggleSearch = { searchOpen = !searchOpen },
            onOpenSettings = onOpenSettings,
        )
        Spacer(Modifier.height(SpaceS))
        AnimatedVisibility(
            visible = searchOpen,
            enter = expandVertically(tween(DurEnter)) + fadeIn(tween(DurEnter)),
            exit = shrinkVertically(tween(DurEnter)) + fadeOut(tween(DurEnter)),
        ) {
            SearchField(query = query, onQueryChange = viewModel::setQuery)
        }
        Spacer(Modifier.height(SpaceS))
        WeekBar()
        Spacer(Modifier.height(SpaceXS))
        ProgressWeatherCard(
            pending = stats.first,
            done = stats.second,
            weather = weather,
            greeting = greeting ?: DEFAULT_GREETING,
            onEditGreeting = { showGreetingEditor = true },
        )
        Spacer(Modifier.height(SpaceS))
        QuickEntriesRow(
            onOpenExpects = onOpenExpects,
            onOpenMusic = onOpenMusic,
            onOpenHabits = onOpenHabits,
        )
        Spacer(Modifier.height(SpaceS))

        if (todos.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.s48b826),
                description = stringResource(R.string.s48c14f),
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.TaskAlt,
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

// 顶部工具栏：左侧品牌字标 + 右侧搜索/设置圆钮（问候语已移入 hero 卡）
@Composable
private fun WelcomeHeader(
    searchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 品牌字标：左上角锚点，避免整行右偏留白
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        // 搜索开关（右上角，设置入口右侧为全 App 唯一设置）
        // 注意：边框由 GlassCard 自带，此处不可再叠 .border()，否则会同时出现「圆 + 圆角方」两圈框
        GlassCard(
            modifier = Modifier
                .size(44.dp)
                .pressScale(onPress = onToggleSearch),
            shape = CircleShape,
            elevation = 0,
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
        Spacer(Modifier.width(SpaceXS))
        // 设置入口（全 App 唯一，首页右上角最右）
        GlassCard(
            modifier = Modifier
                .size(44.dp)
                .pressScale(onPress = onOpenSettings),
            shape = CircleShape,
            elevation = 0,
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
        AppTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            hint = stringResource(R.string.s5b27c9),
            bare = true,
            leading = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
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
    LumiDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.greeting_edit_title),
        actions = {
            LumiDialogButtons {
                // 取消：白底 hairline 胶囊
                SecondaryButton(
                    text = stringResource(R.string.s625fb2),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                // 保存：主操作品牌蓝胶囊
                PrimaryPillButton(
                    text = stringResource(R.string.sbe5fbb),
                    onClick = { onSave(text) },
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Column {
            AppTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                hint = stringResource(R.string.greeting_hint),
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GREETING_PRESETS.forEach { preset ->
                    SelectableChip(
                        text = preset,
                        selected = text == preset,
                        onClick = { text = preset },
                    )
                }
            }
        }
    }
}

