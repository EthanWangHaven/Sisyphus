package cn.wangce.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.ui.theme.DarkSheetBg
import cn.wangce.lumi.ui.theme.LightSheetBg
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.ScrimOverlay

/*
 * 统一底部弹层（界面一致性收敛）：全 App 的 ModalBottomSheet 只走 LumiSheet。
 *
 * 统一项（此前 4 处手写，圆角 / 底色 / dragHandle / 标题档 / 底部内边距各不相同，
 * 且 TodoWidgets 漏了 skipPartiallyExpanded 导致半展开裁掉按钮）：
 * - 强制 skipPartiallyExpanded = true（弹层纪律，避免半展开态裁内容）
 * - 顶部圆角 28dp + 玻璃面板（窗口背后真模糊 + 近乎不透明的 SheetBg + 镜面描边）
 * - 统一自绘 dragHandle（36x4，onSurface@0.22）
 * - 统一标题档 titleLarge + SemiBold，水平内边距 20dp
 * - 统一底部内边距 24dp
 *
 * ModalBottomSheet 是独立 Window，无法被兄弟节点录制模糊，
 * 故用系统跨窗口模糊（LumiWindowBackdropBlur）对弹层背后的页面做真高斯模糊。
 */

/** 底部弹层背后的真模糊半径（px）。 */
private const val SheetBackdropBlurPx = 20f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    scrimColor: Color = ScrimOverlay,
    // 空则用统一 SheetBg；Moment 发布这类深色沉浸弹层可覆写为纯黑
    containerColor: Color? = null,
    // 内容超长时可滚动（弹层底部自动补 navigationBarsPadding）
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = LocalDarkTheme.current
    val bg = containerColor ?: if (dark) DarkSheetBg else LightSheetBg
    // 抓手与标题色跟随弹层底色明暗，避免深色弹层上出现黑抓手
    val onSheet = if (bg.luminance() < 0.5f) Color.White else MaterialTheme.colorScheme.onSurface
    // 玻璃面板：底色留一点透明度让背后模糊透上来；深色沉浸弹层（containerColor 覆写）保持实底
    val panelColor = if (containerColor != null) bg else bg.copy(alpha = 0.88f)
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    LumiWindowBackdropBlur(SheetBackdropBlurPx)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = panelColor,
        scrimColor = scrimColor,
        dragHandle = { SheetDragHandle(onSheet) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = onSheet,
                )
                Box(Modifier.height(16.dp))
            }
            content()
        }
    }
}

// 统一抓手：36x4 圆角条，颜色跟随弹层底色（onSheet@0.22）
@Composable
private fun SheetDragHandle(tint: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(tint.copy(alpha = 0.22f)),
        )
    }
}

// 弹层内的选项行容器（图标 + 文字 + 选中态），供各处 sheet 内容复用
@Composable
fun SheetContentColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}
