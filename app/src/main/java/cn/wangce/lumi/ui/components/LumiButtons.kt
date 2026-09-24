package cn.wangce.lumi.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.ui.theme.AccentInk
import cn.wangce.lumi.ui.theme.AccentPaper
import cn.wangce.lumi.ui.theme.DurState
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.RadiusControl
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight

/*
 * 统一按钮族（界面美化方案 2.1/2.3③）：
 * - PrimaryPillButton：墨黑填充胶囊 + 白字（全宽款 h52 保存/发布，内联款 h44，focus 子页 h46）
 * - SecondaryButton：白底 hairline 描边 + 墨字胶囊（设置/编辑页次级操作）
 * - DangerButton / DangerTextButton：error 语义（@10% 底填充 / 纯文字），破坏性操作专用
 * - AppFab：56dp 墨黑圆钮 + 中性投影
 * - GlassIconButton：44dp 玻璃圆钮（返回/关闭等次级图标操作）
 * 按压一律 scaleOnPress 0.97、无 ripple；破坏性操作不振动（iOS 惯例），触感由调用方决定。
 */

// 主操作胶囊钮：墨黑填充胶囊 + 白字（黑即强调，不随分屏强调色漂移；深色反转为纸白底墨字）；禁用底 @40%
// loading = true 时以 16dp 圈代替文字并禁点（保存/发布进行中）
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
    height: Dp = 44.dp,
    loading: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val dark = LocalDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    val bg = if (dark) AccentPaper else AccentInk
    val fg = if (dark) AccentInk else Color.White
    Box(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = height)
            .scaleOnPress(interaction, pressedScale = if (enabled && !loading) 0.96f else 1f)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick,
                role = Role.Button,
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = fg,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    leading?.invoke()
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        color = fg,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// 次级胶囊钮：白底（深色炭容器）+ hairline 1dp + 墨字
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 44.dp,
) {
    val dark = LocalDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .heightIn(min = height)
            .scaleOnPress(interaction, pressedScale = if (enabled) 0.96f else 1f)
            .clip(shape)
            .background(if (dark) MaterialTheme.colorScheme.primaryContainer else Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                role = Role.Button,
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

// 危险填充钮：error @10% 底 + error 字（按压加深 @16%，禁用 @4%）
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 44.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bgAlpha = when {
        !enabled -> 0.04f
        pressed -> 0.16f
        else -> 0.10f
    }
    val bgAlphaAnim by animateFloatAsState(bgAlpha, tween(DurState), label = "dangerBgAlpha")
    Box(
        modifier = modifier
            .heightIn(min = height)
            .scaleOnPress(interaction, pressedScale = if (enabled) 0.96f else 1f)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.error.copy(alpha = bgAlphaAnim))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                role = Role.Button,
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

// 危险文字钮（轻确认）：error 字 Medium，无底
@Composable
fun DangerTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .scaleOnPress(interaction, pressedScale = if (enabled) 0.96f else 1f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                role = Role.Button,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error.copy(alpha = if (enabled) 1f else 0.4f),
            maxLines = 1,
        )
    }
}

// 中性文字钮：灰字无底（放弃/跳过等轻量次级操作）
@Composable
fun GhostTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .scaleOnPress(interaction, pressedScale = if (enabled) 0.96f else 1f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                role = Role.Button,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.4f),
            maxLines = 1,
        )
    }
}

// 专注子页互动胶囊钮（Pet/Smoke/Print 三页共用，方案 2.1）：46dp 墨黑胶囊 + leading 18dp 图标，与主按钮同语言
@Composable
fun InteractionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PrimaryPillButton(
        text = label,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        height = 46.dp,
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

// FAB：56dp 墨黑圆钮（黑即强调，与底栏中央 Home 锚点同语言，全 App 不随分屏强调色漂移）+ 中性投影（y8）
// darkSurface = true 时反转为纸白底墨字（黑底页专用，避免墨钮沉进深底）
@Composable
fun AppFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    darkSurface: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = LocalDarkTheme.current || darkSurface
    val interaction = remember { MutableInteractionSource() }
    val bg = if (dark) AccentPaper else AccentInk
    val fg = if (dark) AccentInk else Color.White
    // 深色页 / 深色主题：阴影用 ShadowDark，否则黑投影贴在深底上完全不可见（FAB 会「画上去」不浮起）
    val shadowColor = if (LocalDarkTheme.current || darkSurface) ShadowDark else ShadowLight
    Box(
        modifier = modifier
            .size(size)
            .scaleOnPress(interaction)
            .shadow(10.dp, CircleShape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            content()
        }
    }
}

// 玻璃图标圆钮：44dp 圆形 GlassCard（hairline 描边）+ onSurface 图标
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .scaleOnPress(interaction, pressedScale = if (enabled) 0.96f else 1f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlassCard(
            cornerRadius = (size.value / 2).toInt(),
            elevation = 0,
            modifier = Modifier.size(size),
        ) {
            Box(
                Modifier
                    .size(size)
                    .alpha(if (enabled) 1f else 0.4f)
                    .then(
                        if (contentDescription != null) {
                            Modifier.semantics { this.contentDescription = contentDescription }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
