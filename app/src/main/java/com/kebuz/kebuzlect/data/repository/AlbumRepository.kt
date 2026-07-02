package com.kebuz.kebuzlect.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.kebuz.kebuzlect.data.db.AlbumEntity
import com.kebuz.kebuzlect.data.db.ConvertedHashEntity
import com.kebuz.kebuzlect.data.db.KebuzDatabase
import com.kebuz.kebuzlect.data.db.LectureNumberEntity
import com.kebuz.kebuzlect.data.db.ProcessedDateEntity
import com.kebuz.kebuzlect.data.media.MediaStoreSource
import com.kebuz.kebuzlect.data.media.PhotoBucket
import com.kebuz.kebuzlect.data.model.LectureGroup
import com.kebuz.kebuzlect.data.scanner.AlbumScanner
import com.kebuz.kebuzlect.data.scanner.computeGroupHash
import com.kebuz.kebuzlect.data.settings.AppSettings
import com.kebuz.kebuzlect.data.settings.SettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class LectureCounts(val total: Int, val new: Int)

class AlbumRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = KebuzDatabase.get(appContext)
    private val albumDao = db.albumDao()
    private val settings = AppSettings(appContext)
    private val mediaStore = MediaStoreSource(appContext)
    private val scanner = AlbumScanner(mediaStore, db.convertedHashDao())
    private val lectureNumberDao = db.lectureNumberDao()
    private val processedDateDao = db.processedDateDao()

    fun observeAlbums(): Flow<List<AlbumEntity>> = albumDao.observeAll()

    suspend fun getAlbum(bucketId: String): AlbumEntity? = albumDao.findByBucketId(bucketId)

    suspend fun globalSettings(): SettingsState = settings.state.first()

    suspend fun listBuckets(): List<PhotoBucket> = mediaStore.listBuckets()

    suspend fun addAlbum(bucketId: String, displayName: String) {
        if (albumDao.findByBucketId(bucketId) != null) return
        val defaults = settings.state.first()
        albumDao.upsert(
            AlbumEntity(
                bucketId = bucketId,
                displayName = displayName,
                outputUri = "",
                outputFormat = defaults.outputFormat,
                jpegQuality = defaults.jpegQuality,
                pdfDpi = defaults.pdfDpi,
                photosPerPage = defaults.photosPerPage,
                sortOrder = (albumDao.maxSortOrder() ?: -1) + 1,
            ),
        )
    }

    suspend fun removeAlbum(bucketId: String) = albumDao.deleteByBucketId(bucketId)

    suspend fun reorderAlbums(orderedBucketIds: List<String>) {
        db.withTransaction {
            orderedBucketIds.forEachIndexed { index, bucketId ->
                albumDao.updateSortOrder(bucketId, index)
            }
        }
    }

    suspend fun setOutputUri(bucketId: String, outputUri: String) =
        albumDao.updateOutputUri(bucketId, outputUri)

    suspend fun markConverted(bucketId: String, groupHash: String) =
        db.convertedHashDao().markConverted(ConvertedHashEntity(bucketId = bucketId, groupHash = groupHash))

    suspend fun scan(bucketId: String): List<LectureGroup> =
        assignNumbers(bucketId, scanner.scan(bucketId))

    suspend fun bucketNames(): Map<String, String> =
        mediaStore.listBuckets().associate { it.id to it.displayName }

    suspend fun countLectures(bucketId: String): LectureCounts {
        val groups = scanner.scan(bucketId)
        val processed = processedDateDao.getDatesForAlbum(bucketId).toSet()
        val new = groups.count { it.date !in processed && !it.isConverted }
        return LectureCounts(total = groups.size, new = new)
    }

    suspend fun markDatesProcessed(bucketId: String, dates: List<String>) {
        if (dates.isEmpty()) return
        processedDateDao.markProcessed(dates.distinct().map { ProcessedDateEntity(bucketId, it) })
    }

    suspend fun renumberAll(bucketId: String, groups: List<LectureGroup>): List<LectureGroup> {
        lectureNumberDao.clearForAlbum(bucketId)
        return groups.mapIndexed { index, group ->
            val number = index + 1
            lectureNumberDao.upsert(
                LectureNumberEntity(
                    bucketId = bucketId,
                    groupHash = computeGroupHash(group.photos.map { it.filename }),
                    number = number,
                ),
            )
            group.copy(lectureNumber = number)
        }
    }

    private suspend fun assignNumbers(bucketId: String, groups: List<LectureGroup>): List<LectureGroup> {
        val existing = lectureNumberDao.getForAlbum(bucketId).associate { it.groupHash to it.number }
        var maxNumber = existing.values.maxOrNull() ?: 0
        return groups.map { group ->
            val hash = computeGroupHash(group.photos.map { it.filename })
            val number = existing[hash] ?: run {
                maxNumber += 1
                lectureNumberDao.upsert(LectureNumberEntity(bucketId = bucketId, groupHash = hash, number = maxNumber))
                maxNumber
            }
            group.copy(lectureNumber = number)
        }
    }
}
