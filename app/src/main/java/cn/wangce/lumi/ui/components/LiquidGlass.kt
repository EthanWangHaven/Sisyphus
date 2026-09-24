package cn.wangce.lumi.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.view.ViewParent
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

/*
 * 液态玻璃（Liquid Glass）材质：
 * 与旧 GlassSurface（半透明底 + 一条 hairline）的区别在于「边缘折射」——
 * 面板边缘对背后内容做透镜式位移 + 轻微色散 + 饱和度提升 + 内侧辉光，
 * 配合斜向渐变描边形成镜面高光，得到真实的「液态」厚度感。
 *
 * 实现路径：
 *   1) 把页面内容录成 GraphicsLayer 并按 blur 做高斯模糊（API 31+）
 *   2) 再用 AGSL RuntimeShader 对模糊结果做逐像素重采样（API 33+）
 * 低版本 / 着色器编译失败时自动降级：有模糊就只模糊，否则纯半透明底 + hairline。
 */

// 边缘折射着色器：圆角矩形 SDF → 边缘带强度 → 沿内法线位移采样
private const val LIQUID_GLASS_AGSL = """
uniform shader backdrop;
uniform float2 uSize;
uniform float  uRadius;
uniform float  uRefract;
uniform float  uChroma;
uniform float  uSat;
uniform float  uGlow;

half4 main(float2 c) {
    float2 h = uSize * 0.5;
    float2 p = c - h;
    // 圆角矩形有符号距离场：d < 0 在内部，0 在边界
    float2 q = abs(p) - (h - uRadius);
    float d = length(max(q, float2(0.0, 0.0))) + min(max(q.x, q.y), 0.0) - uRadius;

    // k：0 = 紧贴边缘，1 = 已深入内部（超过折射带宽度）
    // band 取短边 55%，保证细长控件（如底栏 56dp）也有明显可见的折射带
    float band = min(h.x, h.y) * 0.55 + 1.0;
    float k = clamp(-d / band, 0.0, 1.0);
    // 边缘最强、向内快速衰减（三次方），形成透镜的「厚边」观感
    float lens = pow(1.0 - k, 3.0);

    // 内法线方向（由边缘指向中心），加极小量避免 p 为零时归一化出 NaN
    float2 n = normalize(-p + float2(0.0001, 0.0001));
    float2 off = n * lens * uRefract;

    // 三通道用略不同的位移量 → 边缘轻微色散，这是「液态」质感的关键。
    // 注意：backdrop 的采样结果是「预乘 alpha」的，直接取 .r/.g/.b 会得到偏暗的脏色，
    // 必须逐通道除以 alpha 还原成直通色（等价于 float3(half4) 的隐式反预乘）。
    half4 sa = backdrop.eval(c + off * (1.0 + uChroma));
    half4 sb = backdrop.eval(c + off);
    half4 sc = backdrop.eval(c + off * (1.0 - uChroma));
    float3 f = float3(
        sa.r / max(sa.a, 0.0001),
        sb.g / max(sb.a, 0.0001),
        sc.b / max(sc.a, 0.0001),
    );

    // 提升饱和度：玻璃会把背后颜色「提纯」
    float lum = dot(f, float3(0.2126, 0.7152, 0.0722));
    f = mix(float3(lum, lum, lum), f, uSat);
    // 内侧辉光：模拟光在玻璃内部的散射堆积
    f += uGlow * lens * lens;

    return half4(half3(clamp(f, float3(0.0), float3(1.0))), 1.0);
}
"""

// 着色器实例：编译失败返回 null，调用方自动降级。
// 注意必须「每个面板一个实例」：同一个 RuntimeShader 不能同时绑到多个 RenderEffect 上，
// 否则共享时（例如底栏左右两段玻璃）会渲染成黑块。
private fun createLiquidGlassShader(): RuntimeShader? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return runCatching { RuntimeShader(LIQUID_GLASS_AGSL) }.getOrNull()
}

/**
 * 液态玻璃面板。
 *
 * @param backdrop 页面内容层（由 Scaffold 录制），为 null 时降级为普通玻璃
 * @param cornerRadius 圆角半径，同时用于折射的 SDF
 * @param blur 背板模糊半径（px）。液态玻璃 = 模糊 + 折射，二者都需要
 * @param refraction 边缘折射位移强度（dp），越大透镜感越强
 * @param chromatic 色散系数，0.02~0.08 之间为宜
 * @param saturation 背后内容饱和度提升倍数
 * @param glow 内侧辉光强度
 * @param tint 玻璃自身色调（半透明，叠在折射之上）
 * @param rimTint 镜面高光描边色（配合左上到右下的斜向渐变）
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    backdrop: GraphicsLayer? = null,
    cornerRadius: Dp = 28.dp,
    blur: Float = 24f,
    refraction: Dp = 12.dp,
    chromatic: Float = 0.05f,
    saturation: Float = 1.45f,
    glow: Float = 0.05f,
    tint: Color,
    rimTint: Color,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val shape = RoundedCornerShape(cornerRadius)
    // 每个面板独立一份 shader 实例，避免多块玻璃共享同一 RuntimeShader 时的绑定冲突
    val shader = remember { createLiquidGlassShader() }
    var panelBounds by remember { mutableStateOf(Rect.Zero) }
    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    // 单层：录制背板后，把「模糊 → 折射」串成一条 RenderEffect 链作用在这一层上。
    // 注意：不能再把带 RenderEffect 的 layer 画进另一个 layer 的 record{}，那样会得到纯黑。
    val glassLayer = rememberGraphicsLayer()

    // 面板落在独立窗口（Dialog / Popup）里时，兄弟节点录制拿不到窗口外内容，
    // 改为让系统对「窗口背后」做真模糊，玻璃才有可折射的底。
    LumiWindowBackdropBlur(blur)

    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // uniform 是实时的，逐帧更新即可；RenderEffect 链只在能力/尺寸变化时重建
    val refractEffect = remember(shader, panelSize, canBlur, blur) {
        if (panelSize == IntSize.Zero) {
            null
        } else {
            runCatching {
                // 先模糊，后折射（inner 先执行）
                val blurFx = if (canBlur) {
                    RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.DECAL)
                } else {
                    null
                }
                val refractFx = shader?.let {
                    RenderEffect.createRuntimeShaderEffect(it, "backdrop")
                }
                when {
                    blurFx != null && refractFx != null ->
                        RenderEffect.createChainEffect(refractFx, blurFx)
                    refractFx != null -> refractFx
                    else -> blurFx
                }?.asComposeRenderEffect()
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                panelBounds = it.boundsInRoot()
                panelSize = it.size
            }
            .clip(shape)
            .drawBehind {
                val source = backdrop
                if (source == null || panelSize == IntSize.Zero) return@drawBehind
                if (refractEffect == null) return@drawBehind

                // 录背板（相对面板坐标系），再让 RenderEffect 链统一做模糊 + 折射
                glassLayer.record(panelSize) {
                    translate(-panelBounds.left, -panelBounds.top) {
                        drawLayer(source)
                    }
                }
                if (shader != null) {
                    runCatching {
                        shader.setFloatUniform("uSize", size.width, size.height)
                        shader.setFloatUniform("uRadius", cornerRadius.toPx())
                        shader.setFloatUniform("uRefract", refraction.toPx())
                        shader.setFloatUniform("uChroma", chromatic)
                        shader.setFloatUniform("uSat", saturation)
                        shader.setFloatUniform("uGlow", glow)
                    }
                }
                glassLayer.renderEffect = refractEffect
                drawLayer(glassLayer)
            }
            .background(tint)
            // 斜向渐变描边：左上亮、右下暗，形成镜面高光边
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        rimTint,
                        rimTint.copy(alpha = rimTint.alpha * 0.18f),
                    ),
                ),
                shape = shape,
            ),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/*
 * 窗口背后真模糊（API 31+）：
 * Compose 的 Dialog / ModalBottomSheet 都是独立 Window，位于 Activity 窗口之上，
 * 无法像同级节点那样被「录屏模糊」。这里用系统跨窗口模糊（BLUR_BEHIND）对
 * 弹窗背后的页面内容做真高斯模糊，玻璃面板因此获得可折射的底。
 * 低版本 / 系统未开启模糊时自动跳过，此时仅剩 tick 半透明底 + hairline 兜底。
 */
@Composable
internal fun LumiWindowBackdropBlur(radiusDp: Float) {
    val view = LocalView.current
    DisposableEffect(view, radiusDp) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            onDispose { }
        } else {
            var window: Window? = null
            var parent: ViewParent? = view.parent
            while (parent != null) {
                if (parent is DialogWindowProvider) {
                    window = parent.window
                    break
                }
                parent = parent.parent
            }
            if (window == null) {
                onDispose { }
            } else {
                runCatching { window.setBackgroundBlurRadius(radiusDp.toInt()) }
                onDispose { runCatching { window.setBackgroundBlurRadius(0) } }
            }
        }
    }
}
