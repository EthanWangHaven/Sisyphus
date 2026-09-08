package cn.wangce.lumi.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.ui.theme.DurState
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight

data class SegmentedOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
)

// 分段选择器：少量选项横向切换（iOS 风格滑动指示器）
// 容器凹陷底（onSurface 6% 深浅自适应），选中段 surface 浮起 + 暖阴影 + 细描边；
// 指示器滑动 tween(DurState)；选中文字 SemiBold/onSurface，未选中 Medium/onSurfaceVariant
@Composable
fun <T> SegmentedControl(
    options: List<SegmentedOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalDarkTheme.current
    val shadowColor = if (dark) ShadowDark else ShadowLight
    val segShape = RoundedCornerShape(percent = 50)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(segShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(4.dp),
    ) {
        val segWidth = maxWidth / options.size
        val index = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        val indicatorX by animateDpAsState(
            targetValue = segWidth * index,
            animationSpec = tween(DurState),
            label = "segmentIndicator",
        )
        // 滑动浮起指示器
        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .width(segWidth)
                .fillMaxHeight()
                .shadow(3.dp, segShape, ambientColor = shadowColor, spotColor = shadowColor)
                .clip(segShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), segShape),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { option ->
                val active = option.value == selected
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(segShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(option.value) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    option.icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = if (active) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
