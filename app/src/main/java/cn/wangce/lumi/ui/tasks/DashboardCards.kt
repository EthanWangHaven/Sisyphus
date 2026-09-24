package cn.wangce.lumi.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.wangce.lumi.R
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.pressScale
import cn.wangce.lumi.ui.theme.AccentInk
import cn.wangce.lumi.ui.theme.AccentPaper
import cn.wangce.lumi.ui.theme.CategoryColor
import cn.wangce.lumi.ui.theme.CategoryGreen
import cn.wangce.lumi.ui.theme.CategoryIndigo
import cn.wangce.lumi.ui.theme.CategoryPink
import cn.wangce.lumi.ui.theme.DurState
import cn.wangce.lumi.ui.theme.LightCardBg
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.RadiusControl
import cn.wangce.lumi.ui.theme.RadiusHero
import cn.wangce.lumi.ui.theme.SpaceS
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import cn.wangce.lumi.ui.theme.squircle
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

// 周一至周日 7 胶囊，今日 = 墨黑实底（黑即强调，全 App 不用彩色渐变大色块）
@Composable
fun WeekBar() {
    val dark = LocalDarkTheme.current
    val today = remember { LocalDate.now() }
    val days = remember(today) {
        (0..6).map { offset ->
            today.minusDays((today.dayOfWeek.value - 1 - offset).toLong())
        }
    }
    // 今日墨黑锚点：浅色墨底白字，深色纸白底墨字
    val todayBg = if (dark) AccentPaper else AccentInk
    val todayFg = if (dark) AccentInk else Color.White
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
                        6.dp,
                        squircle(RadiusControl),
                        spotColor = if (dark) ShadowDark else ShadowLight,
                    )
                    .clip(squircle(RadiusControl))
                    .background(todayBg)
            } else {
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(squircle(RadiusControl))
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
                        todayFg.copy(alpha = 0.70f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isToday) todayFg else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ============ hero 主卡 ============

// 首页 hero 卡（对标组5 灰底白卡）：纯白卡 + 墨色大数字 + 极细描边（去掉蓝渐变与蓝投影，回归单强调色纪律）
// 内容：问候语（长按编辑）+ 今日待办大数字 + 进度条；右侧实时天气，底部逐小时预报
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProgressWeatherCard(
    pending: Int,
    done: Int,
    weather: WeatherData?,
    greeting: String,
    onEditGreeting: () -> Unit = {},
) {
    val dark = LocalDarkTheme.current
    val heroShape = squircle(RadiusHero)
    val fg = MaterialTheme.colorScheme.onSurface
    val trackColor = fg.copy(alpha = 0.10f)
    val fillColor = fg
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, heroShape, ambientColor = ShadowLight, spotColor = ShadowLight)
            .clip(heroShape)
            .background(if (dark) MaterialTheme.colorScheme.surface else LightCardBg)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f), heroShape)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onEditGreeting, // 长按问候语：自定义弹窗
                ),
            )
            Spacer(Modifier.height(12.dp))
            // 今日待办大数字（displaySmall + tnum 防抖版）
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$pending",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.today_todo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            // 进度条 + done/total
            val total = pending + done
            val fraction = if (total == 0) 0f else done.toFloat() / total
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(trackColor),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(fillColor),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$done/$total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(SpaceS))
        Box(contentAlignment = Alignment.Center) {
            if (weather != null) {
                WeatherColumn(weather)
            } else {
                GeometryDecoration()
            }
        }
        }
        // 逐小时预报通栏（hero 底部横条，hairline 分隔）
        if (weather != null && weather.hourly.isNotEmpty()) {
            Spacer(Modifier.height(SpaceS))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(fg.copy(alpha = 0.08f)),
            )
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
                            color = if (index == 0) fg else fg.copy(alpha = 0.55f),
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                        Icon(
                            imageVector = iconForCondition(h.conditionCode),
                            contentDescription = null,
                            tint = if (index == 0) fg else fg.copy(alpha = 0.55f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

// 右侧天气列：线条图标 + 温度/描述（hero 白卡内统一墨色层级）
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
    val fg = MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

// 天气未获取到时的占位装饰：圆环 + 圆角方块 + 小圆（墨色 alpha 层级）
@Composable
private fun GeometryDecoration() {
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = Modifier.size(width = 92.dp, height = 64.dp)) {
        drawCircle(
            color = ink.copy(alpha = 0.55f),
            radius = 22.dp.toPx(),
            center = Offset(30.dp.toPx(), 30.dp.toPx()),
            style = Stroke(width = 10.dp.toPx()),
        )
        drawRoundRect(
            color = ink.copy(alpha = 0.22f),
            topLeft = Offset(60.dp.toPx(), 8.dp.toPx()),
            size = Size(26.dp.toPx(), 26.dp.toPx()),
            cornerRadius = CornerRadius(8.dp.toPx()),
        )
        drawCircle(
            color = ink.copy(alpha = 0.10f),
            radius = 7.dp.toPx(),
            center = Offset(24.dp.toPx(), 56.dp.toPx()),
        )
    }
}

// ============ 快捷入口 ============

// 快捷入口三小卡：期待 / 音乐 / 习惯打卡（pastel 图标块，按压 0.98 spring 反馈）
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
            category = CategoryIndigo,
            onClick = onOpenExpects,
        )
        QuickEntryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.MusicNote,
            title = stringResource(R.string.s95521b),
            category = CategoryPink,
            onClick = onOpenMusic,
        )
        QuickEntryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.EventAvailable,
            title = stringResource(R.string.s0d63c6),
            category = CategoryGreen,
            onClick = onOpenHabits,
        )
    }
}

@Composable
private fun QuickEntryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    category: CategoryColor,
    onClick: () -> Unit,
) {
    // 分类 pastel 图标块：浅色用 bg 底、深色用 fg 低透明浮层（防深色背景上过亮）
    val iconBg = if (LocalDarkTheme.current) category.fg.copy(alpha = 0.18f) else category.bg
    GlassCard(
        modifier = modifier.pressScale(onPress = onClick),
        cornerRadius = 12,
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
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = category.fg,
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
