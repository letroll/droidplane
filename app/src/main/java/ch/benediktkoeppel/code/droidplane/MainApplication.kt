package ch.benediktkoeppel.code.droidplane

import android.app.Application

/**
 * The DroidPlane main application. It stores the loaded Uri and document, so that we can recreate the MainActivity
 * after a screen rotation.
 */
class MainApplication : Application() {
    /**
     * Android Logging TAG
     */
    companion object{
        const val TAG: String = "DroidPlane"
    }
}
