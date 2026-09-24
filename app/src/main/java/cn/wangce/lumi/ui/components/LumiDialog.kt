package cn.wangce.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.wangce.lumi.ui.theme.DarkSheetBg
import cn.wangce.lumi.ui.theme.LightSheetBg
import cn.wangce.lumi.ui.theme.LiquidGlassRimDark
import cn.wangce.lumi.ui.theme.LiquidGlassRimLight
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.RadiusCard

/*
 * 统一弹层（界面一致性收敛）：全 App 的「卡片式弹窗」只走 LumiDialog。
 *
 * 统一项（此前 16 处弹层分散手写，四种圆角 / 三种底色 / 三种标题档 / 三种内边距）：
 * - 圆角 RadiusCard(16)、玻璃面板（弹窗背后真模糊 + 半透明底 + 镜面描边）、内边距 20dp
 * - 标题档统一 titleMedium + SemiBold；可选右上关闭图标（22dp）
 * - 按钮区统一为 LumiDialogButtons（等宽 1:1，间距 10dp）
 * - 命中区域外点击 / 返回键关闭，均走 onDismissRequest
 */

/**
 * 统一卡片弹窗。
 *
 * @param title 标题文案；null 表示无标题（内容自带头部）
 * @param showClose 是否显示右上关闭图标（有标题时才有意义）
 * @param onDismissRequest 点击遮罩 / 返回键 / 关闭图标时回调
 * @param actions 底部按钮区（建议用 LumiDialogButtons）：null 表示无按钮区
 */
@Composable
fun LumiDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    showClose: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = LocalDarkTheme.current
    val shape = RoundedCornerShape(RadiusCard)
    // 玻璃面板：底色留出透明度，让「窗口背后真模糊」的页面内容透上来
    val bg = (if (dark) DarkSheetBg else LightSheetBg).copy(alpha = 0.82f)
    val rim = if (dark) LiquidGlassRimDark else LiquidGlassRimLight
    val rimInner = if (dark) LiquidGlassRimLight else LiquidGlassRimDark
    // 弹窗是独立 Window，兄弟节点录制拿不到背后内容 → 交给系统做跨窗口模糊
    LumiWindowBackdropBlur(20f)

    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bg)
                // 描边：一圈镜面高光（外亮内暗），强化玻璃厚度
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(rim, rimInner.copy(alpha = 0.18f)),
                    ),
                    shape = shape,
                )
                .padding(20.dp),
        ) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (showClose) {
                        val interaction = remember { MutableInteractionSource() }
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    onClick = onDismissRequest,
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            content()
            if (actions != null) {
                Spacer(Modifier.height(18.dp))
                actions()
            }
        }
    }
}

// 弹层按钮区：等宽两钮（如 删除 / 编辑、取消 / 保存），间距 10dp
@Composable
fun LumiDialogButtons(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}
