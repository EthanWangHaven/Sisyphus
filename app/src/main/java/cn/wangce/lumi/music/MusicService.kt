package cn.wangce.lumi.music

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import cn.wangce.lumi.MainActivity
import cn.wangce.lumi.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 音乐前台服务：保活 + 通知栏磨砂媒体卡片（网易云风格：封面模糊背景 + 圆角封面/歌名/歌手 + 三键遥控，
// 浅色/深色形态随系统夜间模式自动切换）；播放逻辑全部在 MusicPlayerManager。
// 另发一条 MediaStyle 系统媒体通知（A13+ 转换为锁屏/快捷设置的媒体卡，真机由 ROM 渲染磨砂样式，
// 爱心/词按钮经 PlaybackState CustomAction 下发，由系统渲染），该通知自身不进抽屉；
// 元数据/进度由独立 MediaSession 提供，与自定义卡互不影响。
@AndroidEntryPoint
class MusicService : Service() {

    @Inject lateinit var manager: MusicPlayerManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collectJob: kotlinx.coroutines.Job? = null
    private var notifyJob: Job? = null
    private var lastNotifyKey: String? = null

    // 系统媒体卡 session：锁屏/快捷设置的磨砂卡由 SystemUI 渲染，样式随 ROM（真机为厂商定制形态）
    private var session: MediaSession? = null
    private var lastSessionPos = Int.MIN_VALUE

    // 封面位图缓存（key = 封面路径，本地文件或 assets 路径）
    private val coverCache = ConcurrentHashMap<String, Bitmap>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> manager.togglePlayPause()
            ACTION_NEXT -> manager.next()
            ACTION_PREV -> manager.prev()
            ACTION_LIKE -> toggleLike()
            ACTION_STOP -> {
                manager.pause()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        startForeground(NOTI_ID, buildNotification(manager.state.value, coverFor(manager.state.value), null))
        // 状态变化刷新媒体卡片（按 歌目/播放态 去重，避免每 500ms 重刷）；其余状态仅校准系统卡进度
        collectJob?.cancel()
        collectJob = scope.launch {
            manager.state.collect { st ->
                val key = "${st.currentIndex}:${st.isPlaying}"
                if (key != lastNotifyKey) {
                    lastNotifyKey = key
                    refreshNotification(st)
                } else {
                    updatePlaybackState(st, force = false)
                }
            }
        }
        return START_NOT_STICKY
    }

    // 系统深浅色切换时按新配色重建卡片
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lastNotifyKey = null
        refreshNotification(manager.state.value)
    }

    override fun onDestroy() {
        scope.cancel()
        session?.release()
        session = null
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTI_ID_SYS)
        super.onDestroy()
    }

    // 通知统一刷新（封面在 IO 线程解码 + 模糊烘焙，完成后再发通知，避免阻塞主线程）
    private fun refreshNotification(st: MusicUiState) {
        notifyJob?.cancel()
        notifyJob = scope.launch {
            val cover = withContext(Dispatchers.IO) { loadCover(st) }
            val blurBg = cover?.let { withContext(Dispatchers.IO) { makeBlurBg(it) } }
            // 先把播放态（含爱心 CustomAction 图标）写进 session，再 post MediaStyle 通知；
            // SystemUI 首次绑定时只读当时的 PlaybackState，若此时动作列表为空会固化空心图标
            ensureSession()
            updatePlaybackState(st, force = true)
            // 封面/歌目就绪后同步给系统媒体卡（锁屏 & 快捷设置）
            updateSessionMetadata(st, cover)
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTI_ID, buildNotification(st, cover, blurBg))
        }
    }

    // 建系统媒体 session（主线程构造；不设 media button receiver，线控暂不控播）
    private fun ensureSession() {
        if (session != null) return
        MediaSession(this, "LumiMusic").also { s ->
            // onCustomAction 由 Java 桥接实现（K2 对 android-35 该方法 override 误报，见桥接类注释）
            s.setCallback(object : MediaSessionCallbackBridge(
                MediaSessionCallbackBridge.CustomActionHandler { action, _ ->
                    when (action) {
                        ACTION_LIKE -> toggleLike()
                        // 词：跳转 App 音乐页查看歌曲详情/歌词（歌词存远端 repo）。
                        // 后台服务直接 startActivity 会被 A13+ BAL 拦截（实测 BAL_BLOCK），
                        // 改用创建/发送双方均声明允许后台启动的 PendingIntent（API 34+ 官方 opt-in）
                        ACTION_LYRICS -> {
                            val intent = Intent(
                                this@MusicService,
                                MainActivity::class.java,
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            val opts = ActivityOptions.makeBasic().apply {
                                if (Build.VERSION.SDK_INT >= 34) {
                                    setPendingIntentCreatorBackgroundActivityStartMode(
                                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                                    )
                                }
                            }
                            val pi = PendingIntent.getActivity(
                                this@MusicService, 7, intent,
                                PendingIntent.FLAG_IMMUTABLE, opts.toBundle(),
                            )
                            // 发送方也需声明允许（Android 15 双侧 opt-in 缺一不可，实测仅创建方仍 BAL_BLOCK）
                            val sendOpts = ActivityOptions.makeBasic().apply {
                                if (Build.VERSION.SDK_INT >= 34) {
                                    setPendingIntentBackgroundActivityStartMode(
                                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                                    )
                                }
                            }
                            pi.send(this@MusicService, 0, null, null, null, null, sendOpts.toBundle())
                        }
                    }
                }
            ) {
                override fun onPlay() = manager.togglePlayPause()
                override fun onPause() = manager.pause()
                override fun onSkipToNext() = manager.next()
                override fun onSkipToPrevious() = manager.prev()
                override fun onSeekTo(pos: Long) = manager.seekTo(pos.toInt())
            })
            s.isActive = true
            session = s
        }
    }

    // 歌目元数据 → 系统卡（封面缩至 ≤512 传原图，磨砂背景/圆角由 SystemUI 自行处理）
    private fun updateSessionMetadata(st: MusicUiState, cover: Bitmap?) {
        ensureSession()
        val track = st.tracks.getOrNull(st.currentIndex)
        val title = track?.title ?: getString(R.string.s95521b)
        val artist = track?.artist?.takeIf { it.isNotEmpty() } ?: getString(R.string.s95521b)
        val md = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, st.durationMs.toLong())
        cover?.let { md.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, scaleForSession(it)) }
        session?.setMetadata(md.build())
        // 歌目/封面变化时同步更新系统媒体通知（锁屏 & 快捷设置的卡随 session 数据刷新）
        postMediaSessionNotification(st)
    }

    // MediaStyle 系统媒体通知：A13+ 由 SystemUI 转换为锁屏/快捷设置的媒体卡；
    // 仅承载三个操作按钮（上一首/暂停/下一首），不内嵌封面/标题/歌手卡片内容
    private fun postMediaSessionNotification(st: MusicUiState) {
        val s = session ?: return
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val prevPi = PendingIntent.getService(
            this, 4,
            Intent(this, MusicService::class.java).setAction(ACTION_PREV),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val togglePi = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val nextPi = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val b = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (st.isPlaying) R.drawable.ic_noti_pause else R.drawable.ic_noti_play)
            .setContentIntent(openApp)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(R.drawable.ic_noti_prev, "prev", prevPi).build())
            .addAction(
                Notification.Action.Builder(
                    if (st.isPlaying) R.drawable.ic_noti_pause else R.drawable.ic_noti_play,
                    "toggle", togglePi,
                ).build(),
            )
            .addAction(Notification.Action.Builder(R.drawable.ic_noti_next, "next", nextPi).build())
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(s.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTI_ID_SYS, b.build())
    }

    // 播放态/进度 → 系统卡：系统按 position+speed 自行推算推进，
    // 正常播放步进（≤1.5s）不重复 set，仅 seek 大跳或强制（歌目/播放态切换）时校准
    private fun updatePlaybackState(st: MusicUiState, force: Boolean) {
        val s = session ?: return
        val jumped = lastSessionPos == Int.MIN_VALUE ||
            kotlin.math.abs(st.positionMs - lastSessionPos) > 1500
        if (!force && !jumped) return
        lastSessionPos = st.positionMs
        val track = st.tracks.getOrNull(st.currentIndex)
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO
                )
                // 爱心/词：CustomAction 随播放态下发，系统媒体卡（锁屏/快捷设置）渲染为操作按钮
                .addCustomAction(
                    PlaybackState.CustomAction.Builder(
                        ACTION_LIKE, "喜欢",
                        if (isLiked(track)) R.drawable.ic_media_heart_filled
                        else R.drawable.ic_media_heart_outline,
                    ).build()
                )
                .addCustomAction(
                    PlaybackState.CustomAction.Builder(
                        ACTION_LYRICS, "词", R.drawable.ic_media_lyrics
                    ).build()
                )
                .setState(
                    if (st.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    st.positionMs.toLong(),
                    if (st.isPlaying) 1f else 0f,
                    SystemClock.elapsedRealtime(),
                )
                .build()
        )
    }

    private fun scaleForSession(src: Bitmap): Bitmap {
        val max = 512
        if (src.width <= max && src.height <= max) return src
        val k = max.toFloat() / maxOf(src.width, src.height)
        return Bitmap.createScaledBitmap(src, (src.width * k).toInt(), (src.height * k).toInt(), true)
    }

    // 同步取缓存封面（首帧先无图，异步解码后由 refreshNotification 补上）
    private fun coverFor(st: MusicUiState): Bitmap? {
        val track = st.tracks.getOrNull(st.currentIndex) ?: return null
        val path = st.coverOverrides[track.url] ?: track.cover.takeIf { it.isNotEmpty() } ?: return null
        return coverCache[path]
    }

    // 解码封面并写缓存：/ 开头 = 本地文件（自定义封面），否则 assets 路径（与 UI 解析规则一致）
    private fun loadCover(st: MusicUiState): Bitmap? {
        val track = st.tracks.getOrNull(st.currentIndex) ?: return null
        val path = st.coverOverrides[track.url] ?: track.cover.takeIf { it.isNotEmpty() } ?: return null
        coverCache[path]?.let { return it }
        val bmp = runCatching {
            if (path.startsWith("/")) BitmapFactory.decodeFile(path)
            else assets.open(path).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        if (bmp != null) coverCache[path] = bmp
        return bmp
    }

    // 磨砂背景：封面多级降采样后再放大，双线性插值自然产生重度模糊（RemoteViews 无法实时模糊）
    private fun makeBlurBg(src: Bitmap): Bitmap {
        var w = (src.width / 10).coerceAtLeast(6)
        var h = (src.height / 10).coerceAtLeast(6)
        var bmp = Bitmap.createScaledBitmap(src, w, h, true)
        repeat(2) {
            w = (w / 2).coerceAtLeast(3)
            h = (h / 2).coerceAtLeast(3)
            bmp = Bitmap.createScaledBitmap(bmp, w, h, true)
        }
        return Bitmap.createScaledBitmap(bmp, 720, 360, true)
    }

    // 圆角封面：中心裁方 + 圆角烘焙进位图（通知布局无法对 ImageView 做圆角裁切）
    private fun roundedCover(src: Bitmap): Bitmap {
        val side = minOf(src.width, src.height)
        val out = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val m = Matrix()
        m.setScale(200f / side, 200f / side)
        m.postTranslate(
            -(src.width - side) / 2f * (200f / side),
            -(src.height - side) / 2f * (200f / side),
        )
        shader.setLocalMatrix(m)
        paint.shader = shader
        canvas.drawRoundRect(RectF(0f, 0f, 200f, 200f), 34f, 34f, paint)
        return out
    }

    private fun buildNotification(st: MusicUiState, cover: Bitmap?, blurBg: Bitmap?): Notification {
        ensureChannel()
        val track = st.tracks.getOrNull(st.currentIndex)
        val title = track?.title ?: getString(R.string.s95521b)
        val artist = track?.artist?.takeIf { it.isNotEmpty() } ?: getString(R.string.s95521b)

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val togglePi = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val nextPi = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_IMMUTABLE,
        )
        // 划掉通知 = 停止播放并退出服务
        val deletePi = PendingIntent.getService(
            this, 3,
            Intent(this, MusicService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val prevPi = PendingIntent.getService(
            this, 4,
            Intent(this, MusicService::class.java).setAction(ACTION_PREV),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val view = RemoteViews(packageName, R.layout.notification_media_card)
        blurBg?.let { view.setImageViewBitmap(R.id.iv_blur_bg, it) }
        cover?.let { view.setImageViewBitmap(R.id.iv_cover, roundedCover(it)) }
        view.setTextViewText(R.id.tv_title, title)
        view.setTextViewText(R.id.tv_artist, artist)
        view.setOnClickPendingIntent(R.id.card_content, openApp)
        view.setOnClickPendingIntent(R.id.btn_prev, prevPi)
        view.setOnClickPendingIntent(R.id.btn_toggle, togglePi)
        view.setOnClickPendingIntent(R.id.btn_next, nextPi)
        view.setImageViewResource(
            R.id.btn_toggle,
            if (st.isPlaying) R.drawable.ic_noti_pause else R.drawable.ic_noti_play,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (st.isPlaying) R.drawable.ic_noti_pause else R.drawable.ic_noti_play)
            .setCustomContentView(view)
            .setCustomBigContentView(view)
            .setContentIntent(openApp)
            .setDeleteIntent(deletePi)
            .setOngoing(st.isPlaying)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, getString(R.string.s298cbe), NotificationManager.IMPORTANCE_LOW),
                )
            }
        }
    }

    // 通知卡片爱心：喜欢当前曲目（SharedPreferences 持久化，跨会话保留）
    private fun isLiked(track: Track?): Boolean {
        track ?: return false
        val key = track.url.ifEmpty { track.fileName }
        return getSharedPreferences(PREFS_LIKE, MODE_PRIVATE)
            .getStringSet(KEY_LIKED, emptySet())?.contains(key) == true
    }

    private fun toggleLike() {
        val st = manager.state.value
        val track = st.tracks.getOrNull(st.currentIndex) ?: return
        val key = track.url.ifEmpty { track.fileName }
        val prefs = getSharedPreferences(PREFS_LIKE, MODE_PRIVATE)
        val set = HashSet(prefs.getStringSet(KEY_LIKED, emptySet()) ?: emptySet())
        if (!set.remove(key)) set.add(key)
        prefs.edit().putStringSet(KEY_LIKED, set).apply()
        refreshNotification(manager.state.value)
        updatePlaybackState(manager.state.value, force = true)
        // A13 SystemUI 收到播放态回调时不重读 CustomAction 列表（仅重绑播放/暂停），
        // 需重发 MediaStyle 通知触发完整重建，系统卡爱心图标才能即时切换
        postMediaSessionNotification(manager.state.value)
    }

    companion object {
        private const val CHANNEL_ID = "music_playback"
        private const val NOTI_ID = 1001

        private const val PREFS_LIKE = "music_like"
        private const val KEY_LIKED = "liked_keys"

        // MediaStyle 系统媒体通知 ID（锁屏/快捷设置媒体卡的载体，A13+ 不显示在抽屉）
        private const val NOTI_ID_SYS = 1002
        const val ACTION_TOGGLE = "cn.wangce.lumi.music.TOGGLE"
        const val ACTION_NEXT = "cn.wangce.lumi.music.NEXT"
        const val ACTION_PREV = "cn.wangce.lumi.music.PREV"
        const val ACTION_LIKE = "cn.wangce.lumi.music.LIKE"
        const val ACTION_LYRICS = "cn.wangce.lumi.music.LYRICS"
        const val ACTION_STOP = "cn.wangce.lumi.music.STOP"
    }
}
