package cn.wangce.lumi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 主题模式：跟随系统 / 浅色 / 深色
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// 毛玻璃卡片样式：半透明背景 + 轻描边（透出纸感/炭感渐变底）
data class GlassStyle(
    val background: Color,
    val border: Color,
)

val LightGlassStyle = GlassStyle(background = GlassBg, border = GlassBorder)
val DarkGlassStyle = GlassStyle(background = DarkGlassBg, border = DarkGlassBorder)

val LocalGlassStyle = staticCompositionLocalOf { LightGlassStyle }

// 辅助文字两档灰（iOS 层级纪律：一级辅助 = 副标题，二级辅助 = 占位/时间戳/脚注）
data class AuxTextColors(val primary: Color, val secondary: Color)

val LocalAuxText = staticCompositionLocalOf { AuxTextColors(LightOnSurfaceVariant, LightOnSurfaceVariantSoft) }

// 定制 typography（含字号增益/缩放）：嵌套 MaterialTheme 不传 typography 会回落默认字阶，
// LumiAccentTheme 嵌套时从这里取回全局字阶
val LocalLumiTypography = staticCompositionLocalOf { Typography() }

// 分屏强调色：默认品牌蓝之外的分屏变体（三色体系，见 Color.kt 分屏强调色段）
enum class LumiAccent { Default, Sage, Coral }

// App 内强制深色只作用于 Compose 主题层，不改 Activity uiMode；
// 组件判断深浅色一律用此值（不能用 isSystemInDarkTheme 或 LocalConfiguration）
val LocalDarkTheme = staticCompositionLocalOf { false }

// 强调色使用约定（v2 彩色版）：强调色 = colorScheme.tertiary（品牌蓝），只管"操作与选中"（FAB/主按钮/选中 chip/进度），每屏 ≤3 处；
// 墨色 AccentInk/AccentPaper 保留但语义降为"文字墨色与中央 Home 锚点钮"，不再做彩色强调
private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnSurface,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = BrandBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE3EDFF), // 品牌蓝浅容器（选中底）
    onTertiaryContainer = Color(0xFF0F3D91), // 容器上的深蓝字
    background = LightBg,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = DangerRed,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF141414),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnSurface,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF141414),
    tertiary = BrandBlueDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF1E3A5F), // 深色品牌蓝容器（选中底）
    onTertiaryContainer = Color(0xFFB8D4FF), // 容器上的浅蓝字
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DangerRedDark,
    onError = Color.White,
)

@Composable
fun LumiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontBoost: Int = 0,
    fontScale: Int = 100,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val glass = if (darkTheme) DarkGlassStyle else LightGlassStyle
    val auxText = if (darkTheme) {
        AuxTextColors(DarkOnSurfaceVariant, DarkOnSurfaceVariantSoft)
    } else {
        AuxTextColors(LightOnSurfaceVariant, LightOnSurfaceVariantSoft)
    }
    val typography = lumiTypography(fontBoost, fontScale)

    // 光标/文本选择手柄全局品牌蓝（界面美化方案 2.3④ 输入框规格）
    val selectionColors = TextSelectionColors(
        handleColor = colorScheme.tertiary,
        backgroundColor = colorScheme.tertiary.copy(alpha = 0.25f),
    )

    CompositionLocalProvider(
        LocalGlassStyle provides glass,
        LocalDarkTheme provides darkTheme,
        LocalLumiTypography provides typography,
        LocalAuxText provides auxText,
        LocalTextSelectionColors provides selectionColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

// 分屏强调色包装：屏内所有 colorScheme.tertiary 引用（按钮/FAB/选中态/进度）自动切换为该屏强调色。
// 只包 NavGraph 各 composable 内的屏幕内容；底部导航栏在根层组合，不受影响。
@Composable
fun LumiAccentTheme(
    accent: LumiAccent,
    content: @Composable () -> Unit,
) {
    if (accent == LumiAccent.Default) {
        content()
        return
    }
    val dark = LocalDarkTheme.current
    val scheme = when (accent) {
        LumiAccent.Sage -> MaterialTheme.colorScheme.copy(
            tertiary = if (dark) AccentSageDark else AccentSage,
            tertiaryContainer = if (dark) SageContainerDark else SageContainerLight,
            onTertiaryContainer = if (dark) SageOnContainerDark else SageOnContainerLight,
        )
        LumiAccent.Coral -> MaterialTheme.colorScheme.copy(
            tertiary = if (dark) AccentCoralDark else AccentCoral,
            tertiaryContainer = if (dark) CoralContainerDark else CoralContainerLight,
            onTertiaryContainer = if (dark) CoralOnContainerDark else CoralOnContainerLight,
        )
        LumiAccent.Default -> MaterialTheme.colorScheme
    }
    // 文本选择手柄跟随该屏强调色
    val selectionColors = TextSelectionColors(
        handleColor = scheme.tertiary,
        backgroundColor = scheme.tertiary.copy(alpha = 0.25f),
    )
    CompositionLocalProvider(
        LocalTextSelectionColors provides selectionColors,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = LocalLumiTypography.current,
            content = content,
        )
    }
}
