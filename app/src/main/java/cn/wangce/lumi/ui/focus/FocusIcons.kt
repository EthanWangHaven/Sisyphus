package cn.wangce.lumi.ui.focus

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector

// 专注自定义项目预设图标：iconKey → 线条图标（弹窗选择 + 分类列表/小票展示共用）
internal val FOCUS_PRESET_ICONS: List<Pair<String, ImageVector>> = listOf(
    "work" to Icons.Outlined.WorkOutline,
    "read" to Icons.Outlined.MenuBook,
    "study" to Icons.Outlined.School,
    "sport" to Icons.Outlined.FitnessCenter,
    "meditate" to Icons.Outlined.SelfImprovement,
    "pet" to Icons.Outlined.Pets,
    "music" to Icons.Outlined.MusicNote,
    "game" to Icons.Outlined.SportsEsports,
    "coffee" to Icons.Outlined.LocalCafe,
    "shop" to Icons.Outlined.ShoppingBag,
    "star" to Icons.Outlined.Star,
    "heart" to Icons.Outlined.Favorite,
)

// 按 key 取图标；未匹配（含旧数据空 key）回落到分类图标
internal fun iconFromKey(key: String): ImageVector =
    FOCUS_PRESET_ICONS.firstOrNull { it.first == key }?.second ?: Icons.Outlined.Category
