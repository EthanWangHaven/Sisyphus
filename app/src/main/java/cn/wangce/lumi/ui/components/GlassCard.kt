package cn.wangce.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.ui.theme.LightCardBg
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.LocalGlassStyle
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight
import cn.wangce.lumi.ui.theme.squircle

// 浮动白卡（对标黑白极简参考）：
// 浅色 = 近白实心 + hairline 描边 + 双层柔和中性阴影（白卡浮于浅灰画布）
// 深色 = 炭黑玻璃 + 半透明白描边（暗层微弱浮起）
// 圆角 = 连续圆角 squircle（iOS 手感），曲率在直边交界处平滑过渡
// shape 非空时覆盖默认 squircle（如小尺寸圆钮需精确正圆，避免超椭圆看起来像圆角方）
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    elevation: Int = 2,
    shape: Shape? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalGlassStyle.current
    val dark = LocalDarkTheme.current
    val cardShape = shape ?: squircle(cornerRadius.dp)
    val shadowColor = if (dark) ShadowDark else ShadowLight

    Box(
        modifier = modifier
            .shadow(elevation.dp, cardShape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(cardShape)
            .background(if (dark) glass.background else LightCardBg)
            .then(
                if (dark) {
                    // 深色：半透明白描边保留层次
                    Modifier.border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color(0x47FFFFFF), Color(0x0DFFFFFF))),
                        cardShape,
                    )
                } else {
                    // 浅色：hairline 极淡描边，白卡边缘在浅灰渐变底上更利落
                    Modifier.border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                        cardShape,
                    )
                },
            ),
    ) {
        content()
    }
}
