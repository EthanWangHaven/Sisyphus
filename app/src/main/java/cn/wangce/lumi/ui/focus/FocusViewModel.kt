package cn.wangce.lumi.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wangce.lumi.data.local.FocusCategoriesDao
import cn.wangce.lumi.data.local.FocusCategoryEntity
import cn.wangce.lumi.data.local.FocusDao
import cn.wangce.lumi.data.local.FocusRecordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 专注统计（数据全部从记录聚合，黑白风 UI 直接消费）
data class FocusStats(
    val rocks: Int = 0,          // 累计专注次数（石头数）
    val totalMinutes: Int = 0,   // 累计分钟
    val streak: Int = 0,         // 连续专注天数（含今天或最近一天）
    val monthSeconds: Map<LocalDate, Int> = emptyMap(), // 当月每日秒数（月历点阵）
    val topLabels: List<Pair<String, Int>> = emptyList(), // Top 分类（分类名 → 总分钟）
    val labelCounts: Map<String, Int> = emptyMap(),       // 分类 → 记录条数（小票打印用）
    val labelStarts: Map<String, String> = emptyMap(),    // 分类 → 当天最近一次开始时间 HH:mm（小票打印用）
)

// 专注页 VM：计时会话提升到全局 FocusSessionManager（页面与通知栏遥控共享同一次计时），本 VM 只做投影 + 统计聚合
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val dao: FocusDao,
    private val categoriesDao: FocusCategoriesDao,
    private val manager: FocusSessionManager,
) : ViewModel() {

    companion object {
        val CATEGORIES = listOf("工作", "阅读", "学习", "运动", "冥想")
        // 固定功能项：选中即开始计时并跳转独立互动页（计时/落库/通知与其他项目完全一致）
        val FIXED_FUNCS = listOf("撸宠", "吸烟")
    }

    // 用户自定义专注项目（持久化，下次可直接选择）
    val customCategories: StateFlow<List<FocusCategoryEntity>> = categoriesDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(name: String, iconKey: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoriesDao.insert(FocusCategoryEntity(name = name.trim(), iconKey = iconKey, createdAt = System.currentTimeMillis()))
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch { categoriesDao.deleteById(id) }
    }

    val running = manager.state
        .map { it.running }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val category = manager.state
        .map { it.category }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CATEGORIES.first())

    // 全部记录 → 统计
    val stats: StateFlow<FocusStats> = dao.observeAll()
        .map { records -> aggregate(records) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusStats())

    fun setCategory(label: String) = manager.setCategory(label)

    fun start() = manager.start()

    fun pause() = manager.pause()

    fun resume() = manager.resume()

    fun finish() = manager.finish()

    fun abandon() = manager.abandon()

    fun elapsedSeconds(): Int = manager.elapsedSeconds()

    private fun aggregate(records: List<FocusRecordEntity>): FocusStats {
        if (records.isEmpty()) return FocusStats()
        val zone = ZoneId.systemDefault()
        val totalSeconds = records.sumOf { it.seconds }
        // 连续天数：从今天（或最近有记录的昨天）往回数
        val days = records.map { it.createdAt }.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()
        var streak = 0
        var cursor = LocalDate.now(zone)
        if (cursor !in days) cursor = cursor.minusDays(1)
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        // 当月每日秒数
        val monthStart = LocalDate.now(zone).withDayOfMonth(1)
        val monthSeconds = records
            .filter { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() >= monthStart }
            .groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
            .mapValues { (_, list) -> list.sumOf { it.seconds } }
        // Top 分类
        val topLabels = records
            .groupBy { it.label }
            .map { (label, list) -> label to list.sumOf { it.seconds } / 60 }
            .sortedByDescending { it.second }
            .take(5)
        // 分类 → 记录条数
        val labelCounts = records.groupBy { it.label }.mapValues { (_, list) -> list.size }
        // 分类 → 当天最近一次开始时间（小票明细行）
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        val labelStarts = records
            .filter { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() == LocalDate.now(zone) }
            .groupBy { it.label }
            .mapValues { (_, list) -> Instant.ofEpochMilli(list.maxOf { it.createdAt }).atZone(zone).format(timeFmt) }
        return FocusStats(
            rocks = records.size,
            totalMinutes = totalSeconds / 60,
            streak = streak,
            monthSeconds = monthSeconds,
            topLabels = topLabels,
            labelCounts = labelCounts,
            labelStarts = labelStarts,
        )
    }
}
