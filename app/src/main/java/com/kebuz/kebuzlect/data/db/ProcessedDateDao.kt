package com.kebuz.kebuzlect.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProcessedDateDao {

    @Query("SELECT date FROM processed_dates WHERE bucketId = :bucketId")
    suspend fun getDatesForAlbum(bucketId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markProcessed(rows: List<ProcessedDateEntity>)
}
