package com.medcounter.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.googlecode.tesseract.android.TessBaseAPI
import com.medcounter.domain.PrescriptionReader
import com.medcounter.domain.SlipScan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads a photo of a prescription slip or pharmacy label entirely on-device with Tesseract.
 * No network is used; the bundled English model is copied from assets
 * into app-private storage on first use.
 */
object RxLabelScanner {
    private const val LANGUAGE = "eng"
    private const val MAX_EDGE = 2400
    private val lock = Mutex()

    suspend fun scan(context: Context, uri: Uri): SlipScan = withContext(Dispatchers.Default) {
        lock.withLock {
            val dataDir = ensureModel(context)
            val bitmap = loadForOcr(context, uri) ?: return@withLock SlipScan()
            val tess = TessBaseAPI()
            try {
                if (!tess.init(dataDir.absolutePath, LANGUAGE)) return@withLock SlipScan()
                tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
                tess.setImage(bitmap)
                val text = tess.utF8Text.orEmpty()
                PrescriptionReader.read(text)
            } finally {
                tess.recycle()
                bitmap.recycle()
            }
        }
    }

    private fun ensureModel(context: Context): File {
        val root = File(context.noBackupFilesDir, "ocr")
        val tessdata = File(root, "tessdata").apply { mkdirs() }
        val model = File(tessdata, "$LANGUAGE.traineddata")
        if (!model.exists() || model.length() == 0L) {
            val tmp = File(tessdata, "$LANGUAGE.traineddata.tmp")
            context.assets.open("tessdata/$LANGUAGE.traineddata").use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            }
            tmp.renameTo(model)
        }
        return root
    }

    /** Decodes with downsampling, applies EXIF rotation and converts to high-contrast grayscale. */
    private fun loadForOcr(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)
        val oriented = if (rotation != 0) {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
                .also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }

        val gray = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
        val contrast = 1.4f
        val shift = -0.2f * 255f
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        matrix.postConcat(
            ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, shift,
                    0f, contrast, 0f, 0f, shift,
                    0f, 0f, contrast, 0f, shift,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
        Canvas(gray).drawBitmap(oriented, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
        oriented.recycle()
        return gray
    }
}
