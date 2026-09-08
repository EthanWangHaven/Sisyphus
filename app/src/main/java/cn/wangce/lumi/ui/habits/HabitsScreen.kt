package cn.wangce.lumi.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.local.HabitEntity
import cn.wangce.lumi.ui.components.bottomNavSpace
import cn.wangce.lumi.ui.components.EmptyState
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.theme.DarkGlassBg
import cn.wangce.lumi.ui.theme.GlassBg
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.PillBgDark
import cn.wangce.lumi.ui.theme.PillBgLight
import java.time.LocalDate
import kotlinx.coroutines.launch

// 灰阶候选色（新建/编辑习惯标签色，对标黑白参考；图标圆底按亮度自适应字色）
private val HabitColors = listOf(
    0xFF1A1A1AL, // 墨黑
    0xFF3D3D3DL, // 深灰
    0xFF5A5A5AL, // 中灰
    0xFF757575L, // 灰
    0xFF8F8F8FL, // 亮灰
    0xFFA6A6A6L, // 浅灰
)

// 旧版马卡龙 habit.color → 灰阶映射（兼容历史习惯数据；新习惯已是灰阶无需映射）
private val LegacyHabitColorMap = mapOf(
    0xFFB4D4FFL to 0xFF1A1A1AL, 0xFFFFD070L to 0xFF3D3D3DL,
    0xFFB8E6C8L to 0xFF5A5A5AL, 0xFFD4C5E8L to 0xFF757575L,
    0xFFFFB4B4L to 0xFF8F8F8FL, 0xFFFFD4A8L to 0xFFA6A6A6L,
)

private fun habitDisplayColor(color: Long): Long = LegacyHabitColorMap[color] ?: color

// 线性矢量图标候选（单色 Outlined 图标；数据库沿用 emoji 字段存 key，兼容旧数据）
private data class HabitIcon(val key: String, val icon: ImageVector)

private val HabitIcons = listOf(
    HabitIcon("water", Icons.Outlined.WaterDrop),          // 喝水
    HabitIcon("run", Icons.AutoMirrored.Outlined.DirectionsRun),   // 运动
    HabitIcon("book", Icons.AutoMirrored.Outlined.MenuBook),      // 阅读
    HabitIcon("sleep", Icons.Outlined.DarkMode),           // 早睡
    HabitIcon("fitness", Icons.Outlined.FitnessCenter),    // 健身
    HabitIcon("meditate", Icons.Outlined.SelfImprovement), // 冥想
    HabitIcon("music", Icons.Outlined.Piano),              // 乐器
    HabitIcon("note", Icons.Outlined.EditNote),            // 笔记
    HabitIcon("write", Icons.Outlined.HistoryEdu),         // 写作
    HabitIcon("paint", Icons.Outlined.Palette),            // 绘画
    HabitIcon("sun", Icons.Outlined.WbSunny),              // 早起
    HabitIcon("diet", Icons.Outlined.Eco),                 // 饮食
)

// 旧版 emoji 数据归一化：历史记录里存的 emoji 映射到对应图标 key
private val LegacyEmojiMap = mapOf(
    "💧" to "water", "🏃" to "run", "📖" to "book", "🌙" to "sleep",
    "💪" to "fitness", "🧘" to "meditate", "🎹" to "music", "📝" to "note",
    "✍️" to "write", "🎨" to "paint", "☀️" to "sun", "🥗" to "diet",
)

// 按 key 取图标：旧 emoji 先归一化，未知 key 兜底第一个
private fun iconOf(key: String): ImageVector {
    val normalized = LegacyEmojiMap[key] ?: key
    return HabitIcons.firstOrNull { it.key == normalized }?.icon ?: HabitIcons.first().icon
}

// 连续打卡天数：今天已打卡从今天起算，否则从昨天起算（未断签）
internal fun calcStreak(checked: Set<String>, today: LocalDate): Int {
    var streak = 0
    var cursor = if (today.toString() in checked) today else today.minusDays(1)
    while (cursor.toString() in checked) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

// 习惯打卡二级页：汇总卡 + 今日习惯列表（打卡圆钮弹跳）+ 卡片展开月历 + 新建/编辑/删除
@Composable
fun HabitsScreen(
    onBack: () -> Unit,
    embedded: Boolean = false,
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val checks by viewModel.checks.collectAsStateWithLifecycle()

    // 按 habitId 聚合打卡日期集合（连续天数/月历/当日判定共用）
    val checksByHabit = remember(checks) {
        checks.groupBy { it.habitId }.mapValues { entry -> entry.value.map { it.date }.toSet() }
    }
    val today = remember { LocalDate.now().toString() }

    var showEditor by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<HabitEntity?>(null) }
    var expandedHabitId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            // 添加习惯/删除确认弹窗（独立窗口）打开时，整页内容高斯模糊突出弹层
            .then(if (showEditor || pendingDelete != null) Modifier.blur(20.dp) else Modifier),
    ) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!embedded) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.s5f4112),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.scd27ec),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.sf0259b),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GlassCard(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        editingHabit = null
                        showEditor = true
                    },
                cornerRadius = 22,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.s0b1895),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        val doneToday = habits.count { today in checksByHabit[it.id].orEmpty() }
        val bestStreak = habits.maxOfOrNull { calcStreak(checksByHabit[it.id].orEmpty(), LocalDate.now()) } ?: 0
        HabitSummaryCard(done = doneToday, total = habits.size, bestStreak = bestStreak)
        Spacer(Modifier.height(14.dp))

        if (habits.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.s584fc5),
                description = stringResource(R.string.s0f5c2c),
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(habits, key = { it.id }) { habit ->
                    val checkedDates = checksByHabit[habit.id].orEmpty()
                    HabitCard(
                        habit = habit,
                        checkedToday = today in checkedDates,
                        streak = calcStreak(checkedDates, LocalDate.now()),
                        expanded = expandedHabitId == habit.id,
                        checkedDates = checkedDates,
                        onToggle = { viewModel.toggleCheck(habit.id, today) },
                        onToggleExpand = {
                            expandedHabitId = if (expandedHabitId == habit.id) null else habit.id
                        },
                        onRequestDelete = { pendingDelete = habit },
                    )
                }
            }
        }
        if (embedded) {
            Spacer(Modifier.height(bottomNavSpace()))
        }
    }

    if (showEditor) {
        HabitEditorDialog(
            existing = editingHabit,
            onSave = { name, emoji, color ->
                viewModel.saveHabit(editingHabit, name, emoji, color)
                showEditor = false
            },
            onDismiss = {
                showEditor = false
                editingHabit = null
            },
        )
    }

    pendingDelete?.let { habit ->
        DeleteHabitDialog(
            habit = habit,
            onConfirm = {
                viewModel.softDelete(habit)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

// 汇总卡：今日已打卡 x/y + 最长连续天数
@Composable
private fun HabitSummaryCard(done: Int, total: Int, bestStreak: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.see8233),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$done",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "/$total",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.s8d40c5),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$bestStreak",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.days_unit),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

// 习惯卡片：emoji 圆底 + 名称/连续天数 + 打卡圆钮；点卡片展开月历，长按删除
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(
    habit: HabitEntity,
    checkedToday: Boolean,
    streak: Int,
    expanded: Boolean,
    checkedDates: Set<String>,
    onToggle: () -> Unit,
    onToggleExpand: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val base = Color(habitDisplayColor(habit.color))
    Column {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onToggleExpand,
                    onLongClick = onRequestDelete,
                ),
            cornerRadius = 20,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(base.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconOf(habit.emoji),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (streak > 0) stringResource(R.string.streak_fmt, streak) else stringResource(R.string.s97b8d9),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                CheckButton(checked = checkedToday, color = base, onToggle = onToggle)
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy)) +
                fadeIn(),
            exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy)) +
                fadeOut(),
        ) {
            HabitMonthCalendar(
                checkedDates = checkedDates,
                color = base,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

// 打卡圆钮：打卡态填充习惯色 + 弹跳动画（0.8 → 1 spring）
@Composable
private fun CheckButton(checked: Boolean, color: Color, onToggle: () -> Unit) {
    val scope = rememberCoroutineScope()
    val bounce = remember { Animatable(1f) }
    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(bounce.value)
            .clip(CircleShape)
            .background(if (checked) color else color.copy(alpha = 0.12f))
            .border(
                2.dp,
                if (checked) color else color.copy(alpha = 0.45f),
                CircleShape,
            )
            .clickable {
                scope.launch {
                    bounce.animateTo(0.8f, spring(stiffness = Spring.StiffnessMedium))
                    bounce.animateTo(
                        1f,
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
                onToggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut(spring()) + fadeOut(),
        ) {
            // 习惯色为灰阶：亮灰配墨勾、深灰配白勾（对比自适应）
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = if (checked) stringResource(R.string.s1c6f49) else stringResource(R.string.scd27ec),
                tint = if (color.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// 当月月历：周一开头网格，打卡日填充习惯色圆点，今日描边
@Composable
private fun HabitMonthCalendar(
    checkedDates: Set<String>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val firstDay = today.withDayOfMonth(1)
    val leadingBlanks = firstDay.dayOfWeek.value - 1 // 周一=1 → 前导空 0..6
    val cells: List<LocalDate?> = List(leadingBlanks) { null } +
        (1..today.lengthOfMonth()).map { firstDay.plusDays((it - 1).toLong()) }

    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 20) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.month_year_fmt, today.year, today.monthValue),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Row {
                listOf(
                    R.string.s7941da,
                    R.string.s2d8be2,
                    R.string.se662ff,
                    R.string.s21716c,
                    R.string.s1fcc29,
                    R.string.s61b453,
                    R.string.s3edddd,
                ).forEach { labelRes ->
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            cells.chunked(7).forEach { week ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day == null) {
                                Spacer(Modifier.height(32.dp))
                            } else {
                                val isChecked = day.toString() in checkedDates
                                val isToday = day == today
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isChecked) color else Color.Transparent)
                                        .then(
                                            if (isToday && !isChecked) {
                                                Modifier.border(1.5.dp, color, CircleShape)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = day.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when {
                                            // 灰阶打卡日：亮灰配墨字、深灰配白字
                                            isChecked -> if (color.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
                                            isToday -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// 图标候选项：选中主题色描边 + 淡底（单色线性图标）
@Composable
private fun IconOption(
    icon: ImageVector,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    Color.Transparent
                },
            )
            .border(
                1.5.dp,
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

// 颜色候选项：马卡龙圆点，选中深色描边
@Composable
private fun ColorOption(colorValue: Long, selected: Boolean, onSelect: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(colorValue))
            .then(
                if (selected) {
                    Modifier.border(
                        2.5.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        CircleShape,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) { }
}

// 新建/编辑习惯弹窗：磨砂半透底 + 名称输入 + emoji/颜色选择
@Composable
private fun HabitEditorDialog(
    existing: HabitEntity?,
    onSave: (String, String, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    // 旧数据存的可能是 emoji，先归一化成图标 key
    var iconKey by remember {
        mutableStateOf(existing?.emoji?.let { LegacyEmojiMap[it] ?: it } ?: HabitIcons.first().key)
    }
    var color by remember { mutableStateOf(habitDisplayColor(existing?.color ?: HabitColors.first())) }
    val dark = LocalDarkTheme.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (dark) DarkGlassBg else GlassBg)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(16.dp),
                )
                .padding(24.dp),
        ) {
            Column {
                Text(
                    text = if (existing == null) stringResource(R.string.s0b1895) else stringResource(R.string.sccf098),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
                // 名称输入：淡底 + 极淡边框（同新建瞬间输入框）
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
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { if (name.isNotBlank()) onSave(name, iconKey, color) },
                        ),
                        decorationBox = { inner ->
                            Box {
                                if (name.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.s253cf0),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.s5ef69f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                HabitIcons.chunked(6).forEach { rowIcons ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowIcons.forEach { candidate ->
                            IconOption(
                                icon = candidate.icon,
                                selected = candidate.key == iconKey,
                                onSelect = { iconKey = candidate.key },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(6 - rowIcons.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = stringResource(R.string.s6b36c6),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HabitColors.forEach { candidate ->
                        ColorOption(
                            colorValue = candidate,
                            selected = candidate == color,
                            onSelect = { color = candidate },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 取消：文字按钮
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.s625fb2),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // 保存：主操作黑胶囊（浅色黑底白字 / 深色浅底黑字）
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (LocalDarkTheme.current) PillBgDark else PillBgLight)
                            .clickable(enabled = name.isNotBlank()) {
                                onSave(name.trim(), iconKey, color)
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.sbe5fbb),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                }
            }
        }
    }
}

// 删除确认弹窗：磨砂半透 + 豆沙红填充胶囊（与瞬间页删除弹窗同款）
@Composable
private fun DeleteHabitDialog(
    habit: HabitEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dark = LocalDarkTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (dark) DarkGlassBg else GlassBg)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(16.dp),
                )
                .padding(24.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.s5a6fc1),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.del_habit_fmt, habit.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.s625fb2),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .shadow(
                                4.dp,
                                RoundedCornerShape(12.dp),
                                ambientColor = Color(0x1AC47878),
                                spotColor = Color(0x1AC47878),
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFC47878))
                            .clickable(onClick = onConfirm)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.s2f4aad),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
