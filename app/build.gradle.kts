plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

val appVersionCode = 31
val appVersionName = "1.0.31"

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

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

kotlin {
    jvmToolchain(21)

    androidTarget()
    jvm("desktop") {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(project(":data"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.font.awesome)
                implementation(libs.tabler.icons)
                implementation(libs.jetbrains.lifecycle.viewmodel)
                implementation(libs.jetbrains.lifecycle.viewmodel.compose)
                implementation(libs.jetbrains.lifecycle.runtime.compose)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.json)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest)
                implementation(libs.kotest.assertions)
                implementation(libs.kotest.runner.junit5)
                implementation(libs.mockk)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.json)
                implementation(libs.xpp3)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.viewmodel.ktx)
                implementation(libs.koin.android)
                implementation(libs.androidx.ui.tooling.preview)
            }
        }
        val desktopMain by getting {
            resources.srcDirs("src/androidMain/res")
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "fr.julien.quievreux.droidplane2.MainKt"
        javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }.get().metadata.installationPath.asFile.absolutePath
    }
}
