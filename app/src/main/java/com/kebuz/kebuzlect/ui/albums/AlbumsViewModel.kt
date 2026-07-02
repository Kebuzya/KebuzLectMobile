package com.kebuz.kebuzlect.ui.albums

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kebuz.kebuzlect.data.db.AlbumEntity
import com.kebuz.kebuzlect.data.repository.AlbumRepository
import com.kebuz.kebuzlect.data.repository.LectureCounts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlbumCard(
    val bucketId: String,
    val name: String,
    val source: String,
    val lectureCount: Int,
    val newCount: Int,
    val loadingCounts: Boolean,
)

class AlbumsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlbumRepository(application)

    private val _cards = MutableStateFlow<List<AlbumCard>>(emptyList())
    val cards: StateFlow<List<AlbumCard>> = _cards.asStateFlow()

    private var current: List<AlbumEntity> = emptyList()
    private val countsById = HashMap<String, LectureCounts>()

    init {
        viewModelScope.launch {
            repository.observeAlbums().collect { albums ->
                current = albums
                rebuild(albums, recountAll = false)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { rebuild(current, recountAll = true) }
    }

    fun moveAlbum(bucketId: String, toIndex: Int) {
        val list = _cards.value
        val fromIndex = list.indexOfFirst { it.bucketId == bucketId }
        if (fromIndex < 0) return
        if (toIndex == fromIndex || toIndex == fromIndex + 1) return
        val insertAt = (if (fromIndex < toIndex) toIndex - 1 else toIndex).coerceIn(0, list.size - 1)
        val reordered = list.toMutableList().apply { add(insertAt, removeAt(fromIndex)) }
        _cards.value = reordered
        viewModelScope.launch { repository.reorderAlbums(reordered.map { it.bucketId }) }
    }

    fun removeAlbum(bucketId: String) {
        countsById.remove(bucketId)
        viewModelScope.launch { repository.removeAlbum(bucketId) }
    }

    private suspend fun rebuild(albums: List<AlbumEntity>, recountAll: Boolean) {
        val names = runCatching { repository.bucketNames() }.getOrDefault(emptyMap())
        _cards.value = albums.map { album ->
            val cached = countsById[album.bucketId]
            AlbumCard(
                bucketId = album.bucketId,
                name = album.displayName,
                source = (names[album.bucketId] ?: "").uppercase(),
                lectureCount = cached?.total ?: 0,
                newCount = cached?.new ?: 0,
                loadingCounts = cached == null,
            )
        }
        val targets = if (recountAll) albums else albums.filter { countsById[it.bucketId] == null }
        for (album in targets) {
            val counts = runCatching { repository.countLectures(album.bucketId) }.getOrNull()
            if (counts != null) countsById[album.bucketId] = counts
            _cards.update { list ->
                list.map {
                    if (it.bucketId == album.bucketId) {
                        it.copy(
                            lectureCount = counts?.total ?: 0,
                            newCount = counts?.new ?: 0,
                            loadingCounts = false,
                        )
                    } else {
                        it
                    }
                }
            }
        }
    }
}
