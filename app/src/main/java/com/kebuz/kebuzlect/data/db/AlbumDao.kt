package com.kebuz.kebuzlect.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY sortOrder, displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<AlbumEntity>>

    @Query("SELECT MAX(sortOrder) FROM albums")
    suspend fun maxSortOrder(): Int?

    @Query("UPDATE albums SET sortOrder = :sortOrder WHERE bucketId = :bucketId")
    suspend fun updateSortOrder(bucketId: String, sortOrder: Int)

    @Query("SELECT * FROM albums WHERE bucketId = :bucketId")
    suspend fun findByBucketId(bucketId: String): AlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(album: AlbumEntity)

    @Query("UPDATE albums SET outputUri = :outputUri WHERE bucketId = :bucketId")
    suspend fun updateOutputUri(bucketId: String, outputUri: String)

    @Query("DELETE FROM albums WHERE bucketId = :bucketId")
    suspend fun deleteByBucketId(bucketId: String)
}
