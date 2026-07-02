package com.kebuz.kebuzlect.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val bucketId: String,
    val displayName: String,
    val outputUri: String,
    val outputFormat: String,
    val jpegQuality: Int,
    val pdfDpi: Int,
    val photosPerPage: Int,
    val sortOrder: Int,
)
