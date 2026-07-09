# Add project specific ProGuard rules here.
# https://developer.android.com/build/shrink-code

# Keep JNI-facing native method signatures.
-keepclasseswithmembernames class * {
    native <methods>;
}
