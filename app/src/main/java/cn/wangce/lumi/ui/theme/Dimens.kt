package cn.wangce.lumi.ui.theme

import androidx.compose.ui.unit.dp

/*
 * 圆角体系（Shape Consistency Lock，全 App 唯一一族）：
 * 24 主卡/仪表盘 → 16 标准卡 → 12 按钮与输入 → 8 小元素
 */
val RadiusHero = 24.dp
val RadiusCard = 16.dp
val RadiusControl = 12.dp
val RadiusSmall = 8.dp

// 旧名兼容映射（既有屏引用）
val RadiusButton = RadiusControl
val RadiusInput = RadiusControl

// 统一间距：只用 4/8dp 倍数（8/12/16/24/32/40/48）
val SpaceXS = 8.dp
val SpaceS = 16.dp
val SpaceM = 24.dp
val SpaceL = 32.dp
val SpaceXL = 40.dp
val SpaceXXL = 48.dp

// 页面左右统一边距
val PagePadding = 20.dp

/*
 * 动效纪律（open-design craft/animation-discipline.md）：
 * 100ms 按压即时反馈 / 150ms 状态确认 / 250ms 进场 / 400ms 跨屏
 * 缩放类用手 spring，颜色透明度用 tween 曲线
 */
const val DurPress = 100
const val DurState = 150
const val DurEnter = 250
const val DurScreen = 400
