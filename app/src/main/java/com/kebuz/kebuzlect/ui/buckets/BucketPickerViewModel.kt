package com.kebuz.kebuzlect.ui.buckets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kebuz.kebuzlect.data.media.PhotoBucket
import com.kebuz.kebuzlect.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BucketPickerUiState(
    val loading: Boolean = true,
    val buckets: List<PhotoBucket> = emptyList(),
    val addedIds: Set<String> = emptySet(),
    val query: String = "",
)

class BucketPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlbumRepository(application)

    private val _state = MutableStateFlow(BucketPickerUiState())
    val state: StateFlow<BucketPickerUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val buckets = repository.listBuckets()
            val added = repository.observeAlbums().first().map { it.bucketId }.toSet()
            _state.update { it.copy(loading = false, buckets = buckets, addedIds = added) }
        }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun addAlbum(bucket: PhotoBucket, onAdded: () -> Unit) {
        viewModelScope.launch {
            repository.addAlbum(bucket.id, bucket.displayName)
            onAdded()
        }
    }
}
