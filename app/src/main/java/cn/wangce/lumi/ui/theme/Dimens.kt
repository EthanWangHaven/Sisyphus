package cn.wangce.lumi.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/*
 * 圆角体系（Shape Consistency Lock，全 App 唯一一族）：
 * 24 主卡/仪表盘 → 16 标准卡 → 12 按钮与输入 → 8 小元素
 */
val RadiusHero = 24.dp
val RadiusCard = 16.dp
val RadiusControl = 12.dp
val RadiusSmall = 8.dp

/*
 * 连续圆角（iOS squircle）：把四分之一圆弧换成超椭圆曲线（指数由圆 2 随 smoothing 升到 4），
 * 曲率在「直边 → 圆角」交界处过渡更缓，视觉上比标准 RoundedCornerShape 更柔和。
 * smoothing = 0 时退化为标准圆弧；四角各采样 16 段，纯 Path 无额外离屏开销。
 */
class SquircleShape(
    private val corner: Dp,
    private val smoothing: Float = 1f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { corner.toPx() }
            .coerceAtMost(minOf(size.width, size.height) / 2f)
        if (r <= 0f) return Outline.Rectangle(Rect(Offset.Zero, size))
        // 超椭圆指数：2 = 正圆，4 = iOS 连续圆角手感
        val n = 2f + 2f * smoothing.coerceIn(0f, 1f)
        val w = size.width
        val h = size.height
        val path = Path()
        var started = false
        // 顺时针四角：右上(-90°→0°) → 右下(0°→90°) → 左下(90°→180°) → 左上(180°→270°)
        fun arc(cx: Float, cy: Float, from: Float) {
            for (i in 0..STEPS) {
                val a = Math.toRadians((from + 90f * i / STEPS).toDouble())
                val c = cos(a).toFloat()
                val s = sin(a).toFloat()
                val x = cx + r * Math.copySign(abs(c).pow(2f / n), c)
                val y = cy + r * Math.copySign(abs(s).pow(2f / n), s)
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }
        }
        arc(w - r, r, -90f)
        arc(w - r, h - r, 0f)
        arc(r, h - r, 90f)
        arc(r, r, 180f)
        path.close()
        return Outline.Generic(path)
    }

    private companion object {
        const val STEPS = 16
    }
}

// 连续圆角便捷工厂（与 RoundedCornerShape(Int) 同签名位置，便于按档位替换）
fun squircle(radius: Dp, smoothing: Float = 1f): Shape = SquircleShape(radius, smoothing)

// 旧名兼容映射（既有屏引用）
val RadiusButton = RadiusControl
val RadiusInput = RadiusControl

// 统一间距：只用 4/8dp 倍数（8/12/16/24/32/40/48）
val SpaceXS = 8.dp
val SpaceS = 16.dp
val SpaceM = 24.dp
val SpaceL = 32.dp
val SpaceXL = 40.dp
val SpaceXXL = 48.dp

// 页面左右统一边距
val PagePadding = 20.dp

/*
 * 动效纪律（open-design craft/animation-discipline.md）：
 * 100ms 按压即时反馈 / 150ms 状态确认 / 250ms 进场 / 400ms 跨屏
 * 缩放类用手 spring，颜色透明度用 tween 曲线
 */
const val DurPress = 100
const val DurState = 150
const val DurEnter = 250
const val DurScreen = 400
