package com.purovida.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Standard form screen chrome: close/back button, centered title, Save action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScaffold(
    title: String,
    onClose: () -> Unit,
    saveLabel: String = "Save",
    onSave: (() -> Unit)?,
    snackbarHostState: SnackbarHostState? = null,
    closeIcon: ImageVector = Icons.Filled.Close,
    showSave: Boolean = true,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = PuroColors.background,
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PuroColors.background,
                    titleContentColor = PuroColors.text,
                ),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(closeIcon, contentDescription = "Close", tint = PuroColors.mutedForeground)
                    }
                },
                title = {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    if (showSave && onSave != null) {
                        Button(
                            onClick = onSave,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PuroColors.primary,
                                contentColor = PuroColors.primaryForeground,
                            ),
                            modifier = Modifier.padding(end = 16.dp),
                        ) {
                            Text(saveLabel)
                        }
                    }
                },
            )
        },
        content = content,
    )
}

@Composable
fun PuroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String = "",
    maxLines: Int = 1,
    numeric: Boolean = false,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = maxLines == 1,
        maxLines = maxLines,
        minLines = maxLines,
        placeholder = { Text(hint, fontSize = 14.sp, color = PuroColors.mutedForeground) },
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PuroColors.card,
            unfocusedContainerColor = PuroColors.card,
            focusedBorderColor = PuroColors.primary,
            unfocusedBorderColor = PuroColors.border,
            focusedTextColor = PuroColors.text,
            unfocusedTextColor = PuroColors.text,
            cursorColor = PuroColors.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun RatingRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = PuroColors.mutedForeground)
        RatingStars(rating = value, size = 24.dp, onChange = onChange)
    }
}

/** Camera / Photo Library chooser, ported from the app's modal bottom sheets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceSheet(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PuroColors.card) {
        Column(Modifier.padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text("Camera", color = PuroColors.text) },
                leadingContent = {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = PuroColors.primary,
                        modifier = Modifier.size(24.dp),
                    )
                },
                colors = ListItemDefaults.colors(containerColor = PuroColors.card),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onCamera()
                    },
            )
            ListItem(
                headlineContent = { Text("Photo Library", color = PuroColors.text) },
                leadingContent = {
                    Icon(
                        Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = PuroColors.primary,
                        modifier = Modifier.size(24.dp),
                    )
                },
                colors = ListItemDefaults.colors(containerColor = PuroColors.card),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onGallery()
                    },
            )
        }
    }
}
