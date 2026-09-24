"""
YouTube to MP3 - a small, phone-friendly web app.

Run it on your computer (or directly on Android via Termux), open the page
in your phone's browser, paste a YouTube link and the MP3 is saved to the
phone's Downloads folder.

Only download content you own or have permission to download
(for example your own uploads or Creative Commons licensed videos).
"""
import io
import os
import shutil
import tempfile
from urllib.parse import urlparse

from flask import Flask, render_template, request, send_file
from yt_dlp import YoutubeDL
from yt_dlp.utils import DownloadError

app = Flask(__name__)

# Only accept links that point at YouTube, so the server can't be used
# to fetch arbitrary URLs.
ALLOWED_HOSTS = {
    "youtube.com",
    "www.youtube.com",
    "m.youtube.com",
    "music.youtube.com",
    "youtu.be",
}

BITRATES = {"128", "192", "320"}


def is_youtube_url(url):
    try:
        parsed = urlparse(url)
    except ValueError:
        return False
    return parsed.scheme in ("http", "https") and parsed.hostname in ALLOWED_HOSTS


def download_mp3(url, bitrate, out_dir):
    """Download the best audio stream and convert it to MP3 with ffmpeg."""
    options = {
        "format": "bestaudio/best",
        "outtmpl": os.path.join(out_dir, "%(title)s.%(ext)s"),
        "noplaylist": True,  # a link inside a playlist downloads just that video
        "restrictfilenames": True,  # safe file names on every OS
        "quiet": True,
        "postprocessors": [
            {
                "key": "FFmpegExtractAudio",
                "preferredcodec": "mp3",
                "preferredquality": bitrate,
            },
            {"key": "FFmpegMetadata"},  # keep title / artist tags
        ],
    }
    with YoutubeDL(options) as ydl:
        ydl.extract_info(url, download=True)

    for name in os.listdir(out_dir):
        if name.endswith(".mp3"):
            return os.path.join(out_dir, name)
    raise DownloadError("Conversion to MP3 failed - is ffmpeg installed?")


@app.get("/")
def index():
    return render_template("index.html")


@app.post("/download")
def download():
    url = request.form.get("url", "").strip()
    bitrate = request.form.get("bitrate", "192")
    if bitrate not in BITRATES:
        bitrate = "192"

    if not is_youtube_url(url):
        return render_template("index.html", error="Please paste a valid YouTube link.", url=url), 400

    work_dir = tempfile.mkdtemp(prefix="yt2mp3-")
    try:
        mp3_path = download_mp3(url, bitrate, work_dir)
        filename = os.path.basename(mp3_path)
        # Read the MP3 into memory so the temporary folder can be removed right away.
        with open(mp3_path, "rb") as f:
            data = io.BytesIO(f.read())
    except DownloadError as err:
        return render_template("index.html", error=f"Download failed: {err}", url=url), 502
    finally:
        shutil.rmtree(work_dir, ignore_errors=True)

    # as_attachment=True makes the phone's browser save the file to Downloads.
    return send_file(data, as_attachment=True, download_name=filename, mimetype="audio/mpeg")


if __name__ == "__main__":
    if shutil.which("ffmpeg") is None:
        print("WARNING: ffmpeg was not found on PATH - MP3 conversion will fail.")
    # 0.0.0.0 lets other devices on your Wi-Fi (your phone) reach the app.
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 5000)))
