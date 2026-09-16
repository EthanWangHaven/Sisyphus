package cn.wangce.lumi.ui.focus

import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.PillBgDark
import cn.wangce.lumi.ui.theme.PillBgLight
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 撸宠粒子：撸动时冒出的爱心
private data class Heart(val id: Int, val x: Float, val y: Float, val born: Long)
private val HeartPink = Color(0xFFF28B82)

// 赛博撸宠：实拍绿幕序列帧宠物（猫/狗）+ 滑动撸（爱心）、喂食、换宠物
@Composable
fun PetScreen(viewModel: FocusViewModel = hiltViewModel(), onBack: () -> Unit) {
    val dark = LocalDarkTheme.current
    val running by viewModel.running.collectAsStateWithLifecycle()
    var elapsedSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) {
        while (running) {
            elapsedSec = viewModel.elapsedSeconds()
            delay(500)
        }
    }

    var kind by remember { mutableIntStateOf(0) } // 0 猫 / 1 狗
    var mood by remember { mutableStateOf("idle") }
    val hearts = remember { mutableStateListOf<Heart>() }
    val foodAnim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val appearScale = remember { Animatable(1f) }

    // 撸动时的猫咕噜声（循环播放；素材取自 cat_pet_sim，ISC 协议，github.com/darkgumby/cat_pet_sim）
    // purring 直接跟随拖拽手势，与 mood 解耦（mood 0.9s 自动回 idle 会在中途断音）
    var purring by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val purrPlayer = remember {
        MediaPlayer.create(context, R.raw.cat_purring)?.apply {
            isLooping = true
            setVolume(0.35f, 0.35f)
        }
    }
    LaunchedEffect(purring) {
        if (purring) purrPlayer?.start()
        else {
            purrPlayer?.pause()
            purrPlayer?.seekTo(0)
        }
    }
    DisposableEffect(Unit) {
        onDispose { purrPlayer?.release() }
    }

    // 撸动结束 0.9s 后回到待机
    LaunchedEffect(mood) {
        if (mood == "pet") {
            delay(900)
            if (mood == "pet") {
                mood = "idle"
                hearts.clear()
            }
        }
    }

    // 喂食重播序号：eat 播放中再次喂食时强制换源重播
    var eatSeq by remember { mutableIntStateOf(0) }

    // 当前段动画源：豆包图生视频（同一定妆照保证形象一致）→ 绿幕抠图 → 无缝循环 WebP。
    // idle/pet 无限循环，eat 单次播放、播完由 AnimatedImageDrawable 回调回 idle。
    // AnimatedImageDrawable 需 API 28+，26/27 老设备降级为不显示。
    val petSrc = remember(kind, mood, eatSeq) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            null
        } else {
            val prefix = if (kind == 0) "cat" else "dog"
            runCatching {
                // ImageDecoder.createSource 无 InputStream 重载，先落 cacheDir 再按 File 解码
                val f = File(context.cacheDir, "pet_${prefix}_$mood.webp")
                if (!f.exists()) {
                    context.assets.open("pet/${prefix}_$mood.webp").use { input ->
                        f.outputStream().use { input.copyTo(it) }
                    }
                }
                ImageDecoder.createSource(f)
            }.getOrElse {
                // 狗素材未就绪时回退猫
                runCatching {
                    val f = File(context.cacheDir, "pet_cat_$mood.webp")
                    if (!f.exists()) {
                        context.assets.open("pet/cat_$mood.webp").use { input ->
                            f.outputStream().use { input.copyTo(it) }
                        }
                    }
                    ImageDecoder.createSource(f)
                }.getOrNull()
            }
        }
    }

    // 首次进入 / 换宠弹出动画
    LaunchedEffect(kind) {
        appearScale.snapTo(0.72f)
        appearScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 顶栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCard(
                modifier = Modifier.size(44.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), CircleShape)
                    .clip(CircleShape).clickable(onClick = onBack),
                cornerRadius = 22,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.s5f4112),
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.focus_pet_title), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (running) {
                Text(formatPetTime(elapsedSec), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.weight(1f))

        // 宠物互动区：实拍序列帧 + 滑动撸
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center,
        ) {
            // 序列帧动画（ImageView + AnimatedImageDrawable）
            AndroidView(
                modifier = Modifier.fillMaxSize().scale(appearScale.value),
                factory = { ctx ->
                    ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
                },
                update = { iv ->
                    if (iv.tag != petSrc) {
                        iv.tag = petSrc
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && petSrc != null) {
                            val drawable = ImageDecoder.decodeDrawable(petSrc)
                            iv.setImageDrawable(drawable)
                            val anim = drawable as? AnimatedImageDrawable
                            if (anim != null) {
                                if (mood == "eat") {
                                    anim.repeatCount = 0
                                    // 播完回 idle；回调可能来自渲染线程，post 回主线程写状态
                                    anim.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                                        override fun onAnimationEnd(drawable: Drawable) {
                                            iv.post { mood = "idle" }
                                        }
                                    })
                                } else {
                                    anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                                    anim.clearAnimationCallbacks()
                                }
                                anim.start()
                            }
                        } else {
                            iv.setImageDrawable(null)
                        }
                    }
                },
            )
            // 爱心粒子叠加
            Canvas(Modifier.fillMaxSize()) { drawHearts(hearts) }
            // 手势层：Scene 的 AndroidView 会消费触摸（cameraManipulator=null 只是不处理
            // 手势，事件仍被 View 层拦截），手势必须覆盖在 Scene 之上才能收到。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var lastHeart = 0L
                        var heartId = 0
                        detectDragGestures(
                            onDragStart = {
                                mood = "pet"
                                purring = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDrag = { change, _ ->
                                val now = System.currentTimeMillis()
                                if (now - lastHeart > 90) {
                                    lastHeart = now
                                    hearts.add(Heart(heartId++, change.position.x, change.position.y, now))
                                    if (hearts.size > 24) hearts.removeAt(0)
                                }
                            },
                            onDragEnd = { purring = false },
                            onDragCancel = { purring = false },
                        )
                    },
            )
        }

        Text(stringResource(R.string.focus_pet_hint), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))

        // 底部操作
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            InteractionButton(stringResource(R.string.focus_pet_feed),
                { Icon(Icons.Outlined.Restaurant, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp)) },
                dark, onClick = {
                    if (!foodAnim.isRunning) {
                        scope.launch {
                            foodAnim.snapTo(0f)
                            foodAnim.animateTo(1f, tween(850, easing = FastOutLinearInEasing))
                            // eat.webp 单次播放，播完由 AnimatedImageDrawable 回调回 idle
                            mood = "eat"; eatSeq++
                        }
                    }
                })
            InteractionButton(stringResource(R.string.focus_pet_switch),
                { Icon(Icons.Outlined.Pets, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp)) },
                dark, onClick = {
                    scope.launch {
                        appearScale.animateTo(0.72f, tween(160))
                        // 复位状态：换宠后旧 eat 段 drawable 被替换，其"播完回 idle"回调不会再触发
                        mood = "idle"
                        hearts.clear()
                        kind = (kind + 1) % 2
                    }
                })
        }
        Spacer(Modifier.weight(1.2f))
    }
}

@Composable
private fun InteractionButton(label: String, icon: @Composable () -> Unit, dark: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.height(46.dp)
            .shadow(5.dp, RoundedCornerShape(14.dp), spotColor = if (dark) ShadowDark else ShadowLight)
            .clip(RoundedCornerShape(14.dp))
            .background(if (dark) PillBgDark else PillBgLight)
            .clickable(onClick = onClick).padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon(); Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.surface)
        }
    }
}

@Composable
private fun formatPetTime(sec: Int): String {
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

// ============ 爱心粒子 ============

private fun DrawScope.drawHearts(hearts: List<Heart>) {
    val now = System.currentTimeMillis()
    hearts.forEach { heart ->
        val age = (now - heart.born) / 1200f
        if (age in 0f..1f) {
            val rise = age * 60f
            val alpha = (1f - age).coerceIn(0f, 1f)
            val s = 0.7f + age * 0.5f
            translate(left = heart.x, top = heart.y - rise) {
                rotate(degrees = -8f + age * 10f) {
                    drawHeartPath(16f * s, HeartPink.copy(alpha = alpha))
                }
            }
        }
    }
}

private fun DrawScope.drawHeartPath(s: Float, color: Color) {
    drawPath(Path().apply {
        moveTo(0f, s * 0.35f)
        cubicTo(-s * 0.55f, -s * 0.15f, -s * 0.42f, -s * 0.52f, 0f, -s * 0.18f)
        cubicTo(s * 0.42f, -s * 0.52f, s * 0.55f, -s * 0.15f, 0f, s * 0.35f)
        close()
    }, color)
}
