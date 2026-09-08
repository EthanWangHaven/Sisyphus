package cn.wangce.lumi.music

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import cn.wangce.lumi.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 循环模式：列表循环 / 单曲循环
enum class RepeatMode { ALL, ONE }

// 播放器完整状态（UI 与前台服务共用）
data class MusicUiState(
    val tracks: List<Track> = Playlist.tracks,
    val currentIndex: Int = -1,   // -1 = 尚未播放
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false, // 在线音源准备中（点击无立即出声的过渡态）
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val sleepMinutes: Int = 0,    // 定时关闭分钟数，0 = 未设置
    val isSyncing: Boolean = false, // 歌单同步进行中（右上角按钮转圈）
    val coverOverrides: Map<String, String> = emptyMap(), // 自定义封面（key = 曲目 url，value = 本地路径）
)

// 全局播放控制器：持有 MediaPlayer，管理播放/切歌/进度/循环/定时；
// 前台服务（MusicService）只负责保活与通知，播放控制统一收敛在此
@Singleton
class MusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var loadJob: Job? = null
    private var sleepJob: Job? = null

    private val _state = MutableStateFlow(MusicUiState())
    val state: StateFlow<MusicUiState> = _state.asStateFlow()

    init {
        // 进度轮询：播放中每 500ms 同步一次位置；
        // prepare 过渡态调用 isPlaying 会抛 IllegalStateException，全部防护
        scope.launch {
            while (isActive) {
                try {
                    val p = player
                    if (p != null && p.isPlaying) {
                        _state.update { it.copy(positionMs = p.currentPosition) }
                    }
                } catch (_: IllegalStateException) {
                }
                delay(500)
            }
        }
        // 冷启动：恢复上次同步的非内置曲目（网站新增歌曲追加在末尾）
        scope.launch(Dispatchers.IO) {
            val extras = PlaylistSync.loadExtras(context)
            if (extras.isNotEmpty()) {
                _state.update { s -> s.copy(tracks = s.tracks + extras) }
            }
        }
        // 自定义封面映射：用户设置后即时联动（列表 / 迷你条 / 大屏 / 毛玻璃）
        scope.launch {
            TrackCoverStore.flow(context).collect { map ->
                _state.update { it.copy(coverOverrides = map) }
            }
        }
    }

    // 播放指定曲目（越界忽略；重复点击当前曲目且播放器健在则跳过——失败释放后允许同曲重播）
    fun play(index: Int) {
        if (index !in _state.value.tracks.indices) return
        if (index == _state.value.currentIndex && player != null) return
        loadJob?.cancel()
        val track = _state.value.tracks[index]
        _state.update { it.copy(currentIndex = index, isPlaying = false, isBuffering = true, positionMs = 0, durationMs = 0) }
        loadJob = scope.launch(Dispatchers.IO) {
            try {
                val p = player ?: MediaPlayer().also {
                    it.setOnCompletionListener { onCompletion() }
                    // 在线音源解码/网络错误：停止并复位 UI，避免卡在假播放态
                    it.setOnErrorListener { mp, what, extra ->
                        Log.e("MusicPlayer", "media error what=$what extra=$extra")
                        mp.release()
                        player = null
                        if (_state.value.currentIndex == index) {
                            _state.update { s -> s.copy(isPlaying = false, isBuffering = false) }
                            notifyPlayFailed()
                        }
                        true
                    }
                    // 锁屏后台播放：持 partial wake lock 防止 CPU 休眠中断音频
                    it.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                    player = it
                }
                p.reset()
                if (track.url.isNotBlank()) {
                    // 在线音源：直接交给 MediaPlayer 流式播放（内部自动缓冲）
                    p.setDataSource(track.url)
                } else {
                    context.assets.openFd("${Playlist.ASSET_DIR}/${track.fileName}").use { fd ->
                        p.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    }
                }
                // 15s 超时：prepare 放独立线程跑（release() 可强制其立即返回），
                // latch.await 到点未完成即判定失败——协程超时无法中断不响应
                // Thread.interrupt() 的 native 阻塞（不可达音源 SYN 重传可达 2 分钟+）
                val latch = CountDownLatch(1)
                var prepareError: Exception? = null
                Thread {
                    try {
                        p.prepare()
                    } catch (e: Exception) {
                        prepareError = e
                    } finally {
                        latch.countDown()
                    }
                }.start()
                if (!latch.await(15, TimeUnit.SECONDS)) throw java.io.IOException("prepare timeout")
                prepareError?.let { throw it }
                p.start()
                // 仅当仍是当前曲目才更新共享状态（旧 job 被新播放取代时避免覆盖新播放的状态）
                if (_state.value.currentIndex == index) {
                    _state.update {
                        it.copy(
                            isPlaying = true,
                            isBuffering = false,
                            positionMs = 0,
                            durationMs = p.duration,
                        )
                    }
                    startService()
                }
            } catch (e: Exception) {
                Log.e("MusicPlayer", "play failed: ${track.fileName}", e)
                // 仅当仍是当前曲目才复位（旧 job 被新播放取代时不动新播放的共享状态）
                if (_state.value.currentIndex == index) {
                    // 彻底释放：置 null 后 togglePlayPause 会走重播路径，避免对 reset 态 start 崩溃
                    player?.release()
                    player = null
                    _state.update { it.copy(isPlaying = false, isBuffering = false) }
                    notifyPlayFailed()
                }
            }
        }
    }

    // 播放失败统一提示（主线程 Toast）
    private fun notifyPlayFailed() {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, R.string.splay_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun togglePlayPause() {
        val s = _state.value
        if (s.currentIndex < 0) {
            play(0)
            return
        }
        if (s.isBuffering) return  // 缓冲中：player 尚未 prepared，start/pause 均非法
        // 播放器已被失败路径释放：重新走网络加载（重播路径）
        val p = player ?: run {
            play(s.currentIndex)
            return
        }
        try {
            if (p.isPlaying) {
                p.pause()
                _state.update { it.copy(isPlaying = false) }
            } else {
                p.start()
                _state.update { it.copy(isPlaying = true) }
                startService()
            }
        } catch (_: IllegalStateException) {
        }
    }

    // 通知/外部要求的纯暂停（不会反向触发播放）
    fun pause() {
        val p = player ?: return
        try {
            if (p.isPlaying) {
                p.pause()
                _state.update { it.copy(isPlaying = false) }
            }
        } catch (_: IllegalStateException) {
        }
    }

    fun next() {
        val s = _state.value
        play(if (s.currentIndex + 1 >= s.tracks.size) 0 else s.currentIndex + 1)
    }

    fun prev() {
        val s = _state.value
        play(if (s.currentIndex - 1 < 0) s.tracks.lastIndex else s.currentIndex - 1)
    }

    fun seekTo(ms: Int) {
        try {
            player?.seekTo(ms)
        } catch (_: IllegalStateException) {
        }
        _state.update { it.copy(positionMs = ms) }
    }

    fun toggleRepeat() {
        _state.update {
            it.copy(
                repeatMode = if (it.repeatMode == RepeatMode.ALL) RepeatMode.ONE else RepeatMode.ALL,
            )
        }
    }

    // 定时关闭：minutes <= 0 取消；到点自动暂停并复位
    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepJob = null
        if (minutes <= 0) {
            _state.update { it.copy(sleepMinutes = 0) }
            return
        }
        _state.update { it.copy(sleepMinutes = minutes) }
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            pause()
            _state.update { it.copy(sleepMinutes = 0) }
        }
    }

    // 同步个人网站歌单：拉取 data/playlist.json → 按 url 去重追加新曲 → 持久化 → Toast 反馈
    fun syncPlaylist() {
        if (_state.value.isSyncing) return
        _state.update { it.copy(isSyncing = true) }
        scope.launch(Dispatchers.IO) {
            var failed = false
            var newCount = 0
            try {
                val fetched = PlaylistSync.fetch()
                val currentUrls = _state.value.tracks.map { it.url }.toSet()
                val newOnes = fetched.filter { it.url.isNotBlank() && it.url !in currentUrls }
                newCount = newOnes.size
                if (newOnes.isNotEmpty()) {
                    val extras = _state.value.tracks.filter { it.url !in PlaylistSync.builtinUrls() } + newOnes
                    PlaylistSync.saveExtras(context, extras)
                    _state.update { it.copy(tracks = it.tracks + newOnes) }
                }
            } catch (e: Exception) {
                Log.e("MusicPlayer", "sync failed", e)
                failed = true
            }
            // 同步成功后后台自动匹配缺失封面（独立协程，不阻塞同步完成的 Toast 反馈）
            if (!failed) scope.launch(Dispatchers.IO) { autoMatchCovers() }
            _state.update { it.copy(isSyncing = false) }
            withContext(Dispatchers.Main) {
                val text = when {
                    failed -> context.getString(R.string.sync_failed)
                    newCount > 0 -> context.getString(R.string.sync_new_fmt, newCount)
                    else -> context.getString(R.string.sync_uptodate)
                }
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 同步曲目自动匹配封面：无覆盖映射的曲目逐首搜索（歌名+歌手，网易云优先、QQ 音乐兜底），
    // 命中即下载写入映射并即时联动 UI；未命中保留默认灰底（仍可长按大屏封面手动换）
    private suspend fun autoMatchCovers() {
        val existing = TrackCoverStore.flow(context).first()
        val targets = _state.value.tracks.filter {
            it.url.isNotBlank() && it.url !in existing && it.title.isNotBlank()
        }
        for (track in targets) {
            runCatching {
                val path = TrackCoverStore.autoFetchCover(context, track.title, track.artist)
                if (path != null) TrackCoverStore.set(context, track.url, path)
            }.onFailure { Log.e("MusicPlayer", "auto match cover failed: ${track.title}", it) }
            delay(300)
        }
    }

    // 获取封面：按输入自动分流（QQ 音乐链接/songmid → QQ 接口；否则网易云 ID/链接）；
    // 回调在主线程（message 已本地化，成功为空串）
    fun fetchCover(trackUrl: String, input: String, onResult: (Boolean, String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val result = runCatching {
                val path = if (TrackCoverStore.isQQInput(input)) {
                    TrackCoverStore.fetchQQCover(context, input)
                } else {
                    TrackCoverStore.fetchNeteaseCover(context, input)
                }
                TrackCoverStore.set(context, trackUrl, path)
                true to ""
            }.getOrElse { e ->
                Log.e("MusicPlayer", "fetch cover failed", e)
                false to (e.message ?: context.getString(R.string.music_cover_bad_input))
            }
            withContext(Dispatchers.Main) { onResult(result.first, result.second) }
        }
    }

    // ── 添加歌曲（对标网页端「添加音乐」弹窗）──

    // 「解析」缓存：网易云 id → 已下载音源（提交时复用，避免重复下载）
    private data class ResolvedAudio(val songId: String, val ext: String, val bytes: ByteArray)

    @Volatile
    private var resolvedAudio: ResolvedAudio? = null

    // 解析音乐 id：取歌名/歌手并预下载音源；成功后回调（ok, 提示文案, 歌名, 歌手），UI 自动填充
    fun resolveId(
        id: String,
        onPhase: (Int) -> Unit,
        onResult: (Boolean, String, String, String) -> Unit,
    ) {
        scope.launch(Dispatchers.IO) {
            var ok = false
            var msg = ""
            var title = ""
            var artist = ""
            try {
                val info = MusicUploader.resolveNeteaseSong(context, id)
                onPhase(R.string.music_add_phase_downloading)
                val bytes = MusicUploader.downloadAudio(context, info.audioUrl)
                resolvedAudio = ResolvedAudio(info.songId, info.ext, bytes)
                title = info.title
                artist = info.artist
                msg = context.getString(
                    R.string.music_add_resolved_fmt,
                    info.title,
                    info.artist,
                    "%.1f".format(bytes.size / 1024f / 1024f),
                )
                ok = true
            } catch (e: Exception) {
                Log.e("MusicPlayer", "resolve id failed", e)
                msg = e.message ?: context.getString(R.string.music_add_err_generic)
            }
            withContext(Dispatchers.Main) { onResult(ok, msg, title, artist) }
        }
    }

    // 提交新歌：id 模式复用 resolveId 缓存的音源；成功后本地立即追加曲目（无需等部署完成）
    // onPhase 回调阶段文案资源 id（匹配歌词中 / 提交 GitHub 中），onResult(ok, 歌词行数, 失败原因)
    fun submitSong(
        neteaseId: String?,
        fileBytes: ByteArray?,
        fileExt: String?,
        title: String,
        artist: String,
        onPhase: (Int) -> Unit,
        onResult: (Boolean, Int, String) -> Unit,
    ) {
        scope.launch(Dispatchers.IO) {
            var ok = false
            var lyricCount = 0
            var errMsg = ""
            try {
                val audio: ByteArray
                val songId: String
                val ext: String
                if (neteaseId != null) {
                    val cached = MusicUploader.extractSongId(context, neteaseId)
                        .let { want -> resolvedAudio?.takeIf { it.songId == want } }
                    if (cached == null) {
                        throw IllegalStateException(context.getString(R.string.music_add_err_need_resolve))
                    }
                    audio = cached.bytes
                    songId = cached.songId
                    ext = cached.ext
                } else {
                    audio = fileBytes
                        ?: throw IllegalStateException(context.getString(R.string.music_add_err_need_file))
                    songId = java.lang.Long.toString(System.currentTimeMillis(), 36) +
                        (1..4).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
                    ext = fileExt ?: "mp3"
                }

                // 自动匹配歌词：id 模式直接取该 id 的歌词；上传模式按歌名+歌手检索（失败不阻塞提交）
                onPhase(R.string.music_add_phase_lyrics)
                val lyrics = runCatching {
                    if (neteaseId != null) {
                        MusicUploader.fetchLyricsById(context, songId)
                    } else {
                        MusicUploader.searchLyrics(context, title, artist)
                    }
                }.getOrElse { e ->
                    Log.e("MusicPlayer", "lyrics match failed", e)
                    emptyList()
                }
                lyricCount = lyrics.size

                onPhase(R.string.music_add_phase_uploading)
                val track = MusicUploader.addSongToRepo(context, songId, title, artist, audio, ext, lyrics)

                // 成功：本地立即追加曲目并持久化（播放器可直接试听，部署完成后网站端一致）
                val extras = _state.value.tracks.filter { it.url !in PlaylistSync.builtinUrls() } + track
                PlaylistSync.saveExtras(context, extras)
                _state.update { it.copy(tracks = it.tracks + track) }
                scope.launch(Dispatchers.IO) { autoMatchCovers() }
                resolvedAudio = null
                ok = true
            } catch (e: Exception) {
                Log.e("MusicPlayer", "submit song failed", e)
                errMsg = e.message ?: context.getString(R.string.music_add_err_generic)
            }
            withContext(Dispatchers.Main) { onResult(ok, lyricCount, errMsg) }
        }
    }

    // 关闭弹窗时清理「解析」缓存
    fun clearResolvedAudio() {
        resolvedAudio = null
    }

    // 一曲播完：单曲循环原地重播，列表循环进入下一首（末尾回首）
    private fun onCompletion() {
        val s = _state.value
        if (s.repeatMode == RepeatMode.ONE) {
            play(s.currentIndex)
        } else {
            next()
        }
    }

    // 前台服务保活（已在运行时 onStartCommand 只会刷新通知）
    private fun startService() {
        val intent = Intent(context, MusicService::class.java)
        context.startForegroundService(intent)
    }
}
