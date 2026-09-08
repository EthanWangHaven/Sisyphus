package cn.wangce.lumi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodosDao {
    // 全部（未完成在前，同状态内按创建日期倒序）
    @Query("SELECT * FROM todos ORDER BY done ASC, createdAt DESC")
    fun observeAll(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE done = :done ORDER BY updatedAt DESC")
    fun observeByDone(done: Boolean): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Long): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE todos SET done = :done, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface NotesDao {
    // 按修改时间倒序
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    // 全部笔记图片（UI 侧按 noteId 分组展示）
    @Query("SELECT * FROM note_images ORDER BY sortOrder ASC")
    fun observeImages(): Flow<List<NoteImageEntity>>

    @Query("SELECT * FROM note_images WHERE noteId = :noteId ORDER BY sortOrder ASC")
    suspend fun getImagesByNote(noteId: Long): List<NoteImageEntity>

    @Insert
    suspend fun insertImages(images: List<NoteImageEntity>)

    @Query("DELETE FROM note_images WHERE id = :id")
    suspend fun deleteImageById(id: Long)

    @Query("DELETE FROM note_images WHERE noteId = :noteId")
    suspend fun deleteImagesByNote(noteId: Long)
}

@Dao
interface MomentsDao {
    // 未删除瞬间，新的在前
    @Query("SELECT * FROM moments WHERE deleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MomentEntity>>

    // 全部图片（UI 侧按 momentId 分组展示）
    @Query("SELECT * FROM moment_images ORDER BY sortOrder ASC")
    fun observeImages(): Flow<List<MomentImageEntity>>

    @Query("SELECT * FROM moments WHERE id = :id")
    suspend fun getById(id: Long): MomentEntity?

    @Query("SELECT * FROM moment_images WHERE momentId = :momentId ORDER BY sortOrder ASC")
    suspend fun getImagesByMoment(momentId: Long): List<MomentImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(moment: MomentEntity): Long

    // 软删除：标记不展示，不物理移除
    @Query("UPDATE moments SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    // 撤销软删除
    @Query("UPDATE moments SET deleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restore(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Insert
    suspend fun insertImages(images: List<MomentImageEntity>)

    @Query("DELETE FROM moment_images WHERE id = :id")
    suspend fun deleteImageById(id: Long)

    @Query("DELETE FROM moment_images WHERE momentId = :momentId")
    suspend fun deleteImagesByMoment(momentId: Long)
}

@Dao
interface FocusDao {
    // 全部专注记录，新的在前（数据量小，统计侧在 VM 聚合）
    @Query("SELECT * FROM focus_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FocusRecordEntity>>

    @Insert
    suspend fun insert(record: FocusRecordEntity): Long

    @Query("DELETE FROM focus_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface HabitsDao {
    // 未删除习惯，按创建顺序（列表顺序稳定）
    @Query("SELECT * FROM habits WHERE deleted = 0 ORDER BY createdAt ASC, id ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    // 全部打卡记录（数据量小，UI 侧按 habitId 聚合算连续天数/月历）
    @Query("SELECT * FROM habit_checks ORDER BY date ASC")
    fun observeChecks(): Flow<List<HabitCheckEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Query("SELECT * FROM habit_checks WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getCheck(habitId: Long, date: String): HabitCheckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity): Long

    // 软删除习惯：保留打卡历史，不展示
    @Query("UPDATE habits SET deleted = 1 WHERE id = :id")
    suspend fun softDeleteHabit(id: Long)

    @Insert
    suspend fun insertCheck(check: HabitCheckEntity)

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCheck(habitId: Long, date: String)
}

@Dao
interface WorkoutsDao {
    // 某月记录（epochDay ∈ [startDay, endDay]），新的在前（数据量小，统计在 VM 聚合）
    @Query("SELECT * FROM workouts WHERE epochDay BETWEEN :startDay AND :endDay ORDER BY epochDay DESC, createdAt DESC")
    fun observeByMonth(startDay: Long, endDay: Long): Flow<List<WorkoutEntity>>

    // 某天记录
    @Query("SELECT * FROM workouts WHERE epochDay = :epochDay ORDER BY createdAt DESC")
    fun observeByDay(epochDay: Long): Flow<List<WorkoutEntity>>

    @Insert
    suspend fun insert(workout: WorkoutEntity): Long

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ExpectsDao {
    @Query("SELECT * FROM expects ORDER BY targetEpochDay ASC, id ASC")
    fun observeAll(): Flow<List<ExpectEntity>>

    @Query("SELECT COUNT(*) FROM expects")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(items: List<ExpectEntity>)

    @Insert
    suspend fun insert(item: ExpectEntity): Long

    @Update
    suspend fun update(item: ExpectEntity)

    @Query("DELETE FROM expects WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface WorkoutTagsDao {
    @Query("SELECT * FROM workout_tags ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<WorkoutTagEntity>>

    @Insert
    suspend fun insert(tag: WorkoutTagEntity): Long

    @Query("UPDATE workout_tags SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM workout_tags WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FocusCategoriesDao {
    @Query("SELECT * FROM focus_categories ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<FocusCategoryEntity>>

    @Insert
    suspend fun insert(category: FocusCategoryEntity): Long

    @Query("DELETE FROM focus_categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
