package cn.wangce.lumi.music

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.wangce.lumi.R
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

// 自定义封面持久化：DataStore 存「曲目 url → 本地封面绝对路径」映射；
// 同步来的歌没有封面，用户长按大屏封面输入网易云链接/ID 后下载到 filesDir/covers
private val Context.trackCoverDataStore by preferencesDataStore(name = "track_covers")
private val KEY_COVER_OVERRIDES = stringPreferencesKey("cover_overrides")

// 网易云/QQ 音乐封面获取与持久化。fetch*Cover 为阻塞 IO，调用方自行切线程
object TrackCoverStore {

    private const val DETAIL_API = "https://music.163.com/api/song/detail/?id=%s&ids=%%5B%s%%5D"
    private const val SEARCH_API = "https://music.163.com/api/search/get?s=%s&type=1&limit=5"
    private const val QQ_DETAIL_API = "https://c.y.qq.com/v8/fcg-bin/fcg_play_single_song.fcg?songmid=%s&format=json"
    private const val QQ_SEARCH_API = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?w=%s&format=json&n=5"
    private const val QQ_COVER_API = "https://y.gtimg.cn/music/photo_new/T002R300x300M000%s.jpg"

    // 封面覆盖映射（key = 曲目 url，value = 本地封面绝对路径）
    fun flow(context: Context): Flow<Map<String, String>> =
        context.trackCoverDataStore.data.map { prefs ->
            runCatching {
                val json = prefs[KEY_COVER_OVERRIDES] ?: return@map emptyMap<String, String>()
                val o = JSONObject(json)
                val keys = o.keys()
                val map = HashMap<String, String>()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = o.optString(k)
                }
                map
            }.getOrDefault(emptyMap())
        }

    suspend fun set(context: Context, trackUrl: String, localPath: String) {
        context.trackCoverDataStore.edit { prefs ->
            val current = prefs[KEY_COVER_OVERRIDES]
            val o = if (current != null) JSONObject(current) else JSONObject()
            o.put(trackUrl, localPath)
            prefs[KEY_COVER_OVERRIDES] = o.toString()
        }
    }

    // 解析用户输入：纯数字 ID / 含 id= 的链接 / song/{id} 形式链接
    fun parseNeteaseId(input: String): String? {
        val s = input.trim()
        if (s.matches(Regex("\\d+"))) return s
        Regex("[?&]id=(\\d+)").find(s)?.let { return it.groupValues[1] }
        Regex("song/(\\d+)").find(s)?.let { return it.groupValues[1] }
        return null
    }

    // 拉取网易云歌曲详情 → 下载专辑封面到 filesDir/covers/netease_{id}.jpg → 返回绝对路径；
    // 失败抛 IllegalStateException（message 已本地化，调用方可直接展示）
    fun fetchNeteaseCover(context: Context, input: String): String {
        val id = parseNeteaseId(input)
            ?: throw IllegalStateException(context.getString(R.string.music_cover_bad_input))

        val picUrl = try {
            val conn = URL(DETAIL_API.format(id, id)).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val songs = JSONObject(body).optJSONArray("songs")
                songs?.optJSONObject(0)?.optJSONObject("album")?.optString("picUrl").orEmpty()
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e("TrackCoverStore", "fetch detail failed", e)
            throw IllegalStateException(context.getString(R.string.music_cover_bad_input))
        }
        if (picUrl.isBlank()) throw IllegalStateException(context.getString(R.string.music_cover_no_pic))
        return downloadCover(context, picUrl, "netease_$id")
    }

    // 下载封面图到 filesDir/covers/{name}.jpg；http 直链统一升级 https（系统 cleartext 策略）
    private fun downloadCover(context: Context, picUrl: String, name: String): String {
        val url = if (picUrl.startsWith("http://")) "https://" + picUrl.removePrefix("http://") else picUrl
        val dir = File(context.filesDir, "covers").apply { mkdirs() }
        val out = File(dir, "$name.jpg")
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 20_000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.inputStream.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e("TrackCoverStore", "download cover failed", e)
            out.delete()
            throw IllegalStateException(context.getString(R.string.music_cover_no_pic))
        }
        if (!out.exists() || out.length() == 0L) {
            throw IllegalStateException(context.getString(R.string.music_cover_no_pic))
        }
        return out.absolutePath
    }

    // 归一化：小写 + 仅保留字母/数字（中文保留），用于歌名/歌手模糊匹配
    private fun norm(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }

    // 自动匹配：网易云搜索（歌名+歌手）→ 逐候选校验（歌手互相包含 + 歌名互相包含）→ 命中返回歌曲 id
    fun searchNeteaseId(title: String, artist: String): String? {
        val titleN = norm(title)
        if (titleN.isEmpty()) return null
        val artistN = norm(artist)
        return try {
            val q = URLEncoder.encode("$title $artist", "UTF-8")
            val conn = URL(SEARCH_API.format(q)).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val songs = JSONObject(body).optJSONObject("result")?.optJSONArray("songs")
                    ?: return null
                for (i in 0 until songs.length()) {
                    val song = songs.optJSONObject(i) ?: continue
                    val id = song.optLong("id").takeIf { it > 0 }?.toString() ?: continue
                    val nameN = norm(song.optString("name"))
                    val joinedN = song.optJSONArray("artists")?.let { a ->
                        (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optString("name") }
                            .joinToString("/").let { norm(it) }
                    }.orEmpty()
                    val artistOk = artistN.isEmpty() ||
                        (joinedN.isNotEmpty() && (joinedN.contains(artistN) || artistN.contains(joinedN)))
                    val nameOk = nameN.contains(titleN) || titleN.contains(nameN)
                    if (artistOk && nameOk) return id
                }
                null
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e("TrackCoverStore", "search cover failed: $title", e)
            null
        }
    }

    // 输入是否指向 QQ 音乐：y.qq.com 链接 / songDetail|song/ 路径 / 疑似 songmid（含大写字母的字母数字串）
    fun isQQInput(input: String): Boolean {
        val s = input.trim()
        if (s.contains("y.qq.com", ignoreCase = true)) return true
        Regex("song(?:Detail)?/([0-9A-Za-z]{8,14})", RegexOption.IGNORE_CASE).find(s)?.let {
            val mid = it.groupValues[1]
            if (!mid.matches(Regex("\\d+"))) return true
        }
        return Regex("[0-9A-Za-z]{10,14}").matches(s) && s.any { it.isUpperCase() }
    }

    // 从输入解析 QQ 音乐 songmid（分享链接 songDetail/{mid}、song/{mid} 或直接粘贴 mid）
    private fun parseQQSongMid(input: String): String? {
        val s = input.trim()
        Regex("song(?:Detail)?/([0-9A-Za-z]{8,14})", RegexOption.IGNORE_CASE).find(s)?.let {
            val mid = it.groupValues[1]
            if (!mid.matches(Regex("\\d+"))) return mid
        }
        if (Regex("[0-9A-Za-z]{10,14}").matches(s) && s.any { it.isUpperCase() }) return s
        return null
    }

    // QQ 音乐 songmid → 歌曲详情拿专辑 mid → 下载专辑封面；失败抛 IllegalStateException
    fun fetchQQCover(context: Context, input: String): String {
        val mid = parseQQSongMid(input)
            ?: throw IllegalStateException(context.getString(R.string.music_cover_bad_input))
        val albumMid = try {
            val conn = URL(QQ_DETAIL_API.format(mid)).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.setRequestProperty("Referer", "https://y.qq.com/")
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body).optJSONArray("data")?.optJSONObject(0)
                    ?.optJSONObject("album")?.optString("mid").orEmpty()
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e("TrackCoverStore", "fetch qq detail failed", e)
            throw IllegalStateException(context.getString(R.string.music_cover_bad_input))
        }
        if (albumMid.isBlank()) throw IllegalStateException(context.getString(R.string.music_cover_no_pic))
        return downloadCover(context, QQ_COVER_API.format(albumMid), "qq_$albumMid")
    }

    // QQ 音乐搜索（歌名+歌手）→ 逐候选校验（歌手互相包含 + 歌名互相包含）→ 命中返回专辑 mid
    fun searchQQAlbumMid(title: String, artist: String): String? {
        val titleN = norm(title)
        if (titleN.isEmpty()) return null
        val artistN = norm(artist)
        return try {
            val q = URLEncoder.encode("$title $artist", "UTF-8")
            val conn = URL(QQ_SEARCH_API.format(q)).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.setRequestProperty("Referer", "https://y.qq.com/")
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val songs = JSONObject(body).optJSONObject("data")
                    ?.optJSONObject("song")?.optJSONArray("list") ?: return null
                for (i in 0 until songs.length()) {
                    val song = songs.optJSONObject(i) ?: continue
                    val mid = song.optString("albummid")
                    if (mid.isBlank()) continue
                    val nameN = norm(song.optString("songname"))
                    val joinedN = song.optJSONArray("singer")?.let { a ->
                        (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optString("name") }
                            .joinToString("/").let { norm(it) }
                    }.orEmpty()
                    val artistOk = artistN.isEmpty() ||
                        (joinedN.isNotEmpty() && (joinedN.contains(artistN) || artistN.contains(joinedN)))
                    val nameOk = nameN.contains(titleN) || titleN.contains(nameN)
                    if (artistOk && nameOk) return mid
                }
                null
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e("TrackCoverStore", "qq search cover failed: $title", e)
            null
        }
    }

    // 同步后自动匹配封面：网易云搜索优先，未命中/下载失败再试 QQ 音乐；均未命中返回 null（保留默认灰底）
    fun autoFetchCover(context: Context, title: String, artist: String): String? {
        val nid = searchNeteaseId(title, artist)
        if (nid != null) {
            try {
                return fetchNeteaseCover(context, nid)
            } catch (e: Exception) {
                Log.e("TrackCoverStore", "auto fetch cover failed: $title", e)
            }
        }
        val mid = searchQQAlbumMid(title, artist) ?: return null
        return try {
            downloadCover(context, QQ_COVER_API.format(mid), "qq_$mid")
        } catch (e: Exception) {
            Log.e("TrackCoverStore", "auto fetch qq cover failed: $title", e)
            null
        }
    }
}
