package fr.julien.quievreux.droidplane2.helper

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }
}