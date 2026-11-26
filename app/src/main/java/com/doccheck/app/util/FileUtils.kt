package com.doccheck.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

sealed class ImageValidationResult {
    data class Valid(val uri: Uri, val sizeBytes: Long, val mimeType: String) : ImageValidationResult()
    data class InvalidType(val mimeType: String?) : ImageValidationResult()
    data class TooLarge(val sizeBytes: Long) : ImageValidationResult()
}

object FileUtils {

    fun validatePickedImage(context: Context, uri: Uri): ImageValidationResult {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)?.lowercase()
        if (mimeType == null || mimeType !in Constants.ALLOWED_MIME_TYPES) {
            return ImageValidationResult.InvalidType(mimeType)
        }

        var sizeBytes = 0L
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        }

        if (sizeBytes <= 0L) {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                if (afd.length > 0L) sizeBytes = afd.length
            }
        }

        if (sizeBytes > Constants.MAX_IMAGE_SIZE_BYTES) {
            return ImageValidationResult.TooLarge(sizeBytes)
        }

        return ImageValidationResult.Valid(uri, sizeBytes, mimeType)
    }

    fun copyUriToTempFile(context: Context, uri: Uri, extension: String = "jpg"): File? {
        return runCatching {
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        }.getOrNull()
    }
}

