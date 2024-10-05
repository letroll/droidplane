package ch.benediktkoeppel.code.droidplane;

import android.app.Application;

/**
 * The DroidPlane main application. It stores the loaded Uri and document, so that we can recreate the MainActivity
 * after a screen rotation.
 */
public class MainApplication extends Application {

    /**
     * Android Logging TAG
     */
    public static final String TAG = "DroidPlane";

}
