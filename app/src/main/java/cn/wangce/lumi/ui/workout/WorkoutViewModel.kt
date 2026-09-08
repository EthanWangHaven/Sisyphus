package cn.wangce.lumi.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wangce.lumi.data.local.WorkoutEntity
import cn.wangce.lumi.data.local.WorkoutTagEntity
import cn.wangce.lumi.data.local.WorkoutsDao
import cn.wangce.lumi.data.local.WorkoutTagsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 锻炼页：当前月份 + 选中日期两个状态源，月/日记录流随其自动切换
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val dao: WorkoutsDao,
    private val tagDao: WorkoutTagsDao,
) : ViewModel() {

    // 当前展示月份（固定取该月 1 号）
    private val month = MutableStateFlow(LocalDate.now().withDayOfMonth(1))

    // 选中日期（默认今天）
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val currentMonth = month.asStateFlow()
    val selected = selectedDate.asStateFlow()

    // 本月记录（monthWorkouts 同时驱动月历圆点/汇总卡/之最卡）
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthWorkouts = month.flatMapLatest { m ->
        dao.observeByMonth(
            startDay = m.toEpochDay(),
            endDay = m.plusMonths(1).minusDays(1).toEpochDay(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 选中日记录
    @OptIn(ExperimentalCoroutinesApi::class)
    val dayWorkouts = selectedDate.flatMapLatest { d ->
        dao.observeByDay(d.toEpochDay())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 任意日期范围记录流（统计 Tab：周/月/年聚合用，复用按月查询的范围语义）
    fun observeRange(startDay: Long, endDay: Long): Flow<List<WorkoutEntity>> =
        dao.observeByMonth(startDay, endDay)

    // 自定义标签：持久化后下次可直接选择
    val customTags = tagDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            tagDao.insert(WorkoutTagEntity(name = trimmed, createdAt = System.currentTimeMillis()))
        }
    }

    fun renameTag(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { tagDao.rename(id, trimmed) }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch { tagDao.deleteById(id) }
    }

    // 切换月份：选中日跟到目标月（当月选中今天，其他月选中 1 号）
    fun shiftMonth(delta: Long) {
        val target = month.value.plusMonths(delta)
        month.value = target
        val today = LocalDate.now()
        selectedDate.value = if (target.year == today.year && target.monthValue == today.monthValue) {
            today
        } else {
            target.withDayOfMonth(1)
        }
    }

    // 点跨月格：连月份一起切
    fun selectDate(date: LocalDate) {
        if (date.year != month.value.year || date.monthValue != month.value.monthValue) {
            month.value = date.withDayOfMonth(1)
        }
        selectedDate.value = date
    }

    fun addWorkout(
        date: LocalDate,
        name: String,
        iconKey: String,
        colorKey: Long,
        durationMin: Int,
        intensity: Int,
    ) {
        viewModelScope.launch {
            dao.insert(
                WorkoutEntity(
                    epochDay = date.toEpochDay(),
                    name = name,
                    iconKey = iconKey,
                    colorKey = colorKey,
                    durationMin = durationMin,
                    intensity = intensity,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteWorkout(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}
