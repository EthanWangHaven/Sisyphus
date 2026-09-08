package cn.wangce.lumi.music

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

// 网站歌单同步持久化：DataStore 存「非内置曲目」（fileName/title/artist/url JSON）
private val Context.musicSyncDataStore by preferencesDataStore(name = "music_sync")
private val KEY_EXTRA_TRACKS = stringPreferencesKey("extra_tracks")

// 个人网站（GitHub Pages）歌单同步：拉取 data/playlist.json → 与内置曲库按 url 合并；
// 非内置曲目经 DataStore 持久化，冷启动恢复。fetch() 为阻塞 IO，调用方自行切线程
object PlaylistSync {
    const val SITE = "https://ethanwanghaven.github.io"

    // 拉取并解析网站歌单；audioUrl 为相对路径（/audio/xxx.mp3）时拼站点前缀
    fun fetch(): List<Track> {
        val conn = URL("$SITE/data/playlist.json").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            val list = ArrayList<Track>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val raw = o.optString("audioUrl")
                if (raw.isBlank()) continue
                list += Track(
                    fileName = raw.substringAfterLast('/'),
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    cover = "",
                    url = if (raw.startsWith("http")) raw else SITE + raw,
                )
            }
            return list
        } finally {
            conn.disconnect()
        }
    }

    // 恢复上次同步的非内置曲目（防止同步结果因进程重启丢失）
    suspend fun loadExtras(context: Context): List<Track> {
        val json = context.musicSyncDataStore.data.first()[KEY_EXTRA_TRACKS] ?: return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Track(
                    fileName = o.getString("fileName"),
                    title = o.getString("title"),
                    artist = o.optString("artist"),
                    cover = "",
                    url = o.getString("url"),
                )
            }.filter { it.url !in builtinUrls() }
        }.getOrDefault(emptyList())
    }

    // 全量覆盖持久化非内置曲目
    suspend fun saveExtras(context: Context, extras: List<Track>) {
        val arr = JSONArray()
        extras.forEach { t ->
            arr.put(
                JSONObject()
                    .put("fileName", t.fileName)
                    .put("title", t.title)
                    .put("artist", t.artist)
                    .put("url", t.url),
            )
        }
        context.musicSyncDataStore.edit { it[KEY_EXTRA_TRACKS] = arr.toString() }
    }

    // 内置曲库 url 集合（按 url 去重合并的基准）
    fun builtinUrls(): Set<String> = Playlist.tracks.map { it.url }.toSet()
}
