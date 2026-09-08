package cn.wangce.lumi.ui.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SmokingRooms
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.local.FocusCategoryEntity
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.bottomNavSpace
import cn.wangce.lumi.ui.theme.DarkOnSurface
import cn.wangce.lumi.ui.theme.DarkOnSurfaceVariant
import cn.wangce.lumi.ui.theme.DarkSheetBg
import cn.wangce.lumi.ui.theme.LightOnSurface
import cn.wangce.lumi.ui.theme.LightSheetBg
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.PillBgDark
import cn.wangce.lumi.ui.theme.PillBgLight
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.min
import kotlinx.coroutines.delay

// 专注页（Sisyphus 石头专注法）：待机欢迎态 / 计时态 / 统计视图，黑白极简
@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel(),
    onOpenPet: () -> Unit = {},
    onOpenSmoke: () -> Unit = {},
    onOpenPrint: () -> Unit = {},
) {
    val running by viewModel.running.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    var showStats by remember { mutableStateOf(false) }
    var terrain by remember { mutableStateOf("hill") } // hill 上坡 / ground 平地（线条形态）
    var elapsedSec by remember { mutableIntStateOf(0) }
    // 分类弹层展开态（由 IdleView 内 CategoryPicker 上抛）：展开时整页内容高斯模糊
    var pickerOpen by remember { mutableStateOf(false) }

    // 固定功能（撸宠/吸烟）：选中即开始计时并跳转独立互动页，计时/落库/通知与其他项目一致
    val pickFixed: (String) -> Unit = { key ->
        viewModel.setCategory(key)
        viewModel.start()
        when (key) {
            "撸宠" -> onOpenPet()
            "吸烟" -> onOpenSmoke()
        }
    }

    // 计时 tick：每 500ms 刷新一次大数字
    LaunchedEffect(running) {
        while (running) {
            elapsedSec = viewModel.elapsedSeconds()
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            // Popup 是独立窗口，无法被兄弟节点模糊，改为对页面内容自身加 blur
            .blur(if (pickerOpen) 20.dp else 0.dp),
    ) {
        when {
            showStats -> StatsView(stats = stats, onBack = { showStats = false })
            running -> RunningView(
                category = category,
                terrain = terrain,
                elapsedSec = elapsedSec,
                onFinish = viewModel::finish,
                onAbandon = viewModel::abandon,
            )
            else -> IdleView(
                category = category,
                customCategories = customCategories,
                terrain = terrain,
                pickerOpen = pickerOpen,
                onPickerOpenChange = { pickerOpen = it },
                onTerrainChange = { terrain = it },
                onPickCategory = viewModel::setCategory,
                onPickFixed = pickFixed,
                onAddCategory = { name, icon -> viewModel.addCategory(name, icon) },
                onDeleteCategory = viewModel::deleteCategory,
                onStart = viewModel::start,
                onOpenStats = { showStats = true },
                onOpenPrint = onOpenPrint,
            )
        }
    }
}

// ============ 待机态：西西弗斯正在等你 ============

@Composable
private fun IdleView(
    category: String,
    customCategories: List<FocusCategoryEntity>,
    terrain: String,
    pickerOpen: Boolean,
    onPickerOpenChange: (Boolean) -> Unit,
    onTerrainChange: (String) -> Unit,
    onPickCategory: (String) -> Unit,
    onPickFixed: (String) -> Unit,
    onAddCategory: (String, String) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onStart: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenPrint: () -> Unit,
) {
    val dark = LocalDarkTheme.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 统计入口（左上圆钮）
            GlassCard(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenStats),
                cornerRadius = 22,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = stringResource(R.string.s042f76),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            // 打印入口（右上圆钮）：打印专注记录小票
            GlassCard(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenPrint),
                cornerRadius = 22,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Print,
                        contentDescription = stringResource(R.string.focus_print_title),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = stringResource(R.string.s6d6126),
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(30.dp))
        SisyphusCanvas(terrain = terrain, running = false, modifier = Modifier.size(width = 150.dp, height = 56.dp))
        Spacer(Modifier.height(46.dp))

        // 分类选择：工作 ›
        CategoryPicker(
            category = category,
            customCategories = customCategories,
            expanded = pickerOpen,
            onExpandedChange = onPickerOpenChange,
            onPick = onPickCategory,
            onPickFixed = onPickFixed,
            onAddCategory = onAddCategory,
            onDeleteCategory = onDeleteCategory,
        )
        Spacer(Modifier.height(30.dp))

        // GO 黑色圆钮
        Box(
            modifier = Modifier
                .size(78.dp)
                .shadow(6.dp, CircleShape, spotColor = if (dark) ShadowDark else ShadowLight)
                .clip(CircleShape)
                .background(if (dark) PillBgDark else PillBgLight)
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "GO",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.surface,
            )
        }

        Spacer(Modifier.weight(1f))

        // 地形切换：Hill / Ground（决定线条形态）
        TerrainSwitch(terrain = terrain, onSelect = onTerrainChange)
        Spacer(Modifier.height(bottomNavSpace()))
    }
}

// ============ 计时态：大数字 + 石头在坡上 ============

@Composable
private fun RunningView(
    category: String,
    terrain: String,
    elapsedSec: Int,
    onFinish: () -> Unit,
    onAbandon: () -> Unit,
) {
    val dark = LocalDarkTheme.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            text = categoryLabel(category),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))

        Text(
            text = formatFocusTime(elapsedSec),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 56.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(34.dp))
        SisyphusCanvas(terrain = terrain, running = true, elapsedSec = elapsedSec, modifier = Modifier.size(width = 210.dp, height = 78.dp))

        Spacer(Modifier.weight(1f))

        // Finish 黑胶囊
        Box(
            modifier = Modifier
                .height(52.dp)
                .fillMaxWidth(0.62f)
                .shadow(5.dp, RoundedCornerShape(16.dp), spotColor = if (dark) ShadowDark else ShadowLight)
                .clip(RoundedCornerShape(16.dp))
                .background(if (dark) PillBgDark else PillBgLight)
                .clickable(onClick = onFinish),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Finish",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.surface,
            )
        }
        TextButton(onClick = onAbandon) {
            Text(
                text = "Give up",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(bottomNavSpace()))
    }
}

// ============ 统计视图：石头数 / 分钟 / 连续 + 月历点阵 + Top 分类 ============

@Composable
private fun StatsView(stats: FocusStats, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassCard(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                cornerRadius = 22,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.s5f4112),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.s042f76),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(44.dp))
        }
        Spacer(Modifier.height(30.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RockIcon(modifier = Modifier.size(width = 64.dp, height = 48.dp))
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatColumn(value = stats.rocks.toString(), label = stringResource(R.string.s9a6ac1), modifier = Modifier.weight(1f))
                StatColumn(value = stats.totalMinutes.toString(), label = stringResource(R.string.s3a17b7), modifier = Modifier.weight(1f))
                StatColumn(value = stats.streak.toString(), label = stringResource(R.string.s2a1a7d), modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(30.dp))
        MonthDotGrid(monthSeconds = stats.monthSeconds)
        Spacer(Modifier.height(26.dp))
        TopLabelBars(topLabels = stats.topLabels)
        Spacer(Modifier.height(bottomNavSpace()))
    }
}

@Composable
private fun StatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ============ 公共小部件 ============

// 分类选择：文字 + 右箭头；点击弹出底部弹窗（普通项目带线条图标 + 固定功能撸宠/吸烟）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    category: String,
    customCategories: List<FocusCategoryEntity>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPick: (String) -> Unit,
    onPickFixed: (String) -> Unit,
    onAddCategory: (String, String) -> Unit,
    onDeleteCategory: (Long) -> Unit,
) {
    val dark = LocalDarkTheme.current
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDeleteCat by remember { mutableStateOf<FocusCategoryEntity?>(null) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = { onExpandedChange(true) })
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = categoryLabel(category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        if (expanded) {
            ModalBottomSheet(
                onDismissRequest = { onExpandedChange(false) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = if (dark) DarkSheetBg else LightSheetBg,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp),
                ) {
                    // 普通项目：单选，点击后收起弹窗
                    FocusViewModel.CATEGORIES.forEach { key ->
                        val selected = key == category
                        SheetRow(
                            icon = categoryIcon(key),
                            label = categoryLabel(key),
                            selected = selected,
                            onClick = {
                                onExpandedChange(false)
                                onPick(key)
                            },
                        )
                    }
                    // 自定义项目：用户添加的，长按可删除
                    customCategories.forEach { cat ->
                        val selected = cat.name == category
                        SheetRow(
                            icon = iconFromKey(cat.iconKey),
                            label = cat.name,
                            selected = selected,
                            onClick = {
                                onExpandedChange(false)
                                onPick(cat.name)
                            },
                            onLongClick = {
                                pendingDeleteCat = cat
                            },
                        )
                    }
                    // 自定义按钮：添加新项目
                    SheetRow(
                        icon = Icons.Outlined.Add,
                        label = stringResource(R.string.wk_custom),
                        selected = false,
                        onClick = { showAddDialog = true },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    )
                    Text(
                        text = stringResource(R.string.focus_fixed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    // 固定功能：点击即开始计时并跳转互动页
                    FocusViewModel.FIXED_FUNCS.forEach { key ->
                        SheetRow(
                            icon = categoryIcon(key),
                            label = categoryLabel(key),
                            selected = false,
                            trailing = Icons.Outlined.KeyboardArrowRight,
                            onClick = {
                                onExpandedChange(false)
                                onPickFixed(key)
                            },
                        )
                    }
                }
            }
        }
        // 自定义项目输入弹窗（名称 + 搭配图标）
        if (showAddDialog) {
            var input by remember { mutableStateOf("") }
            var pickedIcon by remember { mutableStateOf(FOCUS_PRESET_ICONS.first().first) }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = stringResource(R.string.wk_custom),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                text = {
                    Column {
                        androidx.compose.material3.OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.focus_custom_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                        Spacer(Modifier.height(14.dp))
                        // 预设图标：横向一行，点选搭配
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(FOCUS_PRESET_ICONS.size) { i ->
                                val pair = FOCUS_PRESET_ICONS[i]
                                val selected = pair.first == pickedIcon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.12f else 0.04f)
                                        )
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                            shape = CircleShape,
                                        )
                                        .clickable(onClick = { pickedIcon = pair.first }),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = pair.second,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (input.isNotBlank()) {
                            onAddCategory(input.trim(), pickedIcon)
                            showAddDialog = false
                        }
                    }) {
                        Text(text = stringResource(R.string.sbe5fbb))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text(text = stringResource(R.string.s625fb2))
                    }
                },
            )
        }
        // 删除自定义项目确认弹窗
        pendingDeleteCat?.let { cat ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { pendingDeleteCat = null },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = stringResource(R.string.s2f4aad) + " " + cat.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteCategory(cat.id)
                        pendingDeleteCat = null
                    }) {
                        Text(
                            text = stringResource(R.string.s2f4aad),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteCat = null }) {
                        Text(text = stringResource(R.string.s625fb2))
                    }
                },
            )
        }
    }
}

// 弹窗行：线条图标 + 名称（+ 选中勾 / 右箭头）
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    trailing: ImageVector? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f) else Color.Transparent)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        when {
            selected -> Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            trailing != null -> Icon(
                imageVector = trailing,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// 地形切换胶囊：Hill / Ground
@Composable
private fun TerrainSwitch(terrain: String, onSelect: (String) -> Unit) {
    val dark = LocalDarkTheme.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("hill" to "Hill", "ground" to "Ground").forEach { (key, label) ->
            val selected = terrain == key
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            selected && !dark -> MaterialTheme.colorScheme.surface
                            selected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            else -> Color.Transparent
                        },
                    )
                    .border(
                        1.dp,
                        if (selected && !dark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f) else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(key) }
                    .padding(horizontal = 18.dp, vertical = 7.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// 西西弗斯线条 + 石头（圆角正方形，一角触线）：hill 折线（∧）/ ground 平线；running 时石头沿坡持续滚动（30s 一轮，滚动角随位移）
@Composable
private fun SisyphusCanvas(terrain: String, running: Boolean, elapsedSec: Int = 0, modifier: Modifier = Modifier) {
    val dark = LocalDarkTheme.current
    Canvas(modifier = modifier) {
        // 黑白风：浅色黑线黑石，深色浅灰线 + 浅灰白石（保证在深底上可辨）
        val lineColor = if (dark) DarkOnSurfaceVariant else LightOnSurface
        val rockColor = if (dark) DarkOnSurface else LightOnSurface
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        val rs = 13.dp.toPx() // 圆角正方形半边
        if (terrain == "hill") {
            // 上坡折线：左底 → 坡顶 → 右缓降
            val peakX = size.width * 0.52f
            val peakY = size.height * 0.10f
            val path = Path().apply {
                moveTo(0f, size.height * 0.92f)
                lineTo(peakX, peakY)
                lineTo(size.width, size.height * 0.70f)
            }
            drawPath(path, color = lineColor, style = stroke)
            if (running) {
                // 石头沿折线滚动：坡底 → 山顶 → 右侧，30 秒一轮后重来
                val a = Offset(0f, size.height * 0.92f)
                val b = Offset(peakX, peakY)
                val c = Offset(size.width, size.height * 0.70f)
                val l1 = hypot(b.x - a.x, b.y - a.y)
                val l2 = hypot(c.x - b.x, c.y - b.y)
                val dist = ((elapsedSec % 30) / 30f) * (l1 + l2)
                val pos: Offset
                val tangent: Offset
                if (dist <= l1) {
                    val t = if (l1 == 0f) 0f else dist / l1
                    pos = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
                    tangent = if (l1 == 0f) Offset(1f, 0f) else Offset((b.x - a.x) / l1, (b.y - a.y) / l1)
                } else {
                    val t = if (l2 == 0f) 0f else (dist - l1) / l2
                    pos = Offset(b.x + (c.x - b.x) * t, b.y + (c.y - b.y) * t)
                    tangent = if (l2 == 0f) Offset(1f, 0f) else Offset((c.x - b.x) / l2, (c.y - b.y) / l2)
                }
                // 沿法线朝上偏移约半对角（0.608s）：让正方形的一个圆角恰好接触坡线
                val n = Offset(tangent.y, -tangent.x)
                val center = Offset(pos.x + n.x * rs * 1.22f, pos.y + n.y * rs * 1.22f)
                val degrees = Math.toDegrees(dist / rs.toDouble()).toFloat()
                rotate(degrees = degrees, pivot = center) {
                    drawPath(rockPath(Size(rs * 2f, rs * 2f), center), color = rockColor)
                }
            }
        } else {
            val groundY = size.height * 0.62f
            drawLine(lineColor, Offset(0f, groundY), Offset(size.width, groundY), strokeWidth = stroke.width, cap = StrokeCap.Round)
            if (running) {
                // 平地：从左往右循环滚动（圆角触地）
                val travel = size.width + rs * 2f
                val x = -rs + ((elapsedSec % 30) / 30f) * travel
                val center = Offset(x, groundY - rs * 1.22f)
                val degrees = Math.toDegrees((x + rs) / rs.toDouble()).toFloat()
                rotate(degrees = degrees, pivot = center) {
                    drawPath(rockPath(Size(rs * 2f, rs * 2f), center), color = rockColor)
                }
            }
        }
    }
}

// 黑色圆角正方形石头（一角触线）：只构造 Path，由调用方在 DrawScope 内 drawPath
private fun rockPath(size: Size, center: Offset): Path {
    val s = min(size.width, size.height)
    val r = s * 0.24f
    val cx = center.x
    val cy = center.y
    return Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(cx - s / 2f, cy - s / 2f, cx + s / 2f, cy + s / 2f),
                cornerRadius = CornerRadius(r, r),
            ),
        )
    }
}

// 统计页顶部小石头
@Composable
private fun RockIcon(modifier: Modifier = Modifier) {
    val dark = LocalDarkTheme.current
    Canvas(modifier = modifier) {
        drawPath(
            rockPath(Size(min(size.width, 58.dp.toPx()), size.height), Offset(size.width / 2f, size.height / 2f)),
            color = if (dark) DarkOnSurface else LightOnSurface,
        )
    }
}

// 当月月历点阵：一格一天，专注时长 → 黑点强度
@Composable
private fun MonthDotGrid(monthSeconds: Map<LocalDate, Int>) {
    val zone = ZoneId.systemDefault()
    val dark = LocalDarkTheme.current
    val dotColor = if (dark) DarkOnSurface else LightOnSurface
    val today = remember { LocalDate.now(zone) }
    val daysInMonth = remember(today) { today.lengthOfMonth() }
    val leadingBlanks = remember(today) { today.withDayOfMonth(1).dayOfWeek.value - 1 }
    val dayLabels = listOf(
        stringResource(R.string.s7941da),
        stringResource(R.string.s2d8be2),
        stringResource(R.string.se662ff),
        stringResource(R.string.s21716c),
        stringResource(R.string.s1fcc29),
        stringResource(R.string.s61b453),
        stringResource(R.string.s3edddd),
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.focus_month, today.monthValue),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..daysInMonth).map { today.withDayOfMonth(it) }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                week.forEach { day ->
                    Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val seconds = monthSeconds[day] ?: 0
                            Canvas(modifier = Modifier.size(13.dp)) {
                                when {
                                    seconds > 0 -> {
                                        // 强度：30 分钟封顶
                                        val alpha = (seconds / 1800f).coerceIn(0.4f, 1f)
                                        drawCircle(color = dotColor.copy(alpha = alpha), radius = size.minDimension / 2f)
                                    }
                                    day == today -> {
                                        drawCircle(color = dotColor.copy(alpha = 0.18f), radius = size.minDimension / 2f, style = Stroke(width = 1.5.dp.toPx()))
                                    }
                                    else -> {
                                        drawCircle(color = dotColor.copy(alpha = 0.07f), radius = size.minDimension / 2f)
                                    }
                                }
                            }
                        }
                    }
                }
                // 补齐尾行空位
                repeat(7 - week.size) { Spacer(Modifier.size(30.dp)) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

// Top 5 分类条形（黑白：黑条 + 分类名 + 分钟数）
@Composable
private fun TopLabelBars(topLabels: List<Pair<String, Int>>) {
    if (topLabels.isEmpty()) return
    val dark = LocalDarkTheme.current
    val maxMinutes = topLabels.first().second.coerceAtLeast(1)
    Column {
        Text(
            text = "Top ${topLabels.size}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        topLabels.forEach { (label, minutes) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = categoryLabel(label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(64.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(minutes / maxMinutes.toFloat())
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (dark) DarkOnSurface else LightOnSurface),
                    )
                }
                Text(
                    text = stringResource(R.string.minutes_fmt, minutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(56.dp),
                    textAlign = TextAlign.End,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

// 分类显示名：库中存中文键（历史数据兼容），展示层按当前语言映射
@Composable
private fun categoryLabel(stored: String): String = when (stored) {
    "工作" -> stringResource(R.string.s9a018b)
    "阅读" -> stringResource(R.string.s687a7e)
    "学习" -> stringResource(R.string.s4ef520)
    "运动" -> stringResource(R.string.s37b6de)
    "冥想" -> stringResource(R.string.s4baafe)
    "撸宠" -> stringResource(R.string.focus_label_pet)
    "吸烟" -> stringResource(R.string.focus_label_smoke)
    else -> stored
}

// 分类线条图标（弹窗选项目用）
private fun categoryIcon(stored: String): ImageVector = when (stored) {
    "工作" -> Icons.Outlined.WorkOutline
    "阅读" -> Icons.Outlined.MenuBook
    "学习" -> Icons.Outlined.School
    "运动" -> Icons.Outlined.FitnessCenter
    "冥想" -> Icons.Outlined.SelfImprovement
    "撸宠" -> Icons.Outlined.Pets
    "吸烟" -> Icons.Outlined.SmokingRooms
    else -> Icons.Outlined.Category
}

// 计时格式：mm:ss / h:mm:ss
private fun formatFocusTime(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
