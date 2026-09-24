plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.ytmp3"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ytmp3"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // yt-dlp, Python and FFmpeg ship as native libraries for these CPUs.
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Required by youtubedl-android: native libs must be extracted on install.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    val ytdl = "0.18.1"
    implementation("io.github.junkfood02.youtubedl-android:library:$ytdl")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$ytdl")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
