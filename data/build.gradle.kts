import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    jvmToolchain(21)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                api(libs.xpp3)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest)
                implementation(libs.kotest.assertions)
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.mockk)
            }
        }
        val androidUnitTest by getting {
            resources.srcDir("src/commonTest/resources")
        }
    }
}

android {
    namespace = "fr.julien.quievreux.droidplane2.data"
    compileSdk = libs.versions.targetSdkVersion.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdkVersion.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
