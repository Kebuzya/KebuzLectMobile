package com.kebuz.kebuzlect.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.math.min

data class ConvertPhoto(val uri: Uri, val rotation: Int)

class PdfConverter(context: Context) {

    private val resolver = context.applicationContext.contentResolver

    fun writeLecture(
        photos: List<ConvertPhoto>,
        dpi: Int,
        jpegQuality: Int,
        photosPerPage: Int,
        out: OutputStream,
    ) {
        val (slotPxW, slotPxH) = slotPixels(dpi)
        val images = photos.mapNotNull { prepare(it, slotPxW, slotPxH, jpegQuality) }
        if (images.isEmpty()) throw IOException("No readable photos in lecture")
        PdfWriter.write(out, images, photosPerPage)
    }

    private fun prepare(photo: ConvertPhoto, slotPxW: Int, slotPxH: Int, jpegQuality: Int): PdfImage? {
        val raw = decodeBounded(photo.uri, maxOf(slotPxW, slotPxH)) ?: return null
        val oriented = applyOrientation(photo.uri, raw, photo.rotation)
        val fitted = scaleToFit(oriented, slotPxW, slotPxH)
        val baos = ByteArrayOutputStream()
        fitted.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(1, 100), baos)
        val image = PdfImage(baos.toByteArray(), fitted.width, fitted.height)
        fitted.recycle()
        return image
    }

    private fun decodeBounded(uri: Uri, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > maxDim * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return runCatching {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()
    }

    private fun applyOrientation(uri: Uri, bitmap: Bitmap, userRotation: Int): Bitmap {
        val total = (exifRotationDegrees(uri) + userRotation) % 360
        if (total == 0) return bitmap
        val matrix = Matrix().apply { postRotate(total.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun exifRotationDegrees(uri: Uri): Int {
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun scaleToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val scale = min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        if (scale >= 1f) return bitmap
        val target = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (target != bitmap) bitmap.recycle()
        return target
    }

    private fun slotPixels(dpi: Int): Pair<Int, Int> {
        val pxW = (SLOT_WIDTH_PT * dpi / 72f).toInt()
        val pxH = (SLOT_HEIGHT_PT * dpi / 72f).toInt()
        return pxW to pxH
    }

    companion object {
        private const val SLOT_WIDTH_PT = 555f
        private const val SLOT_HEIGHT_PT = 396f
        private const val INVALID_FILENAME_CHARS = "<>:\"/\\|?*"

        fun buildFilename(
            displayName: String,
            date: String,
            lectureNumber: Int?,
            outputFormat: String,
            lectureNumberWidth: Int,
        ): String {
            val number = lectureNumber ?: 0
            val lectionNumber = number.toString().padStart(lectureNumberWidth, '0')
            val name = outputFormat
                .replace("{predmet}", displayName)
                .replace("{YYYYMMDD}", date.replace("-", ""))
                .replace("{lection_number}", lectionNumber)
            return sanitize(name) + ".pdf"
        }

        private fun sanitize(name: String): String =
            name.map { if (it in INVALID_FILENAME_CHARS) '_' else it }.joinToString("")
    }
}
