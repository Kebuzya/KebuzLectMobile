package com.kebuz.kebuzlect.ui.album

import com.kebuz.kebuzlect.data.model.Photo

data class PhotoUi(
    val photo: Photo,
    val selected: Boolean,
    val isBlurry: Boolean,
    val isDuplicate: Boolean,
    val rotation: Int = 0,
)

data class ViewerItem(
    val photo: Photo,
    val displayDate: String,
    val rotation: Int,
    val selected: Boolean,
)

data class GroupUi(
    val date: String,
    val displayDate: String,
    val isConverted: Boolean,
    val photos: List<PhotoUi>,
    val lectureNumber: Int?,
) {
    val allSelected: Boolean get() = photos.isNotEmpty() && photos.all { it.selected }
}

data class AlbumSessionSettings(
    val blurThreshold: Float,
    val duplicateThreshold: Int,
    val jpegQuality: Int,
    val pdfDpi: Int,
    val photosPerPage: Int,
)

data class AlbumUiState(
    val loading: Boolean = true,
    val analyzing: Boolean = false,
    val title: String = "",
    val groups: List<GroupUi> = emptyList(),
    val settings: AlbumSessionSettings? = null,
    val viewerOpen: Boolean = false,
    val viewerInitialIndex: Int = 0,
    val viewerItems: List<ViewerItem> = emptyList(),
    val outputUriSet: Boolean = false,
    val converting: Boolean = false,
    val convertDone: Int = 0,
    val convertTotal: Int = 0,
    val message: String? = null,
)
