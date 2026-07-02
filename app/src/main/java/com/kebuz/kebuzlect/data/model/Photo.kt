package com.kebuz.kebuzlect.data.model

import android.net.Uri

data class Photo(
    val mediaStoreId: Long,
    val uri: Uri,
    val filename: String,
    val dateTaken: Long,
    val size: Long,
    var isBlurry: Boolean = false,
    var isDuplicate: Boolean = false,
    var rotation: Int = 0,
    var isSelected: Boolean = true,
)
