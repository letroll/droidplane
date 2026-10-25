package fr.julien.quievreux.droidplane2.core.log

import android.util.Log

interface Logger {
    fun e(message: String)
    fun e(tag: String, message: String)

    fun w(message: String)
    fun w(tag: String, message: String)

    fun d(message: String)
    fun d(tag: String, message: String)

    companion object {
        val Default: Logger = object : Logger {
            override fun e(message: String) { Log.e("Droidplane", message) }
            override fun e(tag: String, message: String) { Log.e(tag, message) }
            override fun w(message: String) { Log.w("Droidplane", message) }
            override fun w(tag: String, message: String) { Log.w(tag, message) }
            override fun d(message: String) { Log.d("Droidplane", message) }
            override fun d(tag: String, message: String) { Log.d(tag, message) }
        }
    }
}