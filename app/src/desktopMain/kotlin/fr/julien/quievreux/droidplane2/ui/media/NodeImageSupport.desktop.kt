package fr.julien.quievreux.droidplane2.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import java.net.URI

private const val DIALOG_TITLE_SELECT_IMAGE = "Select Image"
private const val FILE_URI_PREFIX = "file:"
private const val FILE_DOUBLE_SLASH_URI_PREFIX = "file://"

private val SUPPORTED_IMAGE_EXTENSIONS = listOf(
    ".png",
    ".jpg",
    ".jpeg",
    ".webp",
    ".gif",
    ".bmp"
)

/**
 * Desktop implementation of [rememberImageFilePicker] using AWT [FileDialog].
 */
@Composable
actual fun rememberImageFilePicker(onUriSelected: (String) -> Unit): () -> Unit {
    val currentOnUriSelected by rememberUpdatedState(onUriSelected)
    return remember {
        {
            val dialog = FileDialog(null as Frame?, DIALOG_TITLE_SELECT_IMAGE, FileDialog.LOAD)
            dialog.filenameFilter = FilenameFilter { _, name ->
                SUPPORTED_IMAGE_EXTENSIONS.any { extension ->
                    name.endsWith(extension, ignoreCase = true)
                }
            }
            dialog.isVisible = true
            val directory = dialog.directory
            val fileName = dialog.file
            if (directory != null && fileName != null) {
                val selectedUri = File(directory, fileName).toURI().toString()
                currentOnUriSelected(selectedUri)
            }
        }
    }
}

/**
 * Desktop implementation of [rememberNodeImageBitmap] decoding local image files on [Dispatchers.IO].
 */
@Suppress("DEPRECATION")
@Composable
actual fun rememberNodeImageBitmap(uri: String): ImageBitmap? {
    val bitmapState = produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        if (uri.isBlank()) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = resolveLocalImageFile(uri)
                if (file.exists() && file.isFile) {
                    file.inputStream().buffered().use { inputStream ->
                        loadImageBitmap(inputStream)
                    }
                } else {
                    null
                }
            }.getOrNull()
        }
    }
    return bitmapState.value
}

internal fun resolveLocalImageFile(uri: String): File {
    return if (uri.startsWith(FILE_URI_PREFIX, ignoreCase = true)) {
        runCatching { File(URI(uri)) }.getOrNull()
            ?: File(uri.removePrefix(FILE_DOUBLE_SLASH_URI_PREFIX).removePrefix(FILE_URI_PREFIX))
    } else {
        File(uri)
    }
}
