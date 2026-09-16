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
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.PillBgDark
import cn.wangce.lumi.ui.theme.PillBgLight
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 烟雾圈：松开时从火头吐出
private data class Puff(val id: Int, val born: Long, val drift: Float, val sizeF: Float)
// 坠落的烟灰块：一节烟灰断裂脱落，成簇多碎块洒落（重力 + 旋转 + 空气扰动）
private data class AshFall(
    val id: Int, val born: Long,
    val segTop: Float,     // 块初始顶端相对燃烧环线的比例（负值 = 长在烟身上方）
    val sizeF: Float,      // 块高占烟身满长的比例
    val shock: Float,      // 弹灰冲击强度 0..1（越大越向外散开）
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
// 烟丝粒子：待机/吸入时从火头上升的细烟
private data class Ember(val id: Int, val born: Long, val drift: Float)

// 烟灰斜纹配色（1:1 复刻参考网页 repeating-linear-gradient(45deg, #4b5563 2px, #9ca3af 2px 4px)）
private val AshStripeBg = Color(0xFF9CA3AF)
private val AshStripeDark = Color(0xFF4B5563)
private val AshCharTop = Color(0xFF1F2937)      // 灰顶焦黑（from-gray-800/50）
private val BurnRing = Color(0xFFEF4444)        // 燃烧环 red-500
private val BurnRingDark = Color(0xFF7F1D1D)    // red-900
private val BurnRingHot = Color(0xFFFB923C)     // orange-400
private val BurnGlow = Color(0xFFFF3C00)        // 燃烧环红光晕 shadow rgba(255,60,0,.8)
private val SmokeGray = Color(0xFF8FA0A8)

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
    val dark = LocalDarkTheme.current
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

    // 掉落一节或多节烟灰：生成碎块簇 + 附带烟尘微粒
    fun dropAshSegments(count: Int, shock: Float = 0f) {
        val segCount = (ashLen * 3f).toInt().coerceAtLeast(1)
        val n = count.coerceAtMost(segCount)
        val now = System.currentTimeMillis()
        repeat(n) { j ->
            ashes.add(
                AshFall(
                    id = fallId++, born = now + j * 90L,
                    segTop = -(j + 1) * SEG_F,
                    sizeF = SEG_F * (0.85f + det(j * 7 + 3) % 30 / 30f * 0.3f),
                    shock = shock,
                ),
            )
            // 碎块落尘：少量细尘微粒
            val dustN = 2 + det(fallId * 3 + j) % 2
            repeat(dustN) {
                dusts.add(AshDust(dustId++, now + j * 90L, Random.nextFloat() * 2f - 1f, 0.5f + Random.nextFloat() * 0.8f))
            }
        }
        while (ashes.size > 9) ashes.removeAt(0)
        while (dusts.size > 40) dusts.removeAt(0)
        while (sparks.size > 24) sparks.removeAt(0)
        ashLen = (ashLen - n / 3f).coerceAtLeast(0f)
    }

    // 吸入循环：火星渐亮、烟灰向上生长、长灰小概率自然断裂；松开后火星渐暗
    LaunchedEffect(inhaling) {
        if (inhaling) {
            while (inhaling) {
                glow = (glow + 0.08f).coerceAtMost(1f)
                ashLen = (ashLen + 0.004f).coerceAtMost(1f)
                // 烟身随燃烧变短；燃尽则整支重置（长度回满、烟灰清零）
                smokeLen = (smokeLen - 0.004f).coerceAtLeast(0f)
                if (smokeLen <= 0f) {
                    smokeLen = 1f
                    ashLen = 0f
                }
                // 烟灰过长：长满临界长度必掉一节（自重断裂）
                if (ashLen >= 1f) {
                    dropAshSegments(1, shock = 0f)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                // 自然断裂：烟灰超过约 1 节后，吸入时有小概率整节掉落
                else if (ashLen > 0.34f && Random.nextFloat() < 0.02f) {
                    dropAshSegments(1 + Random.nextInt(2), shock = 0f)
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
            if (embers.size > 20) embers.removeAt(0)
            delay(if (inhaling) 160 else 750)
        }
    }
    // 过期粒子清理
    LaunchedEffect(Unit) {
        while (true) {
            delay(300)
            val now = System.currentTimeMillis()
            puffs.removeAll { now - it.born > 2200 }
            ashes.removeAll { now - it.born > 1300 }
            dusts.removeAll { now - it.born > 900 }
            sparks.removeAll { now - it.born > 700 }
            embers.removeAll { now - it.born > 1900 }
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
                    inhaling = inhaling,
                    ashLen = ashLen,
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
                                    val n = 2 + Random.nextInt(2)
                                    repeat(n) { i ->
                                        puffs.add(Puff(puffId++, now + i * 140L, Random.nextFloat() * 2f - 1f, 0.7f + Random.nextFloat() * 0.6f))
                                    }
                                    while (puffs.size > 12) puffs.removeAt(0)
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
                icon = { Icon(Icons.Outlined.Air, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp)) },
                dark = dark,
                onClick = {
                    Log.d("SmokeDebug", "弹灰 click ashLen=$ashLen")
                    if (ashLen > 0.06f) {
                        dropAshSegments((ashLen * 3f).toInt().coerceAtLeast(1), shock = 1f)
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
                icon = { Icon(Icons.Outlined.SmokingRooms, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp)) },
                dark = dark,
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

// 底部互动胶囊按钮（与撸宠页同款语言）
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

// ============ 纯 Canvas 2D 香烟：形状 1:1 复刻参考网页 ============
//
// 布局（自底向上，参考 flex-col-reverse，450px 容器坐标）：
//   滤嘴(80, 底部固定, rounded-b-md, 软木纹点阵) → 烟身(300, 白渐变+竖排字)
//   → 燃烧环(6, red-500+红光晕, 固定) → 烟灰(最高60, 从燃烧环向上生长, 45°斜纹)
//   火头光晕在灰顶端（真烟燃烧面），烟从火头上升/吐圈
private fun DrawScope.drawCigarette(
    st: CigStyle,
    glow: Float,
    inhaling: Boolean,
    ashLen: Float,
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
    val ashH = ashLen * REF_ASH_MAX * u
    val emberY = burnY - ashH            // 火头 = 灰顶端（燃烧面）
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

    // ---- 燃烧环：位于烟身顶端，red-500 高 6px + 内部渐变（去掉外圈红晕光圈） ----
    drawRect(
        BurnRing.copy(alpha = 0.85f + 0.15f * glow),
        topLeft = Offset(left, burnY),
        size = Size(paperW, 6f * u),
    )
    // 火线（燃烧面）：吸气时烧红——亮度/宽度随 glow 增强
    drawRect(
        Brush.horizontalGradient(listOf(BurnRingDark, BurnRingHot, BurnRingDark), startX = left, endX = right),
        topLeft = Offset(left, burnY),
        size = Size(paperW, (6f + 2.5f * glow) * u),
    )

    // ---- 烟灰：从燃烧环向上生长，45° 斜纹 + 顶部焦黑 + 两侧阴影，分节带裂纹 ----
    if (ashH > 1f) {
        val ashLeft = cx - paperW * 0.49f
        val ashRight = cx + paperW * 0.49f
        // 顶部一节在接近断裂(临界长度)时预拉扯：裂纹微微撑开再断。0..1 随 ashLen 渐增
        val nearBreak = (ashLen - 0.55f) / 0.30f          // 约 0.55..0.85 → 0..1
        val stretch = if (nearBreak > 0f) nearBreak * 0.06f else 0f   // 顶部横向拉开 0..6%
        val segH = ashH / 3f
        val ashPath = Path().apply {
            addRoundRect(
                RoundRect(
                    ashLeft, emberY, ashRight, burnY,
                    topLeftCornerRadius = CornerRadius(3f * u, 3f * u),
                    topRightCornerRadius = CornerRadius(3f * u, 3f * u),
                ),
            )
        }
        clipPath(ashPath) {
            drawRect(AshStripeBg, topLeft = Offset(ashLeft, emberY), size = Size(ashRight - ashLeft, ashH))
            // repeating-linear-gradient(45deg, #4b5563 2px, #9ca3af 2px 4px)
            rotate(degrees = -45f, pivot = Offset(cx, (emberY + burnY) / 2f)) {
                val span = (paperW + ashH) * 1.7f
                var sx = cx - span / 2f
                while (sx < cx + span / 2f) {
                    drawRect(
                        AshStripeDark,
                        topLeft = Offset(sx, (emberY + burnY) / 2f - span / 2f),
                        size = Size(2f * u, span),
                    )
                    sx += 4f * u
                }
            }
            // 顶部焦黑 from-gray-800/50 to-transparent（灰顶往下渐隐一半）
            drawRect(
                Brush.verticalGradient(
                    listOf(AshCharTop.copy(alpha = 0.50f), Color.Transparent),
                    startY = emberY, endY = emberY + ashH * 0.5f,
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
            // 分节裂纹：节与节之间画暗色横缝（略带焦黑），越靠顶部越深
            for (s in 1..2) {
                val sy = emberY + segH * s
                // 顶部接缝在越接近断裂时越被预拉扯撑开（裂纹先张再断）
                val open = if (s == 2) stretch * segH * 1.4f else 0f
                val wDark = 1.4f * u * (1f + 0.6f * s) + open
                drawRect(
                    AshCharTop.copy(alpha = 0.55f),
                    topLeft = Offset(ashLeft + 0.5f * u, sy - wDark / 2f),
                    size = Size(paperW - u, wDark),
                )
                drawRect(
                    Color.Black.copy(alpha = 0.28f),
                    topLeft = Offset(ashLeft + 0.5f * u, sy + wDark / 2f - 0.4f * u),
                    size = Size(paperW - u, 0.8f * u),
                )
            }
        }
    }

    // 火头红色圆片已移除（燃烧面由火线亮度表现）

    // 烟丝粒子（火头上升的细烟）
    embers.forEach { e ->
        val t = ((now - e.born) / 1800f).coerceIn(0f, 1f)
        if (t in 0f..0.999f) {
            drawCircle(
                SmokeGray.copy(alpha = 0.30f * (1 - t)),
                radius = 2.6.dp.toPx() * (1 - 0.4f * t),
                center = Offset(cx + e.drift * t * 46f, emberY - 6.dp.toPx() - h * 0.09f * t),
            )
        }
    }

    // 呼出的烟雾：松开后多团柔和软烟上飘扩散淡出（无描边，不是圆圈）
    puffs.forEach { p ->
        val t = ((now - p.born) / 2200f)
        if (t in 0f..0.999f) {
            val baseR = 14.dp.toPx() * p.sizeF
            val cy = emberY - 12.dp.toPx() - h * 0.17f * t
            val px = cx + p.drift * h * 0.07f * sin(t * 1.8f + p.id)
            val alpha = 0.15f * (1 - t) * (0.5f + 0.5f * (t * 4f).coerceAtMost(1f))   // 淡入后缓慢淡出
            val r = baseR * (1f + 1.6f * t)   // 烟雾扩散
            // 三层重叠软烟，错位叠加形成烟团
            drawCircle(SmokeGray.copy(alpha = alpha), radius = r, center = Offset(px, cy))
            drawCircle(SmokeGray.copy(alpha = alpha * 0.75f), radius = r * 0.7f, center = Offset(px + r * 0.35f, cy + r * 0.18f))
            drawCircle(SmokeGray.copy(alpha = alpha * 0.6f), radius = r * 0.55f, center = Offset(px - r * 0.4f, cy - r * 0.15f))
        }
    }

    // 坠落的烟灰块：堆叠的每一节独立成簇洒落（重力 + 旋转 + 空气扰动，非直线）
    ashes.forEach { a ->
        val t = ((now - a.born) / 1300f)
        if (t !in 0f..0.999f) return@forEach
        val shPx = REF_BODY_H * u
        val chunkH = a.sizeF * shPx
        val chunkW = paperW * 0.96f
        val startY = burnY + a.segTop * shPx
        // 重力加速下落（t²）叠加初速；shock 越大初速越猛；落到地面后碎散消散
        val groundY = filterTopY + (REF_FILTER_H + 26f) * u
        val g = 0.34f + 0.06f * a.shock
        val fall = h * g * t * t + chunkH * (0.35f + 0.35f * a.shock) * t
        val alphaBase = 1f - t * 0.55f
        // 确定性碎块：尺寸随机不统一，自 a.id 派生，帧间稳定不闪烁
        val fragN = 3 + det(a.id * 31 + 7) % 3
        for (k in 0 until fragN) {
            val h1 = det(a.id * 31 + k * 131 + 11)
            val h2 = det(a.id * 31 + k * 131 + 29)
            val h3 = det(a.id * 31 + k * 131 + 47)
            val dxr = (h1 % 23) / 23f - 0.5f
            val dw = 0.30f + (h2 % 19) / 19f * 0.32f
            val dh = 0.28f + (h3 % 21) / 21f * 0.46f
            val rot0 = (h1 % 29) / 29f * 180f - 90f
            val swayA = 0.005f + (h2 % 17) / 17f * 0.010f   // 空气扰动幅度
            val swayP = (h3 % 37) / 37f * 6.283f
            // 横向：初始偏移 + 冲击散开（shock） + 空气扰动（正弦，非直线）
            val shockX = a.shock * dxr * chunkW * (0.8f + 1.6f * t)
            val swayX = sin(t * 9f + swayP) * swayA * h
            val fx = cx + dxr * chunkW * 0.45f + shockX + swayX
            // 竖向：整簇下落 + 每碎块错位纵向；落到地面后钳制并加速碎散消散
            val fy0 = startY + fall * (0.85f + dh * 0.5f) + k * chunkH * 0.18f
            val fy = fy0.coerceAtMost(groundY + (h3 % 5) * 3f * u)
            val nearGround = ((fy0 - groundY + 34f * u) / (34f * u)).coerceIn(0f, 1f)
            val alpha = alphaBase * (1f - 0.85f * nearGround)
            val wPx = chunkW * dw
            val hPx = chunkH * dh
            // 旋转：重力下翻滚（随位移渐增）
            val rot = rot0 + (h1 % 11) / 11f * 260f * t + dxr * 40f * t
            rotate(degrees = rot, pivot = Offset(fx, fy)) {
                drawAshChunk(fx, fy, wPx, hPx, alpha, u, seed = a.id * 131 + k * 17)
            }
        }
    }

    // 烟尘微粒：碎块下落溅起的细尘，轻飘淡出
    dusts.forEach { d ->
        val t = ((now - d.born) / 900f)
        if (t in 0f..0.999f) {
            drawCircle(
                AshStripeBg.copy(alpha = 0.35f * (1 - t)),
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
            val sy = emberY - 2f * u - s.vy * h * 0.10f * t - t * t * h * 0.10f
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

// 一块烟灰：锯齿轮廓（上缘断离面起伏、两侧缺口内凹、下缘 3 齿参差），45° 斜纹 + 顶部焦黑 + 两侧阴影
private fun DrawScope.drawAshChunk(x: Float, yTop: Float, w: Float, h: Float, alpha: Float, u: Float, seed: Int) {
    val left = x - w / 2f
    val right = x + w / 2f
    // 确定性锯齿（seed 派生，帧间稳定不闪烁）
    fun jit(i: Int, amp: Float) = (det(seed * 13 + i) % 41) / 41f * amp - amp / 2f
    val rV = det(seed * 7 + 1) % 19 / 19f
    val rH = det(seed * 7 + 3) % 19 / 19f
    val ampV = (0.10f + 0.10f * rV) * h
    val ampH = (0.06f + 0.04f * rH) * w
    val bottomY = yTop + h
    val toothW = w / 3f
    val outline = Path().apply {
        // 上缘：断离面轻微起伏（从左到右）
        moveTo(left + jit(1, ampH), yTop)
        // 左侧：中段缺口内凹
        lineTo(left, yTop + h * 0.22f + jit(2, ampV))
        lineTo(left + jit(3, ampH), yTop + h * 0.55f + jit(3, ampV))  // 内凹点
        lineTo(left, yTop + h * 0.78f + jit(4, ampV))
        lineTo(left + jit(5, ampH), bottomY)
        // 下缘：3 齿参差（外凸齿 + 内凹谷交替）
        for (t in 0..2) {
            val tx = left + toothW * t
            // 谷（往内凹）
            lineTo(tx + toothW * 0.5f, bottomY - jit(30 + t, ampV * 0.7f))
            // 齿（外凸）
            lineTo(tx + toothW, bottomY + jit(40 + t, ampV * 0.35f))
        }
        lineTo(right + jit(8, ampH), bottomY)
        // 右侧：中段缺口内凹（镜像）
        lineTo(right, yTop + h * 0.78f + jit(9, ampV))
        lineTo(right - jit(10, ampH), yTop + h * 0.55f + jit(10, ampV))
        lineTo(right, yTop + h * 0.22f + jit(11, ampV))
        close()
    }
    clipPath(outline) {
        // 锯齿轮廓的包围盒背景（回补可能露白的边缘）
        val bbl = left - ampH
        val bbr = right + ampH
        drawRect(AshStripeBg, topLeft = Offset(bbl, yTop - ampV), size = Size(bbr - bbl, h + 2 * ampV), alpha = alpha)
        // 45° 斜纹
        rotate(degrees = -45f, pivot = Offset(x, yTop + h / 2f)) {
            val span = (w + h) * 1.6f
            var sx = x - span / 2f
            while (sx < x + span / 2f) {
                drawRect(AshStripeDark, topLeft = Offset(sx, yTop + h / 2f - span / 2f), size = Size(2f * u, span), alpha = alpha)
                sx += 4f * u
            }
        }
        // 顶部焦黑（断离面）
        drawRect(
            Brush.verticalGradient(listOf(AshCharTop.copy(alpha = 0.6f * alpha), Color.Transparent), startY = yTop, endY = yTop + h * 0.6f),
            topLeft = Offset(bbl, yTop - ampV), size = Size(bbr - bbl, h + 2 * ampV),
        )
        // 焦黑边缘：四周描一圈加深断口
        drawPath(
            Path().apply {
                moveTo(left + jit(1, ampH), yTop)
                lineTo(left, yTop + h * 0.22f + jit(2, ampV))
                lineTo(left + jit(3, ampH), yTop + h * 0.55f + jit(3, ampV))
                lineTo(left, yTop + h * 0.78f + jit(4, ampV))
                lineTo(left + jit(5, ampH), bottomY)
                for (t in 0..2) {
                    val tx = left + toothW * t
                    lineTo(tx + toothW * 0.5f, bottomY - jit(30 + t, ampV * 0.7f))
                    lineTo(tx + toothW, bottomY + jit(40 + t, ampV * 0.35f))
                }
                close()
            },
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
