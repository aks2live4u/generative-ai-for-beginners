# Keep the JavaScript bridge methods reachable from the WebView.
-keepclassmembers class com.careerpilot.ai.WebAppInterface {
    public *;
}
