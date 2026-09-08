package cn.wangce.lumi.navigation

// 路由：底部五 Tab（待办/备忘录/专注/期待/锻炼）+ 二级页（备忘录编辑/治愈音乐/习惯打卡）
object Routes {
    const val TASKS = "tasks"
    const val NOTES = "notes"
    const val FOCUS = "focus"
    const val MOMENTS = "moments"
    const val SETTINGS = "settings"
    const val MUSIC = "music"
    const val HABITS = "habits"
    const val EXPECTS = "expects"
    const val WORKOUT = "workout"
    // 期待卡片进入页：同 ExpectScreen，但不在 topLevel（隐藏底部导航栏+自下而上转场）
    const val EXPECTS_PAGE = "expects_page"
    // 专注 2.0：互动页（撸宠/吸烟）+ 小票打印页
    const val PET = "pet"
    const val SMOKE = "smoke"
    const val PRINT = "print"
    const val NOTE_EDIT = "note_edit?noteId={noteId}"
    // 瞬间详情页（小红书式大图 + 信息面板）：momentId 定位数据，index 为打开时的图片页码
    const val MOMENT_DETAIL = "moment_detail?momentId={momentId}&index={index}"

    fun noteEdit(noteId: Long): String = "note_edit?noteId=$noteId"

    fun momentDetail(momentId: Long, index: Int): String = "moment_detail?momentId=$momentId&index=$index"

    val topLevel = listOf(TASKS, NOTES, FOCUS, EXPECTS, MOMENTS, SETTINGS, WORKOUT)
}
