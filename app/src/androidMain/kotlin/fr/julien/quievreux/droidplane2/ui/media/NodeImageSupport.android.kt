package fr.julien.quievreux.droidplane2.ui.media

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val IMAGE_MIME_TYPE = "image/*"
private const val CONTENT_SCHEME_PREFIX = "content://"
private const val FILE_SCHEME_PREFIX = "file:"
private const val FILE_AUTHORITY_PREFIX = "file://"

@Composable
actual fun rememberImageFilePicker(onUriSelected: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnUriSelected by rememberUpdatedState(onUriSelected)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            currentOnUriSelected(uri.toString())
        }
    }
    return { launcher.launch(arrayOf(IMAGE_MIME_TYPE)) }
}

@Composable
actual fun rememberNodeImageBitmap(uri: String): ImageBitmap? {
    val context = LocalContext.current
    val bitmapState by produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        if (uri.isBlank()) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = when {
                    uri.startsWith(CONTENT_SCHEME_PREFIX, ignoreCase = true) -> {
                        context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }

                    uri.startsWith(FILE_SCHEME_PREFIX, ignoreCase = true) -> {
                        val file = runCatching { File(URI(uri)) }.getOrNull()
                            ?: File(uri.removePrefix(FILE_AUTHORITY_PREFIX).removePrefix(FILE_SCHEME_PREFIX))
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath)
                        } else {
                            context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        }
                    }

                    else -> {
                        val file = File(uri)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath)
                        } else {
                            context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        }
                    }
                }
                bitmap?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bitmapState
}
