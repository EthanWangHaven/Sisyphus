package cn.wangce.lumi.ui.music

import androidx.lifecycle.ViewModel
import cn.wangce.lumi.music.MusicPlayerManager
import cn.wangce.lumi.music.MusicUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

// 治愈音乐页：播放控制统一透传全局 MusicPlayerManager
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val manager: MusicPlayerManager,
) : ViewModel() {

    val state: StateFlow<MusicUiState> = manager.state

    fun play(index: Int) = manager.play(index)
    fun togglePlayPause() = manager.togglePlayPause()
    fun next() = manager.next()
    fun prev() = manager.prev()
    fun seekTo(ms: Int) = manager.seekTo(ms)
    fun toggleRepeat() = manager.toggleRepeat()
    fun setSleepTimer(minutes: Int) = manager.setSleepTimer(minutes)
    fun syncPlaylist() = manager.syncPlaylist()
    fun fetchCover(trackUrl: String, input: String, onResult: (Boolean, String) -> Unit) =
        manager.fetchCover(trackUrl, input, onResult)

    // 添加歌曲（对标网页端弹窗）：解析 id 预下载音源 / 提交到 GitHub 仓库
    fun resolveId(
        id: String,
        onPhase: (Int) -> Unit,
        onResult: (Boolean, String, String, String) -> Unit,
    ) = manager.resolveId(id, onPhase, onResult)

    fun submitSong(
        neteaseId: String?,
        fileBytes: ByteArray?,
        fileExt: String?,
        title: String,
        artist: String,
        onPhase: (Int) -> Unit,
        onResult: (Boolean, Int, String) -> Unit,
    ) = manager.submitSong(neteaseId, fileBytes, fileExt, title, artist, onPhase, onResult)

    fun clearResolvedAudio() = manager.clearResolvedAudio()
}
