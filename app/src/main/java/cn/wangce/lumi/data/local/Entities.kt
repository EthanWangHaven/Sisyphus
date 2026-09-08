package cn.wangce.lumi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// 待办（基础版：标题 + 完成状态；优先级/分类/截止时间二期再以迁移方式加入）
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val done: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

// 备忘录（标题 + 正文 + 分类标签；图片在附表 note_images）
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val tag: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

// 备忘录图片附表（无外键约束，删除时由 DAO 手动级联清理文件与记录）
@Entity(tableName = "note_images")
data class NoteImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long = 0,
    val imagePath: String = "",
    val sortOrder: Int = 0,
)

// 心情标签：6 种极简心情 + 简约 emoji + 低饱和马卡龙色（UI 标签底色用）
enum class Mood(val label: String, val emoji: String, val color: Long) {
    CALM("平静", "😌", 0xFFB4D4FF),
    HAPPY("开心", "😊", 0xFFFFD070),
    HEALING("治愈", "🌿", 0xFFB8E6C8),
    TIRED("疲惫", "😮‍💨", 0xFFD4C5E8),
    ANXIOUS("焦虑", "😖", 0xFFFFB4B4),
    FULFILLED("充实", "✨", 0xFFFFD4A8),
}

// 瞬间记录（软删除：deleted=1 保留数据不展示，可撤销；mood 字段保留兼容历史数据，UI 不再展示）
@Entity(tableName = "moments")
data class MomentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String = "",
    val mood: Mood = Mood.CALM,
    val location: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deleted: Boolean = false,
)

// 瞬间图片附表（无外键约束，删除时由 DAO 手动级联清理文件与记录）
@Entity(tableName = "moment_images")
data class MomentImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val momentId: Long = 0,
    val imagePath: String = "",
    val sortOrder: Int = 0,
)

// 专注记录：一次专注 = 一块石头（label 分类名 / seconds 时长秒 / createdAt 完成时间）
@Entity(tableName = "focus_records")
data class FocusRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val seconds: Int = 0,
    val createdAt: Long = 0,
)

// 习惯打卡主表（软删除：deleted=1 不展示，保留历史打卡数据）
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val emoji: String = "💧",
    val color: Long = 0xFFB4D4FF,
    val createdAt: Long = 0,
    val deleted: Boolean = false,
)

// 习惯打卡记录：date 用 ISO 文本（yyyy-MM-dd），(habitId, date) 唯一防重复打卡
@Entity(
    tableName = "habit_checks",
    indices = [androidx.room.Index(value = ["habitId", "date"], unique = true)],
)
data class HabitCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long = 0,
    val date: String = "",
)

// 锻炼记录：一条 = 某天的一次锻炼（epochDay = LocalDate.toEpochDay() 便于按天/按月聚合）
@Entity(tableName = "workouts", indices = [androidx.room.Index(value = ["epochDay"])])
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long = 0,
    val name: String = "",
    val iconKey: String = "",
    val colorKey: Long = 0xFFB4D4FF,
    val durationMin: Int = 0,
    val intensity: Int = 1, // 0 低 / 1 中 / 2 高
    val createdAt: Long = 0,
)

// 期待（倒计时）：targetEpochDay = 目标日 LocalDate.toEpochDay()；category 存 key，展示名本地化
@Entity(tableName = "expects")
data class ExpectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val emoji: String = "🎉",
    val iconKey: String = "", // 线条图标 key（空 = 旧数据，运行时按 emoji 反查）
    val targetEpochDay: Long = 0,
    val category: String = "", // festival / life / work / study
    val createdAt: Long = 0,
)

// 锻炼自定义标签：用户自定义的运动项目名称，持久化后下次可直接选择，长按可重命名/删除
@Entity(tableName = "workout_tags")
data class WorkoutTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val createdAt: Long = 0,
)

// 专注自定义项目：用户自定义的专注分类名称（+ 搭配的线条图标 key），持久化后下次可直接选择
@Entity(tableName = "focus_categories")
data class FocusCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val iconKey: String = "",
    val createdAt: Long = 0,
)
