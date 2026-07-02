package com.kebuz.kebuzlect.data.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.kebuz.kebuzlect.data.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos

class PhotoAnalyzer(context: Context) {

    private val resolver = context.applicationContext.contentResolver

    suspend fun laplacianVariance(photo: Photo): Double = withContext(Dispatchers.IO) {
        val bitmap = decodeBounded(photo.uri, BLUR_MAX_DIM) ?: return@withContext 0.0
        try {
            laplacianVariance(toGrayscale(bitmap))
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun perceptualHash(photo: Photo): Long? = withContext(Dispatchers.IO) {
        val bitmap = decodeBounded(photo.uri, PHASH_MAX_DIM) ?: return@withContext null
        try {
            val scaled = Bitmap.createScaledBitmap(bitmap, PHASH_IMG_SIZE, PHASH_IMG_SIZE, true)
            try {
                perceptualHash(scaled)
            } finally {
                if (scaled != bitmap) scaled.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeBounded(uri: Uri, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > maxDim) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return runCatching {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()
    }

    private fun toGrayscale(bitmap: Bitmap): GrayImage {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val luma = DoubleArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF

            luma[i] = (r * 299 + g * 587 + b * 114) / 1000.0
        }
        return GrayImage(width, height, luma)
    }

    private fun laplacianVariance(image: GrayImage): Double {
        val w = image.width
        val h = image.height
        if (w < 3 || h < 3) return 0.0
        val px = image.luma
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val center = px[y * w + x]
                val up = px[(y - 1) * w + x]
                val down = px[(y + 1) * w + x]
                val left = px[y * w + (x - 1)]
                val right = px[y * w + (x + 1)]
                val lap = up + down + left + right - 4.0 * center
                sum += lap
                sumSq += lap * lap
                count++
            }
        }
        if (count == 0) return 0.0
        val mean = sum / count
        return sumSq / count - mean * mean
    }

    private fun perceptualHash(scaled: Bitmap): Long {
        val n = PHASH_IMG_SIZE
        val pixels = IntArray(n * n)
        scaled.getPixels(pixels, 0, n, 0, 0, n, n)
        val gray = Array(n) { row ->
            DoubleArray(n) { col ->
                val p = pixels[row * n + col]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                (r * 299 + g * 587 + b * 114) / 1000.0
            }
        }

        val rows = Array(n) { dct1d(gray[it]) }
        val low = Array(PHASH_HASH_SIZE) { DoubleArray(PHASH_HASH_SIZE) }
        for (col in 0 until PHASH_HASH_SIZE) {
            val column = DoubleArray(n) { rows[it][col] }
            val transformed = dct1d(column)
            for (row in 0 until PHASH_HASH_SIZE) {
                low[row][col] = transformed[row]
            }
        }

        val flat = DoubleArray(PHASH_HASH_SIZE * PHASH_HASH_SIZE) { low[it / PHASH_HASH_SIZE][it % PHASH_HASH_SIZE] }
        val median = median(flat)

        var hash = 0L
        for (i in flat.indices) {
            if (flat[i] > median) hash = hash or (1L shl i)
        }
        return hash
    }

    private fun dct1d(input: DoubleArray): DoubleArray {
        val n = input.size
        val out = DoubleArray(n)
        for (k in 0 until n) {
            var sum = 0.0
            for (i in 0 until n) {
                sum += input[i] * cos(Math.PI * (2 * i + 1) * k / (2.0 * n))
            }
            out[k] = sum
        }
        return out
    }

    private fun median(values: DoubleArray): Double {
        val sorted = values.sortedArray()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }

    private class GrayImage(val width: Int, val height: Int, val luma: DoubleArray)

    companion object {
        private const val BLUR_MAX_DIM = 1024
        private const val PHASH_MAX_DIM = 256
        private const val PHASH_HASH_SIZE = 8
        private const val PHASH_IMG_SIZE = PHASH_HASH_SIZE * 4

        fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

        fun isBlurry(variance: Double, threshold: Float): Boolean = variance < threshold
    }
}
