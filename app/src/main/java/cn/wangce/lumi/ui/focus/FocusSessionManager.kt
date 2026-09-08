package cn.wangce.lumi.ui.focus

import cn.wangce.lumi.data.local.FocusDao
import cn.wangce.lumi.data.local.FocusRecordEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 专注会话全局状态：提升到单例后，页面 VM 与通知栏遥控共享同一次计时（App 进程内存活）
data class FocusSessionState(
    val running: Boolean = false,
    val paused: Boolean = false,
    val startedAt: Long = 0L, // 正计时基准（墙钟毫秒；恢复时平移补偿暂停段）
    val pausedAt: Long = 0L,  // 暂停时刻（0 = 未暂停）
    val category: String = FocusViewModel.CATEGORIES.first(),
)

// 专注会话管理器：start/pause/resume/finish/abandon 的唯一事实来源，落库与通知更新都在这里
@Singleton
class FocusSessionManager @Inject constructor(
    private val dao: FocusDao,
    private val notifier: FocusNotifier,
) {
    private val _state = MutableStateFlow(FocusSessionState())
    val state: StateFlow<FocusSessionState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun setCategory(label: String) {
        _state.value = _state.value.copy(category = label)
    }

    fun start() {
        val s = _state.value
        if (s.running) return
        _state.value = s.copy(
            running = true,
            paused = false,
            startedAt = System.currentTimeMillis(),
            pausedAt = 0L,
        )
        notifier.show(_state.value, elapsedMillis())
    }

    // 暂停：记住暂停时刻，计时时长冻结（通知 Chronometer 同步冻结显示）
    fun pause() {
        val s = _state.value
        if (!s.running || s.paused) return
        _state.value = s.copy(paused = true, pausedAt = System.currentTimeMillis())
        notifier.show(_state.value, elapsedMillis())
    }

    // 继续：把开始时刻平移掉暂停段，总时长无缝接续
    fun resume() {
        val s = _state.value
        if (!s.running || !s.paused) return
        val now = System.currentTimeMillis()
        _state.value = s.copy(
            startedAt = s.startedAt + (now - s.pausedAt),
            paused = false,
            pausedAt = 0L,
        )
        notifier.show(_state.value, elapsedMillis())
    }

    // 暂停/继续一键切换（通知栏单按钮用）
    fun toggle() {
        if (_state.value.paused) resume() else pause()
    }

    // Finish：落一块石头（少于 10 秒丢弃视为误触，不保存）
    fun finish() {
        val s = _state.value
        if (!s.running) return
        val seconds = elapsedSeconds()
        _state.value = s.copy(running = false, paused = false, startedAt = 0L, pausedAt = 0L)
        notifier.cancel()
        if (seconds < 10) return
        scope.launch {
            dao.insert(
                FocusRecordEntity(
                    label = s.category,
                    seconds = seconds,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    // 放弃：不保存
    fun abandon() {
        if (!_state.value.running) return
        _state.value = _state.value.copy(running = false, paused = false, startedAt = 0L, pausedAt = 0L)
        notifier.cancel()
    }

    // 当前累计毫秒（暂停时冻结在 pausedAt，不继续走）
    fun elapsedMillis(): Long {
        val s = _state.value
        if (!s.running || s.startedAt == 0L) return 0L
        val end = if (s.paused) s.pausedAt else System.currentTimeMillis()
        return (end - s.startedAt).coerceAtLeast(0L)
    }

    fun elapsedSeconds(): Int = (elapsedMillis() / 1000L).toInt()
}
