package cn.wangce.lumi.ui.expects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.local.ExpectEntity
import cn.wangce.lumi.ui.components.AppFab
import cn.wangce.lumi.ui.components.AppTextField
import cn.wangce.lumi.ui.components.DangerButton
import cn.wangce.lumi.ui.components.EmptyState
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.LumiDialog
import cn.wangce.lumi.ui.components.LumiDialogButtons
import cn.wangce.lumi.ui.components.PrimaryPillButton
import cn.wangce.lumi.ui.components.SecondaryButton
import cn.wangce.lumi.ui.components.SegmentedControl
import cn.wangce.lumi.ui.components.SegmentedOption
import cn.wangce.lumi.ui.components.bottomNavSpace
import cn.wangce.lumi.ui.components.pressScale
import cn.wangce.lumi.ui.theme.AccentInk
import cn.wangce.lumi.ui.theme.AccentPaper
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// 卡片/详情日期展示：数字式与黑白极简一致（yyyy.M.d）
private val CardDateFmt = DateTimeFormatter.ofPattern("yyyy.M.d", Locale.ROOT)

private data class ExpectCategory(val key: String, val labelRes: Int)

private val categories = listOf(
    ExpectCategory("all", R.string.sa8b0c2),
    ExpectCategory("festival", R.string.sx_cat_festival),
    ExpectCategory("life", R.string.sx_cat_life),
    ExpectCategory("work", R.string.s9a018b),
    ExpectCategory("study", R.string.s4ef520),
)

// 线性矢量图标（单色 Outlined，与添加习惯同风格）：emoji 字段存旧值兼容 + iconKey 存图标
private data class ExpectIcon(val key: String, val icon: ImageVector)

private val ExpectIcons = listOf(
    ExpectIcon("celebration", Icons.Outlined.Celebration),    // 庆祝
    ExpectIcon("favorite", Icons.Outlined.FavoriteBorder),    // 喜欢
    ExpectIcon("flower", Icons.Outlined.LocalFlorist),        // 花朵
    ExpectIcon("heart", Icons.Outlined.Favorite),             // 爱心
    ExpectIcon("child", Icons.Outlined.ChildCare),            // 儿童
    ExpectIcon("medal", Icons.Outlined.MilitaryTech),         // 奖章
    ExpectIcon("school", Icons.Outlined.School),              // 学业
    ExpectIcon("flag", Icons.Outlined.Flag),                  // 旗帜
    ExpectIcon("moon", Icons.Outlined.DarkMode),              // 夜晚
    ExpectIcon("tree", Icons.Outlined.Park),                  // 树木
    ExpectIcon("gift", Icons.Outlined.Redeem),                // 礼物
    ExpectIcon("spark", Icons.Outlined.AutoAwesome),          // 闪耀
    ExpectIcon("flight", Icons.Outlined.Flight),              // 旅行
    ExpectIcon("home", Icons.Outlined.Home),                  // 新家
    ExpectIcon("book", Icons.AutoMirrored.Outlined.MenuBook), // 阅读
    ExpectIcon("work", Icons.Outlined.Work),                  // 工作
    ExpectIcon("fitness", Icons.Outlined.FitnessCenter),      // 健身
)

// 旧版 emoji 数据归一化：历史记录里存的 emoji 映射到对应图标 key
private val LegacyEmojiMap = mapOf(
    "🎉" to "celebration", "🎂" to "celebration", "💝" to "favorite", "🌷" to "flower",
    "💗" to "heart", "🎈" to "child", "🎖️" to "medal", "🎓" to "school",
    "🍎" to "school", "🎊" to "flag", "🎃" to "moon", "🎄" to "tree",
    "🎁" to "gift", "🎆" to "spark", "⭐" to "spark", "✈️" to "flight",
    "🏠" to "home", "💼" to "work", "📚" to "book", "💪" to "fitness",
)

// 取期待项图标：iconKey 优先，旧 emoji 反查，分类兜底
private fun expectIconOf(item: ExpectEntity): ImageVector {
    val byEmoji = LegacyEmojiMap[item.emoji]
    val key = item.iconKey.ifEmpty { byEmoji ?: "" }.ifEmpty {
        when (item.category) {
            "festival" -> "celebration"
            "life" -> "favorite"
            "work" -> "work"
            "study" -> "book"
            else -> "celebration"
        }
    }
    return ExpectIcons.firstOrNull { it.key == key }?.icon ?: ExpectIcons.first().icon
}

// 期待页（顶层 Tab）：分类筛选 + 两列倒计时卡 + 详情/编辑/删除弹层
@Composable
fun ExpectScreen(onBack: (() -> Unit)? = null, viewModel: ExpectsViewModel = hiltViewModel()) {
    val expects by viewModel.expects.collectAsStateWithLifecycle()

    var category by remember { mutableStateOf("all") }
    var editorOpen by remember { mutableStateOf(false) }
    var editorFor by remember { mutableStateOf<ExpectEntity?>(null) }
    var detail by remember { mutableStateOf<ExpectEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<ExpectEntity?>(null) }

    val today = remember { LocalDate.now() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .then(
                if (editorOpen || detail != null || pendingDelete != null) {
                    Modifier.blur(20.dp)
                } else {
                    Modifier
                },
            ),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = stringResource(R.string.sx_entry),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.sx_exp_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))

        // 分类 chips（全部/节日/生活/工作/学习）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { c ->
                val active = category == c.key
                Text(
                    text = stringResource(c.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) {
                        if (LocalDarkTheme.current) AccentInk else Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            // 选中 = 墨黑填充（黑即强调，不随分屏强调色漂移）
                            if (active) {
                                if (LocalDarkTheme.current) AccentPaper else AccentInk
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            },
                        )
                        .clickable { category = c.key }
                        // 胶囊左右内边距 10dp：5 个 chips 总宽 310.5dp ≤ 可视区 320dp，无需滚动、「学习」不被裁切
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        val todayEpoch = today.toEpochDay()
        val visible = expects
            .filter { category == "all" || it.category == category }
            .sortedWith(compareBy({ it.targetEpochDay < todayEpoch }, { it.targetEpochDay }))

        Box(modifier = Modifier.weight(1f)) {
            if (visible.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.sx_exp_empty),
                    description = stringResource(R.string.sx_exp_empty_d),
                    modifier = Modifier.fillMaxSize(),
                    icon = Icons.Outlined.Celebration,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = bottomNavSpace() + 72.dp),
                ) {
                    items(visible, key = { it.id }) { item ->
                        ExpectCard(item = item, today = today, onClick = { detail = item })
                    }
                }
            }

            // 悬浮添加钮：品牌蓝圆钮（AppFab 统一样式）
            AppFab(
                onClick = { editorFor = null; editorOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // 顶层 Tab 有底栏 → 让位底栏；独立进入页无底栏 → 只留系统手势条安全距
                    .padding(
                        bottom = if (onBack != null) 20.dp else bottomNavSpace() - 20.dp,
                        // end 2dp：外层 Column 已有 20dp 水平内边距，合计 22dp，与其它页 FAB 对齐
                        end = 2.dp,
                    ),
                size = 52.dp,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.sx_add),
                    tint = LocalContentColor.current,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }

    detail?.let { item ->
        DetailDialog(
            item = item,
            today = today,
            onClose = { detail = null },
            onEdit = {
                editorFor = item
                detail = null
                editorOpen = true
            },
            onDelete = {
                pendingDelete = item
                detail = null
            },
        )
    }
    if (editorOpen) {
        EditorDialog(
            editing = editorFor,
            onClose = { editorOpen = false; editorFor = null },
            onSave = {
                viewModel.upsert(it)
                editorOpen = false
                editorFor = null
            },
        )
    }
    pendingDelete?.let { item ->
        DeleteConfirmDialog(
            item = item,
            onCancel = { pendingDelete = null },
            onConfirm = {
                viewModel.deleteById(item.id)
                pendingDelete = null
            },
        )
    }
}

// 倒计时卡：标题 + 大数字天数 + 日期/线条图标
@Composable
private fun ExpectCard(item: ExpectEntity, today: LocalDate, onClick: () -> Unit) {
    val days = (item.targetEpochDay - today.toEpochDay()).toInt()
    val date = LocalDate.ofEpochDay(item.targetEpochDay)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(onPress = onClick),
        cornerRadius = 20,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            DayNumberRow(days = days, big = false)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = CardDateFmt.format(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = expectIconOf(item),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

// 倒计时数字行：还有 N 天 / 已过 N 天 / 就是今天（数字加粗主导层级，已过整行降透明度）
@Composable
private fun DayNumberRow(days: Int, big: Boolean) {
    val numberStyle = if (big) {
        MaterialTheme.typography.displaySmall
    } else {
        MaterialTheme.typography.headlineSmall
    }
    val suffixStyle = if (big) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium
    val prefixStyle = if (big) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall

    Row(
        modifier = Modifier.alpha(if (days < 0) 0.55f else 1f),
        verticalAlignment = Alignment.Bottom,
    ) {
        when {
            days > 0 -> {
                Text(
                    text = stringResource(R.string.sx_still),
                    style = prefixStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$days",
                    style = numberStyle.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = stringResource(R.string.sx_days),
                    style = suffixStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            days < 0 -> {
                Text(
                    text = stringResource(R.string.sx_passed),
                    style = prefixStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${abs(days)}",
                    style = numberStyle.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = stringResource(R.string.sx_days_ago),
                    style = suffixStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            else -> Text(
                text = stringResource(R.string.sx_today),
                style = numberStyle.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// 详情弹层：大数字 + 信息列表（目标日期/天数/分类）+ 删除/编辑
@Composable
private fun DetailDialog(
    item: ExpectEntity,
    today: LocalDate,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val days = (item.targetEpochDay - today.toEpochDay()).toInt()
    val date = LocalDate.ofEpochDay(item.targetEpochDay)
    val locales = LocalConfiguration.current.locales
    val weekFmt = remember(locales) {
        DateTimeFormatter.ofPattern("EEEE", locales[0])
    }
    val catLabel = when (item.category) {
        "festival" -> stringResource(R.string.sx_cat_festival)
        "life" -> stringResource(R.string.sx_cat_life)
        "work" -> stringResource(R.string.s9a018b)
        "study" -> stringResource(R.string.s4ef520)
        else -> item.category
    }
    val countdownText = when {
        days > 0 ->
            stringResource(R.string.sx_still) + " " + days + " " + stringResource(R.string.sx_days)
        days < 0 ->
            stringResource(R.string.sx_passed) + " " + abs(days) + " " + stringResource(R.string.sx_days_ago)
        else -> stringResource(R.string.sx_today)
    }

    LumiDialog(
        onDismissRequest = onClose,
        title = item.title,
        actions = {
            LumiDialogButtons {
                // 删除：次级破坏性（error 淡底胶囊）
                DangerButton(
                    text = stringResource(R.string.s2f4aad),
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                )
                // 编辑：主操作品牌蓝胶囊
                PrimaryPillButton(
                    text = stringResource(R.string.s95b351),
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                DayNumberRow(days = days, big = true)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = CardDateFmt.format(date) + " · " + weekFmt.format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = expectIconOf(item),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            InfoRow(label = stringResource(R.string.sx_target), value = CardDateFmt.format(date))
            InfoDivider()
            InfoRow(label = stringResource(R.string.sx_count), value = countdownText)
            InfoDivider()
            InfoRow(label = stringResource(R.string.sx_cat), value = catLabel)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun InfoDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
    )
}

// 新增/编辑弹层：标题 + 图标 + 分类 + 年月日 + 保存
@Composable
private fun EditorDialog(
    editing: ExpectEntity?,
    onClose: () -> Unit,
    onSave: (ExpectEntity) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var iconKey by remember {
        mutableStateOf(
            editing?.iconKey?.takeIf { it.isNotEmpty() }
                ?: LegacyEmojiMap[editing?.emoji]
                ?: "celebration",
        )
    }
    var cat by remember { mutableStateOf(editing?.category?.takeIf { it.isNotEmpty() } ?: "life") }
    val initDate = remember {
        editing?.let { LocalDate.ofEpochDay(it.targetEpochDay) } ?: today.plusDays(7)
    }
    var y by remember { mutableStateOf(initDate.year.toString()) }
    var m by remember { mutableStateOf(initDate.monthValue.toString()) }
    var d by remember { mutableStateOf(initDate.dayOfMonth.toString()) }

    LumiDialog(
        onDismissRequest = onClose,
        title = stringResource(if (editing == null) R.string.sx_add else R.string.sx_edit_title),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            FieldLabel(stringResource(R.string.sx_name))
            Spacer(Modifier.height(6.dp))
            AppTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                hint = stringResource(R.string.sx_name_hint),
            )
            Spacer(Modifier.height(14.dp))
            FieldLabel(stringResource(R.string.s5ef69f))
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExpectIcons.forEach { choice ->
                    val active = choice.key == iconKey
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 0.08f else 0.04f))
                            .border(
                                1.dp,
                                if (active) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                } else {
                                    Color.Transparent
                                },
                                CircleShape,
                            )
                            .clickable { iconKey = choice.key },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = choice.icon,
                            contentDescription = null,
                            tint = if (active) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            FieldLabel(stringResource(R.string.sx_cat))
            Spacer(Modifier.height(6.dp))
            SegmentedControl(
                options = listOf(
                    SegmentedOption("festival", stringResource(R.string.sx_cat_festival)),
                    SegmentedOption("life", stringResource(R.string.sx_cat_life)),
                    SegmentedOption("work", stringResource(R.string.s9a018b)),
                    SegmentedOption("study", stringResource(R.string.s4ef520)),
                ),
                selected = cat,
                onSelect = { cat = it },
            )
            Spacer(Modifier.height(14.dp))
            FieldLabel(stringResource(R.string.sx_date))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(
                    value = y,
                    onValueChange = { y = it.take(4) },
                    hint = stringResource(R.string.sx_y),
                    modifier = Modifier.weight(1.2f),
                )
                DateField(
                    value = m,
                    onValueChange = { m = it.take(2) },
                    hint = stringResource(R.string.sx_m),
                    modifier = Modifier.weight(1f),
                )
                DateField(
                    value = d,
                    onValueChange = { d = it.take(2) },
                    hint = stringResource(R.string.sx_d),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))
            // 保存：主操作品牌蓝胶囊
            PrimaryPillButton(
                text = stringResource(R.string.sbe5fbb),
                onClick = {
                    val yy = y.toIntOrNull() ?: return@PrimaryPillButton
                    val mm = m.toIntOrNull() ?: return@PrimaryPillButton
                    val dd = d.toIntOrNull() ?: return@PrimaryPillButton
                    val target = try {
                        LocalDate.of(yy, mm, dd)
                    } catch (e: Exception) {
                        return@PrimaryPillButton
                    }
                    if (title.isBlank()) return@PrimaryPillButton
                    onSave(
                        ExpectEntity(
                            id = editing?.id ?: 0L,
                            title = title.trim(),
                            emoji = editing?.emoji ?: "🎉",
                            iconKey = iconKey,
                            targetEpochDay = target.toEpochDay(),
                            category = cat,
                            createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                height = 46.dp,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun DateField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    // 统一走 AppTextField：同款圆角 / 内边距 / 聚焦态 / 墨色光标，仅额外限制纯数字
    AppTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it.isDigit() }) },
        modifier = modifier,
        hint = hint,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

// 删除确认弹层
@Composable
private fun DeleteConfirmDialog(
    item: ExpectEntity,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    LumiDialog(
        onDismissRequest = onCancel,
        actions = {
            LumiDialogButtons {
                // 取消：次级按钮
                SecondaryButton(
                    text = stringResource(R.string.sx_cancel),
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    height = 42.dp,
                )
                // 确认删除：次级破坏性（error 淡底胶囊，与习惯页删除弹窗同语言）
                DangerButton(
                    text = stringResource(R.string.s2f4aad),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    height = 42.dp,
                )
            }
        },
    ) {
        Text(
            text = stringResource(R.string.del_confirm, item.title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
