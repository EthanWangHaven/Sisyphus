package cn.wangce.lumi.ui.focus

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.SmokingRooms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.InteractionButton
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 烟雾圈：松开时从火头吐出
private data class Puff(val id: Int, val born: Long, val drift: Float, val sizeF: Float)
// 坠落的烟灰：主块整段脱落 + 旁侧零碎小块洒落（重力 + 旋转 + 空气扰动）
private data class AshFall(
    val id: Int, val born: Long,
    val segTop: Float,     // 块初始顶端相对燃烧环线的比例（负值 = 长在烟身上方）
    val sizeF: Float,      // 块高占烟身满长的比例
    val shock: Float,      // 弹灰冲击强度 0..1（越大越向外散开）
    val whole: Boolean = false, // true = 完整灰柱主块（整段脱落）；false = 零碎小块
)
// 烟尘微粒：碎块碰撞空气溅起的细尘
private data class AshDust(val id: Int, val born: Long, val drift: Float, val sizeF: Float)
// 炭火火星：断裂瞬间从火头飞溅的小火星
private data class Spark(val id: Int, val born: Long, val hx: Float, val vy: Float)

// 确定性哈希（seed 派生随机，帧间稳定不闪烁）；与全局播放状态无关，任意 Int 安全
private fun det(n: Int): Int {
    var x = n * 374761393 // 素数乘子
    x = (x xor (x ushr 13)) * 1274126177
    return x xor (x ushr 16)
}
// 非负取模（det 结果可能为负，直接 % 会得到负索引）
private fun hmod(x: Int, m: Int): Int = ((x % m) + m) % m
// 烟丝粒子：待机/吸入时从火头上升的细烟
private data class Ember(val id: Int, val born: Long, val drift: Float)

// 灰柱配色（参考实拍：浅灰白底 + 密集黑灰斑点颗粒，非斜纹）
private val AshBase = Color(0xFFCDD2D8)         // 灰柱底色
private val AshSpeckDark = Color(0xFF1E2227)    // 黑灰斑点颗粒
private val AshCharLow = Color(0xFF3A1D12)      // 灰柱底部焦褐（贴火头处）
private val AshCharTop = Color(0xFF1F2937)      // 灰顶焦黑断离面
private val PaperScorch = Color(0xFF8A5A2B)     // 火头下方烟纸焦黄
private val CoalDark = Color(0xFF70180A)        // 余烬暗红
private val CoalMid = Color(0xFFE2490F)         // 余烬橙红
private val CoalHot = Color(0xFFFFA23E)         // 余烬亮橙颗粒
private val CoalCore = Color(0xFFFFE9B8)        // 余烬白黄热点
private val BurnRingHot = Color(0xFFFB923C)     // 火星橙（飞溅碎屑）
private val BurnGlow = Color(0xFFFF3C00)        // 火星红晕（飞溅碎屑）
private val SmokeSoft = Color(0xFFD8E1E7)       // 烟雾柔光（参考实拍偏亮的灰白）

// 香烟款式：1:1 对齐参考网页的 4 个品牌 + 雪茄
// widthF = 烟宽（参考网页 450px 容器坐标：标准 40 / 细支 24 / 雪茄 56）
// filterTop/Mid/Bot = 滤嘴三段横向渐变；paperTop/Mid/Bot = 烟身三段横向渐变
private data class CigStyle(
    val brand: String,          // 烟纸竖排印刷字
    val widthF: Float,
    val filterTop: Color, val filterMid: Color, val filterBot: Color,
    val paperTop: Color, val paperMid: Color, val paperBot: Color,
    val textColor: Color,
    val ringColor: Color? = null,   // 滤嘴上沿装饰环（参考 yellow-400）
)
private val CigStyles = listOf(
    // 经典：软木纹点阵滤嘴 + 奶白烟身（参考 Marlboro 规格色）
    CigStyle("LUMI 01", 40f,
        Color(0xFFA06D48), Color(0xFFD4A373), Color(0xFF8A5A38),
        Color(0xFFE5E7EB), Color(0xFFFFFFFF), Color(0xFFD1D5DB),
        Color(0xFF111111)),
    // 细支：白灰滤嘴 + 蓝字
    CigStyle("LUMI SLIM", 24f,
        Color(0xFFD1D5DB), Color(0xFFFFFFFF), Color(0xFF9CA3AF),
        Color(0xFFF3F4F6), Color(0xFFFFFFFF), Color(0xFFE5E7EB),
        Color(0xFF60A5FA)),
    // 雪茄：粗壮深褐
    CigStyle("CIGAR", 56f,
        Color(0xFF3A2412), Color(0xFF5C3A1E), Color(0xFF2A1808),
        Color(0xFF7A4A26), Color(0xFF8A5A30), Color(0xFF5C3A1E),
        Color(0xFFE8C88F)),
    // 赤焰：深红滤嘴 + 金环 + 红字（参考 Chunghwa 规格）
    CigStyle("LUMI FLAME", 40f,
        Color(0xFF5A0808), Color(0xFF991B1B), Color(0xFF450A0A),
        Color(0xFFE5E7EB), Color(0xFFFFFFFF), Color(0xFFD1D5DB),
        Color(0xFFB91C1C), Color(0xFFFACC15)),
    // 暗夜：黑滤嘴 + 深灰烟身 + 粉字（参考 Black Devil 规格）
    CigStyle("NIGHT", 40f,
        Color(0xFF18181B), Color(0xFF3F3F46), Color(0xFF09090B),
        Color(0xFF27272A), Color(0xFF3F3F46), Color(0xFF18181B),
        Color(0xFFEC4899)),
)
private val StyleNames = listOf("经典", "细支", "雪茄", "赤焰", "暗夜")

// 参考网页容器坐标（450px 竖条）：滤嘴 80 + 烟身 300，烟灰最高 60 且向上生长
private const val REF_FILTER_H = 80f
private const val REF_BODY_H = 300f
private const val REF_ASH_MAX = 60f
// 烟灰一节占烟身满长的比例（60/3 节 ÷ 300）
private const val SEG_F = REF_ASH_MAX / 3f / REF_BODY_H

// 赛博吸烟：纯 Canvas 2D 香烟，形状 1:1 复刻参考网页
// 按住吸入（火星变亮 + 烟灰向上生长 + 蓄力光环）、松手吐圈、弹灰、换款式
@Composable
fun SmokeScreen(viewModel: FocusViewModel = hiltViewModel(), onBack: () -> Unit) {
    val running by viewModel.running.collectAsStateWithLifecycle()
    var elapsedSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) {
        while (running) {
            elapsedSec = viewModel.elapsedSeconds()
            delay(500)
        }
    }

    var style by remember { mutableIntStateOf(0) }
    var inhaling by remember { mutableStateOf(false) }  // 是否按住吸入
    var ashLen by remember { mutableFloatStateOf(0f) }  // 烟灰长度 0..1（1 = 参考网页 60px 上限）
    var smokeLen by remember { mutableFloatStateOf(1f) } // 烟身剩余 0..1（1 = 满长，随燃烧递减）
    // 灰柱顶端锚点（烟身满长坐标系 0..1）：顶端固定不动，底端随火头燃烧下移；掉灰时重置为当前燃烧位置
    var ashAnchorF by remember { mutableFloatStateOf(1f) }
    var glow by remember { mutableFloatStateOf(0f) }    // 火星亮度 0..1
    val puffs = remember { mutableStateListOf<Puff>() }
    val ashes = remember { mutableStateListOf<AshFall>() }
    val dusts = remember { mutableStateListOf<AshDust>() }
    val sparks = remember { mutableStateListOf<Spark>() }
    val embers = remember { mutableStateListOf<Ember>() }
    val appear = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var fallId by remember { mutableIntStateOf(0) }
    var dustId by remember { mutableIntStateOf(0) }
    var sparkId by remember { mutableIntStateOf(0) }
    var flick by remember { mutableIntStateOf(0) }   // 余烬闪烁种子（周期变化驱动火头明暗呼吸）

    // 掉落烟灰：整段灰柱作为主块原位整体脱落 + 主体旁边洒落零碎不规则碎块（真实烟灰掉落感）
    fun dropAshAll(shock: Float = 0f) {
        val now = System.currentTimeMillis()
        val lenF = ashLen.coerceAtLeast(SEG_F)      // 至少按一节的量掉落
        val sizeF = lenF * REF_ASH_MAX / REF_BODY_H // 灰柱高占烟身满长的比例
        // 主块：完整灰柱整段下坠（从当前灰柱位置原位脱落，保留完整外形与斑点纹理）
        ashes.add(
            AshFall(id = fallId++, born = now, segTop = -sizeF, sizeF = sizeF, shock = shock, whole = true),
        )
        // 零碎碎块：4-7 个不规则小块沿灰柱各段剥离，向四周洒落
        val fragN = 4 + Random.nextInt(4)
        repeat(fragN) { j ->
            val h1 = det(fallId * 37 + j * 91 + 5)
            val h2 = det(fallId * 53 + j * 17 + 9)
            ashes.add(
                AshFall(
                    id = fallId++, born = now + 40L + j * 35L,
                    segTop = -sizeF * (0.12f + hmod(h1, 89) / 89f * 0.82f),
                    sizeF = (sizeF * (0.10f + hmod(h2, 100) / 100f * 0.16f)).coerceAtLeast(0.012f),
                    shock = shock,
                ),
            )
        }
        // 碎块落尘：少量细尘微粒
        val dustN = 4 + Random.nextInt(3)
        repeat(dustN) {
            dusts.add(AshDust(dustId++, now + Random.nextLong(150), Random.nextFloat() * 2f - 1f, 0.5f + Random.nextFloat() * 0.8f))
        }
        while (ashes.size > 16) ashes.removeAt(0)
        while (dusts.size > 40) dusts.removeAt(0)
        while (sparks.size > 24) sparks.removeAt(0)
        // 锚点重置：灰柱顶端回到当前燃烧位置，灰长归零重新累积
        ashAnchorF = smokeLen
        ashLen = 0f
    }

    // 吸入循环：火星渐亮、烟灰向上生长、长灰小概率自然断裂；松开后火星渐暗
    LaunchedEffect(inhaling) {
        if (inhaling) {
            while (inhaling) {
                glow = (glow + 0.08f).coerceAtMost(1f)
                // 烟身随燃烧变短；燃尽则整支重置（长度回满、烟灰清零）
                smokeLen = (smokeLen - 0.0028f).coerceAtLeast(0f)
                if (smokeLen <= 0f) {
                    smokeLen = 1f
                    ashAnchorF = 1f
                    ashLen = 0f
                } else {
                    // 灰长由锚点推导：顶端钉在锚点处不动，底端贴火头随燃烧下移 → 灰长自然增长
                    ashLen = ((ashAnchorF - smokeLen) * REF_BODY_H / REF_ASH_MAX).coerceIn(0f, 1f)
                }
                // 烟灰过长：长满临界长度整段自动脱落（自重断裂）
                if (ashLen >= 1f) {
                    dropAshAll(shock = 0f)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                // 自然断裂：烟灰超过约 1/3 后，吸入时有小概率整段掉落
                else if (ashLen > 0.34f && Random.nextFloat() < 0.015f) {
                    dropAshAll(shock = 0f)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                delay(60)
            }
        } else {
            while (glow > 0f) {
                glow = (glow - 0.06f).coerceAtLeast(0f)
                delay(40)
            }
        }
    }
    // 烟丝粒子：吸入时密集，待机时稀疏
    LaunchedEffect(Unit) {
        var id = 0
        while (true) {
            embers.add(Ember(id++, System.currentTimeMillis(), Random.nextFloat() * 2f - 1f))
            if (embers.size > 40) embers.removeAt(0)
            delay(if (inhaling) 60 else 280)
        }
    }
    // 过期粒子清理
    LaunchedEffect(Unit) {
        while (true) {
            delay(300)
            val now = System.currentTimeMillis()
            puffs.removeAll { now - it.born > 3100 }
            ashes.removeAll { now - it.born > 1300 }
            dusts.removeAll { now - it.born > 900 }
            sparks.removeAll { now - it.born > 700 }
            embers.removeAll { now - it.born > 1900 }
        }
    }
    // 余烬闪烁时钟：即使待机也让火头颗粒微微明暗呼吸（90ms 一帧，代价极小）
    LaunchedEffect(Unit) {
        while (true) {
            delay(90)
            flick++
        }
    }

    // 切换款式：弹跳入场
    LaunchedEffect(style) {
        appear.snapTo(0.72f)
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 顶栏：返回 + 标题 + 计时
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCard(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), CircleShape)
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
                text = stringResource(R.string.focus_smoke_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (running) {
                Text(
                    text = formatSmokeTime(elapsedSec),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // 互动区：按住吸入、松开吐圈
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize().scale(appear.value)) {
                drawCigarette(
                    st = CigStyles[style],
                    glow = glow,
                    flick = flick,
                    inhaling = inhaling,
                    ashLen = ashLen,
                    ashAnchorF = ashAnchorF,
                    smokeLen = smokeLen,
                    puffs = puffs,
                    ashes = ashes,
                    dusts = dusts,
                    sparks = sparks,
                    embers = embers,
                )
            }
            // 手势层：长按吸入，松手吐烟圈
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var puffId = 0
                        detectTapGestures(
                            onPress = {
                                Log.d("SmokeDebug", "press down")
                                inhaling = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                try {
                                    awaitRelease()
                                } finally {
                                    Log.d("SmokeDebug", "press release ashLen=$ashLen")
                                    inhaling = false
                                    val now = System.currentTimeMillis()
                                    val n = 6 + Random.nextInt(2)
                                    repeat(n) { i ->
                                        puffs.add(Puff(puffId++, now + i * 140L, Random.nextFloat() * 2f - 1f, 0.7f + Random.nextFloat() * 0.6f))
                                    }
                                    while (puffs.size > 30) puffs.removeAt(0)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                        )
                    },
            )
        }

        Text(
            text = stringResource(R.string.focus_smoke_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))

        // 底部操作：弹灰 / 换款式
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            InteractionButton(
                label = stringResource(R.string.focus_smoke_ash),
                icon = Icons.Outlined.Air,
                onClick = {
                    Log.d("SmokeDebug", "弹灰 click ashLen=$ashLen")
                    if (ashLen > 0.06f) {
                        // 弹灰：与自动掉落同款动效（整段脱落 + 零碎洒落），冲击更强散得更开
                        dropAshAll(shock = 1f)
                        // 弹灰震断瞬间：火头溅出少量炭火火星
                        val now = System.currentTimeMillis()
                        repeat(4) { s ->
                            sparks.add(Spark(sparkId++, now + s * 24L, Random.nextFloat() * 2f - 1f, 0.6f + Random.nextFloat() * 0.5f))
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
            )
            InteractionButton(
                label = stringResource(R.string.focus_smoke_style),
                icon = Icons.Outlined.SmokingRooms,
                onClick = {
                    Log.d("SmokeDebug", "换款式 click style=$style")
                    scope.launch {
                        appear.animateTo(0.72f, tween(160))
                        style = (style + 1) % CigStyles.size
                    }
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "款式 · ${StyleNames[style]}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1.2f))
    }
}

@Composable
private fun formatSmokeTime(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

// ============ 纯 Canvas 2D 香烟：形状 1:1 复刻参考网页 ============
//
// 布局（自底向上，参考 flex-col-reverse，450px 容器坐标）：
//   滤嘴(80, 底部固定, rounded-b-md, 软木纹点阵) → 烟身(300, 白渐变+竖排字)
//   → 燃烧环(6, red-500+红光晕, 固定) → 烟灰(最高60, 从燃烧环向上生长, 45°斜纹)
//   火头光晕在灰顶端（真烟燃烧面），烟从火头上升/吐圈
private fun DrawScope.drawCigarette(
    st: CigStyle,
    glow: Float,
    flick: Int,
    inhaling: Boolean,
    ashLen: Float,
    ashAnchorF: Float,
    smokeLen: Float,
    puffs: List<Puff>,
    ashes: List<AshFall>,
    dusts: List<AshDust>,
    sparks: List<Spark>,
    embers: List<Ember>,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val now = System.currentTimeMillis()
    val u = h / 500f                          // 参考网页 450px 容器 → 画布映射
    val paperW = st.widthF * u
    val halfW = paperW / 2f
    val bottomY = h * 0.94f                   // 滤嘴底
    val filterTopY = bottomY - REF_FILTER_H * u
    val bodyH = REF_BODY_H * smokeLen * u   // 烟身剩余长度（随燃烧缩短）
    val burnY = filterTopY - bodyH       // 燃烧环线 = 烟身顶（随烟身缩短下移）
    // 灰柱顶端钉在锚点处（固定不动），底端贴燃烧环线随燃烧下移 → 灰长 = 两线间距
    val ashTopY = filterTopY - REF_BODY_H * ashAnchorF * u
    val ashH = (burnY - ashTopY).coerceAtLeast(0f)
    val emberY = ashTopY                 // 灰顶端断离面（烟从灰顶上方冒出，参考实拍）
    val left = cx - halfW
    val right = cx + halfW

    // 地面柔影（参考 -bottom-8 blur-xl 椭圆，多层叠加模拟模糊）
    repeat(3) { i ->
        drawOval(
            Color.Black.copy(alpha = 0.05f + i * 0.045f),
            topLeft = Offset(cx - paperW * (1.6f - i * 0.35f), bottomY + 3f * u + i * 2f * u),
            size = Size(paperW * (3.2f - i * 0.7f), 7f * u),
        )
    }

    // ---- 滤嘴：软木纹点阵 + 三段横向渐变 + 两侧阴影，底部圆角 ----
    val filterH = REF_FILTER_H * u
    val filterPath = Path().apply {
        addRoundRect(
            RoundRect(
                left, filterTopY, right, bottomY,
                bottomRightCornerRadius = CornerRadius(6f * u, 6f * u),
                bottomLeftCornerRadius = CornerRadius(6f * u, 6f * u),
            ),
        )
    }
    clipPath(filterPath) {
        // to right: #a06d48 0% → #d4a373 40% → #8a5a38 100%
        drawRect(
            Brush.horizontalGradient(
                0f to st.filterTop, 0.4f to st.filterMid, 1f to st.filterBot,
                startX = left, endX = right,
            ),
            topLeft = Offset(left, filterTopY),
            size = Size(paperW, filterH),
        )
        // 软木纹点阵：radial-gradient(circle at 2px 2px, rgba(0,0,0,.1) 1px, transparent 1px) 6px 网格
        var gy = filterTopY + 2f * u
        while (gy < bottomY - 1f) {
            var gx = left + 2f * u
            while (gx < right - 1f) {
                drawCircle(Color.Black.copy(alpha = 0.10f), 1.3f * u, Offset(gx, gy))
                gx += 6f * u
            }
            gy += 6f * u
        }
        // 两侧圆柱阴影 from-black/40 via-transparent to-black/40
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent, Color.Black.copy(alpha = 0.40f)),
                startX = left, endX = right,
            ),
            topLeft = Offset(left, filterTopY),
            size = Size(paperW, filterH),
        )
    }
    // 金环（赤焰款）：滤嘴上沿 top-2 h-1 yellow-400
    st.ringColor?.let { rc ->
        drawRect(
            rc.copy(alpha = 0.90f),
            topLeft = Offset(left, filterTopY + 8f * u),
            size = Size(paperW, 4f * u),
        )
    }

    // ---- 烟身：白渐变 + 两侧阴影 + 竖排品牌字（长度随燃烧缩短） ----
    drawRect(
        Brush.horizontalGradient(
            listOf(st.paperTop, st.paperMid, st.paperBot),
            startX = left, endX = right,
        ),
        topLeft = Offset(left, burnY),
        size = Size(paperW, bodyH),
    )
    drawRect(
        Brush.horizontalGradient(
            listOf(Color.Black.copy(alpha = 0.20f), Color.Transparent, Color.Black.copy(alpha = 0.20f)),
            startX = left, endX = right,
        ),
        topLeft = Offset(left, burnY),
        size = Size(paperW, bodyH),
    )
    // 竖排品牌字（rotate-90 从上往下读，bottom-4 处，10px 加粗宽字距）
    drawIntoCanvas { cv ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = st.textColor.copy(alpha = 0.60f).toArgb()
            textSize = 10f * u * 1.15f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            letterSpacing = 0.12f
        }
        val brandY = filterTopY - 16f * u
        cv.save()
        cv.nativeCanvas.rotate(90f, cx, brandY)
        cv.nativeCanvas.drawText(st.brand, cx, brandY + paint.textSize / 3f, paint)
        cv.restore()
    }

    // 灰柱横向范围（参考实拍：灰柱比烟纸略宽，灰烬膨胀）
    val ashLeft = cx - paperW * 0.53f
    val ashRight = cx + paperW * 0.53f

    // ---- 烟纸焦化：火头下方黄褐渐变 + 焦斑（参考实拍过渡带） ----
    val scorchA = 0.45f + 0.55f * glow
    drawRect(
        Brush.verticalGradient(
            listOf(PaperScorch.copy(alpha = 0.40f * scorchA), Color.Transparent),
            startY = burnY, endY = burnY + 15f * u,
        ),
        topLeft = Offset(left, burnY),
        size = Size(paperW, 15f * u),
    )
    repeat(5) { i ->
        val hsc = det(i * 71 + 13)
        drawCircle(
            PaperScorch.copy(alpha = 0.28f * scorchA),
            (0.8f + hmod(hsc, 5) / 5f) * u,
            Offset(left + paperW * (0.15f + hmod(hsc, 70) / 70f * 0.7f), burnY + (3f + hmod(hsc, 9)) * u),
        )
    }

    // ---- 烟灰：浅灰白底 + 密集黑灰斑点颗粒（参考实拍），分节细缝 + 两侧阴影 ----
    if (ashH > 1f) {
        // 顶部一节在接近断裂(临界长度)时预拉扯：裂纹微微撑开再断。0..1 随 ashLen 渐增
        val nearBreak = (ashLen - 0.55f) / 0.30f          // 约 0.55..0.85 → 0..1
        val stretch = if (nearBreak > 0f) nearBreak * 0.06f else 0f   // 顶部横向拉开 0..6%
        val segH = ashH / 3f
        val ashPath = Path().apply {
            addRoundRect(
                RoundRect(
                    ashLeft, emberY, ashRight, burnY,
                    topLeftCornerRadius = CornerRadius(2.5f * u, 2.5f * u),
                    topRightCornerRadius = CornerRadius(2.5f * u, 2.5f * u),
                ),
            )
        }
        clipPath(ashPath) {
            // 底色：浅灰白
            drawRect(AshBase, topLeft = Offset(ashLeft, emberY), size = Size(ashRight - ashLeft, ashH))
            // 密集黑灰斑点：局部坐标网格 + 确定性抖动（斑点长在灰上，随生长整体上移）
            val step = 3.0f * u
            var py = 0f
            while (py < ashH) {
                val row = (py / step).toInt()
                var px = 0f
                while (px < ashRight - ashLeft) {
                    val col = (px / step).toInt()
                    val hsh = det(row * 131 + col * 17 + 5)
                    if (hmod(hsh, 10) < 6) {
                        drawCircle(
                            AshSpeckDark.copy(alpha = 0.22f + hmod(hsh, 7) / 7f * 0.55f),
                            (0.7f + hmod(hsh, 13) / 13f * 1.1f) * u,
                            Offset(ashLeft + px + (hmod(hsh, 5) - 2) * 0.4f * u, emberY + py),
                        )
                    }
                    px += step
                }
                py += step
            }
            // 底部焦褐：贴火头处暗红→黑过渡（参考实拍）
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, AshCharLow.copy(alpha = 0.80f)),
                    startY = burnY - 12f * u, endY = burnY,
                ),
                topLeft = Offset(ashLeft, emberY),
                size = Size(ashRight - ashLeft, ashH),
            )
            // 顶部断离面轻微焦灰
            drawRect(
                Brush.verticalGradient(
                    listOf(AshCharTop.copy(alpha = 0.35f), Color.Transparent),
                    startY = emberY, endY = emberY + ashH * 0.3f,
                ),
                topLeft = Offset(ashLeft, emberY),
                size = Size(ashRight - ashLeft, ashH),
            )
            // 两侧圆柱阴影 from-black/40 via-transparent to-black/40
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent, Color.Black.copy(alpha = 0.40f)),
                    startX = ashLeft, endX = ashRight,
                ),
                topLeft = Offset(ashLeft, emberY),
                size = Size(ashRight - ashLeft, ashH),
            )
            // 分节裂纹：细暗缝（顶部接缝临近断裂时预拉扯撑开）
            for (s in 1..2) {
                val sy = emberY + segH * s
                val open = if (s == 2) stretch * segH * 1.4f else 0f
                val wDark = 1.2f * u * (1f + 0.5f * s) + open
                drawRect(
                    AshCharTop.copy(alpha = 0.42f),
                    topLeft = Offset(ashLeft + 0.5f * u, sy - wDark / 2f),
                    size = Size(ashRight - ashLeft - u, wDark),
                )
                drawRect(
                    Color.Black.copy(alpha = 0.22f),
                    topLeft = Offset(ashLeft + 0.5f * u, sy + wDark / 2f - 0.4f * u),
                    size = Size(ashRight - ashLeft - u, 0.8f * u),
                )
            }
        }
        // 顶部断口颗粒凸起（粗糙断离面，确定性不闪烁）
        for (k in 0..9) {
            val hj = det(k * 41 + 9)
            if (hmod(hj, 3) != 0) {
                drawCircle(
                    AshBase.copy(alpha = 0.85f),
                    (0.8f + hmod(hj, 7) / 7f * 0.9f) * u,
                    Offset(
                        ashLeft + (ashRight - ashLeft) * (k + 0.5f) / 10f + (hmod(hj, 5) - 2) * 0.5f * u,
                        emberY - (0.5f + hmod(hj, 4) / 4f * 1.1f) * u,
                    ),
                )
            }
        }
    }

    // ---- 火头：炽热余烬环（参考实拍：灰柱底端与白纸交界处一圈亮橙碎屑，凹凸不平、颗粒密集、无光晕） ----
    val emberH = (8f + 4f * glow) * u
    // 环带主体：上缘凹凸燃烧线 + 下缘微起伏，横向暗红→亮橙→暗红
    val emberPath = Path().apply {
        val segN = 8
        // 上缘：凹凸不平的燃烧线（骑在灰柱底端）
        moveTo(ashLeft, burnY - emberH * 0.42f)
        for (s in 1..segN) {
            val hs = det(s * 97 + 11)
            lineTo(
                ashLeft + (ashRight - ashLeft) * s / segN,
                burnY - emberH * 0.42f + (hmod(hs, 9) - 4) / 4f * emberH * 0.42f,
            )
        }
        // 下缘：贴烟纸微起伏
        for (s in segN downTo 0) {
            val hs = det(s * 131 + 23)
            lineTo(
                ashLeft + (ashRight - ashLeft) * s / segN,
                burnY + emberH * 0.30f + (hmod(hs, 7) - 3) / 3f * emberH * 0.22f,
            )
        }
        close()
    }
    clipPath(emberPath) {
        drawRect(
            Brush.horizontalGradient(
                listOf(CoalDark, CoalMid, CoalMid, CoalDark),
                startX = ashLeft, endX = ashRight,
            ),
            topLeft = Offset(ashLeft, burnY - emberH),
            size = Size(ashRight - ashLeft, emberH * 2f),
            alpha = 0.55f + 0.45f * glow,
        )
    }
    // 密集余烬颗粒：确定性分布帧间稳定；flick 驱动每颗独立相位闪烁（参考实拍碎屑感）
    repeat(40) { i ->
        val h1 = det(i * 37 + 5)
        val h2 = det(i * 53 + 11)
        val sxp = ashLeft + (ashRight - ashLeft) * (0.03f + hmod(h1, 94) / 94f * 0.94f)
        val syp = burnY - emberH * 0.5f + emberH * (hmod(h2, 80) / 80f)
        val tw = (0.55f + 0.45f * sin(flick * 1.9f + i * 2.399f)).coerceAtLeast(0.15f)
        val hot = ((0.35f + 0.65f * glow) * tw).coerceIn(0f, 1f)
        drawCircle(CoalHot.copy(alpha = hot), (0.7f + hmod(h2, 9) / 9f * 0.9f) * u, Offset(sxp, syp))
        if (i % 3 == 0) {
            drawCircle(CoalCore.copy(alpha = hot * 0.85f), 0.6f * u, Offset(sxp, syp))
        }
    }

    // 烟丝粒子（火头上升的柔光细烟，正弦摆动 + 径向渐变光斑，非实心圆）
    embers.forEach { e ->
        val t = ((now - e.born) / 1900f).coerceIn(0f, 1f)
        if (t in 0f..0.999f) {
            val fadeIn = (t * 5f).coerceAtMost(1f)
            val a = 0.32f * fadeIn * (1f - t)
            val wob = sin(t * 5.2f + e.drift * 3.1f) * h * 0.014f * (0.4f + t)
            val ex = cx + e.drift * t * 42f + wob
            val ey = emberY - 6.dp.toPx() - h * 0.11f * t
            val r = 3.4.dp.toPx() * (1.1f - 0.5f * t)
            drawCircle(
                Brush.radialGradient(
                    listOf(SmokeSoft.copy(alpha = a), Color.Transparent),
                    center = Offset(ex, ey), radius = r,
                ),
                radius = r, center = Offset(ex, ey),
            )
        }
    }

    // 呼出的烟雾：参考实拍 —— 大而柔的光团雾，多团错位上飘、边升边扩散淡出
    puffs.forEach { p ->
        val t = ((now - p.born) / 3100f)
        if (t in 0f..0.999f) {
            // 每口烟 = 16 个错位柔光团（雾量翻倍），各团独立大小/漂移/相位，径向渐变模拟散景
            for (k in 0 until 16) {
                val hk = det(p.id * 53 + k * 29 + 3)
                val ox = hmod(hk, 41) / 41f - 0.5f
                val oy = hmod(hk * 7 + 1, 23) / 23f - 0.5f
                val ph = hmod(hk * 13 + 5, 17) / 17f * 6.283f
                val sc = 0.65f + hmod(hk * 17 + 9, 13) / 13f * 0.75f
                val kt = (t * (0.85f + hmod(hk, 7) / 7f * 0.3f)).coerceAtMost(1f)
                val fadeIn = (kt * 4f).coerceAtMost(1f)
                val alpha = 0.11f * p.sizeF * fadeIn * (1f - kt)
                val r = 17.dp.toPx() * sc * (1f + 2.0f * kt)
                val cyP = emberY - 10.dp.toPx() - h * (0.14f * kt + k * 0.015f) - oy * r * 0.35f
                val pxP = cx + p.drift * h * 0.11f * kt + ox * r * (0.4f + kt) + sin(kt * 2.2f + ph) * h * 0.018f
                drawCircle(
                    Brush.radialGradient(
                        listOf(SmokeSoft.copy(alpha = alpha), Color.Transparent),
                        center = Offset(pxP, cyP), radius = r,
                    ),
                    radius = r, center = Offset(pxP, cyP),
                )
            }
        }
    }

    // 坠落的烟灰：主块整段下坠，旁边洒落零碎不规则碎块（重力 + 翻滚 + 空气扰动，非直线）
    ashes.forEach { a ->
        val t = ((now - a.born) / 1300f)
        if (t !in 0f..0.999f) return@forEach
        val shPx = REF_BODY_H * u
        val chunkH = a.sizeF * shPx
        val chunkW = paperW * 1.04f
        val startY = burnY + a.segTop * shPx
        // 重力加速下落（t²）叠加初速；shock 越大初速越猛；落到地面后碎散消散
        val groundY = filterTopY + (REF_FILTER_H + 26f) * u
        val g = 0.34f + 0.06f * a.shock
        val fall = h * g * t * t + chunkH * (0.35f + 0.35f * a.shock) * t
        val alphaBase = 1f - t * 0.55f
        if (a.whole) {
            // 主块：完整灰柱整段脱落，基本竖直下坠，仅轻微摆动并缓缓翻转
            val hw = det(a.id * 31 + 11)
            val dxr = hmod(hw, 23) / 23f - 0.5f
            val shockX = a.shock * dxr * chunkW * 0.9f * t
            val swayX = sin(t * 7f + hmod(hw, 37) / 37f * 6.283f) * 0.006f * h
            val fx = cx + shockX + swayX
            val fyLimit = groundY - chunkH * 0.5f
            val fy0 = startY + fall
            val fy = fy0.coerceAtMost(fyLimit)
            val nearGround = ((fy0 - fyLimit + 40f * u) / (40f * u)).coerceIn(0f, 1f)
            val alpha = alphaBase * (1f - 0.9f * nearGround)
            val rot = dxr * 26f * t + hmod(hw, 11) / 11f * 44f * t
            rotate(degrees = rot, pivot = Offset(fx, fy + chunkH / 2f)) {
                drawAshChunk(fx, fy, chunkW, chunkH, alpha, u, seed = a.id * 131, jag = 0.8f)
            }
        } else {
            // 零碎碎块：从主体旁剥离洒落，横向散开 + 快速翻滚 + 空气扰动
            val fragN = 2 + hmod(det(a.id * 31 + 7), 2)
            for (k in 0 until fragN) {
                val h1 = det(a.id * 31 + k * 131 + 11)
                val h2 = det(a.id * 31 + k * 131 + 29)
                val h3 = det(a.id * 31 + k * 131 + 47)
                val dxr = hmod(h1, 23) / 23f - 0.5f
                // 更碎更扁的不规则碎片（非长方形）：宽窄高低差异大
                val dw = 0.14f + hmod(h2, 19) / 19f * 0.32f
                val dh = 0.10f + hmod(h3, 21) / 21f * 0.30f
                val rot0 = hmod(h1, 29) / 29f * 180f - 90f
                val swayA = 0.006f + hmod(h2, 17) / 17f * 0.012f   // 空气扰动幅度
                val swayP = hmod(h3, 37) / 37f * 6.283f
                // 横向：自身散开（无冲击也向旁洒）+ 冲击增强 + 空气扰动（正弦，非直线）
                val scatterX = dxr * chunkW * (0.35f + 0.85f * t)
                val shockX = a.shock * dxr * chunkW * (0.8f + 1.6f * t)
                val swayX = sin(t * 9f + swayP) * swayA * h
                val fx = cx + dxr * chunkW * 0.45f + scatterX + shockX + swayX
                // 竖向：碎块更轻，下落略慢于主块，错落剥离；落地后钳制并加速消散
                val fy0 = startY + fall * (0.72f + dh * 0.5f) + k * chunkH * 0.5f
                val fy = fy0.coerceAtMost(groundY + hmod(h3, 5) * 3f * u)
                val nearGround = ((fy0 - groundY + 34f * u) / (34f * u)).coerceIn(0f, 1f)
                val alpha = alphaBase * (1f - 0.85f * nearGround)
                val wPx = chunkW * dw
                val hPx = (chunkH * dh).coerceAtLeast(3f * u)
                // 旋转：快速翻滚（随位移渐增）
                val rot = rot0 + hmod(h1, 11) / 11f * 320f * t + dxr * 60f * t
                rotate(degrees = rot, pivot = Offset(fx, fy)) {
                    drawAshChunk(fx, fy, wPx, hPx, alpha, u, seed = a.id * 131 + k * 17, poly = true)
                }
            }
        }
    }

    // 烟尘微粒：碎块下落溅起的细尘，轻飘淡出
    dusts.forEach { d ->
        val t = ((now - d.born) / 900f)
        if (t in 0f..0.999f) {
            drawCircle(
                AshBase.copy(alpha = 0.35f * (1 - t)),
                radius = (1.6f + 1.2f * t) * u * d.sizeF,
                center = Offset(cx + d.drift * h * 0.05f + sin(t * 13f + d.id) * 3f * u, burnY - 2f * u + h * 0.12f * t),
            )
        }
    }

    // 炭火火星：弹灰/断裂瞬间从火头飞溅，亮橙短焰
    sparks.forEach { s ->
        val t = ((now - s.born) / 700f)
        if (t in 0f..0.999f) {
            val sx = cx + s.hx * paperW * (0.3f + 1.2f * t)
            val sy = burnY - 2f * u - s.vy * h * 0.10f * t - t * t * h * 0.10f
            drawCircle(
                BurnRingHot.copy(alpha = 0.85f * (1 - t)),
                radius = (1.4f - 0.5f * t) * u,
                center = Offset(sx, sy),
            )
            drawCircle(
                BurnGlow.copy(alpha = 0.5f * (1 - t)),
                radius = (2.6f - 1.0f * t) * u,
                center = Offset(sx, sy),
            )
        }
    }

    // 蓄力光环已移除（避免燃烧部分附近出现红色圆圈）
}

// 一块烟灰。两种轮廓模式：
//   poly = false：柱状块（整段主块），上缘断离面起伏 + 两侧折线 + 下缘 4 齿，jag 控制抖动（0.5 轻微粗糙）
//   poly = true ：不规则多边形（零碎碎块），7-10 个径向随机顶点，彻底摆脱长方形感
private fun DrawScope.drawAshChunk(
    x: Float, yTop: Float, w: Float, h: Float, alpha: Float, u: Float, seed: Int,
    jag: Float = 1f, poly: Boolean = false,
) {
    val left = x - w / 2f
    val right = x + w / 2f
    // 确定性锯齿（seed 派生，帧间稳定不闪烁）
    fun jit(i: Int, amp: Float) = (det(seed * 13 + i) % 41) / 41f * amp - amp / 2f
    val rV = det(seed * 7 + 1) % 19 / 19f
    val rH = det(seed * 7 + 3) % 19 / 19f
    val ampV = if (poly) 0f else (0.10f + 0.12f * rV) * h * jag
    val ampH = if (poly) 0f else (0.06f + 0.06f * rH) * w * jag
    val bottomY = yTop + h
    val toothW = w / 4f
    val outline = if (poly) {
        // 径向随机多边形：顶点角度 + 半径双重随机（x/y 独立），真不规则碎屑形
        val cyP = yTop + h / 2f
        val n = 7 + hmod(det(seed * 11 + 3), 4)   // 7-10 个顶点
        Path().apply {
            for (i in 0 until n) {
                val ha = det(seed * 17 + i * 7 + 1)
                val hr = det(seed * 19 + i * 13 + 5)
                val ang = 6.283f * i / n + hmod(ha, 41) / 41f * (6.283f / n) * 0.85f
                val rx = w / 2f * (0.58f + hmod(hr, 46) / 46f * 0.42f)
                val ry = h / 2f * (0.58f + hmod(ha, 53) / 53f * 0.42f)
                val px = x + cos(ang) * rx
                val py = cyP + sin(ang) * ry
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
    } else {
        Path().apply {
            // 上缘：断离面多段起伏（非直边，参考实拍粗糙断口）
            moveTo(left + jit(1, ampH), yTop + jit(21, ampV * 0.5f))
            // 左侧：多段折线（内凹外凸交替，无平直长边）
            lineTo(left - jit(2, ampH), yTop + h * 0.26f + jit(2, ampV))
            lineTo(left + jit(3, ampH), yTop + h * 0.55f + jit(3, ampV))
            lineTo(left - jit(4, ampH), yTop + h * 0.80f + jit(4, ampV))
            lineTo(left + jit(5, ampH), bottomY + jit(25, ampV * 0.4f))
            // 下缘：4 齿参差（外凸齿 + 内凹谷交替）
            for (t in 0..3) {
                val tx = left + toothW * t
                // 谷（往内凹）
                lineTo(tx + toothW * 0.5f, bottomY - jit(30 + t, ampV * 0.8f))
                // 齿（外凸）
                lineTo(tx + toothW, bottomY + jit(40 + t, ampV * 0.4f))
            }
            // 右侧：多段折线（镜像，非平直长边）
            lineTo(right - jit(8, ampH), yTop + h * 0.78f + jit(9, ampV))
            lineTo(right + jit(9, ampH), yTop + h * 0.52f + jit(10, ampV))
            lineTo(right - jit(10, ampH), yTop + h * 0.24f + jit(11, ampV))
            lineTo(right + jit(11, ampH), yTop + jit(26, ampV * 0.5f))
            close()
        }
    }
    clipPath(outline) {
        // 锯齿轮廓的包围盒背景（回补可能露白的边缘）
        val bbl = left - ampH
        val bbr = right + ampH
        drawRect(AshBase, topLeft = Offset(bbl, yTop - ampV), size = Size(bbr - bbl, h + 2 * ampV), alpha = alpha)
        // 密集黑灰斑点（参考实拍颗粒灰；局部坐标，帧间稳定）
        val stepC = 2.6f * u
        var pyC = 0f
        var rowC = 0
        while (pyC < h) {
            var pxC = 0f
            var colC = 0
            while (pxC < w) {
                val hsh = det(seed * 977 + rowC * 131 + colC * 17)
                if (hmod(hsh, 10) < 6) {
                    drawCircle(
                        AshSpeckDark.copy(alpha = (0.20f + hmod(hsh, 7) / 7f * 0.50f) * alpha),
                        (0.6f + hmod(hsh, 13) / 13f * 1.0f) * u,
                        Offset(left + pxC, yTop + pyC),
                    )
                }
                pxC += stepC
                colC++
            }
            pyC += stepC
            rowC++
        }
        // 顶部焦黑（断离面）
        drawRect(
            Brush.verticalGradient(listOf(AshCharTop.copy(alpha = 0.6f * alpha), Color.Transparent), startY = yTop, endY = yTop + h * 0.6f),
            topLeft = Offset(bbl, yTop - ampV), size = Size(bbr - bbl, h + 2 * ampV),
        )
        // 焦黑边缘：四周描一圈加深断口（重用轮廓路径）
        drawPath(
            outline,
            color = AshCharTop.copy(alpha = 0.22f * alpha),
            style = Stroke(width = 1.6f * u),
        )
        // 两侧圆柱阴影
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.Black.copy(alpha = 0.40f * alpha), Color.Transparent, Color.Black.copy(alpha = 0.40f * alpha)),
                startX = left, endX = right,
            ),
            topLeft = Offset(left, yTop - ampV), size = Size(w, h + 2 * ampV),
        )
    }
}
