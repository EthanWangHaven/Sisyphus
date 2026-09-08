package cn.wangce.lumi.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wangce.lumi.data.local.HabitCheckEntity
import cn.wangce.lumi.data.local.HabitEntity
import cn.wangce.lumi.data.local.HabitsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 习惯打卡 VM：习惯/打卡记录数据流 + 新建编辑保存 + 打卡切换 + 软删除
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val dao: HabitsDao,
) : ViewModel() {

    val habits: StateFlow<List<HabitEntity>> = dao.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val checks: StateFlow<List<HabitCheckEntity>> = dao.observeChecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 新建/编辑保存
    fun saveHabit(existing: HabitEntity?, name: String, emoji: String, color: Long) {
        viewModelScope.launch {
            if (existing == null) {
                dao.upsertHabit(
                    HabitEntity(
                        name = name,
                        emoji = emoji,
                        color = color,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            } else {
                dao.upsertHabit(existing.copy(name = name, emoji = emoji, color = color))
            }
        }
    }

    // 打卡切换：当日已打卡则取消，否则记录（唯一索引兜底防重复）
    fun toggleCheck(habitId: Long, date: String) {
        viewModelScope.launch {
            if (dao.getCheck(habitId, date) == null) {
                dao.insertCheck(HabitCheckEntity(habitId = habitId, date = date))
            } else {
                dao.deleteCheck(habitId, date)
            }
        }
    }

    // 软删除习惯（打卡历史保留在库中，不展示）
    fun softDelete(habit: HabitEntity) {
        viewModelScope.launch { dao.softDeleteHabit(habit.id) }
    }
}
