package fr.julien.quievreux.droidplane2.core.extensions

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT
import android.net.Uri
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
