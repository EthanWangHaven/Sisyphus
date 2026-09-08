package cn.wangce.lumi.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wangce.lumi.data.local.TodoEntity
import cn.wangce.lumi.data.local.TodosDao
import cn.wangce.lumi.data.settings.ThemeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 待办筛选档位（基础版无截止时间，故无「今日」）
enum class TodoFilter { PENDING, DONE, ALL }

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val todoDao: TodosDao,
    private val themeStore: ThemeStore,
) : ViewModel() {

    private val _filter = MutableStateFlow(TodoFilter.PENDING)
    val filter: StateFlow<TodoFilter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun setQuery(q: String) {
        _query.value = q
    }

    // 首页欢迎词：null = 未自定义（显示默认）
    val greeting: StateFlow<String?> = themeStore.greeting
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // 保存欢迎词；空文本恢复默认
    fun saveGreeting(text: String) {
        viewModelScope.launch { themeStore.setGreeting(text) }
    }

    // 全量列表在内存中做筛选 + 搜索，两种条件可叠加；observeAll 已按 done ASC, updatedAt DESC 排序
    val todos: StateFlow<List<TodoEntity>> = combine(
        todoDao.observeAll(),
        _filter,
        _query,
    ) { list, f, q ->
        list
            .filter { todo ->
                when (f) {
                    TodoFilter.PENDING -> !todo.done
                    TodoFilter.DONE -> todo.done
                    TodoFilter.ALL -> true
                }
            }
            .filter { q.isBlank() || it.title.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 今日概览统计：未完成数 + 已完成数
    val stats: StateFlow<Pair<Int, Int>> = todoDao.observeAll()
        .map { list -> list.count { !it.done } to list.count { it.done } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0 to 0)

    // 实时天气：Open-Meteo 拉取，失败/未获取到为 null（UI 优雅隐藏）
    private val _weather = MutableStateFlow<WeatherData?>(null)
    val weather: StateFlow<WeatherData?> = _weather.asStateFlow()

    private var weatherJob: Job? = null
    private var lastWeatherFetchAt = 0L

    // 10 分钟内存缓存；失败时保留旧值
    fun fetchWeather() {
        val now = System.currentTimeMillis()
        if (weatherJob?.isActive == true) return
        if (_weather.value != null && now - lastWeatherFetchAt < 10 * 60_000L) return
        weatherJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { fetchWeatherFromOpenMeteo() }
            if (result != null) {
                _weather.value = result
                lastWeatherFetchAt = System.currentTimeMillis()
            }
        }
    }

    fun setFilter(filter: TodoFilter) {
        _filter.value = filter
    }

    fun addTodo(title: String) {
        val t = title.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            todoDao.upsert(TodoEntity(title = t, createdAt = now, updatedAt = now))
        }
    }

    fun toggleTodo(todo: TodoEntity) {
        viewModelScope.launch {
            todoDao.setDone(id = todo.id, done = !todo.done)
        }
    }

    fun updateTodo(todo: TodoEntity, title: String) {
        val t = title.trim()
        if (t.isEmpty() || t == todo.title) return
        viewModelScope.launch {
            todoDao.update(todo.copy(title = t, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch {
            todoDao.deleteById(id)
        }
    }
}
