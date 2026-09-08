package cn.wangce.lumi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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

// App 内强制深色只作用于 Compose 主题层，不改 Activity uiMode；
// 组件判断深浅色一律用此值（不能用 isSystemInDarkTheme 或 LocalConfiguration）
val LocalDarkTheme = staticCompositionLocalOf { false }

// 强调色使用约定（open-design color.md 强调纪律）：
// 一律取 colorScheme.tertiary（浅色墨黑/深色纸白），每屏可见强调色使用 ≤2 处
private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnSurface,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = AccentInk,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0F0F0),
    onTertiaryContainer = Color(0xFF1A1A1A),
    background = LightBg,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = Color(0xFFB3452E),
    onError = Color(0xFFFFF8F5),
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF141414),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnSurface,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF141414),
    tertiary = AccentPaper,
    onTertiary = Color(0xFF141414),
    tertiaryContainer = Color(0xFF2E2E2E),
    onTertiaryContainer = Color(0xFFEDEDED),
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = Color(0xFFE08B76),
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

    CompositionLocalProvider(
        LocalGlassStyle provides glass,
        LocalDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = lumiTypography(fontBoost, fontScale),
            content = content,
        )
    }
}
