plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    defaultConfig {
        applicationId = "fr.julien.quievreux.droidplane2"
        minSdk = libs.versions.minSdkVersion.get().toInt()
        targetSdk = libs.versions.targetSdkVersion.get().toInt()
    }

    buildFeatures {
        compose = true
    }
    packaging {
        // Exclure le fichier en conflit de la dépendance 'kotlinx-coroutines-debug'
        resources.excludes.add("win32-x86-64/attach_hotspot_windows.dll")
        resources.excludes.add("win32-x86/attach_hotspot_windows.dll")
        resources.excludes.add("META-INF/licenses/*")
        resources.excludes.add("META-INF/*")
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    namespace = "fr.julien.quievreux.droidplane2"
    compileSdk = libs.versions.targetSdkVersion.get().toInt()

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.constraintlayout)

    implementation(project(":core"))
    implementation(project(":data"))

    //jetpack compose
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.material3)
    implementation(libs.font.awesome)
    implementation(libs.tabler.icons)

    //koin
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.viewmodel.navigation)

    implementation(libs.mindmapview)

    implementation(libs.turbine)

    //real XmlPullParser for JVM unit tests: android.jar only ships throwing stubs
    testImplementation("xmlpull:xmlpull:1.1.3.1")
    testImplementation("xpp3:xpp3:1.1.4c")

    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)


    // UI Tests
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}