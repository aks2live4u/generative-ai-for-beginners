# YouTube to MP3 (Android app)

An Android app that downloads the audio from a YouTube video, converts it to MP3 and saves it to your phone's **Downloads** folder.

It runs entirely on the phone. You don't need a computer or server once it's installed. The app uses [youtubedl-android](https://github.com/yausername/youtubedl-android), which bundles yt-dlp and FFmpeg.

> **Note:** Only download videos you own or have permission to download, such as your own uploads or Creative Commons videos.

## Install with Android Studio

1. Open Android Studio, choose **File > Open** and select this `youtube-mp3-downloader` folder.
2. Wait for the Gradle sync to finish.
3. On your phone, turn on **Developer options > USB debugging**, then plug the phone in with a USB cable.
4. Pick your phone in the device list and press **Run**.

To get an APK file instead, choose **Build > Build App Bundle(s) / APK(s) > Build APK(s)**. The file is at `app/build/outputs/apk/debug/app-debug.apk`. Copy it to your phone and open it to install.

## Use it

- Paste a YouTube link and tap **Download MP3**, or
- In the YouTube app, tap **Share** and choose **YouTube to MP3**. The link is filled in for you.

The first launch takes a few seconds while the app unpacks its downloader and checks for updates.

If downloads start failing, close and reopen the app. It updates yt-dlp on every launch, which usually fixes YouTube changes.
