package fr.julien.quievreux.droidplane2.core.log

import android.util.Log

class AndLogger : Logger {
    override fun e(message: String) {
        Log.e("droidplane2", message)
    }

    override fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun w(message: String) {
        Log.w("droidplane2", message)
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun d(message: String) {
        Log.d("droidplane2", message)
    }

    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
}