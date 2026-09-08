package cn.wangce.lumi.ui.focus

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SmokingRooms
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.theme.DarkOnSurface
import cn.wangce.lumi.ui.theme.LightOnSurface
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.PillBgDark
import cn.wangce.lumi.ui.theme.PillBgLight
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// 打印条目（编辑态）：勾选 + 可编辑时长
private data class PrintItem(val label: String, val minutes: Int, val checked: Boolean, val count: Int = 1)

// 可补充的活动（与专注页固定分类一致，库中存中文键）：用于补记忘记计时的项目
private val PRINT_CATEGORIES = listOf("工作", "阅读", "学习", "运动", "冥想", "撸宠", "吸烟")

// 小票明细条目：项目 + 自定义时长 + 开始时间 + 次数
private data class ReceiptLine(val label: String, val minutes: Int, val start: String, val count: Int = 1)

// 小票点阵打印机字体（Fusion Pixel 12px zh_hans）
private val ReceiptFont = FontFamily(Font(R.font.fusion_pixel))
private val ReceiptBlack = Color(0xFF1A1A1A)
private val ReceiptPaper = Color(0xFFFFFFFF)

// 打印专注记录：编辑模式勾选项目/编辑时长 → 预览模式打印机吐票动画 → 自动保存小票到相册
@Composable
fun PrintScreen(viewModel: FocusViewModel = hiltViewModel(), onBack: () -> Unit) {
    val context = LocalContext.current
    val dark = LocalDarkTheme.current
    val haptic = LocalHapticFeedback.current
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    // 自定义项目 → 用户搭配的图标（编辑列表与小票共用）
    val customIcons = customCategories.associate { it.name to iconFromKey(it.iconKey) }

    var phase by remember { mutableStateOf("edit") } // edit / preview
    var items by remember { mutableStateOf(listOf<PrintItem>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    LaunchedEffect(stats) {
        if (items.isEmpty() && stats.topLabels.isNotEmpty()) {
            items = stats.topLabels.map { PrintItem(it.first, it.second, it.second > 0, stats.labelCounts[it.first] ?: 1) }
        }
    }

    // API < 29 保存相册需要存储权限
    val storagePerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) phase = "preview"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 顶栏：返回 + 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.focus_print_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (phase == "edit") {
            // ===== 编辑模式：勾选项目 + 时长编辑 =====
            Text(
                text = stringResource(R.string.focus_print_pick),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                if (items.isEmpty() && stats.topLabels.isEmpty()) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = stringResource(R.string.focus_print_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                items.forEachIndexed { i, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.checked,
                            onCheckedChange = { c ->
                                items = items.mapIndexed { j, it -> if (j == i) it.copy(checked = c) else it }
                            },
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = printIcon(item.label, customIcons),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = printLabel(item.label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        // 次数输入框（分钟左侧）
                        BasicTextField(
                            value = if (item.count > 0) item.count.toString() else "",
                            onValueChange = { s ->
                                val v = s.filter(Char::isDigit).take(3).toIntOrNull() ?: 0
                                items = items.mapIndexed { j, it -> if (j == i) it.copy(count = v) else it }
                            },
                            modifier = Modifier
                                .width(44.dp)
                                .height(40.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(10.dp),
                                )
                                .wrapContentSize(Alignment.Center),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.focus_print_count_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = if (item.minutes > 0) item.minutes.toString() else "",
                            onValueChange = { s ->
                                val v = s.filter(Char::isDigit).take(4).toIntOrNull() ?: 0
                                items = items.mapIndexed { j, it -> if (j == i) it.copy(minutes = v) else it }
                            },
                            modifier = Modifier
                                .width(52.dp)
                                .height(40.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(10.dp),
                                )
                                .wrapContentSize(Alignment.Center),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.focus_print_min_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(32.dp),
                        )
                    }
                }
                // 加号按钮：补记忘记计时的项目（活动/次数/分钟）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showAddDialog = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.focus_print_add),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            val canPrint = items.any { it.checked && it.minutes > 0 }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp)
                    .shadow(5.dp, RoundedCornerShape(16.dp), spotColor = if (dark) ShadowDark else ShadowLight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .clickable(enabled = canPrint) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        ) {
                            phase = "preview"
                        } else {
                            storagePerm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Print, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.focus_print_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            if (showAddDialog) {
                AddItemDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { label, count, minutes ->
                        showAddDialog = false
                        if (minutes > 0) {
                            items = if (items.any { it.label == label }) {
                                // 已有同项目：合并时长与次数（补记语义），并自动勾选
                                items.map {
                                    if (it.label == label) {
                                        it.copy(checked = true, count = it.count + count, minutes = it.minutes + minutes)
                                    } else {
                                        it
                                    }
                                }
                            } else {
                                items + PrintItem(label, minutes, checked = true, count = count)
                            }
                        }
                    },
                )
            }
        } else {
            // ===== 预览模式：打印机头 + 小票吐出动画 + 保存相册 =====
            val checked = items.filter { it.checked && it.minutes > 0 }
            val nowStr = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
            val records = checked.map { ReceiptLine(it.label, it.minutes, stats.labelStarts[it.label] ?: nowStr, it.count) }
            val totalRecords = checked.size
            val totalMinutes = checked.sumOf { it.minutes }

            val pull = remember { Animatable(0f) }
            val graphicsLayer = rememberGraphicsLayer()
            var receiptH by remember { mutableIntStateOf(0) }
            var doneReady by remember { mutableStateOf(false) }
            val density = LocalDensity.current

            // 吐票动画 → 完成后保存到相册
            LaunchedEffect(Unit) {
                delay(300)
                pull.animateTo(1f, tween(2600, easing = LinearEasing))
                delay(300)
                val ok = try {
                    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    withContext(Dispatchers.IO) { saveReceipt(context, bitmap) }
                } catch (e: Exception) {
                    false
                }
                if (ok) {
                    Toast.makeText(context, context.getString(R.string.focus_print_saved), Toast.LENGTH_SHORT).show()
                }
                doneReady = true
            }

            val scroll = rememberScrollState()
            // 打印机整体靠上：顶部只留很小的空隙，剩余空间全部留给小票向下生长
            Spacer(Modifier.weight(0.15f))
            PrinterHead(printing = !doneReady)
            Spacer(Modifier.height(2.dp))
            // 吐票：小票底边先从纸槽吐出并逐渐下移，上部内容最后露出；完成后可上下滑动查看整张小票
            Box(
                modifier = Modifier
                    .weight(2.7f)
                    .fillMaxWidth()
                    .clipToBounds(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll, enabled = doneReady),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = if (doneReady) {
                            Modifier
                        } else {
                            Modifier
                                .height(with(density) { (receiptH * pull.value).toInt().toDp() })
                                .clipToBounds()
                        },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        ReceiptCard(
                            records = records,
                            totalRecords = totalRecords,
                            totalMinutes = totalMinutes,
                            customIcons = customIcons,
                            modifier = Modifier
                                // 底对齐：露出窗口向下生长时，先露出小票底边（撕裂缘），
                                // 上部内容随打印推进在纸槽处逐渐出现，符合真实吐票方向
                                .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                                .onSizeChanged { receiptH = it.height }
                                .drawWithContent {
                                    graphicsLayer.record { this@drawWithContent.drawContent() }
                                    drawLayer(graphicsLayer)
                                },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (doneReady) {
                InteractionButton(
                    label = stringResource(R.string.focus_print_done),
                    icon = { Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp)) },
                    dark = dark,
                    onClick = onBack,
                )
            } else {
                Spacer(Modifier.height(46.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// 打印机头：黑色机身 + 指示灯 + 纸槽
@Composable
private fun PrinterHead(printing: Boolean) {
    val blink = rememberInfiniteTransition(label = "led").animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "led",
    )
    Box(
        modifier = Modifier
            .width(320.dp)
            .height(96.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x33000000))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF151515)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (printing) Color(0xFFFF6A3D).copy(alpha = blink.value) else Color(0xFF4CAF50)),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "SISYPHUS · PRINTER",
                    fontFamily = ReceiptFont,
                    fontSize = 12.sp,
                    color = Color(0xFFEDEDED),
                )
            }
        }
        // 纸槽
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .width(260.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF000000)),
        )
    }
}

// 小票：白底黑字点阵字体 + 锯齿底边（版式参考 ANYLOG：明细 → 次数统计 → 合计）
@Composable
private fun ReceiptCard(
    records: List<ReceiptLine>,
    totalRecords: Int,
    totalMinutes: Int,
    customIcons: Map<String, ImageVector> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val dateStr = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) }
    val printedAt = remember { LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")) }
    val noStr = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) }
    Column(modifier = modifier.width(280.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ReceiptPaper)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("SISYPHUS", fontFamily = ReceiptFont, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ReceiptBlack)
            Spacer(Modifier.height(4.dp))
            Text(dateStr, fontFamily = ReceiptFont, fontSize = 12.sp, color = ReceiptBlack.copy(alpha = 0.75f))
            Spacer(Modifier.height(12.dp))
            DashedLine()
            Spacer(Modifier.height(10.dp))
            // 明细区：开始时间 + 图标 + 项目名 ····· 时长
            records.forEach { line ->
                ReceiptRow(line)
                Spacer(Modifier.height(7.dp))
            }
            Spacer(Modifier.height(4.dp))
            DashedLine()
            Spacer(Modifier.height(10.dp))
            // 统计区：图标 + 项目名 ····· 进行次数
            records.forEach { line ->
                ReceiptStatRow(line.label, line.count, customIcons)
                Spacer(Modifier.height(7.dp))
            }
            Spacer(Modifier.height(4.dp))
            DashedLine()
            Spacer(Modifier.height(10.dp))
            // 合计：合计 ····· N 条记录
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.focus_print_total), fontFamily = ReceiptFont, fontSize = 13.sp, color = ReceiptBlack)
                ReceiptDots(Modifier.weight(1f))
                Text(
                    stringResource(R.string.focus_print_records_fmt, totalRecords),
                    fontFamily = ReceiptFont, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ReceiptBlack,
                )
                Spacer(Modifier.width(6.dp))
                Text("${totalMinutes}m", fontFamily = ReceiptFont, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ReceiptBlack)
            }
            Spacer(Modifier.height(8.dp))
            ReceiptKV(stringResource(R.string.focus_print_events), totalRecords.toString())
            Spacer(Modifier.height(4.dp))
            ReceiptKV(stringResource(R.string.focus_print_printed_at), printedAt)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Barcode(Modifier.fillMaxWidth(0.8f).height(38.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.focus_print_no, noStr), fontFamily = ReceiptFont, fontSize = 11.sp, color = ReceiptBlack)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.focus_print_done_flag), fontFamily = ReceiptFont, fontSize = 13.sp, color = ReceiptBlack)
        }
        // 撕裂锯齿底边 + 纸影（真实撕断效果）
        ReceiptTornEdge()
    }
}

// 小票锯齿底边：白色三角撕裂缘 + 向下渐隐、两端羽化的柔和纸影
@Composable
private fun ReceiptTornEdge() {
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        val toothW = 4.5.dp.toPx()
        val toothH = 3.dp.toPx()
        val count = ceil(size.width / toothW).toInt()
        val path = Path().apply {
            moveTo(0f, 0f)
            for (i in 0 until count) {
                val x0 = i * toothW
                lineTo(x0 + toothW / 2f, toothH)
                lineTo(min(x0 + toothW, size.width), 0f)
            }
            close()
        }
        // 纸影：垂直渐隐 × 两端羽化（离屏层 + DST_IN 蒙版）
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            val rect = android.graphics.RectF(0f, toothH * 0.3f, size.width, size.height)
            val saveCount = nc.saveLayer(rect, null)
            nc.drawRect(
                rect,
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    shader = android.graphics.LinearGradient(
                        0f, rect.top, 0f, rect.bottom,
                        intArrayOf(0x00000000, 0x30000000, 0x00000000),
                        floatArrayOf(0f, 0.22f, 1f),
                        android.graphics.Shader.TileMode.CLAMP,
                    )
                },
            )
            nc.drawRect(
                rect,
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
                    shader = android.graphics.LinearGradient(
                        0f, 0f, size.width, 0f,
                        intArrayOf(0x00FFFFFF, android.graphics.Color.WHITE, android.graphics.Color.WHITE, 0x00FFFFFF),
                        floatArrayOf(0f, 0.12f, 0.88f, 1f),
                        android.graphics.Shader.TileMode.CLAMP,
                    )
                },
            )
            nc.restoreToCount(saveCount)
        }
        // 白色锯齿（纸的延伸）
        drawPath(path, ReceiptPaper)
    }
}

// 小票明细行：开始时间 · 项目名    时长（无引导虚线）
@Composable
private fun ReceiptRow(line: ReceiptLine) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(line.start, fontFamily = ReceiptFont, fontSize = 11.sp, color = ReceiptBlack.copy(alpha = 0.8f))
        Spacer(Modifier.width(6.dp))
        Text(printLabel(line.label), fontFamily = ReceiptFont, fontSize = 11.sp, color = ReceiptBlack, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(formatReceiptMin(line.minutes), fontFamily = ReceiptFont, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ReceiptBlack)
    }
}

// 小票统计行：图标 · 项目名    × N（无引导虚线）
@Composable
private fun ReceiptStatRow(label: String, count: Int, customIcons: Map<String, ImageVector> = emptyMap()) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(printIcon(label, customIcons), null, tint = ReceiptBlack, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(6.dp))
        Text(printLabel(label), fontFamily = ReceiptFont, fontSize = 11.sp, color = ReceiptBlack, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text("× $count", fontFamily = ReceiptFont, fontSize = 13.sp, color = ReceiptBlack)
    }
}

// 小票左右信息行（事件 / 打印时间）
@Composable
private fun ReceiptKV(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, fontFamily = ReceiptFont, fontSize = 11.sp, color = ReceiptBlack.copy(alpha = 0.7f))
        Text(value, fontFamily = ReceiptFont, fontSize = 11.sp, color = ReceiptBlack.copy(alpha = 0.7f))
    }
}

// 引导点线（密集虚线）
@Composable
private fun ReceiptDots(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .padding(horizontal = 8.dp)
            .height(2.dp),
    ) {
        drawLine(
            ReceiptBlack.copy(alpha = 0.5f),
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx())),
        )
    }
}

// 虚线分隔
@Composable
private fun DashedLine() {
    Canvas(Modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            ReceiptBlack.copy(alpha = 0.55f),
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 2.dp.toPx())),
        )
    }
}

// 条形码：随机宽窄竖线（固定序列，稳定渲染）
@Composable
private fun Barcode(modifier: Modifier = Modifier) {
    val bars = remember { List(64) { if (Random.nextBoolean()) 2.dp else Random.nextInt(4, 7).dp } }
    Canvas(modifier) {
        var x = 0f
        var i = 0
        while (x < size.width && i < bars.size) {
            val bw = bars[i].toPx()
            drawRect(ReceiptBlack, topLeft = Offset(x, 0f), size = Size(bw, size.height))
            x += bw + 2.5.dp.toPx()
            i++
        }
    }
}

private fun formatReceiptMin(min: Int): String =
    if (min >= 60) "${min / 60}h ${min % 60}m" else "${min}m"

// 保存小票到相册 Pictures/Sisyphus（API 29+ MediaStore，26-28 传统路径）
private fun saveReceipt(context: Context, bitmap: Bitmap): Boolean = try {
    val name = "sisyphus_receipt_${System.currentTimeMillis()}.png"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Sisyphus")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            false
        } else {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: false
        }
    } else {
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Sisyphus")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, name)
        file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
    }
    true
} catch (e: Exception) {
    false
}

// 添加项目弹窗：下拉框选活动 + 次数/分钟输入（补记忘记计时的项目）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemDialog(onDismiss: () -> Unit, onConfirm: (String, Int, Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf(PRINT_CATEGORIES.first()) }
    var count by remember { mutableStateOf("1") }
    var minutes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        // 与专注页自定义项目弹窗一致的背景色（默认色偏粉）
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.focus_print_add)) },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = printLabel(label),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.focus_print_add_activity)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        PRINT_CATEGORIES.forEach { key ->
                            DropdownMenuItem(
                                text = { Text(printLabel(key)) },
                                onClick = {
                                    label = key
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = count,
                        onValueChange = { s -> count = s.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.focus_print_add_count)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { s -> minutes = s.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.focus_print_min_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        label,
                        (count.toIntOrNull() ?: 1).coerceAtLeast(1),
                        minutes.toIntOrNull() ?: 0,
                    )
                },
                enabled = (minutes.toIntOrNull() ?: 0) > 0,
            ) { Text(stringResource(R.string.focus_print_add_btn)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.s625fb2)) }
        },
    )
}

// 底部互动胶囊按钮（与撸宠/吸烟页同款语言）
@Composable
private fun InteractionButton(label: String, icon: @Composable () -> Unit, dark: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .shadow(5.dp, RoundedCornerShape(14.dp), spotColor = if (dark) ShadowDark else ShadowLight)
            .clip(RoundedCornerShape(14.dp))
            .background(if (dark) PillBgDark else PillBgLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

// 分类显示名（与专注页弹窗一致：库中存中文键，展示层按语言映射）
@Composable
private fun printLabel(stored: String): String = when (stored) {
    "工作" -> stringResource(R.string.s9a018b)
    "阅读" -> stringResource(R.string.s687a7e)
    "学习" -> stringResource(R.string.s4ef520)
    "运动" -> stringResource(R.string.s37b6de)
    "冥想" -> stringResource(R.string.s4baafe)
    "撸宠" -> stringResource(R.string.focus_label_pet)
    "吸烟" -> stringResource(R.string.focus_label_smoke)
    else -> stored
}

// 分类线条图标（与专注页弹窗一致；自定义项目优先取用户搭配的图标）
private fun printIcon(stored: String, custom: Map<String, ImageVector> = emptyMap()) =
    custom[stored] ?: when (stored) {
        "工作" -> Icons.Outlined.WorkOutline
        "阅读" -> Icons.Outlined.MenuBook
        "学习" -> Icons.Outlined.School
        "运动" -> Icons.Outlined.FitnessCenter
        "冥想" -> Icons.Outlined.SelfImprovement
        "撸宠" -> Icons.Outlined.Pets
        "吸烟" -> Icons.Outlined.SmokingRooms
        else -> Icons.Outlined.Check
    }
