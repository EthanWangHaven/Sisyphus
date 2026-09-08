package cn.wangce.lumi

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.wangce.lumi.data.settings.AppLanguage
import cn.wangce.lumi.data.settings.ThemeStore
import cn.wangce.lumi.navigation.LumiNavGraph
import cn.wangce.lumi.navigation.Routes
import cn.wangce.lumi.ui.components.BottomNavBar
import cn.wangce.lumi.ui.theme.DarkGradientBottom
import cn.wangce.lumi.ui.theme.DarkGradientMid
import cn.wangce.lumi.ui.theme.DarkGradientTop
import cn.wangce.lumi.ui.theme.LightGradientBottom
import cn.wangce.lumi.ui.theme.LightGradientMid
import cn.wangce.lumi.ui.theme.LightGradientTop
import cn.wangce.lumi.ui.theme.LumiTheme
import cn.wangce.lumi.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// 单 Activity：读取主题偏好 → LumiTheme → 天空渐变背景 + 导航 + 悬浮底部导航栏
// 语言：attachBaseContext 按偏好包一层 locale，切换语言后 recreate() 生效
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeStore: ThemeStore

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.wrapAppLocale(ThemeStore(newBase).currentLanguage()))
    }

    private fun Context.wrapAppLocale(lang: AppLanguage): Context {
        val tag = lang.tag ?: return this
        val config = Configuration(resources.configuration)
        config.setLocales(LocaleList(java.util.Locale.forLanguageTag(tag)))
        return createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val fontBoost by themeStore.fontBoost.collectAsState(initial = 0)
            val fontScale by themeStore.fontScale.collectAsState(initial = 100)
            LumiTheme(themeMode = themeMode, fontBoost = fontBoost, fontScale = fontScale) {
                val darkTheme = when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
                // 开屏动画：图标缩放淡入 → 整体淡出（用户要求）
                SplashOverlay(darkTheme = darkTheme) {
                    LumiRoot(darkTheme = darkTheme)
                }
            }
        }
    }
}

@Composable
fun LumiRoot(darkTheme: Boolean) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.TASKS
    val showBottomBar = Routes.topLevel.contains(currentRoute)

    // 导航图内容层：底部导航栏用它做 backdrop 磨砂模糊（每帧录制页面绘制）
    val navContentLayer = rememberGraphicsLayer()
    var navGraphSize by remember { mutableStateOf(IntSize.Zero) }

    // 天空渐变全局背景：浅色雾蓝 / 深色深蓝夜空，玻璃卡片透出底色
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (darkTheme) {
                    Brush.verticalGradient(listOf(DarkGradientTop, DarkGradientMid, DarkGradientBottom))
                } else {
                    Brush.verticalGradient(listOf(LightGradientTop, LightGradientMid, LightGradientBottom))
                },
            ),
    ) {
        // 页面导航
        LumiNavGraph(
            navController = navController,
            modifier = Modifier
                .matchParentSize()
                .onGloballyPositioned { navGraphSize = it.size }
                .drawWithContent {
                    navContentLayer.record(navGraphSize) { this@drawWithContent.drawContent() }
                    drawContent()
                },
        )

        // 顶层页面显示悬浮胶囊导航栏（中间凸起新建钮）
        if (showBottomBar) {
            BottomNavBar(
                currentRoute = currentRoute,
                contentLayer = navContentLayer,
                modifier = Modifier.align(Alignment.BottomCenter),
                onNavigate = { route ->
                    when {
                        // 已在该页：不响应
                        currentRoute == route -> Unit
                        // 回首页：栈里可能有二级页（瞬间卡片/设置进入），直接弹回起始页最可靠
                        route == Routes.TASKS -> navController.popBackStack(Routes.TASKS, false)
                        else -> navController.navigate(route) {
                            launchSingleTop = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            restoreState = true
                        }
                    }
                },
                // 中间黑色 Home 钮：任意页面一键回首页
                onHomeClick = { navController.popBackStack(Routes.TASKS, false) },
            )
        }
    }
}

// 开屏动画：居中 App 图标（白圆底 + 西西弗斯石）缩放淡入，随后整体淡出露出主页
@Composable
private fun SplashOverlay(darkTheme: Boolean, content: @Composable () -> Unit) {
    var phase by remember { mutableIntStateOf(0) } // 0=展示 1=淡出中 2=已移除
    val iconScale = remember { Animatable(0.85f) }
    val iconAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { iconScale.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        launch { iconAlpha.animateTo(1f, tween(450)) }
        delay(900)
        phase = 1
        delay(360)
        phase = 2
    }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (phase == 0) 1f else 0f,
        animationSpec = tween(350),
        label = "splashOverlay",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        if (phase < 2) {
            // 与系统闪屏同底色（浅 #F5F5F7 / 深 #1C1C1E），衔接无跳变
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = overlayAlpha }
                    .background(if (darkTheme) Color(0xFF1C1C1E) else Color(0xFFF5F5F7)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .graphicsLayer {
                                scaleX = iconScale.value
                                scaleY = iconScale.value
                                alpha = iconAlpha.value
                            }
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Sisyphus",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.graphicsLayer { alpha = iconAlpha.value },
                    )
                }
            }
        }
    }
}
