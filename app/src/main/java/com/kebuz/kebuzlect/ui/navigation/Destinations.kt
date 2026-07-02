package com.kebuz.kebuzlect.ui.navigation

object Destinations {
    const val ALBUMS = "albums"
    const val BUCKET_PICKER = "bucket_picker"
    const val SETTINGS = "settings"

    const val ALBUM_ARG_BUCKET_ID = "bucketId"
    const val ALBUM = "album/{$ALBUM_ARG_BUCKET_ID}"

    fun album(bucketId: String): String = "album/$bucketId"
}
