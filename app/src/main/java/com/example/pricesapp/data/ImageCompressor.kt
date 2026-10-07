package com.example.pricesapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * Reduce una foto a [maxSize] px por lado y la guarda como JPEG.
 * Una foto de cámara (3–8 MB) queda en ~100–300 KB, suficiente para el listado.
 */
object ImageCompressor {

    fun compress(context: Context, uri: Uri, maxSize: Int = 1280, quality: Int = 80): ByteArray {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "El archivo no es una imagen válida" }

        // Decodificar ya reducido para no cargar la foto completa en memoria
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSize || bounds.outHeight / (sample * 2) >= maxSize) {
            sample *= 2
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("No se pudo leer la imagen")

        val scale = maxSize.toFloat() / maxOf(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            postRotate(readRotation(context, uri).toFloat())
            if (scale < 1f) postScale(scale, scale)
        }
        val bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (bitmap !== decoded) bitmap.recycle()
            decoded.recycle()
            out.toByteArray()
        }
    }

    private fun readRotation(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (e: Exception) {
        0
    }
}
