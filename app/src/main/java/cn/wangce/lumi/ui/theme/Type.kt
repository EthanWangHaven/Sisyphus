package cn.wangce.lumi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * 字体体系：
 * - 统一采用系统默认字体（Roboto + 厂商 CJK）：静态字重档最全，真机 ROM 字重调节 100% 生效
 *   （自定义可变字体依赖 FontVariation/fontVariationSettings，部分国产 ROM 不应用该设置，导致真机字重永远偏细）
 * - 字号纪律（用户要求）：全表 1.5 倍后再全局 0.85 倍（用户要求再缩小）——56 大数字 / 48 大标题 / 36 页面标题 / 27 正文标题 / 24 正文 / 21 辅助 / 18 小标签 / 12 元信息
 * - CJK 行高地板（typography.md）：正文 1.7，标题 1.3+；CJK 不做负字距，拉丁大标题收紧
 * - 等宽数字（tnum）：大数字与倒计时档启用 tabular figures，数字跳动不抖版（Apple 式细节）
 * - 字体粗细档（设置页可调）：-200 细 / 0 标准 / +200 粗——TextStyle fontWeight 加档后映射系统静态字重
 * - 字体大小档（设置页可调）：100 较大 / 90 标准 / 80 较小——全部 fontSize/lineHeight 按百分比缩放
 * - 字重梯度（iOS 动态类型纪律，三档收敛，避免「忽粗忽细」）：
 *     大标题 = SemiBold(600) / 正文 = Normal(400) / 辅助小字 = Light(300) / 元信息 = Normal(400)
 *   旧版全表 SemiBold、大标题 ExtraBold 导致只有「粗」与「普通」两种手感，层级扁平。
 */

// TextStyle 字重按档位加档（SemiBold 600+boost 等），clamp 到合法 100-900
private fun boosted(weight: FontWeight, boost: Int): FontWeight =
    FontWeight((weight.weight + boost).coerceIn(100, 900))

// 统一字号表（1.5 倍后再全局 0.85）：
// 56 大数字(displayHuge) / 48 大标题 / 36 页面标题 / 27 正文标题 / 24 正文 / 21 辅助 / 18 小标签 / 12 元信息(caption)
// scale 为字体大小档位百分比（100 较大 / 90 标准 / 80 较小），乘到全部 fontSize/lineHeight
fun lumiTypography(boost: Int, scale: Int = 100): Typography {
    val k = scale / 100f * 0.85f // 三档整体再缩至 85%（用户要求）
    val family = FontFamily.Default
    val H0 = TextStyle( // displayHuge：专注计时/hero 大数字，tnum 防抖版
        fontFamily = family,
        fontWeight = boosted(FontWeight.Bold, boost),
        fontSize = (56 * k).sp,
        lineHeight = (64 * k).sp,
        letterSpacing = (-1.0).sp,
        fontFeatureSettings = "tnum",
    )
    val H1 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.SemiBold, boost),
        fontSize = (48 * k).sp,
        lineHeight = (62 * k).sp, // CJK 1.3 地板
        letterSpacing = (-0.6).sp,
    )
    val H2 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.SemiBold, boost),
        fontSize = (36 * k).sp,
        lineHeight = (48 * k).sp,
        letterSpacing = (-0.3).sp,
    )
    val H3 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.SemiBold, boost),
        fontSize = (27 * k).sp,
        lineHeight = (38 * k).sp,
    )
    val B1 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.Normal, boost),
        fontSize = (24 * k).sp,
        lineHeight = (40 * k).sp, // CJK 正文 1.7
    )
    val NUM = TextStyle( // 中号数字档：倒计时"还有 N 天"等，tnum 防抖版
        fontFamily = family,
        fontWeight = boosted(FontWeight.Bold, boost),
        fontSize = (24 * k).sp,
        lineHeight = (32 * k).sp,
        fontFeatureSettings = "tnum",
    )
    val B2 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.Normal, boost),
        fontSize = (21 * k).sp,
        lineHeight = (34 * k).sp,
    )
    val B3 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.Normal, boost),
        fontSize = (18 * k).sp,
        lineHeight = (28 * k).sp,
    )
    val AUX = TextStyle( // 辅助小字档：空态说明/表单提示，细体 + 略宽松行高（与 B3 同字号，只降字重）
        fontFamily = family,
        fontWeight = boosted(FontWeight.Light, boost),
        fontSize = (18 * k).sp,
        lineHeight = (30 * k).sp,
    )
    val CAP = TextStyle( // caption：元信息/时间戳/小徽标
        fontFamily = family,
        fontWeight = boosted(FontWeight.Normal, boost),
        fontSize = (12 * k).sp,
        lineHeight = (16 * k).sp,
        letterSpacing = 0.2.sp,
    )

    return Typography(
        displayLarge = H1,
        displayMedium = H2,
        displaySmall = H0, // displayHuge 槽位（56 大数字，专注计时与期待倒计时大数字共用）
        headlineLarge = H3,
        headlineMedium = H3,
        headlineSmall = NUM, // 中号数字档（倒计时普通数字）
        titleLarge = H3,
        titleMedium = B1.copy(fontWeight = boosted(FontWeight.SemiBold, boost)),
        titleSmall = B2.copy(fontWeight = boosted(FontWeight.SemiBold, boost)),
        bodyLarge = B1,
        bodyMedium = B2,
        bodySmall = AUX, // 辅助小字（Light）
        labelLarge = B2.copy(fontWeight = boosted(FontWeight.SemiBold, boost), letterSpacing = 0.3.sp),
        labelMedium = B3.copy(fontWeight = boosted(FontWeight.Medium, boost)),
        labelSmall = CAP, // 元信息/时间戳档
    )
}
