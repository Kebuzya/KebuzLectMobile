package com.kebuz.kebuzlect.data.db

import androidx.room.Entity

@Entity(tableName = "processed_dates", primaryKeys = ["bucketId", "date"])
data class ProcessedDateEntity(
    val bucketId: String,
    val date: String,
)
