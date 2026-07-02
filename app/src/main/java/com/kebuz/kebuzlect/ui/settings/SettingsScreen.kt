package com.kebuz.kebuzlect.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kebuz.kebuzlect.R
import com.kebuz.kebuzlect.ui.common.AccentMark
import com.kebuz.kebuzlect.ui.common.BarIcon
import com.kebuz.kebuzlect.ui.common.kebuz
import com.kebuz.kebuzlect.ui.theme.MonoCaps
import com.kebuz.kebuzlect.ui.theme.MonoMeta
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = kebuz()

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 6.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarIcon(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
                tint = colors.foreground,
            )
            AccentMark(colors.neonCyan)
            Text(
                text = stringResource(R.string.settings_title).uppercase(),
                style = MonoCaps,
                color = colors.foreground,
                modifier = Modifier.padding(start = 11.dp),
            )
        }
        HorizontalDivider(color = colors.divider)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
        ) {
            OutlinedTextField(
                value = state.outputFormat,
                onValueChange = viewModel::setOutputFormat,
                label = { Text(stringResource(R.string.setting_output_format)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            LabeledSlider(
                label = stringResource(R.string.setting_blur_threshold, state.blurThreshold.roundToInt()),
                value = state.blurThreshold,
                valueRange = 0f..300f,
                onValueChange = viewModel::setBlurThreshold,
            )
            LabeledSlider(
                label = stringResource(R.string.setting_duplicate_threshold, state.duplicateThreshold),
                value = state.duplicateThreshold.toFloat(),
                valueRange = 0f..20f,
                steps = 19,
                onValueChange = { viewModel.setDuplicateThreshold(it.roundToInt()) },
            )
            LabeledSlider(
                label = stringResource(R.string.setting_jpeg_quality, state.jpegQuality),
                value = state.jpegQuality.toFloat(),
                valueRange = 1f..100f,
                onValueChange = { viewModel.setJpegQuality(it.roundToInt()) },
            )
            LabeledSlider(
                label = stringResource(R.string.setting_pdf_dpi, state.pdfDpi),
                value = state.pdfDpi.toFloat(),
                valueRange = 72f..300f,
                onValueChange = { viewModel.setPdfDpi(it.roundToInt()) },
            )
            LabeledSlider(
                label = stringResource(R.string.setting_lecture_number_width, state.lectureNumberWidth),
                value = state.lectureNumberWidth.toFloat(),
                valueRange = 1f..6f,
                steps = 4,
                onValueChange = { viewModel.setLectureNumberWidth(it.roundToInt()) },
            )

            ChoiceRow(label = stringResource(R.string.setting_photos_per_page)) {
                Choice(state.photosPerPage == 1, { viewModel.setPhotosPerPage(1) }, "1")
                Choice(state.photosPerPage == 2, { viewModel.setPhotosPerPage(2) }, "2")
            }

            ChoiceRow(label = stringResource(R.string.setting_theme)) {
                Choice(state.theme == "system", { viewModel.setTheme("system") }, stringResource(R.string.theme_system))
                Choice(state.theme == "light", { viewModel.setTheme("light") }, stringResource(R.string.theme_light))
                Choice(state.theme == "dark", { viewModel.setTheme("dark") }, stringResource(R.string.theme_dark))
            }

            ChoiceRow(label = stringResource(R.string.setting_language)) {
                Choice(state.language == "ru", { viewModel.setLanguage("ru") }, "Русский")
                Choice(state.language == "en", { viewModel.setLanguage("en") }, "English")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(end = 8.dp),
    )
}

@Composable
private fun ChoiceRow(label: String, content: @Composable () -> Unit) {
    val colors = kebuz()
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(label, style = MonoMeta, color = colors.muted)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
) {
    val colors = kebuz()
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(label, style = MonoMeta, color = colors.muted)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps)
    }
}
