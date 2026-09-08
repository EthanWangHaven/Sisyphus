package cn.wangce.lumi.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.wangce.lumi.R
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.pressScale
import cn.wangce.lumi.ui.theme.AccentGradientEnd
import cn.wangce.lumi.ui.theme.AccentGradientEndDark
import cn.wangce.lumi.ui.theme.AccentGradientStart
import cn.wangce.lumi.ui.theme.AccentGradientStartDark
import cn.wangce.lumi.ui.theme.DurState
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt
import org.json.JSONObject

// ============ 天气（Open-Meteo，无 key，零依赖） ============

// 实时天气：城市 + 温度 + 条目码（显示层按语言取天气文案、按条目码取线条图标）+ 未来 4 小时预报
data class WeatherData(
    val city: String,
    val temperatureC: Int,
    val conditionCode: Int,
    val hourly: List<HourlyTemp> = emptyList(),
)

// 逐小时预报点：hour 为 24 小时制数字（列表首项为当前小时，显示「现在」）
data class HourlyTemp(
    val hour: Int,
    val conditionCode: Int,
    val temperatureC: Int,
)

// WMO weather_code → 天气条目码（0晴 1多云 2阴 3雾 4细雨 5冻雨 6雨 7雪 8雷雨）
private fun wmoToWeather(code: Int): Int = when (code) {
    0 -> 0
    1, 2 -> 1
    3 -> 2
    45, 48 -> 3
    51, 53, 55 -> 4
    56, 57, 66, 67 -> 5
    61, 63, 65, 80, 81, 82 -> 6
    71, 73, 75, 77, 85, 86 -> 7
    95, 96, 99 -> 8
    else -> 1
}

// 天气条目码 → Material 线条图标（替代 emoji，用户要求）
private fun iconForCondition(code: Int): ImageVector = when (code) {
    0 -> Icons.Outlined.WbSunny
    1 -> Icons.Outlined.FilterDrama
    2 -> Icons.Outlined.Cloud
    3 -> Icons.Outlined.BlurOn
    4 -> Icons.Outlined.Grain
    5 -> Icons.Outlined.WaterDrop
    6 -> Icons.Outlined.Umbrella
    7 -> Icons.Outlined.AcUnit
    else -> Icons.Outlined.FlashOn
}

// HttpURLConnection + org.json 拉取上海实时天气；任何异常返回 null（调用方优雅隐藏）
fun fetchWeatherFromOpenMeteo(): WeatherData? = try {
    val conn = URL(
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=31.2304&longitude=121.4737" +
            "&current=temperature_2m,weather_code" +
            "&hourly=temperature_2m,weather_code&forecast_days=2&timezone=Asia%2FShanghai",
    ).openConnection() as HttpURLConnection
    conn.connectTimeout = 8_000
    conn.readTimeout = 8_000
    val body = conn.inputStream.use { it.bufferedReader().readText() }
    conn.disconnect()
    val root = JSONObject(body)
    val current = root.getJSONObject("current")
    val condition = wmoToWeather(current.getInt("weather_code"))
    // 逐小时：定位当前小时（上海时区）索引，取未来 4 个点；定位失败则留空（UI 隐藏该行）
    val hourly = try {
        val obj = root.getJSONObject("hourly")
        val times = obj.getJSONArray("time")
        val temps = obj.getJSONArray("temperature_2m")
        val codes = obj.getJSONArray("weather_code")
        val now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
        val key = "%04d-%02d-%02dT%02d".format(now.year, now.monthValue, now.dayOfMonth, now.hour)
        var start = -1
        for (i in 0 until times.length()) {
            if (times.getString(i).startsWith(key)) { start = i; break }
        }
        if (start >= 0 && start + 4 <= times.length()) {
            (start until start + 4).map { i ->
                HourlyTemp(
                    hour = times.getString(i).substring(11, 13).toInt(),
                    conditionCode = wmoToWeather(codes.getInt(i)),
                    temperatureC = temps.getDouble(i).roundToInt(),
                )
            }
        } else {
            emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
    WeatherData(
        city = "上海",
        temperatureC = current.getDouble("temperature_2m").roundToInt(),
        conditionCode = condition,
        hourly = hourly,
    )
} catch (_: Exception) {
    null
}

// ============ 本周日历胶囊栏 ============

// 周一至周日 7 胶囊，今日琥珀渐变圆钮（品牌强调色本屏两处之一，呼应 Lumi 烛光）
@Composable
fun WeekBar() {
    val dark = LocalDarkTheme.current
    val today = remember { LocalDate.now() }
    val days = remember(today) {
        (0..6).map { offset ->
            today.minusDays((today.dayOfWeek.value - 1 - offset).toLong())
        }
    }
    // 墨色玻璃：垂直渐变底 + 上亮下暗白描边，模拟玻璃受光
    val todayBg = if (dark) {
        Brush.verticalGradient(listOf(AccentGradientStartDark, AccentGradientEndDark))
    } else {
        Brush.verticalGradient(listOf(AccentGradientStart, AccentGradientEnd))
    }
    val todayBorder = Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color(0x0DFFFFFF)))
    // 周几标签（按当前语言取 一/二/… 或 Mon/Tue/…）
    val weekLabels = listOf(
        stringResource(R.string.s7941da),
        stringResource(R.string.s2d8be2),
        stringResource(R.string.se662ff),
        stringResource(R.string.s21716c),
        stringResource(R.string.s1fcc29),
        stringResource(R.string.s61b453),
        stringResource(R.string.s3edddd),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.forEach { day ->
            val isToday = day == today
            val bg by animateColorAsState(
                targetValue = if (isToday) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                },
                animationSpec = tween(DurState),
                label = "weekBg",
            )
            val modifier = if (isToday) {
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .shadow(
                        3.dp,
                        RoundedCornerShape(12.dp),
                        spotColor = if (dark) ShadowDark else ShadowLight,
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(todayBg)
                    .border(1.dp, todayBorder, RoundedCornerShape(12.dp))
            } else {
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
            }
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = weekLabels[day.dayOfWeek.value - 1],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isToday) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ============ 环形进度 + 天气主卡 ============

// 仪表盘主卡：左侧环形完成进度 + 右侧实时天气（无天气时几何点缀，布局高度稳定）
// 黑色 hero 卡（对标参考图「350 天」黑卡）：tertiary 实底 + onTertiary 内容，深浅模式自动反色
@Composable
fun ProgressWeatherCard(pending: Int, done: Int, weather: WeatherData?) {
    val dark = LocalDarkTheme.current
    val heroShape = RoundedCornerShape(24.dp)
    val heroShadow = if (dark) ShadowDark else ShadowLight
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, heroShape, ambientColor = heroShadow, spotColor = heroShadow)
            .clip(heroShape)
            .background(MaterialTheme.colorScheme.tertiary),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RingProgress(done = done, total = pending + done, modifier = Modifier.size(108.dp))
            Spacer(Modifier.width(20.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (weather != null) {
                    WeatherColumn(weather)
                } else {
                    GeometryDecoration()
                }
            }
        }
    }
}

// 环形进度：onTertiary 单色环（hero 卡内反色内容），中心 done/total +「已完成」
@Composable
private fun RingProgress(done: Int, total: Int, modifier: Modifier = Modifier) {
    val target = if (total == 0) 0f else done.toFloat() / total
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(500),
        label = "ringProgress",
    )
    val fg = MaterialTheme.colorScheme.onTertiary
    val trackColor = fg.copy(alpha = 0.18f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val diameter = minOf(size.width, size.height) - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (progress > 0f) {
                rotate(degrees = -90f) {
                    drawArc(
                        color = fg,
                        startAngle = 0f,
                        sweepAngle = 359.99f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$done/$total",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg,
            )
            Text(
                text = stringResource(R.string.sfad522),
                style = MaterialTheme.typography.bodySmall,
                color = fg.copy(alpha = 0.65f),
            )
        }
    }
}

// 右侧天气列：线条图标 + 温度/描述 + 城市 + 逐小时预报行（hero 卡内统一 onTertiary 反色系）
@Composable
private fun WeatherColumn(weather: WeatherData) {
    // 天气条目码 → 按当前语言的天气文案
    val condLabel = when (weather.conditionCode) {
        0 -> stringResource(R.string.seabe42)
        1 -> stringResource(R.string.s786139)
        2 -> stringResource(R.string.sca4062)
        3 -> stringResource(R.string.s090606)
        4 -> stringResource(R.string.s42b8a6)
        5 -> stringResource(R.string.sa65bb4)
        6 -> stringResource(R.string.s954337)
        7 -> stringResource(R.string.sc4fccd)
        else -> stringResource(R.string.sb2df13)
    }
    val fg = MaterialTheme.colorScheme.onTertiary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = iconForCondition(weather.conditionCode),
            contentDescription = condLabel,
            tint = fg,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${weather.temperatureC}°",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = condLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = fg.copy(alpha = 0.70f),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        // 逐小时预报：现在 + 未来 3 小时（时间 / 图标 / 温度，对标参考图 hourly 条）
        if (weather.hourly.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                weather.hourly.forEachIndexed { index, h ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "%02d:00".format(h.hour),
                            fontSize = 13.sp,
                            fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (index == 0) fg else fg.copy(alpha = 0.65f),
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                        Icon(
                            imageVector = iconForCondition(h.conditionCode),
                            contentDescription = null,
                            tint = if (index == 0) fg else fg.copy(alpha = 0.65f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

// 天气未获取到时的占位装饰：圆环 + 圆角方块 + 小圆（onTertiary alpha 层级，随 hero 反色）
@Composable
private fun GeometryDecoration() {
    val ink = MaterialTheme.colorScheme.onTertiary
    Canvas(modifier = Modifier.size(width = 92.dp, height = 64.dp)) {
        drawCircle(
            color = ink.copy(alpha = 0.70f),
            radius = 22.dp.toPx(),
            center = Offset(30.dp.toPx(), 30.dp.toPx()),
            style = Stroke(width = 10.dp.toPx()),
        )
        drawRoundRect(
            color = ink.copy(alpha = 0.30f),
            topLeft = Offset(60.dp.toPx(), 8.dp.toPx()),
            size = Size(26.dp.toPx(), 26.dp.toPx()),
            cornerRadius = CornerRadius(8.dp.toPx()),
        )
        drawCircle(
            color = ink.copy(alpha = 0.14f),
            radius = 7.dp.toPx(),
            center = Offset(24.dp.toPx(), 56.dp.toPx()),
        )
    }
}

// ============ 快捷入口 ============

// 快捷入口三小卡：期待 / 音乐 / 习惯打卡（中性图标圆底，按压 0.98 spring 反馈）
@Composable
fun QuickEntriesRow(
    onOpenExpects: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenHabits: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickEntryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.HourglassEmpty,
            title = stringResource(R.string.sx_entry),
            onClick = onOpenExpects,
        )
        QuickEntryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.MusicNote,
            title = stringResource(R.string.s95521b),
            onClick = onOpenMusic,
        )
        QuickEntryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.EventAvailable,
            title = stringResource(R.string.s0d63c6),
            onClick = onOpenHabits,
        )
    }
}

@Composable
private fun QuickEntryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .pressScale(onPress = onClick),
        cornerRadius = 20,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
