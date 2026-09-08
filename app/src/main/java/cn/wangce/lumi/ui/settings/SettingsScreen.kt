package cn.wangce.lumi.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wangce.lumi.R
import cn.wangce.lumi.data.settings.AppLanguage
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.SegmentedControl
import cn.wangce.lumi.ui.components.SegmentedOption
import cn.wangce.lumi.ui.components.bottomNavSpace
import cn.wangce.lumi.ui.theme.ThemeMode

// 设置页：外观/字体粗细/字体大小/语言分段选择器 + 作者入口 + 关于
// 切换语言后 recreate() 重建 Activity，attachBaseContext 用新 locale 重建资源
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val fontBoost by viewModel.fontBoost.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val versionName = rememberVersionName()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.se366cc),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(24.dp))

        // 外观：分段选择器（跟随系统/浅色/深色）
        SectionLabel(text = stringResource(R.string.safcde2))
        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            options = listOf(
                SegmentedOption(ThemeMode.SYSTEM, stringResource(R.string.s71bbc7), Icons.Filled.BrightnessAuto),
                SegmentedOption(ThemeMode.LIGHT, stringResource(R.string.s8755e9), Icons.Filled.LightMode),
                SegmentedOption(ThemeMode.DARK, stringResource(R.string.s18d148), Icons.Filled.DarkMode),
            ),
            selected = themeMode,
            onSelect = viewModel::setThemeMode,
        )

        Spacer(Modifier.height(24.dp))

        // 字体粗细：三档全局调节（细/标准/粗），DataStore 持久化
        SectionLabel(text = stringResource(R.string.sf8a3c1))
        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            options = listOf(
                SegmentedOption(-200, stringResource(R.string.s4d8e2a)), // 细
                SegmentedOption(0, stringResource(R.string.s2b7e9d)),    // 标准
                SegmentedOption(200, stringResource(R.string.s7c1b9f)),  // 粗
            ),
            selected = fontBoost,
            onSelect = viewModel::setFontBoost,
        )

        Spacer(Modifier.height(24.dp))

        // 字体大小：三档全局缩放（较小 80% / 标准 90% / 较大 100%），DataStore 持久化
        SectionLabel(text = stringResource(R.string.s9a4c7e))
        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            options = listOf(
                SegmentedOption(80, stringResource(R.string.s3f8b2d)),  // 较小
                SegmentedOption(90, stringResource(R.string.s2b7e9d)),  // 标准（复用）
                SegmentedOption(100, stringResource(R.string.s5e1d9c)), // 较大
            ),
            selected = fontScale,
            onSelect = viewModel::setFontScale,
        )

        Spacer(Modifier.height(24.dp))

        // 语言：分段选择器；中/英文项按自身语言固定显示
        SectionLabel(text = stringResource(R.string.language))
        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            options = listOf(
                SegmentedOption(AppLanguage.SYSTEM, stringResource(R.string.s71bbc7)),
                SegmentedOption(AppLanguage.ZH, "中文"),
                SegmentedOption(AppLanguage.EN, "English"),
            ),
            selected = language,
            onSelect = {
                viewModel.setLanguage(it)
                activity?.recreate()
            },
        )

        Spacer(Modifier.height(24.dp))

        SectionLabel(text = stringResource(R.string.sx_author))
        Spacer(Modifier.height(8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://ethanwanghaven.github.io/")),
                            )
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sx_author),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal, // 细体：与页面常规文本一致，不加粗
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "https://ethanwanghaven.github.io/",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal, // 细体：与卡片内文本一致，不加粗
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionLabel(text = stringResource(R.string.s81d9f5))
        Spacer(Modifier.height(8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Sisyphus",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal, // 细体：与作者卡片一致，不加粗
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.version_fmt, versionName),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal, // 细体：不加粗
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.sb86dfc),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal, // 细体：不加粗
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(bottomNavSpace()))
    }
}

// 分组标题：轻量灰字，卡片外左对齐
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

// 从 PackageManager 读取 versionName，读不到时兜底
@Composable
private fun rememberVersionName(): String {
    val context = LocalContext.current
    return remember {
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
        }.getOrNull() ?: "1.0.0"
    }
}
