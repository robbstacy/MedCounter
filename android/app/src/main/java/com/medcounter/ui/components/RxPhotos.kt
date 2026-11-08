package com.medcounter.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.medcounter.data.RxPhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Prescription photos attached to a medication: add existing photos (several at once)
 * or take a new one, view full screen with pinch-zoom, read, share, delete.
 */
@Composable
fun RxPhotoSection(
    photos: List<File>,
    onAddUris: suspend (List<Uri>) -> Int,
    onRead: (File) -> Unit,
    onDelete: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<File?>(null) }
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun addAll(uris: List<Uri>, cleanup: File? = null) {
        if (uris.isEmpty()) return
        busy = true
        error = null
        scope.launch {
            val saved = runCatching { onAddUris(uris) }.getOrDefault(0)
            cleanup?.delete()
            busy = false
            val failed = uris.size - saved
            if (failed > 0) error = "Couldn't attach $failed photo${if (failed == 1) "" else "s"}. Try a different image."
        }
    }

    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        addAll(uris)
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingPath?.let(::File)
        pendingPath = null
        if (saved && file != null && file.length() > 0) addAll(listOf(Uri.fromFile(file)), file) else file?.delete()
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Prescription photos", style = MaterialTheme.typography.titleMedium)
        if (photos.isEmpty()) {
            Text("No photos yet. Attach the doctor's prescription so it's handy at the pharmacy.", style = MaterialTheme.typography.bodySmall)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(photos, key = { it.name }) { file ->
                    RxPhotoThumb(file, onClick = { viewing = file })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                enabled = !busy,
                modifier = Modifier.weight(1f),
                onClick = { pickMany.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Text(" Attach photos")
            }
            OutlinedButton(
                enabled = !busy,
                onClick = {
                    val dir = File(context.cacheDir, "scans").apply { mkdirs() }
                    val file = File(dir, "attach-${System.currentTimeMillis()}.jpg")
                    pendingPath = file.absolutePath
                    try {
                        takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                    } catch (_: ActivityNotFoundException) {
                        pendingPath = null
                        file.delete()
                    }
                },
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Take a photo")
            }
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }

    viewing?.let { file ->
        RxPhotoViewer(
            file = file,
            onDismiss = { viewing = null },
            onRead = { viewing = null; onRead(file) },
            onShare = { sharePhotos(context, listOf(file)) },
            onDelete = { viewing = null; onDelete(file) },
        )
    }
}

@Composable
fun RxPhotoThumb(file: File, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { RxPhotoStore.decodeSampled(file, 320) }
    }
    Box(
        modifier
            .size(88.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let {
            Image(it.asImageBitmap(), contentDescription = "Prescription photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } ?: CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun RxPhotoViewer(
    file: File,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val bitmap by produceState<android.graphics.Bitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { RxPhotoStore.decodeSampled(file, 2400) }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                bitmap?.let {
                    Image(
                        it.asImageBitmap(),
                        contentDescription = "Prescription photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                    )
                } ?: CircularProgressIndicator()
            }
            // Controls float at the top: the dialog window can extend below the visible
            // area on edge-to-edge devices, which clipped a bottom bar.
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onDismiss) { Text("Close") }
                TextButton(onClick = onRead) { Text("Read Rx") }
                TextButton(onClick = onShare) { Text("Send") }
                TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete photo?") },
            text = { Text("This removes the photo from PillStock. Your original in the gallery isn't touched.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Share photos (optionally with text) through the Android share sheet. */
fun sharePhotos(context: Context, files: List<File>, text: String? = null) {
    val uris = ArrayList(RxPhotoStore.shareUris(context, files))
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
    }
    text?.let { intent.putExtra(Intent.EXTRA_TEXT, it) }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Send prescription"))
}
