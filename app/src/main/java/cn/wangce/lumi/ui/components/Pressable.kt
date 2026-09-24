package cn.wangce.lumi.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/*
 * 按压态（Taste Skill §4.5 交互态纪律 + 界面美化方案 2.4）：
 * 可点卡片/按钮按压时 spring 缩至 0.96，松手回弹；替代生硬 ripple。
 * 动效只动 transform（animation-discipline.md：仅 transform/opacity）。
 */
fun Modifier.pressScale(
    onPress: () -> Unit,
    pressedScale: Float = 0.96f,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onPress,
        )
}

// 供统一按钮组件使用：绑定外部 interactionSource 的按压缩放（不自带 clickable，
// 便于按钮自定义 enabled/role/语义），规格同 pressScale 0.96
fun Modifier.scaleOnPress(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "scaleOnPress",
    )
    this.scale(scale)
}
