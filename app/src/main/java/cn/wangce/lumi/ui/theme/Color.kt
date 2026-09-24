package cn.wangce.lumi.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 「墨与纸 + 品牌蓝」三层色彩系统（v2 彩色版）
 * 第一层 中性底座：浅灰画布 + 白玻璃卡 + 墨色文字层级 —— 界面骨架
 * 第二层 品牌色：BrandBlue 只管"操作与选中"（FAB/主按钮/选中 chip/底栏选中/进度），每屏 ≤3 处
 * 第三层 分类色：CategoryColors pastel 成对色，只上小面积（图标底块/圆点/打卡钮/图表）
 * 纪律：红绿橙只做语义（删除/完成/提醒），不做装饰；token 按用途命名，不按色相命名
 */

// ============ 第一层：中性底座（浅色：浅灰画布 + 纯白浮卡） ============

// 冷白画布（白卡靠极淡阴影分层）
val LightGradientTop = Color(0xFFFBFCFD)
val LightGradientMid = Color(0xFFF8F9FB)
val LightGradientBottom = Color(0xFFF3F5F7)
val LightBg = LightGradientMid

// 墨色文字
val LightOnSurface = Color(0xFF1A1A1A)
val LightOnSurfaceVariant = Color(0xFF8C8C8C) // 一级辅助灰：副标题/说明
val LightOnSurfaceVariantSoft = Color(0xFFAEAEB2) // 二级辅助灰：占位/时间戳/脚注（更轻）

// 半透明白表面（毛玻璃基底）
val LightSurface = Color(0xE6FFFFFF)
// 浮卡白：非死白（#FFFFFF），留一丝冷灰让白卡与白字底色分离
val LightCardBg = Color(0xFFFCFCFD)
val LightSurfaceVariant = Color(0xFFF2F2F2)

// 弹层容器（不透明：BottomSheet 没有真模糊，半透明底会让页面内容透出形成重叠）
val LightSheetBg = Color(0xFFFFFFFF)

// 墨色主色（次级胶囊等中性对比面）
val LightPrimary = Color(0xFF1A1A1A)
val LightPrimaryContainer = Color(0xFFF0F0F0)
val LightSecondary = Color(0xFF8C8C8C)

// ============ 深色：近黑炭 ============

val DarkGradientTop = Color(0xFF1C1C1C)
val DarkGradientMid = Color(0xFF141414)
val DarkGradientBottom = Color(0xFF0D0D0D)
val DarkBg = DarkGradientMid

val DarkOnSurface = Color(0xFFEDEDED)
val DarkOnSurfaceVariant = Color(0xFF9C9C9C) // 一级辅助灰
val DarkOnSurfaceVariantSoft = Color(0xFF6E6E73) // 二级辅助灰（iOS systemGray 深色档）

val DarkSurface = Color(0xE6202020)
val DarkSurfaceVariant = Color(0xFF2A2A2A)
val DarkSheetBg = Color(0xFF202020)
// 深色主色与浅色同构：反白主色
val DarkPrimary = Color(0xFFEDEDED)
val DarkPrimaryContainer = Color(0xFF2A2A2A)
val DarkSecondary = Color(0xFF9C9C9C)

// ============ 第二层：品牌色（全 App 唯一彩色 accent） ============

val BrandBlue = Color(0xFF2F7BFF) // 主品牌色（浅色模式）
val BrandBlueDark = Color(0xFF0A84FF) // 深色模式品牌色（iOS 深色蓝）
val BrandPressed = Color(0xFF2668E0) // 按压态
val BrandBlueDeep = Color(0xFF1C6FE8) // 小号文字级品牌色（正文对比度达标用）

// 品牌渐变（hero 大卡 / 主胶囊按钮 / 选中日高亮）
// 保留 AccentGradient* 旧名（Workout/Dashboard 引用），值已指向品牌蓝
val BrandGradientStart = Color(0xFF2F7BFF)
val BrandGradientEnd = Color(0xFF1E5FD6)
val BrandGradientStartDark = Color(0xFF2F7BFF)
val BrandGradientEndDark = Color(0xFF1E4FA8)
val AccentGradientStart = BrandGradientStart
val AccentGradientEnd = BrandGradientEnd
val AccentGradientStartDark = BrandGradientStartDark
val AccentGradientEndDark = BrandGradientEndDark

// 输入框聚焦底色（深色微亮炭；浅色用 Color.White）
val DarkInputFocusBg = Color(0xFF303030)

// ============ 第二层补充：分屏强调色（三色体系：蓝 / 鼠尾草绿 / 珊瑚陶土） ============
// 参考「一天一个App UI灵感 · 好看的首页设计」配色板（蓝/珊瑚/鼠尾草绿）：
// 全局默认强调色仍是品牌蓝；习惯/音乐屏用鼠尾草绿，期待/瞬间屏用珊瑚陶土。
// 生效方式：NavGraph 内 LumiAccentTheme 包裹目标屏，屏内 tertiary 引用自动跟随；
// 底部导航栏在根层不受包裹影响，始终保持品牌蓝锚点。

// 鼠尾草绿（Sage）
val AccentSage = Color(0xFF5F8468)
val AccentSageDark = Color(0xFF93B4A0)
val SageContainerLight = Color(0xFFE3EEE6)
val SageOnContainerLight = Color(0xFF274D33)
val SageContainerDark = Color(0xFF27382D)
val SageOnContainerDark = Color(0xFFB9D6C5)

// 珊瑚陶土（Coral）
val AccentCoral = Color(0xFFC4674A)
val AccentCoralDark = Color(0xFFE08A6B)
val CoralContainerLight = Color(0xFFFAE7DF)
val CoralOnContainerLight = Color(0xFF8A3D22)
val CoralContainerDark = Color(0xFF4A2A1E)
val CoralOnContainerDark = Color(0xFFF3C4B1)

// ============ 功能语义（iOS 系统色，只做语义不做装饰） ============

val SuccessGreen = Color(0xFF34C759) // 完成/打卡成功
val WarnOrange = Color(0xFFFF9500) // 到期提醒/临近
val DangerRed = Color(0xFFFF3B30) // 删除/警示（浅色，接 colorScheme.error）
val DangerRedDark = Color(0xFFFF453A) // 删除/警示（深色）

// ============ 第三层：分类 pastel 色板（成对使用：bg 底 + fg 前景） ============

data class CategoryColor(val bg: Color, val fg: Color)

// fg 已按"在 bg 上对比度 ≥3:1"校准（图标可辨）；不要直接替换为亮色原值
val CategoryCoral = CategoryColor(Color(0xFFFFE5E5), Color(0xFFE5484D))
val CategoryOrange = CategoryColor(Color(0xFFFFF0DC), Color(0xFFDD7A00))
val CategoryYellow = CategoryColor(Color(0xFFFFF8DC), Color(0xFFC79000)) // 黄必须重压
val CategoryGreen = CategoryColor(Color(0xFFE3F9E5), Color(0xFF30A46C))
val CategoryTeal = CategoryColor(Color(0xFFD3F9EE), Color(0xFF0CA678))
val CategoryBlue = CategoryColor(Color(0xFFD8EFFF), Color(0xFF1C7ED6))
val CategoryIndigo = CategoryColor(Color(0xFFE5EAFE), Color(0xFF4263EB))
val CategoryPurple = CategoryColor(Color(0xFFEFE6FF), Color(0xFF7048E8))
val CategoryPink = CategoryColor(Color(0xFFFFE3EF), Color(0xFFD6336C))
val CategoryGray = CategoryColor(Color(0xFFEFF1F4), Color(0xFF6B7280))

val CategoryColors = listOf(
    CategoryCoral,
    CategoryOrange,
    CategoryYellow,
    CategoryGreen,
    CategoryTeal,
    CategoryBlue,
    CategoryIndigo,
    CategoryPurple,
    CategoryPink,
    CategoryGray,
)

// ============ 遮罩与悬浮玻璃 ============

// 全屏遮罩统一 60%（收敛原 0x66/0x99 两档混用）
val ScrimOverlay = Color(0x99000000)

// 底栏/迷你播放条悬浮玻璃（72% 卡底的次级档）
val GlassPillBg = Color(0x99FFFFFF)
val DarkGlassPillBg = Color(0x991F1F1F)

// ============ 玻璃材质 ============

// 浅色 72% 白玻璃；深色炭黑半透明，透出灰阶渐变底
val GlassBg = Color(0xB8FFFFFF)
val GlassBorder = Color(0x59FFFFFF)
val DarkGlassBg = Color(0xB31F1F1F)
val DarkGlassBorder = Color(0x2EFFFFFF)
val LightOutline = Color(0x141A1A1A)
val DarkOutline = Color(0x24EDEDED)

// 墨色胶囊（中性主按钮/发布钮）：浅色墨底白字，深色纸白底墨字
val PillBgLight = Color(0xFF1A1A1A)
val PillBgDark = Color(0xFFEDEDED)

// 墨纸强调（文字层级与中央 Home 锚点钮用；彩色 accent 已移交品牌蓝）
val AccentInk = Color(0xFF1A1A1A)
val AccentPaper = Color(0xFFEDEDED)

// 中性灰阶辅助（撤销/次级图标）
val NeutralGray = Color(0xFFA8A8A8)

// 中性阴影（禁暖色阴影）
val ShadowLight = Color(0x1F000000)
val ShadowDark = Color(0x4D000000)

// ============ 液态玻璃（Liquid Glass） ============
// 折射层之上的自身色调，比旧 GlassPillBg 更透（折射本身已提供质感，底色只需压一层薄雾）
val LiquidGlassTintLight = Color(0x59FFFFFF)
val LiquidGlassTintDark = Color(0x59141414)
// 镜面高光描边：左上亮、右下暗的斜向渐变两端
val LiquidGlassRimLight = Color(0xB3FFFFFF)
val LiquidGlassRimDark = Color(0x40FFFFFF)
