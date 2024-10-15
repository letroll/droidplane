package fr.julien.quievreux.droidplane2.helper

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

object AndroidHelper {
    fun <T : Activity?> getActivity(context: Context?, clazz: Class<T>): T? {
        var context = context
        while (context is ContextWrapper) {
            if (clazz.isInstance(context)) {
                return clazz.cast(context)
            }
            context = context.baseContext
        }
        return null
    }
}
