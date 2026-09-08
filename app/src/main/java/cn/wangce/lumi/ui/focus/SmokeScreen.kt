package cn.wangce.lumi.ui.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 烟雾圈：松开时从火头吐出
private data class Puff(val id: Int, val born: Long, val drift: Float)
// 坠落的烟灰块：弹灰时灰段整体脱落
private data class AshFall(val id: Int, val born: Long, val frac: Float, val lean: Float)
// 烟丝粒子：待机/吸入时从火头上升的细烟
private data class Ember(val id: Int, val born: Long, val drift: Float)

// Canvas 叠加层配色
private val SmokeAsh = Color(0xFF9E9E9E)
private val TipRed = Color(0xFFE85D3A)
private val TipGlow = Color(0xFFFF8A50)
private val TipCore = Color(0xFFFFD9A0)
private val SmokeGray = Color(0xFF8FA0A8)

// 3D 圆柱体尺寸常量
private const val CIG_H = 3.0f
private const val CIG_ASH_MAX = 0.66f
private const val TIP_Y = CIG_H / 2f

// 赛博吸烟：3D 圆柱体香烟（普通/细支/雪茄）+ Canvas 烟雾粒子叠加，
// 按住吸入（火星变亮+烟灰增长+蓄力光环）、松开吐圈、弹灰、换款式
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

    var style by remember { mutableIntStateOf(0) }      // 0 普通烟 / 1 细支 / 2 雪茄
    var inhaling by remember { mutableStateOf(false) }  // 是否按住吸入
    var ashLen by remember { mutableFloatStateOf(0f) }  // 烟灰长度 0..1
    var glow by remember { mutableFloatStateOf(0f) }    // 火星亮度 0..1
    val puffs = remember { mutableStateListOf<Puff>() }
    val ashes = remember { mutableStateListOf<AshFall>() }
    val embers = remember { mutableStateListOf<Ember>() }
    val appear = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // 3D 引擎
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val view = rememberView(engine)
    val renderer = rememberRenderer(engine)
    val scene = rememberScene(engine)
    val collisionSystem = rememberCollisionSystem(view)
    val childNodes = rememberNodes()

    // 节点引用
    var ashNode by remember { mutableStateOf<CylinderNode?>(null) }
    var paperNode by remember { mutableStateOf<CylinderNode?>(null) }
    var tipLight by remember { mutableStateOf<LightNode?>(null) }

    // 吸入循环：火星渐亮、烟灰增长；松开后火星渐暗
    LaunchedEffect(inhaling) {
        if (inhaling) {
            while (inhaling) {
                glow = (glow + 0.08f).coerceAtMost(1f)
                ashLen = (ashLen + 0.012f).coerceAtMost(1f)
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
            puffs.removeAll { now - it.born > 1700 }
            ashes.removeAll { now - it.born > 1100 }
            embers.removeAll { now - it.born > 1900 }
        }
    }

    // 构建 3D 香烟：切换款式时触发
    LaunchedEffect(style) {
        childNodes.clear()
        val radius = when (style) { 1 -> 0.08f; 2 -> 0.18f; else -> 0.12f }
        val filterH = CIG_H * (if (style == 2) 0.16f else 0.24f)
        val totalPaperH = CIG_H - filterH
        val ashH = ashLen * CIG_ASH_MAX
        val realPaperH = (totalPaperH - ashH).coerceAtLeast(0.001f)

        // 烟灰段（顶部）
        val ashMat = materialLoader.createColorInstance(
            Float4(0.62f, 0.62f, 0.62f, 1f), 0f, 0.9f, 0.5f,
        )
        val ash = CylinderNode(
            engine = engine,
            radius = radius,
            height = ashH.coerceAtLeast(0.001f),
            center = Position(0f, TIP_Y - ashH / 2f, 0f),
            materialInstance = ashMat,
        ) {}
        ash.isVisible = ashH > 0.01f
        ashNode = ash
        childNodes.add(ash)

        // 纸身（中间）
        val paperColor = if (style == 2) Float4(0.48f, 0.29f, 0.17f, 1f) else Float4(0.98f, 0.97f, 0.94f, 1f)
        val paperRough = if (style == 2) 0.6f else 0.75f
        val paperMat = materialLoader.createColorInstance(paperColor, 0f, paperRough, 0.5f)
        val paper = CylinderNode(
            engine = engine,
            radius = radius,
            height = realPaperH,
            center = Position(0f, TIP_Y - ashH - realPaperH / 2f, 0f),
            materialInstance = paperMat,
        ) {}
        paperNode = paper
        childNodes.add(paper)

        // 滤嘴（底部）
        val filterColor = when (style) {
            1 -> Float4(0.75f, 0.83f, 0.81f, 1f)
            2 -> Float4(0.36f, 0.20f, 0.09f, 1f)
            else -> Float4(0.91f, 0.58f, 0.35f, 1f)
        }
        val filterMat = materialLoader.createColorInstance(filterColor, 0f, 0.5f, 0.5f)
        val filter = CylinderNode(
            engine = engine,
            radius = radius,
            height = filterH,
            center = Position(0f, -TIP_Y + filterH / 2f, 0f),
            materialInstance = filterMat,
        ) {}
        childNodes.add(filter)

        // 火头点光源（亮度随 glow 变化）
        val light = LightNode(engine, LightManager.Type.POINT) {
            intensity(300f + glow * 30000f)
            color(1f, 0.5f, 0.2f)
            falloff(1.5f)
            position(0f, TIP_Y + 0.1f, 0f)
        }
        tipLight = light
        childNodes.add(light)

        scope.launch {
            appear.snapTo(0.72f)
            appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    // 烟灰增长 / 火星变化时更新几何体和光源
    LaunchedEffect(ashLen, style, glow) {
        val ash = ashNode ?: return@LaunchedEffect
        val paper = paperNode ?: return@LaunchedEffect
        val light = tipLight ?: return@LaunchedEffect
        val radius = when (style) { 1 -> 0.08f; 2 -> 0.18f; else -> 0.12f }
        val filterH = CIG_H * (if (style == 2) 0.16f else 0.24f)
        val totalPaperH = CIG_H - filterH
        val ashH = ashLen * CIG_ASH_MAX
        val realPaperH = (totalPaperH - ashH).coerceAtLeast(0.001f)

        ash.updateGeometry(radius = radius, height = ashH.coerceAtLeast(0.001f), center = Position(0f, TIP_Y - ashH / 2f, 0f))
        ash.isVisible = ashH > 0.01f
        paper.updateGeometry(radius = radius, height = realPaperH, center = Position(0f, TIP_Y - ashH - realPaperH / 2f, 0f))
        light.intensity = glow * 30000f
    }

    // 呼吸相位（火头微光脉动）
    val breathT = rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "breath",
    )

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
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 32.sp),
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
            modifier = Modifier
                .size(300.dp)
                .pointerInput(Unit) {
                    var puffId = 0
                    detectTapGestures(
                        onPress = {
                            inhaling = true
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            try {
                                awaitRelease()
                            } finally {
                                inhaling = false
                                val now = System.currentTimeMillis()
                                val n = 2 + Random.nextInt(2)
                                repeat(n) { i ->
                                    puffs.add(Puff(puffId++, now + i * 140L, Random.nextFloat() * 2f - 1f))
                                }
                                while (puffs.size > 12) puffs.removeAt(0)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.fillMaxSize().scale(appear.value)) {
                // 3D 场景：圆柱体香烟
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    environmentLoader = environmentLoader,
                    view = view,
                    renderer = renderer,
                    scene = scene,
                    collisionSystem = collisionSystem,
                    mainLightNode = rememberMainLightNode(engine) {
                        intensity = 80_000f
                    },
                    cameraNode = rememberCameraNode(engine) {
                        position = Position(z = 5f)
                    },
                    cameraManipulator = rememberCameraManipulator(),
                    childNodes = childNodes,
                )
                // Canvas 烟雾粒子叠加（烟圈、烟丝、坠灰、火头光晕）
                Canvas(Modifier.fillMaxSize()) {
                    drawSmokeParticles(
                        style = style,
                        glow = glow,
                        breath = breathT.value,
                        inhaling = inhaling,
                        puffs = puffs,
                        ashes = ashes,
                        embers = embers,
                    )
                }
            }
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
                    if (ashLen > 0.06f) {
                        ashes.add(AshFall(ashes.size + Random.nextInt(1000), System.currentTimeMillis(), ashLen, Random.nextFloat() * 0.4f - 0.2f))
                        if (ashes.size > 6) ashes.removeAt(0)
                        ashLen = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
            )
            InteractionButton(
                label = stringResource(R.string.focus_smoke_style),
                icon = { Icon(Icons.Outlined.SmokingRooms, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp)) },
                dark = dark,
                onClick = {
                    scope.launch {
                        appear.animateTo(0.72f, tween(160))
                        style = (style + 1) % 3
                    }
                },
            )
        }
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

// ============ Canvas 叠加层：烟雾粒子 + 火头光晕 ============

private fun DrawScope.drawSmokeParticles(
    style: Int,
    glow: Float,
    breath: Float,
    inhaling: Boolean,
    puffs: List<Puff>,
    ashes: List<AshFall>,
    embers: List<Ember>,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val now = System.currentTimeMillis()

    val paperW = w * when (style) { 1 -> 0.11f; 2 -> 0.22f; else -> 0.16f }
    val sh = h * 0.56f
    val top = h * 0.12f
    val halfW = paperW / 2f

    // 烟丝粒子（火头上升的细烟，画在最底层）
    embers.forEach { e ->
        val t = ((now - e.born) / 1800f).coerceIn(0f, 1f)
        if (t in 0f..0.999f) {
            drawCircle(
                SmokeGray.copy(alpha = 0.30f * (1 - t)),
                radius = 2.6.dp.toPx() * (1 - 0.4f * t),
                center = Offset(cx + e.drift * t * 46f, top - 6.dp.toPx() - h * 0.09f * t),
            )
        }
    }

    // 吐出的烟圈（上飘扩大淡出）
    puffs.forEach { p ->
        val t = ((now - p.born) / 1500f)
        if (t in 0f..0.999f) {
            drawCircle(
                SmokeGray.copy(alpha = 0.50f * (1 - t)),
                radius = (10.dp.toPx() + 40.dp.toPx() * t),
                center = Offset(cx + p.drift * h * 0.06f * sin(t * 2f), top - 12.dp.toPx() - h * 0.16f * t),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }

    // 坠落的烟灰块（弹灰）
    ashes.forEach { a ->
        val t = ((now - a.born) / 1000f).coerceIn(0f, 1f)
        if (t in 0f..0.999f) {
            val fallH = a.frac * sh * 0.22f
            val startY = top + fallH / 2f
            rotate(degrees = a.lean * 60f * t, pivot = Offset(cx, startY)) {
                drawRect(
                    SmokeAsh.copy(alpha = (1 - t)),
                    topLeft = Offset(cx - halfW + w * 0.02f * t, startY - fallH / 2f + h * 0.5f * t * t),
                    size = Size(paperW * 0.96f, fallH),
                )
            }
        }
    }

    // 火头光晕（3D 点光源的视觉补充）
    val tipPulse = 0.06f * sin(breath)
    drawCircle(
        TipGlow.copy(alpha = 0.16f + 0.34f * glow),
        radius = paperW * (0.95f + 0.55f * glow + tipPulse),
        center = Offset(cx, top + 2f),
    )
    drawOval(
        TipRed.copy(alpha = 0.88f + 0.12f * glow),
        topLeft = Offset(cx - halfW, top - paperW * 0.28f),
        size = Size(paperW, paperW * 0.56f),
    )
    drawCircle(
        TipCore.copy(alpha = 0.30f + 0.55f * glow),
        radius = paperW * (0.16f + 0.06f * glow),
        center = Offset(cx, top + paperW * 0.02f),
    )

    // 蓄力光环（吸入时脉动）
    if (inhaling) {
        drawCircle(
            TipGlow.copy(alpha = 0.35f),
            radius = paperW * (1.35f + 0.22f * sin(breath)),
            center = Offset(cx, top + 2f),
            style = Stroke(width = 2.5.dp.toPx()),
        )
        drawCircle(
            TipGlow.copy(alpha = 0.18f),
            radius = paperW * (1.85f + 0.30f * sin(breath + 1.2f)),
            center = Offset(cx, top + 2f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
