package com.doccheck.app.util

object Constants {
    const val MAX_IMAGE_SIZE_BYTES = 25L * 1024L * 1024L // 25 MB
    val ALLOWED_MIME_TYPES = setOf("image/png", "image/jpeg", "image/jpg")
    const val BASE_SCANNED_COUNT = 142
    const val BASE_TAMPERED_COUNT = 11
    const val BASE_ACCURACY = 99.4
}

