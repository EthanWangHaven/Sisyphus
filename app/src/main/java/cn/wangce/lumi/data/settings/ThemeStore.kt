package cn.wangce.lumi.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.wangce.lumi.ui.theme.ThemeMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme")
private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
private val KEY_FONT_BOOST = intPreferencesKey("font_boost")
private val KEY_FONT_SCALE = intPreferencesKey("font_scale")
private val KEY_GREETING = stringPreferencesKey("home_greeting")

private const val LANG_PREFS = "lumi_settings"
private const val KEY_LANGUAGE = "app_language"

// 应用内语言：跟随系统 / 中文 / English
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ZH("zh"),
    EN("en"),
}

// 偏好持久化：主题模式走 DataStore；语言走 SharedPreferences
// （attachBaseContext 需要同步读取，DataStore 是异步的读不了）
class ThemeStore(private val context: Context) {

    private val langPrefs: SharedPreferences =
        context.getSharedPreferences(LANG_PREFS, Context.MODE_PRIVATE)

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    // 字体粗细档位：-200 细 / 0 标准 / +200 粗（旧版 0/100/200，非新档值统一归 0）
    val fontBoost: Flow<Int> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_FONT_BOOST]?.takeIf { it == -200 || it == 200 } ?: 0
    }

    suspend fun setFontBoost(boost: Int) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_FONT_BOOST] = boost
        }
    }

    // 字体大小档位：80 较小 / 90 标准 / 100 较大（默认较大 = 现状字号）
    val fontScale: Flow<Int> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_FONT_SCALE]?.takeIf { it == 80 || it == 90 } ?: 100
    }

    suspend fun setFontScale(scale: Int) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_FONT_SCALE] = scale
        }
    }

    // 首页欢迎词：null = 未自定义（显示默认 "Hi, WangCe"）
    val greeting: Flow<String?> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_GREETING]?.trim()?.takeIf { it.isNotEmpty() }
    }

    suspend fun setGreeting(text: String?) {
        context.themeDataStore.edit { prefs ->
            val t = text?.trim().orEmpty()
            if (t.isEmpty()) prefs.remove(KEY_GREETING) else prefs[KEY_GREETING] = t
        }
    }

    // 语言变化流：监听 SharedPreferences 变更，设置页实时响应
    val language: Flow<AppLanguage> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LANGUAGE) trySend(currentLanguage())
        }
        langPrefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(currentLanguage())
        awaitClose { langPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // 同步读取当前语言（attachBaseContext / Service 均可用）
    fun currentLanguage(): AppLanguage =
        runCatching {
            AppLanguage.valueOf(
                langPrefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name,
            )
        }.getOrDefault(AppLanguage.SYSTEM)

    fun setLanguage(lang: AppLanguage) {
        langPrefs.edit().putString(KEY_LANGUAGE, lang.name).apply()
    }
}
