package cn.wangce.lumi.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.R
import cn.wangce.lumi.navigation.Routes
import cn.wangce.lumi.ui.theme.AccentInk
import cn.wangce.lumi.ui.theme.DurState
import cn.wangce.lumi.ui.theme.LiquidGlassRimDark
import cn.wangce.lumi.ui.theme.LiquidGlassRimLight
import cn.wangce.lumi.ui.theme.LiquidGlassTintDark
import cn.wangce.lumi.ui.theme.LiquidGlassTintLight
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.ShadowDark
import cn.wangce.lumi.ui.theme.ShadowLight

// 底部导航预留空间（导航条本体 56dp + 悬浮留白 12dp + 系统手势条，避免内容被遮挡）
@Composable
fun bottomNavSpace(): Dp =
    96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

data class NavItem(
    @StringRes val labelRes: Int,
    val route: String,
    val icon: ImageVector,
)

// 底部导航五 Tab（备忘录/专注 | Home | 瞬间/锻炼）：2 + 1 + 2 对称，设置入口只保留首页右上角
private val leftNavItems = listOf(
    NavItem(labelRes = R.string.s7e66eb, route = Routes.NOTES, icon = Icons.Outlined.EditNote),
    NavItem(labelRes = R.string.se217c5, route = Routes.FOCUS, icon = Icons.Outlined.AvTimer),
)

private val rightNavItems = listOf(
    NavItem(labelRes = R.string.s49e233, route = Routes.MOMENTS, icon = Icons.Outlined.PhotoLibrary),
    NavItem(labelRes = R.string.s37b6de, route = Routes.WORKOUT, icon = DumbbellIcon),
)

// 分体式导航（对标黑白极简参考）：
// 左右两个半圆弧液态玻璃段（背板高斯模糊 + 边缘折射 + 镜面描边）+ 中间独立墨黑实底圆钮
// 选中态 = 墨色图标 + 墨色小圆点（黑即强调，全 App 底栏不出现彩色）；按压 0.98 spring 反馈
// 玻璃色调只跟随 App 主题：黑底页（瞬间/详情）不再强制切深色玻璃，
// 否则浅色模式下点进黑底页时底栏会整块变黑（视觉上像底栏消失）
@Composable
fun BottomNavBar(
    currentRoute: String,
    contentLayer: GraphicsLayer? = null,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    onHomeClick: () -> Unit,
) {
    val darkSurface = LocalDarkTheme.current
    val arcShape = RoundedCornerShape(28.dp) // 高 56dp → 28dp 即半圆弧端
    val arcShadow = if (darkSurface) ShadowDark else ShadowLight
    // 液态玻璃：低透明度色调让背板折射透出，高光边取左上亮/右下暗的斜向渐变
    val arcTint = if (darkSurface) LiquidGlassTintDark else LiquidGlassTintLight
    val arcRim = if (darkSurface) LiquidGlassRimDark else LiquidGlassRimLight
    // 选中态墨色：浅色玻璃上用墨黑，深色玻璃上用纸白（黑即强调，不用彩色 accent）
    val activeTint = if (darkSurface) Color(0xFFFFFFFF) else AccentInk
    val idleTint = if (darkSurface) Color(0x8AFFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左半圆弧（备忘录/专注）：液态玻璃（背板模糊 + 边缘折射）
        LiquidGlassSurface(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .shadow(6.dp, arcShape, ambientColor = arcShadow, spotColor = arcShadow),
            backdrop = contentLayer,
            cornerRadius = 28.dp,
            blur = 20f,
            refraction = 8.dp,
            chromatic = 0.04f,
            tint = arcTint,
            rimTint = arcRim,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leftNavItems.forEach { item ->
                    NavIcon(
                        item = item,
                        isActive = currentRoute == item.route,
                        activeTint = activeTint,
                        idleTint = idleTint,
                        onClick = { onNavigate(item.route) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 中间独立黑圆钮：Home
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(6.dp, CircleShape, ambientColor = arcShadow, spotColor = arcShadow)
                .clip(CircleShape)
                .background(if (darkSurface) Color(0xFFF5F5F5) else AccentInk)
                .pressScale(onPress = onHomeClick, pressedScale = 0.96f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = stringResource(R.string.s1a183a),
                tint = if (darkSurface) AccentInk else Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 右半圆弧（瞬间/锻炼）：液态玻璃（背板模糊 + 边缘折射）
        LiquidGlassSurface(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .shadow(6.dp, arcShape, ambientColor = arcShadow, spotColor = arcShadow),
            backdrop = contentLayer,
            cornerRadius = 28.dp,
            blur = 20f,
            refraction = 8.dp,
            chromatic = 0.04f,
            tint = arcTint,
            rimTint = arcRim,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rightNavItems.forEach { item ->
                    NavIcon(
                        item = item,
                        isActive = currentRoute == item.route,
                        activeTint = activeTint,
                        idleTint = idleTint,
                        onClick = { onNavigate(item.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavIcon(
    item: NavItem,
    isActive: Boolean,
    activeTint: Color,
    idleTint: Color,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = tween(DurState),
        label = "navScale",
    )
    // 选中小圆点 = 墨色强调（黑即强调，全 App 底栏不出现彩色）
    val dotColor by animateColorAsState(
        targetValue = if (isActive) activeTint else Color.Transparent,
        animationSpec = tween(DurState),
        label = "navDot",
    )
    // 图标绝对居中（上下左右对称），选中指示小圆点叠加在底部、仅选中时出现
    Box(
        modifier = Modifier
            .size(44.dp)
            .pressScale(onPress = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = stringResource(item.labelRes),
            tint = if (isActive) activeTint else idleTint,
            modifier = Modifier
                .size(24.dp)
                .scale(scale),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(4.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
    }
}
