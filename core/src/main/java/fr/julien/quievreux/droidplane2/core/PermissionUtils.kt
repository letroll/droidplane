package fr.julien.quievreux.droidplane2.core

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import fr.julien.quievreux.droidplane2.core.log.Logger

object PermissionUtils {

    fun checkStoragePermissions(context: Context): Boolean {
        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            //Android is 11 (R) or above
            return Environment.isExternalStorageManager()
        } else {
            //Below android 11
            val write = ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE)
            val read = ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE)
            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestForStoragePermissions(
        activity: ComponentActivity,
        logger: Logger,
    ) {
        val storageActivityResultLauncher = getStorageActivityResultLauncher(
            activity,
            logger,
        )
        //Android is 11 (R) or above
        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            try {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", activity.packageName, null)
                intent.setData(uri)
                storageActivityResultLauncher.launch(intent)
            } catch (e: java.lang.Exception) {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storageActivityResultLauncher.launch(intent)
            }
        } else {
            //Below android 11
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    WRITE_EXTERNAL_STORAGE,
                    READ_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    private fun getStorageActivityResultLauncher(
        activity: ComponentActivity,
        logger: Logger,
    ) = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            //Android is 11 (R) or above
            if (Environment.isExternalStorageManager()) {
                //Manage External Storage Permissions Granted
                logger.e("onActivityResult: Manage External Storage Permissions Granted")
            } else {
                Toast.makeText(activity, "Storage Permissions Denied", Toast.LENGTH_SHORT).show()
            }
        } else {
            //Below android 11
        }
    }

    const val STORAGE_PERMISSION_CODE: Int = 23
}