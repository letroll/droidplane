plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val appVersionCode = 25
val appVersionName = "1.0.25"

android {
    defaultConfig {
        applicationId = "fr.julien.quievreux.droidplane2"
        minSdk = libs.versions.minSdkVersion.get().toInt()
        targetSdk = libs.versions.targetSdkVersion.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildFeatures {
        compose = true
    }
    packaging {
        resources.excludes.add("META-INF/licenses/*")
        resources.excludes.add("META-INF/*")
    }

    signingConfigs {
        create("release") {
            // Récupère le chemin du keystore depuis la variable d'environnement,
            // ou utilise un fichier local par défaut pour vos builds en local sur votre machine.
            val storePathEnv = System.getenv("KEYSTORE_PATH")
            storeFile = if (!storePathEnv.isNullOrEmpty()) {
                file(storePathEnv)
            } else {
                file("release-key.jks") // Chemin par défaut en local si la variable n'est pas définie
            }

            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEYSTORE_ALIAS")
            keyPassword = System.getenv("KEYSTORE_ALIAS_PASSWORD")
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true && !releaseSigning.storePassword.isNullOrEmpty()) {
                signingConfig = releaseSigning
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    namespace = "fr.julien.quievreux.droidplane2"
    compileSdk = libs.versions.targetSdkVersion.get().toInt()

    kotlinOptions {
        jvmTarget = "21"
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(project(":core"))
    implementation(project(":data"))

    //jetpack compose
    val composeBom = platform(libs.androidx.compose.bom)
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

    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":data")))
    testImplementation(libs.turbine)
    testImplementation(kotlin("test"))
    testImplementation(libs.json)
    //real XmlPullParser for JVM unit tests: android.jar only ships throwing stubs
    testImplementation(libs.xmlpull)
    testImplementation(libs.xpp3)

    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    // UI Tests
    androidTestImplementation(testFixtures(project(":data")))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.ui.test)
    debugImplementation(libs.androidx.ui.test.manifest)
}