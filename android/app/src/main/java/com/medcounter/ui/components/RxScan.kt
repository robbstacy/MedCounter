package com.medcounter.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.medcounter.domain.SlipScan
import com.medcounter.ocr.RxLabelScanner
import kotlinx.coroutines.launch
import java.io.File

/**
 * "Scan label" + "From photo" buttons. Takes a picture of a pharmacy label
 * (or picks an existing one), reads it on-device, and hands back what it found.
 * The temporary photo is deleted as soon as OCR finishes.
 */
@Composable
fun RxScanButtons(
    onResult: (SlipScan) -> Unit,
    modifier: Modifier = Modifier,
    scanLabel: String = "Scan prescription",
    /** Called with the photo that was read, before the temporary copy is deleted, so it can be attached. */
    keepPhoto: (suspend (Uri) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    var rawText by remember { mutableStateOf<String?>(null) }
    var showRaw by remember { mutableStateOf(false) }

    fun runOcr(uri: Uri, cleanup: File?) {
        busy = true
        message = null
        scope.launch {
            val info = runCatching { RxLabelScanner.scan(context, uri) }.getOrElse { SlipScan() }
            keepPhoto?.let { keep -> runCatching { keep(uri) } }
            cleanup?.delete()
            busy = false
            rawText = info.rawText.takeIf { it.isNotBlank() }
            showRaw = false
            message = if (info.isEmpty) {
                "Couldn't find a medication. Try again in good light with the paper flat and filling the frame. Handwriting often can't be read; typed or printed slips work best."
            } else {
                "Found: ${info.summary()}. Check the fields before saving."
            }
            if (!info.isEmpty) onResult(info)
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingPath?.let(::File)
        pendingPath = null
        if (saved && file != null && file.length() > 0) {
            runOcr(scanUri(context, file), file)
        } else {
            file?.delete()
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) runOcr(uri, null)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val file = newScanFile(context)
                    pendingPath = file.absolutePath
                    try {
                        takePicture.launch(scanUri(context, file))
                    } catch (_: ActivityNotFoundException) {
                        pendingPath = null
                        file.delete()
                        message = "No camera app found. Use \"From photos\" instead."
                    }
                },
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null)
                Text("  $scanLabel")
            }
            OutlinedButton(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Text("  From photos")
            }
        }
        if (keepPhoto != null) {
            Text("The photo is kept with this medication.", style = MaterialTheme.typography.bodySmall)
        }
        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Reading prescription on this device…", style = MaterialTheme.typography.bodySmall)
            }
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
        }
        rawText?.let { raw ->
            TextButton(onClick = { showRaw = !showRaw }) {
                Text(if (showRaw) "Hide text read" else "Show text read")
            }
            if (showRaw) {
                SelectionContainer {
                    Text(raw, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

private fun newScanFile(context: Context): File {
    val dir = File(context.cacheDir, "scans").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() } // never keep old label photos around
    return File(dir, "label-${System.currentTimeMillis()}.jpg")
}

private fun scanUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
