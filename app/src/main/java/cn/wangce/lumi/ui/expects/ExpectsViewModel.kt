package cn.wangce.lumi.ui.expects

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wangce.lumi.R
import cn.wangce.lumi.data.local.ExpectEntity
import cn.wangce.lumi.data.local.ExpectsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 期待（倒计时）：全部记录按目标日期升序；首启播种 12 个公历节日（今年已过自动顺延明年）
@HiltViewModel
class ExpectsViewModel @Inject constructor(
    private val dao: ExpectsDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val expects: StateFlow<List<ExpectEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (dao.count() == 0) dao.insertAll(seedFestivals())
        }
    }

    fun upsert(item: ExpectEntity) {
        viewModelScope.launch {
            if (item.id == 0L) dao.insert(item) else dao.update(item)
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }

    // 公历节日预置：目标日已过则取明年同日
    private fun seedFestivals(): List<ExpectEntity> {
        val today = LocalDate.now().toEpochDay()
        val year = LocalDate.now().year
        val festivals = listOf(
            FestivalSeed(R.string.sx_f01, "🎉", "celebration", 1, 1),
            FestivalSeed(R.string.sx_f02, "💝", "favorite", 2, 14),
            FestivalSeed(R.string.sx_f03, "🌷", "flower", 3, 8),
            FestivalSeed(R.string.sx_f04, "💗", "heart", 5, 20),
            FestivalSeed(R.string.sx_f05, "🎈", "child", 6, 1),
            FestivalSeed(R.string.sx_f06, "🎖️", "medal", 8, 1),
            FestivalSeed(R.string.sx_f07, "🍎", "school", 9, 10),
            FestivalSeed(R.string.sx_f08, "🎊", "flag", 10, 1),
            FestivalSeed(R.string.sx_f09, "🎃", "moon", 10, 31),
            FestivalSeed(R.string.sx_f10, "🎄", "tree", 12, 24),
            FestivalSeed(R.string.sx_f11, "🎁", "gift", 12, 25),
            FestivalSeed(R.string.sx_f12, "🎆", "spark", 12, 31),
        )
        return festivals.map { seed ->
            var date = LocalDate.of(year, seed.month, seed.day)
            if (date.toEpochDay() < today) date = date.plusYears(1)
            ExpectEntity(
                title = context.getString(seed.nameRes),
                emoji = seed.emoji,
                iconKey = seed.iconKey,
                targetEpochDay = date.toEpochDay(),
                category = "festival",
                createdAt = System.currentTimeMillis(),
            )
        }
    }
}

// 节日播种条目：名称资源 + 旧 emoji（兼容字段）+ 线条图标 key + 月/日
private data class FestivalSeed(
    val nameRes: Int,
    val emoji: String,
    val iconKey: String,
    val month: Int,
    val day: Int,
)
