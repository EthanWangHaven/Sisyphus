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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.local.HabitEntity
import cn.wangce.lumi.ui.components.AppTextField
import cn.wangce.lumi.ui.components.bottomNavSpace
import cn.wangce.lumi.ui.components.EmptyState
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.LumiDialog
import cn.wangce.lumi.ui.components.LumiDialogButtons
import cn.wangce.lumi.ui.components.PrimaryPillButton
import cn.wangce.lumi.ui.components.SecondaryButton
import cn.wangce.lumi.ui.theme.CategoryBlue
import cn.wangce.lumi.ui.theme.CategoryColor
import cn.wangce.lumi.ui.theme.CategoryCoral
import cn.wangce.lumi.ui.theme.CategoryGreen
import cn.wangce.lumi.ui.theme.CategoryOrange
import cn.wangce.lumi.ui.theme.CategoryPink
import cn.wangce.lumi.ui.theme.CategoryPurple
import cn.wangce.lumi.ui.theme.CategoryTeal
import cn.wangce.lumi.ui.theme.CategoryYellow
import cn.wangce.lumi.ui.theme.AccentInk
import cn.wangce.lumi.ui.theme.AccentPaper
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import java.time.LocalDate
import kotlinx.coroutines.launch

// pastel 分类色候选（新建/编辑习惯标签色；打卡钮/月历/图标底块均取分类色）
private val HabitCategories = listOf(
    CategoryBlue, CategoryGreen, CategoryCoral, CategoryOrange,
    CategoryYellow, CategoryTeal, CategoryPurple, CategoryPink,
)

// 旧版马卡龙 habit.color → pastel 分类映射（兼容历史习惯数据，数据无需迁移）
private val LegacyHabitColorMap = mapOf(
    0xFFB4D4FFL to CategoryBlue, 0xFFFFD070L to CategoryYellow,
    0xFFB8E6C8L to CategoryGreen, 0xFFD4C5E8L to CategoryPurple,
    0xFFFFB4B4L to CategoryCoral, 0xFFFFD4A8L to CategoryOrange,
)

// 存库值 = 分类 fg 的 ARGB（低 32 位）；由值反查分类，旧值走映射，未知兜底第一个
private fun CategoryColor.storedValue(): Long = fg.toArgb().toLong() and 0xFFFFFFFFL

private fun habitCategory(color: Long): CategoryColor =
    HabitCategories.firstOrNull { it.storedValue() == color }
        ?: LegacyHabitColorMap[color]
        ?: HabitCategories.first()

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
                icon = Icons.Outlined.Eco,
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
                        modifier = Modifier.padding(start = 6.dp, bottom = 2.dp),
                    )
                }
            }
        }
    }
}

// 习惯卡片：pastel 图标底块 + 标题 / 超大墨色连续天数 + 灰单位 / 今日状态灰字 / 右侧分类色虚线圆打卡钮
// 点卡片展开月历，长按删除
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
    val category = habitCategory(habit.color)
    val iconBg = if (LocalDarkTheme.current) category.fg.copy(alpha = 0.18f) else category.bg
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(iconBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = iconOf(habit.emoji),
                                contentDescription = null,
                                tint = category.fg,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$streak",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.days_unit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (checkedToday) stringResource(R.string.s1c6f49) else stringResource(R.string.s97b8d9),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                CheckButton(checked = checkedToday, color = category.fg, onToggle = onToggle)
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
                color = category.fg,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

// 打卡圆钮：未打卡为分类色虚线圆，打卡后填充分类色；弹跳动画（0.8 → 1 spring）保留
@Composable
private fun CheckButton(checked: Boolean, color: Color, onToggle: () -> Unit) {
    val scope = rememberCoroutineScope()
    val bounce = remember { Animatable(1f) }
    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(bounce.value)
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (checked) {
                drawCircle(color = color)
            } else {
                drawCircle(
                    color = color.copy(alpha = 0.8f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
        }
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut(spring()) + fadeOut(),
        ) {
            // 勾图标颜色按填充亮度自适应（极亮 fg 配墨勾，其余白勾）
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = if (checked) stringResource(R.string.s1c6f49) else stringResource(R.string.scd27ec),
                tint = if (color.luminance() > 0.5f) AccentInk else Color.White,
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
                                            isChecked -> if (color.luminance() > 0.5f) AccentInk else Color.White
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

// 图标候选项：选中墨色描边 + 淡底（单色线性图标，全 App「黑即强调」）
@Composable
private fun IconOption(
    icon: ImageVector,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalDarkTheme.current
    val ink = if (dark) AccentPaper else AccentInk
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    ink.copy(alpha = 0.14f)
                } else {
                    Color.Transparent
                },
            )
            .border(
                1.5.dp,
                if (selected) {
                    ink
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

// 颜色候选项：pastel 双色圆（bg 底 + fg 内点），选中墨色描边
@Composable
private fun ColorOption(category: CategoryColor, selected: Boolean, onSelect: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(category.bg)
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
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(category.fg),
        )
    }
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
    var color by remember {
        mutableStateOf(existing?.color?.let(::habitCategory) ?: HabitCategories.first())
    }
    LumiDialog(
        onDismissRequest = onDismiss,
        title = if (existing == null) stringResource(R.string.s0b1895) else stringResource(R.string.sccf098),
        actions = {
            LumiDialogButtons {
                // 取消：次级按钮
                SecondaryButton(
                    text = stringResource(R.string.s625fb2),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                // 保存：主操作品牌蓝胶囊
                PrimaryPillButton(
                    text = stringResource(R.string.sbe5fbb),
                    onClick = { onSave(name.trim(), iconKey, color.storedValue()) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        // 名称输入：统一走 AppTextField（聚焦墨色描边 + 墨色光标）
        AppTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            hint = stringResource(R.string.s253cf0),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (name.isNotBlank()) onSave(name, iconKey, color.storedValue()) },
            ),
        )
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
            HabitCategories.forEach { candidate ->
                ColorOption(
                    category = candidate,
                    selected = candidate == color,
                    onSelect = { color = candidate },
                )
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
    LumiDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.s5a6fc1),
        actions = {
            LumiDialogButtons {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.s625fb2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.s2f4aad),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    ) {
        Text(
            text = stringResource(R.string.del_habit_fmt, habit.name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
