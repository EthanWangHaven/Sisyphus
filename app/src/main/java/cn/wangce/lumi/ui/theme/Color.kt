package cn.wangce.lumi.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 「墨与纸」黑白极简色板（Ink & Paper Monochrome）
 * 设计规则：中性色 90%+ / 单一强调色 ≤10%（浅色=墨黑、深色=纸白，每屏强调 ≤2 处）
 * - 背景纯白/浅灰（深色近黑），卡片白玻璃，按钮墨底白字（深色反白）
 * - 马卡龙 Morandi 色仅限数据可视化（习惯色板/锻炼汇总条），不进入界面 chrome
 * - token 按用途命名，不按色相命名
 */

// ============ 浅色：浅灰画布 + 纯白浮卡（对标黑白极简参考） ============

// 冷白画布（对标参考：近白微冷，白卡靠极淡阴影分层，克制高级）
val LightGradientTop = Color(0xFFFBFCFD)
val LightGradientMid = Color(0xFFF8F9FB)
val LightGradientBottom = Color(0xFFF3F5F7)
val LightBg = LightGradientMid

// 墨色文字
val LightOnSurface = Color(0xFF1A1A1A)
val LightOnSurfaceVariant = Color(0xFF8C8C8C)

// 半透明白表面（毛玻璃基底）
val LightSurface = Color(0xE6FFFFFF)
val LightSurfaceVariant = Color(0xFFF2F2F2)

// 弹层容器（不透明：BottomSheet 没有真模糊，半透明底会让页面内容透出形成重叠）
val LightSheetBg = Color(0xFFFFFFFF)

// 墨色主色（胶囊按钮等品牌对比面）
val LightPrimary = Color(0xFF1A1A1A)
val LightPrimaryContainer = Color(0xFFF0F0F0)
val LightSecondary = Color(0xFF8C8C8C)

// ============ 深色：近黑炭 ============

val DarkGradientTop = Color(0xFF1C1C1C)
val DarkGradientMid = Color(0xFF141414)
val DarkGradientBottom = Color(0xFF0D0D0D)
val DarkBg = DarkGradientMid

val DarkOnSurface = Color(0xFFEDEDED)
val DarkOnSurfaceVariant = Color(0xFF9C9C9C)

val DarkSurface = Color(0xE6202020)
val DarkSurfaceVariant = Color(0xFF2A2A2A)
val DarkSheetBg = Color(0xFF202020)
// 深色主色与浅色同构：反白主色
val DarkPrimary = Color(0xFFEDEDED)
val DarkPrimaryContainer = Color(0xFF2A2A2A)
val DarkSecondary = Color(0xFF9C9C9C)

// ============ 强调色（全 App 唯一 accent） ============

// 浅色用墨黑、深色用纸白；其上的文字反色（onTertiary 由 Theme 按模式给出）
val AccentInk = Color(0xFF1A1A1A)
val AccentPaper = Color(0xFFEDEDED)
// 强调色渐变（选中日/主按钮/进度环等大面积使用）
val AccentGradientStart = Color(0xFF363636)
val AccentGradientEnd = Color(0xFF141414)
val AccentGradientStartDark = Color(0xFFF2F2F2)
val AccentGradientEndDark = Color(0xFFD4D4D4)

// ============ 玻璃材质 ============

// 浅色 72% 白玻璃；深色炭黑半透明，透出灰阶渐变底
val GlassBg = Color(0xB8FFFFFF)
val GlassBorder = Color(0x59FFFFFF)
val DarkGlassBg = Color(0xB31F1F1F)
val DarkGlassBorder = Color(0x2EFFFFFF)
val LightOutline = Color(0x141A1A1A)
val DarkOutline = Color(0x24EDEDED)

// 墨色胶囊主按钮：浅色墨底白字，深色纸白底墨字
val PillBgLight = Color(0xFF1A1A1A)
val PillBgDark = Color(0xFFEDEDED)

// 中性灰阶辅助（撤销/次级文字，黑白体系内）
val MorandiPink = Color(0xFFA8A8A8)

// 中性阴影（黑白风格，禁暖色阴影）
val ShadowLight = Color(0x1F000000)
val ShadowDark = Color(0x4D000000)
