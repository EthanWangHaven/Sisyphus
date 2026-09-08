package cn.wangce.lumi.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wangce.lumi.data.settings.AppLanguage
import cn.wangce.lumi.data.settings.ThemeStore
import cn.wangce.lumi.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeStore: ThemeStore,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themeStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val language: StateFlow<AppLanguage> = themeStore.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), themeStore.currentLanguage())

    val fontBoost: StateFlow<Int> = themeStore.fontBoost
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val fontScale: StateFlow<Int> = themeStore.fontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 100)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeStore.setThemeMode(mode)
        }
    }

    fun setLanguage(lang: AppLanguage) {
        themeStore.setLanguage(lang)
    }

    fun setFontBoost(boost: Int) {
        viewModelScope.launch {
            themeStore.setFontBoost(boost)
        }
    }

    fun setFontScale(scale: Int) {
        viewModelScope.launch {
            themeStore.setFontScale(scale)
        }
    }
}
