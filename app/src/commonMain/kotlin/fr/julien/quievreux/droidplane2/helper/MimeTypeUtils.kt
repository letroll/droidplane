package fr.julien.quievreux.droidplane2.helper

object MimeTypeUtils {
    private val extensionToMimeType: Map<String, String> = mapOf(
        "mm" to "application/x-freemind",
        "txt" to "text/plain",
        "html" to "text/html",
        "htm" to "text/html",
        "pdf" to "application/pdf",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif" to "image/gif",
        "svg" to "image/svg+xml",
        "webp" to "image/webp",
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "mp4" to "video/mp4",
        "json" to "application/json",
        "xml" to "application/xml",
        "zip" to "application/zip",
        "md" to "text/markdown",
        "csv" to "text/csv",
    )

    fun getMimeTypeFromExtension(extension: String): String? {
        if (extension.isBlank()) return null
        return extensionToMimeType[extension.trim().lowercase()]
    }
}
