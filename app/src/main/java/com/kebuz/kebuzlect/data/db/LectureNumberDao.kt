package com.kebuz.kebuzlect.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LectureNumberDao {

    @Query("SELECT * FROM lecture_numbers WHERE bucketId = :bucketId")
    suspend fun getForAlbum(bucketId: String): List<LectureNumberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LectureNumberEntity)

    @Query("DELETE FROM lecture_numbers WHERE bucketId = :bucketId")
    suspend fun clearForAlbum(bucketId: String)
}
