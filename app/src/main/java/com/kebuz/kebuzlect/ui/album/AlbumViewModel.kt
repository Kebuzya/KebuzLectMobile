package com.kebuz.kebuzlect.ui.album

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.kebuz.kebuzlect.R
import com.kebuz.kebuzlect.data.analyzer.PhotoAnalyzer
import com.kebuz.kebuzlect.data.model.LectureGroup
import com.kebuz.kebuzlect.data.model.Photo
import com.kebuz.kebuzlect.data.pdf.ConvertPhoto
import com.kebuz.kebuzlect.data.pdf.PdfConverter
import com.kebuz.kebuzlect.data.repository.AlbumRepository
import com.kebuz.kebuzlect.data.scanner.computeGroupHash
import com.kebuz.kebuzlect.data.storage.StorageCompat
import com.kebuz.kebuzlect.data.storage.StorageCompat.DeleteResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class AlbumViewModel(
    application: Application,
    private val bucketId: String,
) : AndroidViewModel(application) {

    private val app = application
    private val repository = AlbumRepository(application)
    private val analyzer = PhotoAnalyzer(application)
    private val converter = PdfConverter(application)

    private var scanned: List<LectureGroup> = emptyList()
    private val varianceById = HashMap<Long, Double>()
    private val phashById = HashMap<Long, Long?>()
    private val deselected = HashSet<Long>()
    private val rotationById = HashMap<Long, Int>()
    private var outputUri: String = ""

    private val _state = MutableStateFlow(AlbumUiState())
    val state: StateFlow<AlbumUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val album = repository.getAlbum(bucketId)
                val global = repository.globalSettings()
                val settings = AlbumSessionSettings(
                    blurThreshold = global.blurThreshold,
                    duplicateThreshold = global.duplicateThreshold,
                    jpegQuality = album?.jpegQuality ?: global.jpegQuality,
                    pdfDpi = album?.pdfDpi ?: global.pdfDpi,
                    photosPerPage = album?.photosPerPage ?: global.photosPerPage,
                )

                outputUri = album?.outputUri.orEmpty()
                scanned = repository.scan(bucketId)
                _state.value = AlbumUiState(
                    loading = false,
                    analyzing = true,
                    title = album?.displayName.orEmpty(),
                    settings = settings,
                    groups = deriveGroups(settings),
                    viewerItems = deriveViewerItems(),
                    outputUriSet = outputUri.isNotBlank(),
                )

                for (group in scanned) {
                    for (photo in group.photos) {
                        runCatching {
                            varianceById[photo.mediaStoreId] = analyzer.laplacianVariance(photo)
                            phashById[photo.mediaStoreId] = analyzer.perceptualHash(photo)
                        }
                    }
                }
                _state.value = _state.value.copy(analyzing = false).renderDerived()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                _state.value = _state.value.copy(
                    loading = false,
                    analyzing = false,
                    message = app.getString(R.string.error_no_access),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    analyzing = false,
                    message = app.getString(R.string.error_load_failed),
                )
            }
        }
    }

    fun togglePhoto(mediaStoreId: Long) {
        if (!deselected.add(mediaStoreId)) deselected.remove(mediaStoreId)
        _state.value = _state.value.renderDerived()
    }

    fun toggleGroup(date: String) {
        val group = scanned.firstOrNull { it.date == date } ?: return
        val ids = group.photos.map { it.mediaStoreId }
        val allSelected = ids.none { it in deselected }
        if (allSelected) deselected.addAll(ids) else deselected.removeAll(ids.toSet())
        _state.value = _state.value.renderDerived()
    }

    fun setBlurThreshold(value: Float) = updateSettings { it.copy(blurThreshold = value) }
    fun setDuplicateThreshold(value: Int) = updateSettings { it.copy(duplicateThreshold = value) }
    fun setJpegQuality(value: Int) = updateSettings { it.copy(jpegQuality = value) }
    fun setPdfDpi(value: Int) = updateSettings { it.copy(pdfDpi = value) }
    fun setPhotosPerPage(value: Int) = updateSettings { it.copy(photosPerPage = value) }

    fun openViewer(mediaStoreId: Long) {
        val index = _state.value.viewerItems.indexOfFirst { it.photo.mediaStoreId == mediaStoreId }
        if (index < 0) return
        _state.value = _state.value.copy(viewerOpen = true, viewerInitialIndex = index)
    }

    fun closeViewer() {
        _state.value = _state.value.copy(viewerOpen = false)
    }

    fun rotate(mediaStoreId: Long, deltaDegrees: Int) {
        val current = rotationById[mediaStoreId] ?: 0
        rotationById[mediaStoreId] = ((current + deltaDegrees) % 360 + 360) % 360
        _state.value = _state.value.renderDerived()
    }

    fun renumber() {
        viewModelScope.launch {
            scanned = repository.renumberAll(bucketId, scanned)
            _state.value = _state.value.renderDerived()
        }
    }

    fun movePhoto(fromId: Long, toDate: String, toIndex: Int) {
        val sourceGroup = scanned.firstOrNull { group ->
            group.photos.any { it.mediaStoreId == fromId }
        } ?: return
        val sourceIndex = sourceGroup.photos.indexOfFirst { it.mediaStoreId == fromId }
        val photo = sourceGroup.photos[sourceIndex]
        val sameGroup = sourceGroup.date == toDate
        if (sameGroup && (toIndex == sourceIndex || toIndex == sourceIndex + 1)) return
        scanned = scanned.map { group ->
            val without = group.photos.filterNot { it.mediaStoreId == fromId }
            if (group.date == toDate) {
                val insertAt = (if (sameGroup && sourceIndex < toIndex) toIndex - 1 else toIndex)
                    .coerceIn(0, without.size)
                group.copy(photos = without.toMutableList().apply { add(insertAt, photo) })
            } else {
                group.copy(photos = without)
            }
        }.filter { it.photos.isNotEmpty() }
        _state.value = _state.value.renderDerived()
    }

    fun requestDelete(ids: List<Long>): DeleteResult {
        val uris = ids.mapNotNull { uriOf(it) }
        val result = StorageCompat.deletePhotos(app, uris)
        if (result is DeleteResult.Deleted) applyDeletion(ids)
        return result
    }

    fun onDeleteConfirmed(ids: List<Long>) {
        val uris = ids.mapNotNull { uriOf(it) }
        StorageCompat.finalizeDeletion(app, uris)
        applyDeletion(ids)
    }

    private fun applyDeletion(ids: List<Long>) {
        val removed = ids.toSet()
        scanned = scanned
            .map { group -> group.copy(photos = group.photos.filterNot { it.mediaStoreId in removed }) }
            .filter { it.photos.isNotEmpty() }
        removed.forEach {
            varianceById.remove(it)
            phashById.remove(it)
            rotationById.remove(it)
            deselected.remove(it)
        }
        val items = deriveViewerItems()
        _state.value = _state.value.copy(
            viewerOpen = _state.value.viewerOpen && items.isNotEmpty(),
        ).renderDerived()
    }

    private fun uriOf(mediaStoreId: Long): Uri? =
        scanned.firstNotNullOfOrNull { group ->
            group.photos.firstOrNull { it.mediaStoreId == mediaStoreId }?.uri
        }

    fun onOutputFolderChosen(treeUriString: String) {
        viewModelScope.launch {
            repository.setOutputUri(bucketId, treeUriString)
            outputUri = treeUriString
            _state.value = _state.value.copy(outputUriSet = true)
            convertSelected()
        }
    }

    fun convertSelected() {
        if (_state.value.converting) return
        val settings = _state.value.settings ?: return
        if (outputUri.isBlank()) return

        viewModelScope.launch {
            val album = repository.getAlbum(bucketId)
            val global = repository.globalSettings()
            val outputFormat = album?.outputFormat?.ifBlank { global.outputFormat } ?: global.outputFormat
            val numberWidth = global.lectureNumberWidth
            val displayName = _state.value.title

            val targets = scanned.filter { group -> group.photos.any { it.mediaStoreId !in deselected } }
            if (targets.isEmpty()) {
                _state.value = _state.value.copy(message = app.getString(R.string.convert_nothing_selected))
                return@launch
            }

            _state.value = _state.value.copy(converting = true, convertDone = 0, convertTotal = targets.size)
            var succeeded = 0
            var outOfSpace = false
            withContext(Dispatchers.IO) {
                for (group in targets) {
                    val photos = group.photos
                        .filter { it.mediaStoreId !in deselected }
                        .map { ConvertPhoto(it.uri, rotationById[it.mediaStoreId] ?: 0) }
                    val filename = PdfConverter.buildFilename(
                        displayName = displayName,
                        date = group.date,
                        lectureNumber = group.lectureNumber,
                        outputFormat = outputFormat,
                        lectureNumberWidth = numberWidth,
                    )
                    val result = runCatching {
                        val out = StorageCompat.createPdfInTree(app, outputUri, filename)
                            ?: error("Could not create output file")
                        out.use {
                            converter.writeLecture(photos, settings.pdfDpi, settings.jpegQuality, settings.photosPerPage, it)
                        }
                        repository.markConverted(bucketId, computeGroupHash(group.photos.map { p -> p.filename }))
                    }
                    result.onSuccess {
                        succeeded++
                        markGroupConverted(group.date)
                    }.onFailure { e ->
                        if (isOutOfSpace(e)) outOfSpace = true
                    }
                    _state.value = _state.value.copy(convertDone = _state.value.convertDone + 1)
                }
            }
            if (succeeded > 0) {
                repository.markDatesProcessed(bucketId, scanned.map { it.date })
            }
            val failed = targets.size - succeeded
            val message = when {
                outOfSpace -> app.getString(R.string.convert_no_space)
                failed > 0 -> app.getString(R.string.convert_some_failed, succeeded, targets.size, failed)
                else -> app.getString(R.string.convert_done, succeeded, targets.size)
            }
            _state.value = _state.value.copy(
                converting = false,
                message = message,
            ).renderDerived()
        }
    }

    fun consumeMessage() {
        if (_state.value.message != null) _state.value = _state.value.copy(message = null)
    }

    private fun markGroupConverted(date: String) {
        scanned = scanned.map { if (it.date == date) it.copy(isConverted = true) else it }
    }

    private fun isOutOfSpace(error: Throwable): Boolean {
        var cause: Throwable? = error
        while (cause != null) {
            val message = cause.message ?: ""
            if (cause is IOException &&
                (message.contains("ENOSPC", ignoreCase = true) || message.contains("No space", ignoreCase = true))
            ) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    private fun updateSettings(transform: (AlbumSessionSettings) -> AlbumSessionSettings) {
        val current = _state.value.settings ?: return
        _state.value = _state.value.copy(settings = transform(current)).renderDerived()
    }

    private fun AlbumUiState.renderDerived(): AlbumUiState {
        val s = settings ?: return this
        return copy(groups = deriveGroups(s), viewerItems = deriveViewerItems())
    }

    private fun deriveGroups(settings: AlbumSessionSettings): List<GroupUi> =
        scanned.map { group ->
            val duplicateIds = duplicateIds(group.photos, settings.duplicateThreshold)
            val photos = group.photos.map { photo ->
                PhotoUi(
                    photo = photo,
                    selected = photo.mediaStoreId !in deselected,

                    isBlurry = PhotoAnalyzer.isBlurry(
                        varianceById[photo.mediaStoreId] ?: Double.MAX_VALUE,
                        settings.blurThreshold,
                    ),
                    isDuplicate = photo.mediaStoreId in duplicateIds,
                    rotation = rotationById[photo.mediaStoreId] ?: 0,
                )
            }
            GroupUi(
                date = group.date,
                displayDate = formatDate(group.date),
                isConverted = group.isConverted,
                photos = photos,
                lectureNumber = group.lectureNumber,
            )
        }

    private fun deriveViewerItems(): List<ViewerItem> =
        scanned.flatMap { group ->
            group.photos.map { photo ->
                ViewerItem(
                    photo = photo,
                    displayDate = formatDate(group.date),
                    rotation = rotationById[photo.mediaStoreId] ?: 0,
                    selected = photo.mediaStoreId !in deselected,
                )
            }
        }

    private fun duplicateIds(photos: List<Photo>, threshold: Int): Set<Long> {
        val kept = ArrayList<Long>()
        val duplicates = HashSet<Long>()
        for (photo in photos) {
            val hash = phashById[photo.mediaStoreId] ?: continue
            if (kept.any { PhotoAnalyzer.hammingDistance(it, hash) <= threshold }) {
                duplicates.add(photo.mediaStoreId)
            } else {
                kept.add(hash)
            }
        }
        return duplicates
    }

    private fun formatDate(yyyymmdd: String): String =
        if (yyyymmdd.length == 8) "${yyyymmdd.substring(0, 4)}-${yyyymmdd.substring(4, 6)}-${yyyymmdd.substring(6, 8)}"
        else yyyymmdd

    companion object {
        fun factory(bucketId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                @Suppress("UNCHECKED_CAST")
                return AlbumViewModel(app, bucketId) as T
            }
        }
    }
}
