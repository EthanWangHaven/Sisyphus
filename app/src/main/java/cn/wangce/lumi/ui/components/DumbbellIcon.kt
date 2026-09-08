package cn.wangce.lumi.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// 精简哑铃简笔画（描边风格，与 Outlined 系图标同视觉重量）：
// 中间横杆 + 左右内长片 + 左右外短片，2dp 圆头线宽
val DumbbellIcon: ImageVector = ImageVector.Builder(
    name = "Dumbbell",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color(0xFF000000)),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
    ) {
        // 中间横杆
        moveTo(6.5f, 12f)
        horizontalLineToRelative(11f)
        // 左右内长片
        moveTo(6.5f, 7f)
        verticalLineToRelative(10f)
        moveTo(17.5f, 7f)
        verticalLineToRelative(10f)
        // 左右外短片
        moveTo(3f, 9.5f)
        verticalLineToRelative(5f)
        moveTo(21f, 9.5f)
        verticalLineToRelative(5f)
    }
}.build()
