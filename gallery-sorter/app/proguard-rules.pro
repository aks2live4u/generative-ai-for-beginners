# R8 rules for the release build. Libraries below ship consumer rules, but several rely on
# reflection / JNI, so the entry points are kept explicitly.

# Tink (vault crypto): keysets and key managers are looked up reflectively; protobuf-lite fields too.
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}

# ML Kit bundled models (face, text, labeling, barcode) and the Google common/vision layers.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.android.gms.internal.vision.** { *; }

# LiteRT (TensorFlow Lite): native code calls back into these Java classes by name.
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.ai.edge.litert.** { *; }
-keepclasseswithmembernames class * { native <methods>; }

# SQLCipher: JNI layer reaches into these classes.
-keep class net.zetetic.database.** { *; }

# Room: generated *_Impl classes are instantiated reflectively.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# BouncyCastle: only Argon2id is used, via direct references.
-keep class org.bouncycastle.crypto.generators.Argon2BytesGenerator { *; }
-keep class org.bouncycastle.crypto.params.Argon2Parameters { *; }
-keep class org.bouncycastle.crypto.params.Argon2Parameters$Builder { *; }

# Media3 (ExoPlayer): renderers/extractors are created reflectively in places.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
