# Keep the JavaScript bridge methods so they remain callable from the WebView.
-keepclassmembers class com.braindump.app.bridge.AppBridge {
    @android.webkit.JavascriptInterface <methods>;
}
