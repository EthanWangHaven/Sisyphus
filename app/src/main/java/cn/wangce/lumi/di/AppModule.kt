package cn.wangce.lumi.di

import android.content.Context
import androidx.room.Room
import cn.wangce.lumi.data.local.ExpectsDao
import cn.wangce.lumi.data.local.FocusCategoriesDao
import cn.wangce.lumi.data.local.FocusDao
import cn.wangce.lumi.data.local.HabitsDao
import cn.wangce.lumi.data.local.LumiDatabase
import cn.wangce.lumi.data.local.MomentsDao
import cn.wangce.lumi.data.local.NotesDao
import cn.wangce.lumi.data.local.TodosDao
import cn.wangce.lumi.data.local.WorkoutsDao
import cn.wangce.lumi.data.local.WorkoutTagsDao
import cn.wangce.lumi.data.settings.ThemeStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LumiDatabase =
        Room.databaseBuilder(context, LumiDatabase::class.java, "lumi.db")
            .addMigrations(
                LumiDatabase.MIGRATION_1_2,
                LumiDatabase.MIGRATION_2_3,
                LumiDatabase.MIGRATION_3_4,
                LumiDatabase.MIGRATION_4_5,
                LumiDatabase.MIGRATION_5_6,
                LumiDatabase.MIGRATION_6_7,
                LumiDatabase.MIGRATION_7_8,
                LumiDatabase.MIGRATION_8_9,
                LumiDatabase.MIGRATION_9_10,
                LumiDatabase.MIGRATION_10_11,
                LumiDatabase.MIGRATION_11_12,
            )
            .build()

    @Provides
    fun provideTodosDao(db: LumiDatabase): TodosDao = db.todosDao()

    @Provides
    fun provideNotesDao(db: LumiDatabase): NotesDao = db.notesDao()

    @Provides
    fun provideMomentsDao(db: LumiDatabase): MomentsDao = db.momentsDao()

    @Provides
    fun provideHabitsDao(db: LumiDatabase): HabitsDao = db.habitsDao()

    @Provides
    fun provideFocusDao(db: LumiDatabase): FocusDao = db.focusDao()

    @Provides
    fun provideWorkoutsDao(db: LumiDatabase): WorkoutsDao = db.workoutsDao()

    @Provides
    fun provideExpectsDao(db: LumiDatabase): ExpectsDao = db.expectsDao()

    @Provides
    fun provideWorkoutTagsDao(db: LumiDatabase): WorkoutTagsDao = db.workoutTagsDao()

    @Provides
    fun provideFocusCategoriesDao(db: LumiDatabase): FocusCategoriesDao = db.focusCategoriesDao()

    @Provides
    @Singleton
    fun provideThemeStore(@ApplicationContext context: Context): ThemeStore = ThemeStore(context)
}
