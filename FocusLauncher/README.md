# Focus Launcher

A calm Android home screen that helps you use your phone less: no unnecessary shopping, no
aimless browsing.

## What it does

- **Only essentials on the home screen.** Up to 8 apps, shown as text with no icons or badges.
  On first run it picks Phone, Messages, Camera and Clock.
- **Everything else is behind a wall.** Swipe up, or tap "everything else", to reach it. You first
  wait a few seconds on a "What do you need?" screen. By default the wall only searches, so you
  type the app you came for and there's no list to scroll through.
- **A pause before distracting apps.** Shopping (Amazon, Flipkart, Myntra, Meesho, Temu, Shein,
  and others), social, video and browser apps are detected automatically. Opening one shows a
  breathing countdown, asks why you're opening it, and tells you how many times you've opened it
  today and how long you've used it. The big button takes you home, and the open button stays
  greyed out until the countdown ends and you pick a reason.
- **Positive feedback.** The home screen shows today's screen time and how many distractions
  you skipped.
- **Set as default in one tap.** Until Focus Launcher is your default home app, a "Make this
  your home screen" banner sits at the top of the home screen. Tapping it opens Android's
  "set default home app" dialog. If that isn't available, it opens the Home-app settings.
  Settings shows whether it's the default and lets you switch back at any time.
- **Make it yours:**
  - Wallpaper: a photo from your gallery, a colour gradient, or your phone's own wallpaper,
    with a dim slider.
  - Clocks: Digital, Stacked, Words ("it's quarter past seven"), Analog or Minimal, with a
    size slider and a 12/24-hour switch.
  - Fonts: 8 built-in styles, or import any `.ttf`/`.otf` font file.
  - Text colour, and a "home note" line under the clock for an intention or reminder.

## Gestures

| Action | Gesture |
| --- | --- |
| Open the wall | Swipe up, or tap "↑ everything else" |
| Settings | Long-press anywhere on the home screen, or tap "settings" |
| Pin an app or mark it distracting | Long-press it inside the wall |

## Install

1. Open the repository's **Actions** tab, then the latest **Build Focus Launcher APK** run.
2. Download the `focus-launcher-apk` artifact, unzip it, and open the `.apk` on your phone.
   You'll need to allow installs from that source.
3. Open Focus Launcher and tap the **Make this your home screen** banner, then choose
   Focus Launcher. You can also do this from Settings.
4. Optional: in Settings → Screen time, tap **Grant usage access** to see screen time.

To build it yourself, open this folder in Android Studio, or run `./gradlew assembleDebug`
with an Android SDK installed. Requires Android 8.0 or newer.

## Privacy

Everything stays on your phone. The app has no internet permission, no analytics and no
account.
