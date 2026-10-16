package fr.julien.quievreux.droidplane2.core.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity

fun FragmentActivity.getOpenFileLauncher(): ActivityResultLauncher<String>{
    return registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val openFileIntent = Intent(this.applicationContext, this::class.java)
            openFileIntent.setData(uri)
            openFileIntent.setAction(ACTION_OPEN_DOCUMENT)
            startActivity(openFileIntent)
        }
    }
}

fun FragmentActivity.getSaveFileLauncher(
    actionOnResultOk: (Uri) -> Unit
): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                actionOnResultOk(uri)
            }
        }
    }
}

fun buildCreateDocumentIntent(filename: String, initialUri: Uri? = null): Intent {
    return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        putExtra(Intent.EXTRA_TITLE, filename)
        if (initialUri != null && initialUri.scheme != "android.resource" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        }
    }
}

fun deleteSafDocument(context: Context, uri: Uri): Boolean {
    return try {
        DocumentsContract.deleteDocument(context.contentResolver, uri)
    } catch (_: Exception) {
        try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (_: Exception) {
            false
        }
    }
}

