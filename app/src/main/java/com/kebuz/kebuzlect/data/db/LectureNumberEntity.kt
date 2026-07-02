package com.kebuz.kebuzlect.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lecture_numbers",
    indices = [Index(value = ["bucketId", "groupHash"], unique = true)],
)
data class LectureNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bucketId: String,
    val groupHash: String,
    val number: Int,
)
