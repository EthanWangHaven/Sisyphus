package cn.wangce.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.LocalGlassStyle
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight

// 浮动白卡（对标黑白极简参考）：
// 浅色 = 纯白实心 + 无描边 + 柔和中性阴影（白卡浮于浅灰画布）
// 深色 = 炭黑玻璃 + 半透明白描边（暗层微弱浮起）
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    elevation: Int = 2,
    content: @Composable () -> Unit,
) {
    val glass = LocalGlassStyle.current
    val dark = LocalDarkTheme.current
    val shape = RoundedCornerShape(cornerRadius.dp)
    val shadowColor = if (dark) ShadowDark else ShadowLight

    Box(
        modifier = modifier
            .shadow(elevation.dp, shape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(shape)
            .background(if (dark) glass.background else Color.White)
            .then(
                if (dark) {
                    // 深色：半透明白描边保留层次
                    Modifier.border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color(0x47FFFFFF), Color(0x0DFFFFFF))),
                        shape,
                    )
                } else {
                    // 浅色：hairline 极淡描边，白卡边缘在浅灰渐变底上更利落
                    Modifier.border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                        shape,
                    )
                },
            ),
    ) {
        content()
    }
}
