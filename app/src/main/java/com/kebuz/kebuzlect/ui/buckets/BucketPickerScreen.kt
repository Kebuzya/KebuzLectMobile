package com.kebuz.kebuzlect.ui.buckets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kebuz.kebuzlect.R
import com.kebuz.kebuzlect.ui.common.BarIcon
import com.kebuz.kebuzlect.ui.common.RequireReadPermission
import com.kebuz.kebuzlect.ui.common.kebuz
import com.kebuz.kebuzlect.ui.theme.Manrope

@Composable
fun BucketPickerScreen(
    onAdded: () -> Unit,
    onBack: () -> Unit,
    viewModel: BucketPickerViewModel = viewModel(),
) {
    val colors = kebuz()
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarIcon(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
                tint = colors.foreground,
                size = 22,
            )
            Text(
                text = stringResource(R.string.bucket_picker_title),
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
                color = colors.foreground,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            RequireReadPermission {
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { viewModel.load() }

                Column(modifier = Modifier.fillMaxSize()) {
                    SearchField(value = state.query, onValueChange = viewModel::setQuery)
                    HorizontalDivider(color = colors.divider)

                    val query = state.query.trim()
                    val filtered = if (query.isEmpty()) {
                        state.buckets
                    } else {
                        state.buckets.filter { it.displayName.contains(query, ignoreCase = true) }
                    }

                    when {
                        state.loading -> Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = colors.cyan)
                        }

                        state.buckets.isEmpty() -> Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(R.string.bucket_empty), color = colors.muted)
                        }

                        filtered.isEmpty() -> Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(R.string.bucket_search_empty), color = colors.muted)
                        }

                        else -> LazyColumn(Modifier.fillMaxSize()) {
                            items(filtered, key = { it.id }) { bucket ->
                                FolderRow(
                                    name = bucket.displayName,
                                    count = stringResource(R.string.bucket_photo_count, bucket.photoCount),
                                    added = bucket.id in state.addedIds,
                                    onClick = { viewModel.addAlbum(bucket) { onAdded() } },
                                )
                                HorizontalDivider(
                                    color = colors.divider,
                                    modifier = Modifier.padding(start = 78.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val colors = kebuz()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.cyan,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.bucket_search_hint),
                    fontFamily = Manrope,
                    fontSize = 16.sp,
                    color = colors.muted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = Manrope,
                    fontSize = 16.sp,
                    color = colors.foreground,
                ),
                cursorBrush = SolidColor(colors.cyan),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            BarIcon(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.action_clear_search),
                onClick = { onValueChange("") },
                tint = colors.muted,
                size = 18,
            )
        }
    }
}

@Composable
private fun FolderRow(name: String, count: String, added: Boolean, onClick: () -> Unit) {
    val colors = kebuz()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !added) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(colors.tile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = name,
                fontFamily = Manrope,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = if (added) colors.muted else colors.foreground,
            )
            Text(
                text = if (added) stringResource(R.string.bucket_added) else count,
                fontFamily = Manrope,
                fontSize = 14.sp,
                color = if (added) colors.green else colors.muted,
            )
        }
        if (added) {
            Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.bucket_added),
                tint = colors.green,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
