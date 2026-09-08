package cn.wangce.lumi.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.local.WorkoutEntity
import cn.wangce.lumi.ui.components.DumbbellIcon
import cn.wangce.lumi.ui.components.EmptyState
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.SegmentedControl
import cn.wangce.lumi.ui.components.SegmentedOption
import cn.wangce.lumi.ui.components.SwipeToDeleteRow
import cn.wangce.lumi.ui.components.bottomNavSpace
import cn.wangce.lumi.ui.components.pressScale
import cn.wangce.lumi.ui.theme.AccentGradientEnd
import cn.wangce.lumi.ui.theme.AccentGradientEndDark
import cn.wangce.lumi.ui.theme.AccentGradientStart
import cn.wangce.lumi.ui.theme.AccentGradientStartDark
import cn.wangce.lumi.ui.theme.DarkSheetBg
import cn.wangce.lumi.ui.theme.LightSheetBg
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

// 灰阶数据色块上的内容色自适应：亮灰配墨字、深灰配白字（黑白体系）
private fun onInk(bg: Color): Color =
    if (bg.luminance() > 0.5f) Color(0xFF1A1A1A) else Color(0xFFFFFFFF)

// 旧版各色 colorKey → 统一散步浅灰（兼容历史记录数据显示；用户要求所有项目卡片同色）
private val LegacyColorMap = mapOf(
    0xFFB4D4FFL to 0xFFD9D9D9L, 0xFFD4C5E8L to 0xFFD9D9D9L,
    0xFFB8E6C8L to 0xFFD9D9D9L, 0xFFFFD4A8L to 0xFFD9D9D9L,
    0xFFFFB4B4L to 0xFFD9D9D9L, 0xFFFFD070L to 0xFFD9D9D9L,
    0xFFC9E4D2L to 0xFFD9D9D9L, 0xFFE8DCC0L to 0xFFD9D9D9L,
    0xFFD8CFC4L to 0xFFD9D9D9L,
)

private fun displayColor(colorKey: Long): Long = LegacyColorMap[colorKey] ?: colorKey

// 自定义项目的图标 key 与颜色
private const val CUSTOM_KEY = "custom"
private const val CUSTOM_COLOR = 0xFFD9D9D9L

// 锻炼预设：MET 取 Ainswright 体动 compendium 常用近似值，颜色统一为散步卡片浅灰（用户要求同色）
private data class WorkoutPreset(
    val key: String,
    val nameRes: Int,
    val met: Double,
    val color: Long,
    val icon: ImageVector,
)

private val WorkoutPresets = listOf(
    WorkoutPreset("run", R.string.wk_run, 9.8, 0xFFD9D9D9, Icons.AutoMirrored.Outlined.DirectionsRun),
    WorkoutPreset("cycling", R.string.wk_cycling, 6.8, 0xFFD9D9D9, Icons.AutoMirrored.Outlined.DirectionsBike),
    WorkoutPreset("swim", R.string.wk_swim, 8.3, 0xFFD9D9D9, Icons.Outlined.Pool),
    WorkoutPreset("strength", R.string.wk_strength, 6.0, 0xFFD9D9D9, DumbbellIcon),
    WorkoutPreset("yoga", R.string.wk_yoga, 3.0, 0xFFD9D9D9, Icons.Outlined.SelfImprovement),
    WorkoutPreset("ball", R.string.wk_ball, 7.0, 0xFFD9D9D9, Icons.Outlined.SportsBasketball),
    WorkoutPreset("stairs", R.string.wk_stairs, 8.0, 0xFFD9D9D9, Icons.Outlined.Hiking),
    WorkoutPreset("walk", R.string.wk_walk, 3.5, 0xFFD9D9D9, Icons.AutoMirrored.Outlined.DirectionsWalk),
)

// 按 key 取图标：自定义/未知 key 兜底 Add
private fun workoutIcon(key: String): ImageVector =
    WorkoutPresets.firstOrNull { it.key == key }?.icon ?: Icons.Outlined.Add

// 该项目的 MET 值（自定义/未知按中等 6.0 估）
private fun metOf(iconKey: String): Double =
    WorkoutPresets.firstOrNull { it.key == iconKey }?.met ?: 6.0

// 强度系数：低 0.8 / 中 1.0 / 高 1.2
private fun intensityFactor(intensity: Int): Double = when (intensity) {
    0 -> 0.8
    2 -> 1.2
    else -> 1.0
}

// 耗能估算：kcal = MET × 强度系数 × 分钟 × 70kg / 60（按 70kg 成人近似）
private fun kcalOf(workout: WorkoutEntity): Int =
    (metOf(workout.iconKey) * intensityFactor(workout.intensity) * workout.durationMin * 70.0 / 60.0)
        .roundToInt()

// 时长显示：<60 → "45m"，否则 "4h25m"（h/m 为国际通用单位缩写）
private fun formatHoursMinutes(min: Int): String {
    val h = min / 60
    val m = min % 60
    return if (h > 0) "${h}h${m}m" else "${m}m"
}

// 日期本地化格式（zh: 2026年9月5日 / en: Sep 5, 2026），locale 取自 config 以跟随 app 内语言
@Composable
private fun rememberDayDateFmt(): DateTimeFormatter {
    val locales = LocalConfiguration.current.locales
    return remember(locales) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locales[0])
    }
}

// 锻炼页（顶层 Tab）：运动大标题 + 概览/统计双 Tab + 记录 BottomSheet
@Composable
fun WorkoutScreen(viewModel: WorkoutViewModel = hiltViewModel()) {
    val month by viewModel.currentMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selected.collectAsStateWithLifecycle()
    val monthWorkouts by viewModel.monthWorkouts.collectAsStateWithLifecycle()
    val dayWorkouts by viewModel.dayWorkouts.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(0) }
    var showSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.s37b6de),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        WorkoutTabs(tab = tab, onSelect = { tab = it })
        Spacer(Modifier.height(14.dp))
        if (tab == 0) {
            OverviewTab(
                month = month,
                selected = selectedDate,
                monthWorkouts = monthWorkouts,
                dayWorkouts = dayWorkouts,
                onSelectDate = viewModel::selectDate,
                onShiftMonth = viewModel::shiftMonth,
                onLog = { showSheet = true },
                onDelete = viewModel::deleteWorkout,
            )
        } else {
            // 统计 Tab（无返回钮，底部留出导航栏空间）
            StatsTab(viewModel = viewModel)
        }
    }

    if (showSheet) {
        LogWorkoutSheet(
            date = selectedDate,
            customTags = viewModel.customTags.collectAsStateWithLifecycle().value,
            onDismiss = { showSheet = false },
            onSave = { name, iconKey, colorKey, duration, intensity ->
                viewModel.addWorkout(selectedDate, name, iconKey, colorKey, duration, intensity)
                showSheet = false
            },
            onAddTag = viewModel::addTag,
            onRenameTag = viewModel::renameTag,
            onDeleteTag = viewModel::deleteTag,
        )
    }
}

// 双 Tab：概览 | 统计（墨色下划线，模仿参考图 Workouts/Nutritions 款式）
@Composable
private fun WorkoutTabs(tab: Int, onSelect: (Int) -> Unit) {
    val labels = listOf(
        stringResource(R.string.wk_overview),
        stringResource(R.string.sx_stats),
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val active = tab == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) }
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 3.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (active) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        ),
                )
            }
        }
    }
}

// 概览 Tab：月历卡 + 当日记录卡 +（有记录时）锻炼时间卡 + 本月之最卡
@Composable
private fun ColumnScope.OverviewTab(
    month: LocalDate,
    selected: LocalDate,
    monthWorkouts: List<WorkoutEntity>,
    dayWorkouts: List<WorkoutEntity>,
    onSelectDate: (LocalDate) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onLog: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
    ) {
        WorkoutMonthCalendar(
            month = month,
            workouts = monthWorkouts,
            selected = selected,
            onSelectDate = onSelectDate,
            onShiftMonth = onShiftMonth,
        )
        Spacer(Modifier.height(14.dp))
        DayRecordsCard(date = selected, records = dayWorkouts, onLog = onLog, onDelete = onDelete)
        if (monthWorkouts.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            MonthSummaryCard(monthWorkouts)
            Spacer(Modifier.height(14.dp))
            MonthBestCard(monthWorkouts)
        }
        Spacer(Modifier.height(bottomNavSpace()))
    }
}

// 月历卡：月份切换头 + 周标签 + 6 行 × 7 列固定网格（周一开头，跨月日期淡显）
@Composable
private fun WorkoutMonthCalendar(
    month: LocalDate,
    workouts: List<WorkoutEntity>,
    selected: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onShiftMonth: (Long) -> Unit,
) {
    val dark = LocalDarkTheme.current
    val today = remember { LocalDate.now() }
    // 每天取当天第一条记录的色做圆点（数据多彩纪律：点不占强调色预算）
    val dotColors = remember(workouts) {
        workouts.groupBy { it.epochDay }.mapValues { entry -> Color(displayColor(entry.value.first().colorKey)) }
    }
    // 网格起点：本月 1 号所在周的周一（固定 42 格，切月高度稳定）
    val gridStart = remember(month) {
        month.minusDays(((month.dayOfWeek.value + 6) % 7).toLong())
    }
    val cells = remember(month, gridStart) { (0 until 42).map { gridStart.plusDays(it.toLong()) } }
    val accentBrush = Brush.verticalGradient(
        listOf(
            if (dark) AccentGradientStartDark else AccentGradientStart,
            if (dark) AccentGradientEndDark else AccentGradientEnd,
        ),
    )

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 月份切换头
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onShiftMonth(-1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = stringResource(R.string.s5f4112),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.month_year_fmt, month.year, month.monthValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onShiftMonth(1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = stringResource(R.string.s5f4112),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // 周标签（一 ~ 日 / Mon ~ Sun，周一开头）
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
                        val inMonth = day.year == month.year && day.monthValue == month.monthValue
                        val isSelected = day == selected
                        val isToday = day == today
                        val dotColor = dotColors[day.toEpochDay()]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .then(if (isSelected) Modifier.background(accentBrush) else Modifier)
                                    .then(
                                        if (isToday && !isSelected) {
                                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { onSelectDate(day) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = when {
                                            isSelected || isToday -> FontWeight.SemiBold
                                            else -> FontWeight.Medium
                                        },
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onTertiary
                                            !inMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                            isToday -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    // 有记录日小圆点（选中态藏在琥珀底里）
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.55f)
                                                    dotColor != null -> dotColor
                                                    else -> Color.Transparent
                                                },
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// 当日记录卡：日期标题 + 记录行（左滑删除）+ 记录锻炼主按钮（琥珀渐变）
@Composable
private fun DayRecordsCard(
    date: LocalDate,
    records: List<WorkoutEntity>,
    onLog: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    val dark = LocalDarkTheme.current
    val shadow = if (dark) ShadowDark else ShadowLight
    val accentBrush = Brush.verticalGradient(
        listOf(
            if (dark) AccentGradientStartDark else AccentGradientStart,
            if (dark) AccentGradientEndDark else AccentGradientEnd,
        ),
    )
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = rememberDayDateFmt().format(date),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            if (records.isEmpty()) {
                Text(
                    text = stringResource(R.string.wk_empty_day),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                )
            } else {
                records.forEach { record ->
                    SwipeToDeleteRow(onDelete = { onDelete(record.id) }) {
                        WorkoutRow(record)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // 记录锻炼主按钮（琥珀强调色预算之一）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .shadow(4.dp, RoundedCornerShape(23.dp), ambientColor = shadow, spotColor = shadow)
                    .clip(RoundedCornerShape(23.dp))
                    .background(accentBrush)
                    .pressScale(onPress = onLog, pressedScale = 0.96f),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.wk_log),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
        }
    }
}

// 记录行：马卡龙圆底图标 + 名称 / 时长·强度
@Composable
private fun WorkoutRow(record: WorkoutEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(displayColor(record.colorKey))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = workoutIcon(record.iconKey),
                contentDescription = null,
                tint = onInk(Color(displayColor(record.colorKey))),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.wk_min_fmt, record.durationMin) + " · " +
                    stringResource(
                        when (record.intensity) {
                            0 -> R.string.wk_low
                            2 -> R.string.wk_high
                            else -> R.string.wk_mid
                        },
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatHoursMinutes(record.durationMin),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// 按项目聚合的数据行
private data class WorkoutGroup(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val totalMin: Int,
    val count: Int,
)

// 锻炼时间卡：本月各项目马卡龙横条（模仿参考图，条内右侧 X 次数，右列时长 + 项目名）
@Composable
private fun MonthSummaryCard(workouts: List<WorkoutEntity>) {
    val groups = remember(workouts) {
        workouts
            .groupBy { it.name }
            .values.map { list ->
                WorkoutGroup(
                    name = list.first().name,
                    icon = workoutIcon(list.first().iconKey),
                    color = Color(displayColor(list.first().colorKey)),
                    totalMin = list.sumOf { it.durationMin },
                    count = list.size,
                )
            }
            .sortedByDescending { it.totalMin }
    }
    val maxMin = groups.maxOf { it.totalMin }.coerceAtLeast(1)

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.wk_this_month),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            groups.forEach { group ->
                val frac = group.totalMin.toFloat() / maxMin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .width(maxWidth * frac)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(group.color)
                                .padding(end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(Modifier.weight(1f))
                            if (frac >= 0.12f) {
                                Icon(
                                    imageVector = group.icon,
                                    contentDescription = null,
                                    tint = onInk(group.color),
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            if (frac >= 0.22f) {
                                Text(
                                    text = "X${group.count}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onInk(group.color),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.width(88.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = formatHoursMinutes(group.totalMin),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// 本月之最卡：最长运动时间 / 最高强度 / 最高耗能（图 1 底部纪录列表）
@Composable
private fun MonthBestCard(workouts: List<WorkoutEntity>) {
    val longest = workouts.maxByOrNull { it.durationMin }
    val peak = workouts.maxByOrNull { metOf(it.iconKey) * intensityFactor(it.intensity) }
    val burn = workouts.maxByOrNull { kcalOf(it) }
    if (longest == null || peak == null || burn == null) return

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.wk_best),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            BestRow(
                workout = longest,
                label = stringResource(R.string.wk_longest),
                value = stringResource(R.string.wk_min_fmt, longest.durationMin),
            )
            BestDivider()
            BestRow(
                workout = peak,
                label = stringResource(R.string.wk_peak),
                value = String.format(Locale.US, "%.1f", metOf(peak.iconKey) * intensityFactor(peak.intensity)) + "METs",
            )
            BestDivider()
            BestRow(
                workout = burn,
                label = stringResource(R.string.wk_burn),
                value = stringResource(R.string.wk_kcal_fmt, kcalOf(burn)),
            )
        }
    }
}

// 之最行：马卡龙圆底图标 + 标签/大数字 + 右侧日期
@Composable
private fun BestRow(workout: WorkoutEntity, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(displayColor(workout.colorKey))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = workoutIcon(workout.iconKey),
                contentDescription = null,
                tint = onInk(Color(displayColor(workout.colorKey))),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = rememberDayDateFmt().format(LocalDate.ofEpochDay(workout.epochDay)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BestDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
    )
}

// 记录 BottomSheet：项目 chips（预设 + 自定义）→ 时长 chips → 强度分段器 → 琥珀保存钮
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LogWorkoutSheet(
    date: LocalDate,
    customTags: List<cn.wangce.lumi.data.local.WorkoutTagEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, iconKey: String, colorKey: Long, durationMin: Int, intensity: Int) -> Unit,
    onAddTag: (String) -> Unit,
    onRenameTag: (Long, String) -> Unit,
    onDeleteTag: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dark = LocalDarkTheme.current
    val shadow = if (dark) ShadowDark else ShadowLight
    val accentBrush = Brush.verticalGradient(
        listOf(
            if (dark) AccentGradientStartDark else AccentGradientStart,
            if (dark) AccentGradientEndDark else AccentGradientEnd,
        ),
    )

    var customMode by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var selectedKey by remember { mutableStateOf(WorkoutPresets.first().key) }
    var selectedTagId by remember { mutableStateOf<Long?>(null) }
    var useCustomDuration by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(30) }
    var customDurationText by remember { mutableStateOf("") }
    var intensity by remember { mutableStateOf(1) }

    val preset = WorkoutPresets.first { it.key == selectedKey }
    val name = if (customMode) customName else stringResource(preset.nameRes)
    val colorKey = if (customMode) CUSTOM_COLOR else preset.color
    val valid = name.isNotBlank() && duration > 0

    var renameTagId by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (dark) DarkSheetBg else LightSheetBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.wk_log),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = rememberDayDateFmt().format(date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            SheetSectionLabel(stringResource(R.string.wk_exercise))
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WorkoutPresets.forEach { candidate ->
                    PresetChip(
                        icon = candidate.icon,
                        label = stringResource(candidate.nameRes),
                        color = Color(candidate.color),
                        selected = !customMode && selectedKey == candidate.key,
                        onClick = {
                            customMode = false
                            selectedKey = candidate.key
                        },
                    )
                }
                customTags.forEach { tag ->
                    PresetChip(
                        icon = Icons.Outlined.Add,
                        label = tag.name,
                        color = Color(CUSTOM_COLOR),
                        selected = customMode && selectedTagId == tag.id,
                        onClick = {
                            customMode = true
                            selectedTagId = tag.id
                            customName = tag.name
                        },
                        onLongClick = {
                            renameTagId = tag.id
                            renameText = tag.name
                        },
                    )
                }
                PresetChip(
                    icon = Icons.Outlined.Add,
                    label = stringResource(R.string.wk_custom),
                    color = Color(CUSTOM_COLOR),
                    selected = customMode && selectedTagId == null,
                    onClick = {
                        customMode = true
                        selectedTagId = null
                        customName = ""
                    },
                )
            }
            if (customMode) {
                Spacer(Modifier.height(10.dp))
                SheetInput(
                    value = customName,
                    onValueChange = { customName = it },
                    hint = stringResource(R.string.wk_name_hint),
                )
            }
            Spacer(Modifier.height(18.dp))
            SheetSectionLabel(stringResource(R.string.wk_duration))
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(15, 30, 45, 60, 90).forEach { mins ->
                    DurationChip(
                        text = "$mins",
                        selected = !useCustomDuration && duration == mins,
                        onClick = {
                            useCustomDuration = false
                            duration = mins
                        },
                    )
                }
                DurationChip(
                    text = stringResource(R.string.wk_custom),
                    selected = useCustomDuration,
                    onClick = {
                        useCustomDuration = true
                        duration = customDurationText.toIntOrNull() ?: 0
                    },
                )
            }
            if (useCustomDuration) {
                Spacer(Modifier.height(10.dp))
                SheetInput(
                    value = customDurationText,
                    onValueChange = {
                        customDurationText = it.filter { ch -> ch.isDigit() }.take(3)
                        duration = customDurationText.toIntOrNull() ?: 0
                    },
                    hint = stringResource(R.string.wk_min_hint),
                    keyboardType = KeyboardType.Number,
                )
            }
            Spacer(Modifier.height(18.dp))
            SheetSectionLabel(stringResource(R.string.wk_intensity))
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                options = listOf(
                    SegmentedOption(0, stringResource(R.string.wk_low)),
                    SegmentedOption(1, stringResource(R.string.wk_mid)),
                    SegmentedOption(2, stringResource(R.string.wk_high)),
                ),
                selected = intensity,
                onSelect = { intensity = it },
            )
            Spacer(Modifier.height(22.dp))
            // 保存（琥珀强调色预算之二；无效输入降透明置灰）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .alpha(if (valid) 1f else 0.45f)
                    .shadow(4.dp, RoundedCornerShape(24.dp), ambientColor = shadow, spotColor = shadow)
                    .clip(RoundedCornerShape(24.dp))
                    .background(accentBrush)
                    .then(
                        if (valid) {
                            Modifier.pressScale(
                                onPress = {
                                    if (customMode && selectedTagId == null && customName.trim().isNotEmpty()) {
                                        onAddTag(customName.trim())
                                    }
                                    onSave(
                                        name.trim(),
                                        if (customMode) CUSTOM_KEY else selectedKey,
                                        colorKey,
                                        duration,
                                        intensity,
                                    )
                                },
                                pressedScale = 0.96f,
                            )
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.sbe5fbb),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
        }
    }

    // 长按自定义标签：重命名 / 删除
    if (renameTagId != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renameTagId = null },
            title = { Text(renameText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.wk_name_hint)) },
                    // 模仿音乐页「添加音乐」输入框：无边框灰底胶囊（用户要求）
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onRenameTag(renameTagId!!, renameText.trim())
                        renameTagId = null
                    },
                ) { Text(stringResource(R.string.sbe5fbb)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onDeleteTag(renameTagId!!)
                        renameTagId = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.wk_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// 项目 chip：马卡龙底 + 图标/名称，选中全饱和 + 墨描边
@Composable
private fun PresetChip(
    icon: ImageVector,
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) color else color.copy(alpha = 0.45f))
            .border(
                1.5.dp,
                if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f) else Color.Transparent,
                RoundedCornerShape(14.dp),
            )
            .then(
                if (onLongClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { onLongClick() },
                        )
                    }
                } else {
                    Modifier.clickable(onClick = onClick)
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onInk(color),
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = onInk(color),
        )
    }
}

// 时长 chip：中性凹陷底，选中淡墨底 + 描边（不占琥珀预算）
@Composable
private fun DurationChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.10f else 0.04f))
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Sheet 通用输入框：淡底 + 细描边（同新建习惯输入框样式）
@Composable
private fun SheetInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (LocalDarkTheme.current) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
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
    }
}


// 统计 Tab：周/月/年范围切换 + 汇总卡 + 各项运动次数横向条形图
@Composable
private fun ColumnScope.StatsTab(viewModel: WorkoutViewModel) {
    var period by remember { mutableStateOf(0) } // 0 周 / 1 月 / 2 年
    val today = remember { LocalDate.now() }
    val (start, end) = remember(period) {
        when (period) {
            0 -> today.with(DayOfWeek.MONDAY) to today.with(DayOfWeek.SUNDAY)
            1 -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
            else -> today.withDayOfYear(1) to today.withDayOfYear(today.lengthOfYear())
        }
    }
    val workouts by remember(period, start, end) {
        viewModel.observeRange(start.toEpochDay(), end.toEpochDay())
    }.collectAsStateWithLifecycle(initialValue = emptyList<WorkoutEntity>())

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
    ) {
        SegmentedControl(
            options = listOf(
                SegmentedOption(0, stringResource(R.string.sx_p_w)),
                SegmentedOption(1, stringResource(R.string.sx_p_m)),
                SegmentedOption(2, stringResource(R.string.sx_p_y)),
            ),
            selected = period,
            onSelect = { period = it },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = remember(start, end) { formatRange(start, end) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        if (workouts.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            EmptyState(
                title = stringResource(R.string.sx_wk_empty),
                description = stringResource(R.string.sx_wk_empty_d),
            )
        } else {
            Spacer(Modifier.height(14.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${workouts.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.sx_total_cnt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${workouts.sumOf { it.durationMin }}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.sx_total_min),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.sx_chart_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    val byName = workouts
                        .groupBy { it.name }
                        .map { it.key to it.value.size }
                        .sortedByDescending { it.second }
                    val maxCount = byName.maxOf { it.second }
                    byName.forEachIndexed { i, (name, count) ->
                        if (i > 0) Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.34f),
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((count.toFloat() / maxCount).coerceAtLeast(0.05f))
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(MaterialTheme.colorScheme.onSurface),
                                )
                            }
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(bottomNavSpace()))
    }
}

// 统计范围文案：同年同月 "2026.09.01 – 07"，同年 "2026.09.01 – 09.07"，跨年全显
private fun formatRange(start: LocalDate, end: LocalDate): String = when {
    start.year == end.year && start.monthValue == end.monthValue ->
        String.format(
            Locale.ROOT, "%04d.%02d.%02d – %02d",
            start.year, start.monthValue, start.dayOfMonth, end.dayOfMonth,
        )
    start.year == end.year ->
        String.format(
            Locale.ROOT, "%04d.%02d.%02d – %02d.%02d",
            start.year, start.monthValue, start.dayOfMonth, end.monthValue, end.dayOfMonth,
        )
    else ->
        String.format(
            Locale.ROOT, "%04d.%02d.%02d – %04d.%02d.%02d",
            start.year, start.monthValue, start.dayOfMonth, end.year, end.monthValue, end.dayOfMonth,
        )
}

