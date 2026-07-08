package com.purovida.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Image helpers matching the Flutter app's image_picker settings
 * (maxWidth 1024, JPEG quality 80).
 */
object ImageUtils {

    fun readScaledJpeg(context: Context, uri: Uri, maxWidth: Int = 1024, quality: Int = 80): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: throw IllegalArgumentException("Could not decode image")

        val scaled = if (bitmap.width > maxWidth) {
            val h = (bitmap.height.toLong() * maxWidth / bitmap.width).toInt()
            Bitmap.createScaledBitmap(bitmap, maxWidth, h, true)
        } else {
            bitmap
        }

        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
    }

    /** Creates an empty cache file + content Uri for the camera to write into. */
    fun newCameraCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File.createTempFile("capture_", ".jpg", dir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    /** Copies a picked image into app-private storage so the path stays valid. */
    fun copyToAppStorage(context: Context, uri: Uri, prefix: String): String {
        val dest = File(context.filesDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        } ?: throw IllegalArgumentException("Could not read image")
        return dest.absolutePath
    }
}
