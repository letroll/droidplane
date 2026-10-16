# Droidplane R8 / ProGuard configuration

# Keep source file names and line numbers for Crashlytics stack traces
-keepattributes SourceFile,LineNumberTable

# Keep Koin DSL & ViewModel classes instantiated via reflection / KClass
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# Keep XMLPullParser & Freeplane domain models
-dontwarn org.xmlpull.v1.**
-keep class org.xmlpull.v1.** { *; }
-keep class fr.julien.quievreux.droidplane2.data.model.** { *; }
