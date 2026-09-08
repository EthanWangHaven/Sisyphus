package cn.wangce.lumi.ui.moments

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wangce.lumi.data.image.ImageStore
import cn.wangce.lumi.data.local.MomentEntity
import cn.wangce.lumi.data.local.MomentImageEntity
import cn.wangce.lumi.data.local.MomentsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 瞬间记录 VM：列表/图片分组数据流 + 发布编辑保存 + 软删除与撤销
@HiltViewModel
class MomentsViewModel @Inject constructor(
    private val dao: MomentsDao,
    val imageStore: ImageStore,
) : ViewModel() {

    val moments: StateFlow<List<MomentEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val images: StateFlow<Map<Long, List<MomentImageEntity>>> = dao.observeImages()
        .map { list -> list.groupBy { it.momentId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // 保存中（新图压缩入库期间禁用发布按钮，避免重复提交）
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    // 最近软删除的条目（撤销恢复用）
    private var lastDeleted: MomentEntity? = null

    // 发布/编辑保存：级联清理未保留图片 + 新图压缩入库（逐张出现，列表 Flow 自动刷新）
    fun saveMoment(
        existing: MomentEntity?,
        content: String,
        location: String,
        keepImageIds: Set<Long>,
        newImageUris: List<Uri>,
        onDone: () -> Unit,
    ) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = if (existing == null) {
                dao.upsert(
                    MomentEntity(
                        content = content,
                        location = location,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                dao.upsert(existing.copy(content = content, location = location, updatedAt = now))
                existing.id
            }
            // 编辑时被移除的图片：删除记录 + 删除文件
            if (existing != null) {
                dao.getImagesByMoment(id)
                    .filter { it.id !in keepImageIds }
                    .forEach { removed ->
                        imageStore.delete(removed.imagePath)
                        dao.deleteImageById(removed.id)
                    }
            }
            // 新图压缩入库，续接已有 sortOrder
            var order = dao.getImagesByMoment(id).size
            newImageUris.forEach { uri ->
                val path = imageStore.compress(uri)
                path?.let {
                    dao.insertImages(
                        listOf(MomentImageEntity(momentId = id, imagePath = path, sortOrder = order)),
                    )
                    order++
                }
            }
            _saving.value = false
            onDone()
        }
    }

    // 软删除：标记不展示，不物理移除
    fun softDelete(moment: MomentEntity) {
        lastDeleted = moment
        viewModelScope.launch { dao.softDelete(moment.id) }
    }

    // 撤销软删除
    fun undoDelete() {
        val moment = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { dao.restore(moment.id) }
    }
}
