package com.kebuz.kebuzlect.data.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.kebuz.kebuzlect.data.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhotoBucket(
    val id: String,
    val displayName: String,
    val photoCount: Int,
)

class MediaStoreSource(context: Context) {

    private val resolver = context.applicationContext.contentResolver
    private val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    suspend fun listBuckets(): List<PhotoBucket> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val counts = LinkedHashMap<String, MutableBucket>()
        resolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idCol) ?: continue
                val name = cursor.getString(nameCol) ?: ""
                val bucket = counts.getOrPut(id) { MutableBucket(id, name) }
                bucket.count++
            }
        }
        counts.values
            .map { PhotoBucket(it.id, it.name, it.count) }
            .sortedBy { it.displayName.lowercase() }
    }

    suspend fun listPhotos(bucketId: String): List<Photo> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
        )
        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val args = arrayOf(bucketId)
        val photos = ArrayList<Photo>()
        resolver.query(collection, projection, selection, args, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue

                val dateTaken = cursor.getLong(dateTakenCol)
                val dateModified = cursor.getLong(dateModifiedCol)
                val takenMillis = if (dateTaken > 0L) dateTaken else dateModified * 1000L
                photos += Photo(
                    mediaStoreId = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    filename = name,
                    dateTaken = takenMillis,
                    size = cursor.getLong(sizeCol),
                )
            }
        }
        photos
    }

    private class MutableBucket(val id: String, val name: String, var count: Int = 0)
}
