package com.campus.service.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Ảnh đã nén (để upload) kèm ảnh nhỏ (để xem trước trong form). */
class CompressedImage(
    val bytes: ByteArray,
    val thumbnail: Bitmap
)

object ImageCompressor {

    private const val MAX_SIDE = 1280
    private const val THUMB_SIDE = 256
    private const val QUALITY = 80

    /** Trả về null nếu không đọc được ảnh. Nên gọi trên luồng nền (Dispatchers.IO). */
    fun compress(context: Context, uri: Uri): CompressedImage? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > MAX_SIDE * 2 || bounds.outHeight / sample > MAX_SIDE * 2) {
            sample *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        val decodeStream = resolver.openInputStream(uri) ?: return null
        val decoded = decodeStream.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: return null

        val rotation = readRotation(context, uri)
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val out = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)

        val thumbScale = minOf(1f, THUMB_SIDE.toFloat() / maxOf(result.width, result.height))
        val thumb = Bitmap.createScaledBitmap(
            result,
            maxOf(1, (result.width * thumbScale).toInt()),
            maxOf(1, (result.height * thumbScale).toInt()),
            true
        )

        if (result !== decoded) decoded.recycle()
        if (result !== thumb) result.recycle()

        return CompressedImage(bytes = out.toByteArray(), thumbnail = thumb)
    }

    private fun readRotation(context: Context, uri: Uri): Float {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return 0f
            stream.use {
                val exif = ExifInterface(it)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        } catch (e: Exception) {
            0f
        }
    }
}