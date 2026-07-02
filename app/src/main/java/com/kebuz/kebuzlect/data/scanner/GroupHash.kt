package com.kebuz.kebuzlect.data.scanner

import java.security.MessageDigest

fun computeGroupHash(filenames: Collection<String>): String {
    val joined = filenames.sorted().joinToString("\n")
    val digest = MessageDigest.getInstance("MD5").digest(joined.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
