package com.kebuz.kebuzlect.data.scanner

import com.kebuz.kebuzlect.data.db.ConvertedHashDao
import com.kebuz.kebuzlect.data.media.MediaStoreSource
import com.kebuz.kebuzlect.data.model.LectureGroup
import com.kebuz.kebuzlect.data.model.Photo

class AlbumScanner(
    private val mediaStore: MediaStoreSource,
    private val convertedHashDao: ConvertedHashDao,
) {

    suspend fun scan(bucketId: String): List<LectureGroup> {
        val photos = mediaStore.listPhotos(bucketId)
        val converted = convertedHashDao.getHashesForAlbum(bucketId).toSet()
        return groupPhotos(photos, converted)
    }

    companion object {
        fun groupPhotos(photos: List<Photo>, convertedHashes: Set<String>): List<LectureGroup> {
            val photosByDate = LinkedHashMap<String, MutableList<Photo>>()
            for (photo in photos) {
                if (!isImageFilename(photo.filename)) continue
                val date = parseDateFromFilename(photo.filename) ?: continue
                photosByDate.getOrPut(date) { mutableListOf() }.add(photo)
            }
            return photosByDate.keys.sorted().map { date ->
                val groupPhotos = photosByDate.getValue(date)
                    .sortedWith(compareBy({ it.dateTaken }, { it.filename }))
                val hash = computeGroupHash(groupPhotos.map { it.filename })
                LectureGroup(
                    date = date,
                    photos = groupPhotos,
                    isConverted = hash in convertedHashes,
                )
            }
        }
    }
}
