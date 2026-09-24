package cn.wangce.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.ui.theme.DarkGlassPillBg
import cn.wangce.lumi.ui.theme.GlassPillBg
import cn.wangce.lumi.ui.theme.LocalDarkTheme

/*
 * 悬浮玻璃面（界面美化方案 2.1 GlassSurface）：
 * 底栏 / 迷你播放条等悬浮层的统一材质 = GlassPillBg（60% 玻璃）+ 顶部 hairline。
 * 背景模糊沿用现有手法（调用方对页面内容做 Modifier.blur），此处不引入 RenderEffect，避免大面积模糊的性能开销。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    topHairline: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = LocalDarkTheme.current
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (dark) DarkGlassPillBg else GlassPillBg)
            .drawBehind {
                if (topHairline) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, 0.5f),
                        end = Offset(size.width, 0.5f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            },
    ) {
        content()
    }
}
