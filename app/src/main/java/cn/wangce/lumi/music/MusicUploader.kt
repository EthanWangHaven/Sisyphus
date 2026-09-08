package cn.wangce.lumi.music

import android.content.Context
import android.util.Base64
import cn.wangce.lumi.R
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

/* ============================================================
 * 网站音乐上传：对标网页端「添加音乐」弹窗（EthanWangHaven.github.io）的完整逻辑
 * 1. 音乐 id 模式：Meting 公共实例解析网易云歌曲 id（歌名/歌手/音源直链）→ 下载音源
 * 2. 上传文件模式：直接使用本地音频
 * 3. 歌词：id 模式直接取该 id 的 LRC；文件模式按歌名+歌手在 LRCLIB 检索（简体未命中时转繁体重试）
 * 4. GitHub Contents API 提交：音源 → public/audio/，歌单 → data/playlist.json + public/data/（线上副本，供 APP 同步拉取）
 *    提交后由网站仓库 deploy.yml 自动构建部署（约 1-2 分钟生效）
 * ============================================================ */

object MusicUploader {

    // 与网页端构建注入的 NEXT_PUBLIC_GH_TOKEN 同源（个人 debug 应用，安全级别与网页前端内联一致）
    private const val GH_TOKEN = "" // 从 BuildConfig 或环境变量注入，勿硬编码
    private const val GH_OWNER = "EthanWangHaven"
    private const val GH_REPO = "EthanWangHaven.github.io"
    private const val GH_BRANCH = "main"

    // Meting 公共解析实例，若失效可替换为其他实例
    private const val METING_API = "https://api.injahow.cn/meting/"

    const val MAX_AUDIO_SIZE = 60L * 1024 * 1024
    private const val MIN_AUDIO_SIZE = 100L * 1024

    // 解析结果：歌曲 id / 歌名 / 歌手 / 音源直链（已升级 https）/ 扩展名
    data class ResolvedSong(
        val songId: String,
        val title: String,
        val artist: String,
        val audioUrl: String,
        val ext: String,
    )

    // 一行歌词：秒数时间轴 + 文本
    data class LyricLine(val time: Double, val text: String)

    /** 从输入中提取网易云歌曲 id：支持纯数字 id 或完整歌曲链接（含分享链接） */
    fun extractSongId(context: Context, input: String): String {
        val raw = input.trim()
        if (Regex("^\\d+$").matches(raw)) return raw
        Regex("song\\?id=(\\d+)").find(raw)?.let { return it.groupValues[1] }
        // 常见非歌曲页面，给出针对性提示
        if (Regex("artist\\?id=\\d+").containsMatchIn(raw)) {
            throw IllegalStateException(context.getString(R.string.music_add_err_artist_page))
        }
        if (Regex("album\\?id=\\d+").containsMatchIn(raw)) {
            throw IllegalStateException(context.getString(R.string.music_add_err_album_page))
        }
        if (Regex("playlist\\?id=\\d+").containsMatchIn(raw)) {
            throw IllegalStateException(context.getString(R.string.music_add_err_playlist_page))
        }
        if (Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(raw)) {
            throw IllegalStateException(context.getString(R.string.music_add_err_bad_link))
        }
        throw IllegalStateException(context.getString(R.string.music_add_err_need_id_raw))
    }

    /** 解析网易云歌曲 id（Meting 实例返回数组，取首条） */
    fun resolveNeteaseSong(context: Context, input: String): ResolvedSong {
        val songId = extractSongId(context, input)
        val body = httpGetString("$METING_API?type=song&id=${urlEnc(songId)}")
            ?: throw IllegalStateException(context.getString(R.string.music_add_err_meting_conn))
        val data = runCatching { JSONArray(body) }.getOrNull()
            ?: throw IllegalStateException(context.getString(R.string.music_add_err_meting_body))
        val info = if (data.length() > 0) data.optJSONObject(0) else null
        if (info == null || info.optString("url").isBlank()) {
            throw IllegalStateException(context.getString(R.string.music_add_err_no_song))
        }
        val audioUrl = info.optString("url").replaceFirst("http:", "https:")
        val ext = Regex("\\.(\\w{2,5})$").find(audioUrl.split('?')[0].split('#')[0])
            ?.groupValues?.get(1)?.lowercase() ?: "mp3"
        return ResolvedSong(
            songId = songId,
            title = info.optString("name").trim(),
            artist = info.optString("artist").trim(),
            audioUrl = audioUrl,
            ext = ext,
        )
    }

    // 依次尝试的音源下载通道：直连 → 公共跨域代理
    /** 下载远端音源为字节（含体积校验） */
    fun downloadAudio(context: Context, remoteUrl: String): ByteArray {
        val channels = listOf(
            remoteUrl,
            "https://corsproxy.io/?url=${urlEnc(remoteUrl)}",
            "https://api.allorigins.win/raw?url=${urlEnc(remoteUrl)}",
        )
        var lastReason = ""
        for (url in channels) {
            val conn = httpGet(url)
            try {
                if (conn.responseCode !in 200..299) {
                    lastReason = "HTTP ${conn.responseCode}"
                    continue
                }
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.size < MIN_AUDIO_SIZE) {
                    lastReason = context.getString(R.string.music_add_err_not_audio)
                    continue
                }
                if (bytes.size > MAX_AUDIO_SIZE) {
                    throw IllegalStateException(context.getString(R.string.music_add_err_too_large_remote))
                }
                return bytes
            } catch (e: IllegalStateException) {
                throw e // 体积超限直接终止，不再换通道
            } catch (e: Exception) {
                lastReason = e.message ?: e.javaClass.simpleName
            } finally {
                conn.disconnect()
            }
        }
        throw IllegalStateException(context.getString(R.string.music_add_err_download_fmt, lastReason))
    }

    // ── 歌词匹配 ──

    // 简→繁单字映射（assets/s2t-map.json，与网页端 lib/s2t-map.json 同源，仅用于 LRCLIB 检索重试）
    private var s2tMap: Map<String, String>? = null

    private fun toTraditional(context: Context, text: String): String {
        if (s2tMap == null) {
            s2tMap = runCatching {
                val json = JSONObject(
                    context.assets.open("s2t-map.json").bufferedReader().use { it.readText() },
                )
                val map = HashMap<String, String>(json.length())
                json.keys().forEach { key -> map[key] = json.optString(key) }
                map
            }.getOrDefault(emptyMap())
        }
        return text.map { s2tMap?.get(it.toString()) ?: it.toString() }.joinToString("")
    }

    /** 歌名/歌手归一化：去括号注释、分隔符与大小写，便于比对 */
    private fun normalizeName(s: String): String = s.lowercase()
        .replace(Regex("[（(【\\[〔].*?[)）\\]】〕]"), "")
        .replace(Regex("[\\s\\-–—_·・]+"), "")

    /** LRC 文本 → 带秒数时间轴的歌词行（自动跳过元数据标签） */
    fun parseLrc(lrc: String): List<LyricLine> {
        val lines = ArrayList<LyricLine>()
        for (raw in lrc.split("\r\n", "\n")) {
            val tags = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\]").findAll(raw).toList()
            if (tags.isEmpty()) continue
            val text = raw.replace(Regex("\\[[^\\]]*\\]"), "").trim()
            for (m in tags) {
                val time = m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt() +
                    (m.groupValues[3].takeIf { it.isNotEmpty() }
                        ?.let { it.padEnd(3, '0').toInt() / 1000.0 } ?: 0.0)
                lines += LyricLine(Math.round(time * 100) / 100.0, text)
            }
        }
        return lines.sortedBy { it.time }
    }

    private fun lrclibSearch(query: String): JSONArray {
        // 失败返回空数组（歌词匹配失败不阻塞提交）
        val body = httpGetString("https://lrclib.net/api/search?$query").orEmpty()
        return runCatching { JSONArray(body) }.getOrDefault(JSONArray())
    }

    /** 在候选中挑选最匹配且带同步歌词的条目（歌名相等 > 包含，歌手匹配加分） */
    private fun pickBestLyric(context: Context, hits: JSONArray, title: String, artist: String): JSONObject? {
        // LRCLIB 华语歌词多为繁体收录，简体/繁体都要参与评分匹配
        val nts = listOf(normalizeName(title), normalizeName(toTraditional(context, title))).filter { it.isNotEmpty() }
        val nas = listOf(normalizeName(artist), normalizeName(toTraditional(context, artist))).filter { it.isNotEmpty() }
        var best: JSONObject? = null
        var bestScore = 0
        for (i in 0 until hits.length()) {
            val hit = hits.optJSONObject(i) ?: continue
            if (hit.optString("syncedLyrics").isBlank()) continue
            val name = normalizeName(hit.optString("trackName"))
            var score = when {
                nts.contains(name) -> 2
                nts.any { name.contains(it) } -> 1
                else -> 0
            }
            if (score > 0 && nas.isNotEmpty() && nas.any { hit.optString("artistName").contains(it) }) score += 1
            if (score > bestScore) {
                best = hit
                bestScore = score
            }
        }
        return best
    }

    /** 按歌名+歌手在 LRCLIB 检索同步歌词（简体未命中时自动转繁体重试） */
    fun searchLyrics(context: Context, title: String, artist: String): List<LyricLine> {
        val t = title.trim()
        val a = artist.trim()
        if (t.isEmpty()) throw IllegalStateException(context.getString(R.string.music_add_err_need_title))

        val tTrad = toTraditional(context, t)
        val aTrad = toTraditional(context, a)
        val queries = mutableListOf(
            "track_name=${urlEnc(t)}&artist_name=${urlEnc(a)}",
            "q=${urlEnc("$t $a")}",
        )
        // 简繁字形不同才追加繁体查询（LRCLIB 华语歌词多为繁体收录）
        if (tTrad != t || aTrad != a) {
            queries += "track_name=${urlEnc(tTrad)}&artist_name=${urlEnc(aTrad)}"
            queries += "q=${urlEnc("$tTrad $aTrad")}"
        }
        for (query in queries) {
            pickBestLyric(context, lrclibSearch(query), t, a)
                ?.optString("syncedLyrics")?.takeIf { it.isNotEmpty() }
                ?.let { return parseLrc(it) }
        }
        throw IllegalStateException(context.getString(R.string.music_add_err_search_no_lyrics))
    }

    /** 按网易云歌曲 id 直接取歌词（Meting lrc 接口，返回纯 LRC 文本） */
    fun fetchLyricsById(context: Context, songId: String): List<LyricLine> {
        val text = httpGetString("$METING_API?type=lrc&id=${urlEnc(songId)}")
            ?: throw IllegalStateException(context.getString(R.string.music_add_err_lyrics_conn))
        val lines = parseLrc(text)
        if (lines.isEmpty()) throw IllegalStateException(context.getString(R.string.music_add_err_no_lyrics))
        return lines
    }

    // ── GitHub Contents API 基础操作 ──

    private fun ghUrl(path: String) = "https://api.github.com/repos/$GH_OWNER/$GH_REPO/contents/$path"

    private fun httpGet(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 120_000
        conn.requestMethod = "GET"
        return conn
    }

    private fun httpGetString(url: String): String? {
        val conn = httpGet(url)
        return try {
            if (conn.responseCode !in 200..299) null else conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun authHeaders(conn: HttpURLConnection) {
        conn.setRequestProperty("Authorization", "Bearer $GH_TOKEN")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
    }

    /** 读取仓库文件信息（内容 + sha），不存在时返回 null */
    private fun getRepoFile(context: Context, path: String): Pair<String, String>? {
        val conn = httpGet("${ghUrl(path)}?ref=$GH_BRANCH")
        return try {
            authHeaders(conn)
            when (conn.responseCode) {
                404 -> null
                !in 200..299 -> throw IllegalStateException(
                    context.getString(R.string.music_add_err_gh_read_fmt, conn.responseCode),
                )
                else -> {
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val decoded = String(Base64.decode(json.optString("content"), Base64.DEFAULT), Charsets.UTF_8)
                    decoded to json.optString("sha")
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    /** 创建或更新仓库文件（网页端同款兜底：未显式传 sha 时自动读取现有文件 sha，更新已存在文件必需） */
    private fun putRepoFile(context: Context, path: String, base64: String, message: String, sha: String? = null) {
        val finalSha = sha ?: getRepoFile(context, path)?.second
        val conn = URL(ghUrl(path)).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 120_000
            conn.requestMethod = "PUT"
            authHeaders(conn)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val body = JSONObject()
                .put("message", message)
                .put("content", base64)
                .put("branch", GH_BRANCH)
            if (!finalSha.isNullOrBlank()) body.put("sha", finalSha)
            val payload = body.toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(payload.size)
            conn.outputStream.use { it.write(payload) }
            if (conn.responseCode !in 200..299) {
                val msg = runCatching {
                    JSONObject(conn.errorStream.bufferedReader().use { it.readText() }).optString("message")
                }.getOrDefault("")
                throw IllegalStateException(
                    context.getString(R.string.music_add_err_gh_fmt, msg.ifEmpty { "HTTP ${conn.responseCode}" }),
                )
            }
        } finally {
            conn.disconnect()
        }
    }

    /** 将新歌提交到仓库：先传音源，再追加歌单（含线上副本），返回生成的本地曲目（立即可加入播放列表） */
    fun addSongToRepo(
        context: Context,
        songId: String,
        title: String,
        artist: String,
        audio: ByteArray,
        ext: String,
        lyrics: List<LyricLine>,
    ): Track {
        if (GH_TOKEN.isBlank()) {
            throw IllegalStateException(context.getString(R.string.music_add_err_no_token))
        }
        // id 仅保留安全文件名字符，防止链接等输入破坏 API 路径
        val safeId = songId.replace(Regex("[^a-zA-Z0-9_-]"), "").take(64)
        if (safeId.isEmpty()) throw IllegalStateException(context.getString(R.string.music_add_err_bad_id))
        val safeExt = (ext.ifEmpty { "mp3" }).replace(Regex("[^a-z0-9]"), "").take(5).ifEmpty { "mp3" }
        val filename = "$safeId.$safeExt"
        // 音源提交与歌单提交分开：音源提交不触发部署（deploy.yml paths-ignore）
        val audioMsg = "music: upload audio $safeId"
        val playlistMsg = "add song: $title - $artist"

        // 1. 上传音源
        putRepoFile(context, "public/audio/$filename", Base64.encodeToString(audio, Base64.NO_WRAP), audioMsg)

        // 2. 追加歌单
        val existing = getRepoFile(context, "data/playlist.json")
        val arr = existing?.let { (text, _) -> runCatching { JSONArray(text) }.getOrDefault(JSONArray()) } ?: JSONArray()
        for (i in 0 until arr.length()) {
            if (arr.optJSONObject(i)?.optString("id") == safeId) {
                throw IllegalStateException(context.getString(R.string.music_add_err_dup))
            }
        }
        val lyricsArr = JSONArray()
        lyrics.forEach { line ->
            lyricsArr.put(JSONObject().put("time", line.time).put("text", line.text))
        }
        val updated = JSONArray()
        for (i in 0 until arr.length()) updated.put(arr.get(i))
        updated.put(
            JSONObject()
                .put("id", safeId)
                .put("title", title)
                .put("artist", artist)
                .put("audioUrl", "/audio/$filename")
                .put("lyrics", lyricsArr),
        )
        val updatedText = updated.toString(2) + "\n"
        val updatedB64 = Base64.encodeToString(updatedText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        putRepoFile(context, "data/playlist.json", updatedB64, playlistMsg, existing?.second)
        // 线上副本：public/ 随静态导出部署，Sisyphus APP 同步功能拉取该端点
        putRepoFile(context, "public/data/playlist.json", updatedB64, playlistMsg)

        return Track(
            fileName = filename,
            title = title,
            artist = artist,
            cover = "",
            url = PlaylistSync.SITE + "/audio/" + filename,
        )
    }

    private fun urlEnc(s: String) = URLEncoder.encode(s, "UTF-8")
}
