package com.kebuz.kebuzlect.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConvertedHashDao {

    @Query("SELECT groupHash FROM converted_hashes WHERE bucketId = :bucketId")
    fun observeHashesForAlbum(bucketId: String): Flow<List<String>>

    @Query("SELECT groupHash FROM converted_hashes WHERE bucketId = :bucketId")
    suspend fun getHashesForAlbum(bucketId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markConverted(entry: ConvertedHashEntity)

    @Query("DELETE FROM converted_hashes WHERE bucketId = :bucketId")
    suspend fun clearForAlbum(bucketId: String)
}
