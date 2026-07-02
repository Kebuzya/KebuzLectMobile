package com.kebuz.kebuzlect.data.model

data class LectureGroup(
    val date: String,
    val photos: List<Photo>,
    var isConverted: Boolean = false,
    var lectureNumber: Int? = null,
)
