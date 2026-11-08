package com.medcounter.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Prescription photos attached to a medication.
 *
 * Stored in app-private `noBackupFilesDir`, so they are never included in
 * Android cloud backup. Each photo is re-encoded on import, which strips EXIF
 * metadata (including GPS location) and caps the size.
 */
object RxPhotoStore {
    private const val MAX_EDGE = 2400
    private const val PENDING = "pending"

    private fun root(context: Context) = File(context.noBackupFilesDir, "rx_photos")

    private fun dir(context: Context, medicationId: Long?): File =
        File(root(context), medicationId?.toString() ?: PENDING).apply { mkdirs() }

    fun list(context: Context, medicationId: Long): List<File> =
        dir(context, medicationId).listFiles { f -> f.extension == "jpg" }
            ?.sortedByDescending { it.name }
            .orEmpty()

    fun listPending(context: Context): List<File> =
        dir(context, null).listFiles { f -> f.extension == "jpg" }?.sortedByDescending { it.name }.orEmpty()

    /** Copies [uri] in as a new photo. [medicationId] null = staged for a medication not saved yet. */
    suspend fun add(context: Context, medicationId: Long?, uri: Uri): File? = withContext(Dispatchers.IO) {
        val bitmap = decode(context, uri) ?: return@withContext null
        val out = File(dir(context, medicationId), "rx-${System.currentTimeMillis()}-${(0..9999).random()}.jpg")
        try {
            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            out
        } catch (t: Throwable) {
            out.delete()
            null
        } finally {
            bitmap.recycle()
        }
    }

    /** Moves photos staged while adding a new medication onto the saved medication. */
    fun claimPending(context: Context, medicationId: Long) {
        val target = dir(context, medicationId)
        listPending(context).forEach { it.renameTo(File(target, it.name)) }
    }

    fun clearPending(context: Context) {
        listPending(context).forEach { it.delete() }
    }

    fun deleteAll(context: Context, medicationId: Long) {
        dir(context, medicationId).deleteRecursively()
    }

    /** Copies photos to the share cache and returns content URIs other apps can read. */
    fun shareUris(context: Context, files: List<File>): List<Uri> {
        val shareDir = File(context.cacheDir, "exports/rx_photos").apply {
            deleteRecursively()
            mkdirs()
        }
        return files.map { src ->
            val copy = File(shareDir, src.name)
            src.copyTo(copy, overwrite = true)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", copy)
        }
    }

    fun decodeSampled(file: File, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val opened = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds); true } ?: false
        if (!opened) return null // decodeStream returns null when only reading bounds; check the stream instead
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)
        if (rotation == 0) return decoded
        val rotated = Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height,
            Matrix().apply { postRotate(rotation.toFloat()) }, true,
        )
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }
}
