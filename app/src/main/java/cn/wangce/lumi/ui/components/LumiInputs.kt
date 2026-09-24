package cn.wangce.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.ui.theme.AccentInk
import cn.wangce.lumi.ui.theme.AccentPaper
import cn.wangce.lumi.ui.theme.CategoryColor
import cn.wangce.lumi.ui.theme.DarkInputFocusBg
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.RadiusControl

/*
 * 统一输入与选择控件（界面美化方案 2.1/2.3④⑤）：
 * - AppTextField：全 App 唯一输入框。未聚焦 surfaceVariant 底、圆角 12、无边框；
 *   聚焦用墨黑/纸白锚点色 1.5dp 描边 + 白底（深色亮炭）；错误态 error 描边 + caption 提示字；
 *   光标墨色（AccentInk/AccentPaper），不随分屏强调色漂移；
 *   bare = true 时自带底与描边全部让给外层容器（如搜索框的 GlassCard）；
 *   soft = true 时改用扁平软灰底（同「点击选择音频文件」选择框：圆角 12、onSurface 6% 底、
 *   无边框、bodySmall 提示字），聚焦/错误仍保留锚点描边以便定位光标
 * - SelectableChip：主筛选（选中=墨黑填充白字 / 未选中=白底灰字 hairline）与
 *   分类标签（分类浅底+深前景+6dp 圆点，选中加 fg 描边）两种模式；h32、圆角 12、可带计数
 */

// 单行 h48 / 多行 minH 120 的统一输入框（BasicTextField + 自绘容器，无下划线）
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    bare: Boolean = false,
    soft: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val dark = LocalDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(RadiusControl)

    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        // 聚焦描边用墨黑/纸白锚点色，不随分屏强调色漂移
        focused -> if (dark) AccentPaper else AccentInk
        else -> Color.Transparent
    }
    val borderWidth = if (bare) 0.dp else if (isError || focused) 1.5.dp else 0.dp
    val bg = when {
        bare -> Color.Transparent
        // soft：扁平软灰底（对齐「点击选择音频文件」选择框），聚焦不换底只加描边
        soft -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        // 聚焦白底（深色微亮炭），未聚焦 surfaceVariant；禁用整体淡化
        focused && !isError -> if (dark) DarkInputFocusBg else Color.White
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    // soft 用 bodySmall 档（与选择框提示字一致），其余保持 bodyLarge
    val textStyle = (if (soft) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge)
        .copy(color = MaterialTheme.colorScheme.onSurface)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        enabled = enabled,
        textStyle = textStyle,
        cursorBrush = SolidColor(if (dark) AccentPaper else AccentInk),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        minLines = minLines,
        interactionSource = interaction,
        decorationBox = { inner ->
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (singleLine) if (soft) 52.dp else 48.dp else 120.dp)
                        .clip(shape)
                        .background(bg)
                        .border(borderWidth, borderColor, shape)
                        .padding(
                            horizontal = 14.dp,
                            vertical = if (soft) 16.dp else 12.dp,
                        ),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    leading?.invoke()
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && hint.isNotEmpty()) {
                            Text(
                                text = hint,
                                style = if (soft) {
                                    MaterialTheme.typography.bodySmall
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (soft) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                    trailing?.invoke()
                }
                if (isError && supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    )
                }
            }
        },
    )
}

// 可选中的筛选/分类 chip：h32、圆角 12（RadiusControl）、padding h14
// accent == null → 主筛选模式；accent != null → 分类标签模式（分类色浅底 + 深前景字 + 圆点）
@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: CategoryColor? = null,
    count: Int? = null,
) {
    val dark = LocalDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(RadiusControl)

    val bg: Color
    val fg: Color
    var borderColor: Color = Color.Transparent
    if (accent != null) {
        // 分类模式：浅色用分类 bg 底；深色用 fg@22% 浮在深底上（方案 1.1 深色分类色规则）
        bg = if (dark) accent.fg.copy(alpha = 0.22f) else accent.bg
        fg = accent.fg
        if (selected) borderColor = accent.fg
    } else if (selected) {
        // 主筛选选中：墨黑填充白字（黑即强调，不随分屏强调色漂移；深色反转为纸白底墨字）
        bg = if (dark) AccentPaper else AccentInk
        fg = if (dark) AccentInk else Color.White
    } else {
        // 主筛选未选中：白底 + hairline + 灰字（深色炭容器）
        bg = if (dark) MaterialTheme.colorScheme.primaryContainer else Color.White
        fg = MaterialTheme.colorScheme.onSurfaceVariant
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
    }
    val bgAlpha = if (pressed && selected && accent == null) 0.85f else 1f

    Row(
        modifier = modifier
            .scaleOnPress(interaction)
            .clip(shape)
            .background(bg.copy(alpha = bg.alpha * bgAlpha))
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                role = Role.Button,
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (accent != null) {
            // 组5 "• 生活" 式前缀彩色圆点
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.6f),
                maxLines = 1,
            )
        }
    }
}
