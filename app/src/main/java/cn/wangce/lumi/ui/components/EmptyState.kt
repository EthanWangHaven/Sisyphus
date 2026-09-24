package cn.wangce.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.ui.theme.AccentInk
import cn.wangce.lumi.ui.theme.AccentPaper
import cn.wangce.lumi.ui.theme.DurEnter
import cn.wangce.lumi.ui.theme.LocalAuxText
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.SpaceL

// 列表空状态：图标圆底 + 标题 + 说明三层级（state-coverage.md 空态规范）
// 图标可选传入，不传时保持轻量两行文案
//
// 留白纪律（iOS 空态）：内容不收在屏幕正中，而是「向上收」——
// 顶部固定留白（SpaceL），块内图标→标题 20、标题→说明 8（近标题、远副标题的
// 反向收束）。调用方给 weight(1f) 时内容自然落在上半区，底部留出更大呼吸区。
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val dark = LocalDarkTheme.current
    val aux = LocalAuxText.current
    // 空态柔和渐入：淡入 + 轻微上浮（只有 transform/opacity，符合动效纪律）
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(DurEnter),
        label = "emptyEnter",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * 12f
            }
            .padding(top = SpaceL),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            // 墨/纸中性圆底 + 同色图标：空态是列表唯一视觉锚点，但同样遵守单强调色纪律
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (dark) AccentPaper.copy(alpha = 0.10f) else AccentInk.copy(alpha = 0.06f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (dark) AccentPaper else AccentInk,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = aux.secondary,
            textAlign = TextAlign.Center,
        )
    }
}
