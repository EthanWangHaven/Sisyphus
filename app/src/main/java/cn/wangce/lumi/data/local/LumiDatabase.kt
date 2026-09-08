package cn.wangce.lumi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TodoEntity::class,
        NoteEntity::class,
        NoteImageEntity::class,
        MomentEntity::class,
        MomentImageEntity::class,
        HabitEntity::class,
        HabitCheckEntity::class,
        FocusRecordEntity::class,
        WorkoutEntity::class,
        ExpectEntity::class,
        WorkoutTagEntity::class,
        FocusCategoryEntity::class,
    ],
    version = 12,
    exportSchema = false,
)
abstract class LumiDatabase : RoomDatabase() {
    abstract fun todosDao(): TodosDao
    abstract fun notesDao(): NotesDao
    abstract fun momentsDao(): MomentsDao
    abstract fun habitsDao(): HabitsDao
    abstract fun focusDao(): FocusDao
    abstract fun workoutsDao(): WorkoutsDao
    abstract fun expectsDao(): ExpectsDao
    abstract fun workoutTagsDao(): WorkoutTagsDao
    abstract fun focusCategoriesDao(): FocusCategoriesDao

    companion object {
        // v1 → v2：新增瞬间记录两张表（显式 CREATE TABLE，保留存量待办/备忘录数据）
        private const val MOMENTS_SQL = "CREATE TABLE IF NOT EXISTS moments (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "content TEXT NOT NULL, " +
            "mood TEXT NOT NULL, " +
            "createdAt INTEGER NOT NULL, " +
            "updatedAt INTEGER NOT NULL, " +
            "deleted INTEGER NOT NULL)"

        private const val MOMENT_IMAGES_SQL = "CREATE TABLE IF NOT EXISTS moment_images (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "momentId INTEGER NOT NULL, " +
            "imagePath TEXT NOT NULL, " +
            "sortOrder INTEGER NOT NULL)"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(MOMENTS_SQL)
                db.execSQL(MOMENT_IMAGES_SQL)
            }
        }

        // v2 → v3：新增习惯打卡两张表 + (habitId, date) 唯一索引
        private const val HABITS_SQL = "CREATE TABLE IF NOT EXISTS habits (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "name TEXT NOT NULL, " +
            "emoji TEXT NOT NULL, " +
            "color INTEGER NOT NULL, " +
            "createdAt INTEGER NOT NULL, " +
            "deleted INTEGER NOT NULL)"

        private const val HABIT_CHECKS_SQL = "CREATE TABLE IF NOT EXISTS habit_checks (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "habitId INTEGER NOT NULL, " +
            "date TEXT NOT NULL)"

        private const val HABIT_CHECKS_INDEX =
            "CREATE UNIQUE INDEX IF NOT EXISTS index_habit_checks_habitId_date ON habit_checks (habitId, date)"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(HABITS_SQL)
                db.execSQL(HABIT_CHECKS_SQL)
                db.execSQL(HABIT_CHECKS_INDEX)
            }
        }

        // v3 → v4：moments 新增地点列（旧数据默认空串）
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE moments ADD COLUMN location TEXT NOT NULL DEFAULT ''")
            }
        }

        // v4 → v5：新增专注记录表
        private const val FOCUS_RECORDS_SQL = "CREATE TABLE IF NOT EXISTS focus_records (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "label TEXT NOT NULL, " +
            "seconds INTEGER NOT NULL, " +
            "createdAt INTEGER NOT NULL)"

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(FOCUS_RECORDS_SQL)
            }
        }

        // v5 → v6：备忘录新增分类标签列 + 备忘录图片附表
        private const val NOTE_TAG_SQL = "ALTER TABLE notes ADD COLUMN tag TEXT NOT NULL DEFAULT ''"
        private const val NOTE_IMAGES_SQL = "CREATE TABLE IF NOT EXISTS note_images (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "noteId INTEGER NOT NULL, " +
            "imagePath TEXT NOT NULL, " +
            "sortOrder INTEGER NOT NULL)"

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(NOTE_TAG_SQL)
                db.execSQL(NOTE_IMAGES_SQL)
            }
        }

        // v6 → v7：新增锻炼记录表 + epochDay 索引
        private const val WORKOUTS_SQL = "CREATE TABLE IF NOT EXISTS workouts (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "epochDay INTEGER NOT NULL, " +
            "name TEXT NOT NULL, " +
            "iconKey TEXT NOT NULL, " +
            "colorKey INTEGER NOT NULL, " +
            "durationMin INTEGER NOT NULL, " +
            "intensity INTEGER NOT NULL, " +
            "createdAt INTEGER NOT NULL)"

        private const val WORKOUTS_INDEX =
            "CREATE INDEX IF NOT EXISTS index_workouts_epochDay ON workouts (epochDay)"

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(WORKOUTS_SQL)
                db.execSQL(WORKOUTS_INDEX)
            }
        }

        // v7 → v8：新增期待（倒计时）表
        private const val EXPECTS_SQL = "CREATE TABLE IF NOT EXISTS expects (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "title TEXT NOT NULL, " +
            "emoji TEXT NOT NULL, " +
            "targetEpochDay INTEGER NOT NULL, " +
            "category TEXT NOT NULL, " +
            "createdAt INTEGER NOT NULL)"

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(EXPECTS_SQL)
            }
        }

        // v8 → v9：expects 新增线条图标 key 列（旧数据默认空串，运行时按 emoji 反查）
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expects ADD COLUMN iconKey TEXT NOT NULL DEFAULT ''")
            }
        }

        // v9 → v10：新增锻炼自定义标签表
        private const val WORKOUT_TAGS_SQL = "CREATE TABLE IF NOT EXISTS workout_tags (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "name TEXT NOT NULL, " +
            "createdAt INTEGER NOT NULL)"

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(WORKOUT_TAGS_SQL)
            }
        }

        // v10 → v11：新增专注自定义项目表
        private const val FOCUS_CATEGORIES_SQL = "CREATE TABLE IF NOT EXISTS focus_categories (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "name TEXT NOT NULL, " +
            "createdAt INTEGER NOT NULL)"

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(FOCUS_CATEGORIES_SQL)
            }
        }

        // v11 → v12：focus_categories 新增线条图标 key 列（旧数据默认空串）
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_categories ADD COLUMN iconKey TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
