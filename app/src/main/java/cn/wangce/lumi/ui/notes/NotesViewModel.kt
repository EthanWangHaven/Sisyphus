package cn.wangce.lumi.ui.notes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wangce.lumi.data.image.ImageStore
import cn.wangce.lumi.data.local.NoteEntity
import cn.wangce.lumi.data.local.NoteImageEntity
import cn.wangce.lumi.data.local.NotesDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteDao: NotesDao,
    val imageStore: ImageStore,
) : ViewModel() {

    // 串行化保存：保证自动保存与退出保存不会以旧 id 并发插入
    private val saveMutex = Mutex()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // 选中的分类标签（空串 = 全部）
    private val _selectedTag = MutableStateFlow("")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    // 搜索防抖 300ms + 标签过滤（数据量小，内存过滤）
    val notes: StateFlow<List<NoteEntity>> = combine(
        _query.debounce(300),
        _selectedTag,
    ) { q, tag -> q to tag }
        .flatMapLatest { (q, tag) ->
            noteDao.observeAll().map { list ->
                val keyword = q.trim()
                list.filter { note ->
                    (tag.isEmpty() || note.tag == tag) &&
                        (keyword.isEmpty() ||
                            note.title.contains(keyword, ignoreCase = true) ||
                            note.content.contains(keyword, ignoreCase = true))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 全部分类标签（去重、非空，按出现顺序）
    val tags: StateFlow<List<String>> = noteDao.observeAll()
        .map { list -> list.mapNotNull { it.tag.takeIf(String::isNotBlank) }.distinct() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 全部图片（UI 侧按 noteId 分组展示）
    val images: StateFlow<Map<Long, List<NoteImageEntity>>> = noteDao.observeImages()
        .map { list -> list.groupBy { it.noteId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setQuery(q: String) {
        _query.value = q
    }

    fun selectTag(tag: String) {
        _selectedTag.value = tag
    }

    // 编辑页标签建议：现有标签去重
    suspend fun existingTags(): List<String> =
        noteDao.observeAll().first().mapNotNull { it.tag.takeIf(String::isNotBlank) }.distinct()

    // 删除笔记：级联清理图片文件与记录
    fun deleteNote(id: Long) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                noteDao.getImagesByNote(id).forEach { imageStore.delete(it.imagePath) }
                noteDao.deleteImagesByNote(id)
                noteDao.deleteById(id)
            }
        }
    }

    suspend fun getNote(id: Long): NoteEntity? = noteDao.getById(id)

    // 编辑页加载既有图片
    suspend fun getImagesByNote(noteId: Long): List<NoteImageEntity> = noteDao.getImagesByNote(noteId)

    /**
     * 新增或更新笔记；非 force 且标题正文均为空时返回 -1（跳过）。
     * Mutex 串行化自动保存与退出保存；NonCancellable 保证离开编辑页后写入仍完成。
     * 返回最终 id，由编辑页回填，后续自动保存据此更新而非重复新增。
     */
    suspend fun saveNote(
        title: String,
        content: String,
        tag: String,
        currentId: Long,
        createdAt: Long = 0,
        force: Boolean = false,
    ): Long = saveMutex.withLock {
        val t = title.trim()
        if (!force && t.isEmpty() && content.isBlank()) return -1
        val now = System.currentTimeMillis()
        val entity = NoteEntity(
            id = currentId,
            title = t,
            content = content,
            tag = tag.trim(),
            createdAt = if (createdAt > 0) createdAt else now,
            updatedAt = now,
        )
        withContext(NonCancellable) { noteDao.upsert(entity) }
    }

    /**
     * 笔记图片保存：删除未保留图片（记录+文件）+ 新图压缩入库（续接 sortOrder）。
     * 与 saveNote 共用 saveMutex 串行化；NonCancellable 保证退出编辑页后仍完成。
     * 返回保存后的图片列表（编辑页回填 keepIds、清空待选新图，防重复插入）。
     */
    suspend fun saveNoteImages(
        noteId: Long,
        keepIds: Set<Long>,
        newUris: List<Uri>,
    ): List<NoteImageEntity> = saveMutex.withLock {
        withContext(NonCancellable) {
            noteDao.getImagesByNote(noteId)
                .filter { it.id !in keepIds }
                .forEach { removed ->
                    imageStore.delete(removed.imagePath)
                    noteDao.deleteImageById(removed.id)
                }
            var order = noteDao.getImagesByNote(noteId).size
            newUris.forEach { uri ->
                val path = imageStore.compress(uri, subdir = "notes")
                path?.let {
                    noteDao.insertImages(
                        listOf(NoteImageEntity(noteId = noteId, imagePath = path, sortOrder = order)),
                    )
                    order++
                }
            }
        noteDao.getImagesByNote(noteId)
        }
    }
}