import com.android.build.api.artifact.SingleArtifact
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.aks.gallerysorter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aks.gallerysorter"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // The target phone (vivo X200) is arm64-only; dropping the other ABIs shrinks the APK.
        ndk { abiFilters += "arm64-v8a" }
    }

    // Release signing comes from a git-ignored keystore.properties (see scripts/make-release.sh).
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    signingConfigs {
        if (keystoreProps.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        // LiteRT memory-maps models straight out of the APK, which needs them stored uncompressed.
        noCompress += "tflite"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    implementation(libs.paging.common)
    implementation(libs.paging.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.sqlcipher)
    implementation(libs.biometric)
    implementation(libs.lifecycle.process)
    implementation(libs.exifinterface)
    implementation(libs.work.runtime.ktx)
    ksp(libs.room.compiler)

    // On-device ML: ML Kit bundled models only (never play-services-mlkit-*), plus LiteRT.
    implementation(libs.mlkit.face.detection)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.image.labeling)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.litert)
    implementation(libs.litert.gpu)
    implementation(libs.litert.gpu.api)

    // Vault crypto: Tink (hybrid + streaming AEAD) and BouncyCastle (Argon2id only).
    implementation(libs.tink)
    implementation(libs.bouncycastle)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
}

// ---- No-network guard -------------------------------------------------------
// Fails the build if any variant's merged manifest declares INTERNET or
// ACCESS_NETWORK_STATE (e.g. merged in from a dependency).
abstract class VerifyNoNetworkTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val variantName: Property<String>

    @TaskAction
    fun verify() {
        val forbidden = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
        )
        val text = mergedManifest.get().asFile.readText()
        val found = forbidden.filter { text.contains("\"$it\"") }
        if (found.isNotEmpty()) {
            throw GradleException(
                "verifyNoNetwork FAILED for variant '${variantName.get()}': merged manifest declares " +
                    found.joinToString() + " (${mergedManifest.get().asFile})"
            )
        }
    }
}

val verifyNoNetwork = tasks.register("verifyNoNetwork") {
    group = "verification"
    description = "Fails if any variant's merged manifest requests INTERNET or ACCESS_NETWORK_STATE."
}

androidComponents {
    onVariants { variant ->
        val cap = variant.name.replaceFirstChar { it.uppercase() }
        val perVariant = tasks.register<VerifyNoNetworkTask>("verifyNoNetwork$cap") {
            group = "verification"
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            variantName.set(variant.name)
        }
        verifyNoNetwork.configure { dependsOn(perVariant) }
        tasks.matching { it.name == "assemble$cap" }.configureEach { dependsOn(perVariant) }
    }
}

tasks.named("check") { dependsOn(verifyNoNetwork) }
