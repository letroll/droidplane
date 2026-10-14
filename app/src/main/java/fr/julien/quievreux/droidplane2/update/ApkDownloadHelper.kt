package fr.julien.quievreux.droidplane2.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

/**
 * Helper delegating APK downloading to Android's native DownloadManager.
 */
object ApkDownloadHelper {

    /**
     * Enqueues an APK download request into the system DownloadManager.
     * Shows progress in the system notification shade and displays a completion
     * notification allowing the user to tap and install directly.
     */
    fun enqueueDownload(
        context: Context,
        apkUrl: String,
        fileName: String,
        title: String = "Droidplane Update"
    ): Long {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle(title)
            .setDescription(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: throw IllegalStateException("DownloadManager system service unavailable")

        return downloadManager.enqueue(request)
    }
}
