package com.kebuz.kebuzlect.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "converted_hashes",
    indices = [Index(value = ["bucketId", "groupHash"], unique = true)],
)
data class ConvertedHashEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bucketId: String,
    val groupHash: String,
)
