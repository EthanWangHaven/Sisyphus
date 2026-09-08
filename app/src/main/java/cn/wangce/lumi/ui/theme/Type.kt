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
 * - 字号纪律（用户要求）：全表 1.5 倍后再全局 0.85 倍（用户要求再缩小）——48 大标题 / 36 页面标题 / 27 正文标题 / 24 正文 / 21 辅助 / 18 小标签
 * - CJK 行高地板（typography.md）：正文 1.7，标题 1.3+；CJK 不做负字距，拉丁大标题收紧
 * - 字体粗细档（设置页可调）：-200 细 / 0 标准 / +200 粗——TextStyle fontWeight 加档后映射系统静态字重
 * - 字体大小档（设置页可调）：100 较大 / 90 标准 / 80 较小——全部 fontSize/lineHeight 按百分比缩放
 */

// TextStyle 字重按档位加档（SemiBold 600+boost 等），clamp 到合法 100-900
private fun boosted(weight: FontWeight, boost: Int): FontWeight =
    FontWeight((weight.weight + boost).coerceIn(100, 900))

// 统一字号表（仅 6 级，1.5 倍后再全局 0.85）：48 大标题 / 36 页面标题 / 27 正文标题 / 24 正文 / 21 辅助 / 18 小标签
// scale 为字体大小档位百分比（100 较大 / 90 标准 / 80 较小），乘到全部 fontSize/lineHeight
fun lumiTypography(boost: Int, scale: Int = 100): Typography {
    val k = scale / 100f * 0.85f // 三档整体再缩至 85%（用户要求）
    val family = FontFamily.Default
    val H1 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.ExtraBold, boost),
        fontSize = (48 * k).sp,
        lineHeight = (62 * k).sp, // CJK 1.3 地板
        letterSpacing = (-0.6).sp,
    )
    val H2 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.ExtraBold, boost),
        fontSize = (36 * k).sp,
        lineHeight = (48 * k).sp,
        letterSpacing = (-0.3).sp,
    )
    val H3 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.ExtraBold, boost),
        fontSize = (27 * k).sp,
        lineHeight = (38 * k).sp,
    )
    val B1 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.SemiBold, boost),
        fontSize = (24 * k).sp,
        lineHeight = (40 * k).sp, // CJK 正文 1.7
    )
    val B2 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.SemiBold, boost),
        fontSize = (21 * k).sp,
        lineHeight = (34 * k).sp,
    )
    val B3 = TextStyle(
        fontFamily = family,
        fontWeight = boosted(FontWeight.SemiBold, boost),
        fontSize = (18 * k).sp,
        lineHeight = (28 * k).sp,
    )

    return Typography(
        displayLarge = H1,
        displayMedium = H2,
        headlineLarge = H3,
        headlineMedium = H3,
        titleLarge = H3,
        titleMedium = B1.copy(fontWeight = boosted(FontWeight.Bold, boost)),
        titleSmall = B2.copy(fontWeight = boosted(FontWeight.Bold, boost)),
        bodyLarge = B1,
        bodyMedium = B2,
        bodySmall = B3,
        labelLarge = B2.copy(fontWeight = boosted(FontWeight.Bold, boost), letterSpacing = 0.3.sp),
        labelMedium = B3.copy(fontWeight = boosted(FontWeight.Bold, boost)),
        labelSmall = B3.copy(fontWeight = boosted(FontWeight.Bold, boost)),
    )
}
