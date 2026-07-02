package com.kebuz.kebuzlect.data.scanner

private val DATE_RUN = Regex("""\d{8}""")

val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png")

fun isImageFilename(filename: String): Boolean {
    val lower = filename.lowercase()
    return IMAGE_EXTENSIONS.any { lower.endsWith(it) }
}

fun parseDateFromFilename(filename: String): String? {
    for (match in DATE_RUN.findAll(filename)) {
        val candidate = match.value
        val month = candidate.substring(4, 6).toInt()
        val day = candidate.substring(6, 8).toInt()
        if (month in 1..12 && day in 1..31) {
            return candidate
        }
    }
    return null
}
