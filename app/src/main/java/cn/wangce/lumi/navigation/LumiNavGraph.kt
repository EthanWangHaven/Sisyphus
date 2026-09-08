package cn.wangce.lumi.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.wangce.lumi.ui.expects.ExpectScreen
import cn.wangce.lumi.ui.focus.FocusScreen
import cn.wangce.lumi.ui.focus.PetScreen
import cn.wangce.lumi.ui.focus.PrintScreen
import cn.wangce.lumi.ui.focus.SmokeScreen
import cn.wangce.lumi.ui.habits.HabitsScreen
import cn.wangce.lumi.ui.moments.MomentDetailScreen
import cn.wangce.lumi.ui.moments.MomentsScreen
import cn.wangce.lumi.ui.music.MusicScreen
import cn.wangce.lumi.ui.notes.NoteEditScreen
import cn.wangce.lumi.ui.notes.NotesScreen
import cn.wangce.lumi.ui.settings.SettingsScreen
import cn.wangce.lumi.ui.tasks.TasksScreen
import cn.wangce.lumi.ui.workout.WorkoutScreen

// 导航：底部五 Tab（待办/备忘录/专注/期待/锻炼）+ 二级页（备忘录编辑/治愈音乐/习惯打卡）
@Composable
fun LumiNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.TASKS,
        modifier = modifier,
        // 一级 Tab 切换默认转场（150/100ms，用户要求整体加速一倍）
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = { fadeIn(animationSpec = tween(150)) },
        popExitTransition = { fadeOut(animationSpec = tween(100)) },
    ) {
        composable(Routes.TASKS) {
            TasksScreen(
                onOpenExpects = { navController.navigate(Routes.EXPECTS_PAGE) },
                onOpenMusic = { navController.navigate(Routes.MUSIC) },
                onOpenHabits = { navController.navigate(Routes.HABITS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.NOTES) {
            NotesScreen(
                onOpenNote = { noteId -> navController.navigate(Routes.noteEdit(noteId)) },
                onNewNote = { navController.navigate(Routes.noteEdit(0L)) },
            )
        }
        composable(Routes.MOMENTS) {
            MomentsScreen(
                onOpenDetail = { momentId, index ->
                    navController.navigate(Routes.momentDetail(momentId, index))
                },
            )
        }
        composable(Routes.FOCUS) {
            FocusScreen(
                onOpenPet = { navController.navigate(Routes.PET) },
                onOpenSmoke = { navController.navigate(Routes.SMOKE) },
                onOpenPrint = { navController.navigate(Routes.PRINT) },
            )
        }
        // 互动页（撸宠/吸烟）：计时在后台 FocusSessionManager 中继续，返回即回到专注页
        composable(
            route = Routes.PET,
            enterTransition = {
                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeIn(animationSpec = tween(150))
            },
            exitTransition = { fadeOut(animationSpec = tween(100)) },
            popEnterTransition = { fadeIn(animationSpec = tween(100)) },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeOut(animationSpec = tween(150))
            },
        ) {
            PetScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.SMOKE,
            enterTransition = {
                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeIn(animationSpec = tween(150))
            },
            exitTransition = { fadeOut(animationSpec = tween(100)) },
            popEnterTransition = { fadeIn(animationSpec = tween(100)) },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeOut(animationSpec = tween(150))
            },
        ) {
            SmokeScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.PRINT,
            enterTransition = {
                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeIn(animationSpec = tween(150))
            },
            exitTransition = { fadeOut(animationSpec = tween(100)) },
            popEnterTransition = { fadeIn(animationSpec = tween(100)) },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeOut(animationSpec = tween(150))
            },
        ) {
            PrintScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.EXPECTS) {
            ExpectScreen()
        }
        // 期待卡片进入页：与打卡页同款自下而上转场，不在 topLevel 故隐藏底部导航栏
        composable(
            route = Routes.EXPECTS_PAGE,
            enterTransition = {
                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeIn(animationSpec = tween(150))
            },
            exitTransition = { fadeOut(animationSpec = tween(100)) },
            popEnterTransition = { fadeIn(animationSpec = tween(100)) },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeOut(animationSpec = tween(150))
            },
        ) {
            ExpectScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
        composable(Routes.WORKOUT) {
            WorkoutScreen()
        }
        composable(
            route = Routes.MUSIC,
            enterTransition = {
                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeIn(animationSpec = tween(150))
            },
            exitTransition = { fadeOut(animationSpec = tween(100)) },
            popEnterTransition = { fadeIn(animationSpec = tween(100)) },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeOut(animationSpec = tween(150))
            },
        ) {
            // 整页 UI 缩小 20%：覆盖 LocalDensity（dp/sp 同比缩小、布局自然回流；insets 换算后物理尺寸不变）
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density * 0.8f, base.fontScale),
            ) {
                MusicScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable(
            route = Routes.HABITS,
            enterTransition = {
                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeIn(animationSpec = tween(150))
            },
            exitTransition = { fadeOut(animationSpec = tween(100)) },
            popEnterTransition = { fadeIn(animationSpec = tween(100)) },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeOut(animationSpec = tween(150))
            },
        ) {
            HabitsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.NOTE_EDIT,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
            enterTransition = {
                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeIn(animationSpec = tween(150))
            },
            exitTransition = { fadeOut(animationSpec = tween(100)) },
            popEnterTransition = { fadeIn(animationSpec = tween(100)) },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeOut(animationSpec = tween(150))
            },
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            NoteEditScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.MOMENT_DETAIL,
            arguments = listOf(
                navArgument("momentId") { type = NavType.LongType },
                navArgument("index") {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
            enterTransition = {
                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeIn(animationSpec = tween(150))
            },
            exitTransition = { fadeOut(animationSpec = tween(100)) },
            popEnterTransition = { fadeIn(animationSpec = tween(100)) },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(150)) +
                    fadeOut(animationSpec = tween(150))
            },
        ) { backStackEntry ->
            val momentId = backStackEntry.arguments?.getLong("momentId") ?: 0L
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            MomentDetailScreen(
                momentId = momentId,
                initialIndex = index,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
