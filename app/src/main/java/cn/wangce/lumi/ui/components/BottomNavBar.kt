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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.R
import cn.wangce.lumi.navigation.Routes
import cn.wangce.lumi.ui.theme.DurState
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
// 左右两个半圆弧磨砂玻璃段（backdrop 高斯模糊 + 半透明底 + 细描边）+ 中间独立黑色实底圆钮
// 选中态 = tertiary 墨色图标 + 小圆点；按压 0.98 spring 反馈
@Composable
fun BottomNavBar(
    currentRoute: String,
    contentLayer: GraphicsLayer? = null,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    onHomeClick: () -> Unit,
) {
    val dark = LocalDarkTheme.current
    val arcShape = RoundedCornerShape(28.dp) // 高 56dp → 28dp 即半圆弧端
    val arcShadow = if (dark) ShadowDark else ShadowLight
    // 磨砂玻璃：backdrop 模糊层 + 低透明度底 + 细描边
    val arcBg = if (dark) Color(0x991F1F1F) else Color(0x99FFFFFF)
    val arcBorder = MaterialTheme.colorScheme.outline

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左半圆弧（备忘录/专注）：backdrop 磨砂（API 31+ 录制导航图内容高斯模糊）
        var leftBounds by remember { mutableStateOf(Rect.Zero) }
        var leftSize by remember { mutableStateOf(IntSize.Zero) }
        val leftBlurLayer = rememberGraphicsLayer()
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .onGloballyPositioned {
                    leftBounds = it.boundsInRoot()
                    leftSize = it.size
                }
                .shadow(6.dp, arcShape, ambientColor = arcShadow, spotColor = arcShadow)
                .clip(arcShape)
                .drawBehind {
                    val content = contentLayer ?: return@drawBehind
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        leftBlurLayer.record(leftSize) {
                            translate(-leftBounds.left, -leftBounds.top) {
                                drawLayer(content)
                            }
                        }
                        leftBlurLayer.renderEffect = BlurEffect(24f, 24f, TileMode.Decal)
                        drawLayer(leftBlurLayer)
                    }
                }
                .background(arcBg)
                .border(1.dp, arcBorder, arcShape),
            contentAlignment = Alignment.Center,
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
                .background(MaterialTheme.colorScheme.tertiary)
                .pressScale(onPress = onHomeClick, pressedScale = 0.96f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = stringResource(R.string.s1a183a),
                tint = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 右半圆弧（瞬间/锻炼）：backdrop 磨砂（API 31+ 录制导航图内容高斯模糊）
        var rightBounds by remember { mutableStateOf(Rect.Zero) }
        var rightSize by remember { mutableStateOf(IntSize.Zero) }
        val rightBlurLayer = rememberGraphicsLayer()
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .onGloballyPositioned {
                    rightBounds = it.boundsInRoot()
                    rightSize = it.size
                }
                .shadow(6.dp, arcShape, ambientColor = arcShadow, spotColor = arcShadow)
                .clip(arcShape)
                .drawBehind {
                    val content = contentLayer ?: return@drawBehind
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        rightBlurLayer.record(rightSize) {
                            translate(-rightBounds.left, -rightBounds.top) {
                                drawLayer(content)
                            }
                        }
                        rightBlurLayer.renderEffect = BlurEffect(24f, 24f, TileMode.Decal)
                        drawLayer(rightBlurLayer)
                    }
                }
                .background(arcBg)
                .border(1.dp, arcBorder, arcShape),
            contentAlignment = Alignment.Center,
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
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = tween(DurState),
        label = "navScale",
    )
    // 选中小圆点用墨色强调色（colorScheme.tertiary）
    val dotColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.tertiary else Color.Transparent,
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
            tint = if (isActive) MaterialTheme.colorScheme.tertiary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
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
