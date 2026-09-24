# YouTube to MP3 Downloader

A small web app that downloads the audio from a YouTube video, converts it to MP3 and saves it to your phone.

It uses [yt-dlp](https://github.com/yt-dlp/yt-dlp) to download and [FFmpeg](https://ffmpeg.org/) to convert. The page is built for phone screens: paste a link, tap **Download MP3**, and your browser saves the file to your **Downloads** folder.

> **Note:** Only download videos you own or have permission to download, such as your own uploads or videos with a Creative Commons license. Downloading other content may break YouTube's Terms of Service and copyright law.

## How it works

1. You paste a YouTube link into the page on your phone.
2. The Flask server downloads the best audio stream with yt-dlp.
3. FFmpeg converts it to MP3 at the quality you pick (128, 192 or 320 kbps) and keeps the title tags.
4. The server sends the MP3 back as a download, so the phone saves it to Downloads. Then the server deletes its temporary copy.

## Option A: Run it on your computer and use it from your phone

Your phone and computer must be on the same Wi-Fi network.

1. Install Python 3.9 or newer and FFmpeg:
   - macOS: `brew install ffmpeg`
   - Windows: `winget install ffmpeg`
   - Ubuntu/Debian: `sudo apt install ffmpeg`
2. Install the app and start it:

   ```bash
   cd youtube-mp3-downloader
   python3 -m venv venv
   source venv/bin/activate      # Windows: venv\Scripts\activate
   pip install -r requirements.txt
   python app.py
   ```

3. Find your computer's local IP address, for example `192.168.1.20`:
   - macOS/Linux: `ipconfig getifaddr en0` or `hostname -I`
   - Windows: `ipconfig`
4. On your phone, open `http://192.168.1.20:5000`. Use your own IP address.
5. Optional: tap **Add to Home Screen** in the browser menu so it opens like an app.

## Option B: Run it on your Android phone (no computer needed)

1. Install [Termux](https://f-droid.org/packages/com.termux/) from F-Droid. The Play Store version is outdated.
2. Copy this folder to the phone, or clone the repository inside Termux with `pkg install git`.
3. In Termux, run:

   ```bash
   cd youtube-mp3-downloader
   bash termux-setup.sh
   python app.py
   ```

4. Open `http://localhost:5000` in Chrome on the phone. MP3s are saved to **Downloads**.

## iPhone

iOS can't run the server itself, so use Option A. Safari saves the MP3 to the **Files** app, under **Downloads**. From there you can play it or share it to a music app.

## Troubleshooting

| Problem | Fix |
| --- | --- |
| `Conversion to MP3 failed` | FFmpeg isn't installed or isn't on your `PATH`. |
| `Download failed: ... Sign in to confirm` or other YouTube errors | YouTube changes often. Update yt-dlp: `pip install -U yt-dlp` |
| The phone can't open the page | Check that both devices are on the same Wi-Fi and that your computer's firewall allows port 5000. |
| The **Paste** button does nothing | Browsers only allow clipboard access on `localhost` or HTTPS. Long-press the box and paste instead. |

To use a different port, set the `PORT` environment variable, for example `PORT=8080 python app.py`.
