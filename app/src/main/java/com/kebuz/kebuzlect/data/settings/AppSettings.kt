package com.kebuz.kebuzlect.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppSettings(private val context: Context) {

    val state: Flow<SettingsState> =
        context.settingsDataStore.data.map { prefs ->
            SettingsState(
                outputFormat = prefs[Keys.OUTPUT_FORMAT] ?: SettingsDefaults.OUTPUT_FORMAT,
                jpegQuality = prefs[Keys.JPEG_QUALITY] ?: SettingsDefaults.JPEG_QUALITY,
                pdfDpi = prefs[Keys.PDF_DPI] ?: SettingsDefaults.PDF_DPI,
                photosPerPage = prefs[Keys.PHOTOS_PER_PAGE] ?: SettingsDefaults.PHOTOS_PER_PAGE,
                blurThreshold = prefs[Keys.BLUR_THRESHOLD] ?: SettingsDefaults.BLUR_THRESHOLD,
                duplicateThreshold = prefs[Keys.DUPLICATE_THRESHOLD] ?: SettingsDefaults.DUPLICATE_THRESHOLD,
                lectureNumberWidth = prefs[Keys.LECTURE_NUMBER_WIDTH] ?: SettingsDefaults.LECTURE_NUMBER_WIDTH,
                theme = prefs[Keys.THEME] ?: SettingsDefaults.THEME,
                language = prefs[Keys.LANGUAGE] ?: SettingsDefaults.LANGUAGE,
            )
        }

    suspend fun setOutputFormat(value: String) = put(Keys.OUTPUT_FORMAT, value)
    suspend fun setJpegQuality(value: Int) = put(Keys.JPEG_QUALITY, value)
    suspend fun setPdfDpi(value: Int) = put(Keys.PDF_DPI, value)
    suspend fun setPhotosPerPage(value: Int) = put(Keys.PHOTOS_PER_PAGE, value)
    suspend fun setBlurThreshold(value: Float) = put(Keys.BLUR_THRESHOLD, value)
    suspend fun setDuplicateThreshold(value: Int) = put(Keys.DUPLICATE_THRESHOLD, value)
    suspend fun setLectureNumberWidth(value: Int) = put(Keys.LECTURE_NUMBER_WIDTH, value)
    suspend fun setTheme(value: String) {
        put(Keys.THEME, value)
        writeBootTheme(context, value)
    }
    suspend fun setLanguage(value: String) = put(Keys.LANGUAGE, value)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    companion object {
        private const val BOOT_PREFS = "boot"
        private const val BOOT_THEME_KEY = "theme"

        fun bootTheme(context: Context): String =
            context.getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE)
                .getString(BOOT_THEME_KEY, SettingsDefaults.THEME) ?: SettingsDefaults.THEME

        fun writeBootTheme(context: Context, value: String) {
            context.getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(BOOT_THEME_KEY, value)
                .apply()
        }
    }

    private object Keys {
        val OUTPUT_FORMAT = stringPreferencesKey("output_format")
        val JPEG_QUALITY = intPreferencesKey("jpeg_quality")
        val PDF_DPI = intPreferencesKey("pdf_dpi")
        val PHOTOS_PER_PAGE = intPreferencesKey("photos_per_page")
        val BLUR_THRESHOLD = floatPreferencesKey("blur_threshold")
        val DUPLICATE_THRESHOLD = intPreferencesKey("duplicate_threshold")
        val LECTURE_NUMBER_WIDTH = intPreferencesKey("lecture_number_width")
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
    }
}

object SettingsDefaults {
    const val OUTPUT_FORMAT = "{predmet}_{YYYYMMDD}"
    const val JPEG_QUALITY = 70
    const val PDF_DPI = 144
    const val PHOTOS_PER_PAGE = 2
    const val BLUR_THRESHOLD = 100f
    const val DUPLICATE_THRESHOLD = 10
    const val LECTURE_NUMBER_WIDTH = 3
    const val THEME = "light"
    const val LANGUAGE = "ru"
}

data class SettingsState(
    val outputFormat: String = SettingsDefaults.OUTPUT_FORMAT,
    val jpegQuality: Int = SettingsDefaults.JPEG_QUALITY,
    val pdfDpi: Int = SettingsDefaults.PDF_DPI,
    val photosPerPage: Int = SettingsDefaults.PHOTOS_PER_PAGE,
    val blurThreshold: Float = SettingsDefaults.BLUR_THRESHOLD,
    val duplicateThreshold: Int = SettingsDefaults.DUPLICATE_THRESHOLD,
    val lectureNumberWidth: Int = SettingsDefaults.LECTURE_NUMBER_WIDTH,
    val theme: String = SettingsDefaults.THEME,
    val language: String = SettingsDefaults.LANGUAGE,
)
