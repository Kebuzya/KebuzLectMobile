package com.kebuz.kebuzlect.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kebuz.kebuzlect.data.settings.AppSettings
import com.kebuz.kebuzlect.data.settings.SettingsState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = AppSettings(application)

    val state: StateFlow<SettingsState> =
        settings.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setBlurThreshold(value: Float) = launchUpdate { settings.setBlurThreshold(value) }
    fun setDuplicateThreshold(value: Int) = launchUpdate { settings.setDuplicateThreshold(value) }
    fun setJpegQuality(value: Int) = launchUpdate { settings.setJpegQuality(value) }
    fun setPdfDpi(value: Int) = launchUpdate { settings.setPdfDpi(value) }
    fun setPhotosPerPage(value: Int) = launchUpdate { settings.setPhotosPerPage(value) }
    fun setLectureNumberWidth(value: Int) = launchUpdate { settings.setLectureNumberWidth(value) }
    fun setOutputFormat(value: String) = launchUpdate { settings.setOutputFormat(value) }
    fun setTheme(value: String) = launchUpdate { settings.setTheme(value) }
    fun setLanguage(value: String) = launchUpdate { settings.setLanguage(value) }

    private fun launchUpdate(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
