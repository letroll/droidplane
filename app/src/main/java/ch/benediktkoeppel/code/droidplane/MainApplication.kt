package ch.benediktkoeppel.code.droidplane

import android.app.Application
import ch.benediktkoeppel.code.droidplane.di.koinModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

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

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(koinModule)
        }
    }
}
