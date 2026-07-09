# AI Video Transcriber (Android)

A private, on-device video/audio transcriber for Android. Pick a video from your phone's gallery
or files, and it transcribes speech to text **entirely on the device** — using
[whisper.cpp](https://github.com/ggerganov/whisper.cpp) (OpenAI Whisper, running locally). There
is no backend server, no account, no analytics, and nothing you transcribe ever leaves your phone.

> **Scope note:** the original spec for this app described a full cloud pipeline (Whisper
> **Large**, speaker diarization, YouTube/TikTok/Instagram downloading, 20GB uploads, live
> translation, meeting summaries, etc.). That's a multi-service backend with GPU hosting, which
> isn't something that can be handed to you as an "Android Studio zip" — a GPU server has to
> actually be rented and run somewhere. This app instead does the on-device version: it runs
> Whisper locally with whisper.cpp, so everything works with **zero infrastructure and zero
> ongoing cost**, at the cost of not having speaker diarization and topping out around Whisper
> `small` accuracy instead of `large`. See "Possible next steps" below if you want to add a
> cloud backend later.

## What's implemented

- Pick a **video** via Android's system Photo Picker — shows your gallery/camera roll directly,
  not just a raw file browser — or an **audio** file (MP3, WAV, M4A, AAC, FLAC, OGG - voice
  memos, podcast exports, meeting recordings, etc.) via a document picker, since the Photo
  Picker only understands photos/videos.
- **No link/URL input.** This was tried, but every plausible use case for it was a social
  platform share link (Facebook, YouTube, Instagram, TikTok...), which don't point to an actual
  video file — they point to a webpage, and turning that into a real download requires a scraper
  that breaks constantly and generally violates those platforms' Terms of Service. If you have a
  video from one of those apps, save it to your phone (most apps have a built-in "save video" or
  share-to-Files option) and use the picker instead.
- On-device transcription via whisper.cpp — the only network calls this app ever makes are to
  download the Whisper model once (see Accuracy below).
- Three accuracy tiers (Fast / Best / Ultra), mapped to the `tiny` / `base` / `small` multilingual
  Whisper models — downloaded once (75–466 MB) and reused after that.
- Auto language detection, or pick from the ~99 languages Whisper supports.
- Segment-level timestamps; tap a timestamp to jump the video preview to that point.
- Optional heuristic filler-word cleanup ("um", "uh", "like", repeated words).
- Search within the transcript.
- Copy, share (plain text), and download as `.txt` or `.srt`.
- Nothing is copied anywhere: the app reads the picked video in place via its content URI and
  never makes its own copy, so there's nothing of ours to clean up afterward.
- CPU-aware threading: on the (near-universal) big.LITTLE phone chips, transcription threads are
  scheduled only on the fast "performance" cores, detected via each core's max clock speed —
  see `WhisperCpuConfig.kt`.

## Requirements

- Android Studio (Hedgehog/2023.1.1 or newer recommended)
- The Android NDK and CMake, installed via **Android Studio → Settings → SDK Manager → SDK
  Tools** (check "NDK (Side by side)" and "CMake"). Android Studio will also offer to install
  these automatically on first sync if they're missing.
- A device or emulator running **Android 8.0 (API 26) or newer**, `arm64-v8a`, `armeabi-v7a`, or
  `x86_64`.
- Internet access on first run, to download the on-device model (once).

## Setup

This project embeds [whisper.cpp](https://github.com/ggerganov/whisper.cpp) as a **git
submodule** (MIT licensed) rather than vendoring its C++ source directly, so it always builds
against a real, unmodified copy of the engine.

```bash
git clone <this repo/branch>
cd AIVideoTranscriber
git submodule update --init --recursive   # pulls in app/src/main/cpp/whisper.cpp
```

If you received this project as a **zip file instead of a git clone**, the submodule folder
(`app/src/main/cpp/whisper.cpp`) will be empty — download
<https://github.com/ggerganov/whisper.cpp> yourself (as a zip or `git clone`) and copy its
contents into `app/src/main/cpp/whisper.cpp/` before opening the project.

Then:

1. Open the `AIVideoTranscriber` folder in Android Studio.
2. Let Gradle sync — it will download the Android Gradle Plugin, Kotlin, and Jetpack Compose
   dependencies, and (if prompted) install the NDK/CMake.
3. Run on a device or emulator (**arm64-v8a is fastest**; x86_64 emulators work but are slow for
   inference).
4. On first transcription, pick an accuracy tier — the app downloads that model once from
   Hugging Face and caches it.

**This project was authored and reviewed without access to an Android SDK/NDK toolchain**
(the environment that generated it had no `ANDROID_HOME`), so while every native API call was
checked against the actual `whisper.cpp` headers for this pinned submodule commit, it has not
been through a real `./gradlew assembleDebug`. Please treat the first build as the actual test,
and check Android Studio's Build/Logcat output if something doesn't compile — most likely culprits
are NDK/CMake version mismatches, which are easy to fix by bumping `ndkVersion` in
`app/build.gradle` to whatever Android Studio has installed.

## Known limitations

- **Not cloud Whisper Large.** "Ultra" tops out at the `small` model (~466 MB) because larger
  models are impractical to run on a phone CPU. Accuracy is very good but not GPU-cloud-large
  level.
- **No speaker diarization.** That needs a separate model (e.g. pyannote.audio) that doesn't
  have a mature on-device Android story yet.
- **Whole file is transcribed in one pass** — whisper.cpp's API takes one full float array, so
  the final 16kHz mono PCM (about 115 KB per minute of audio) is held in memory for the whole
  `whisper_full()` call, and whisper.cpp's own internal buffers scale with audio length too. Audio
  *decoding* itself is streamed chunk-by-chunk (see `AudioExtractor.kt`) so the original
  full-resolution file is never fully buffered, but very long recordings (multi-hour) can still
  run out of memory on lower-RAM devices during the transcription step itself. `android:largeHeap`
  is enabled to give some headroom. Keep clips to well under an hour for reliable results.
- **Filler-word cleanup is a simple regex heuristic**, not an AI model — it will occasionally
  strip intentional words like "like" or "actually". It's an optional toggle for that reason.
- **Saving to Downloads on Android 9 (API 28) and below** needs the legacy
  `WRITE_EXTERNAL_STORAGE` permission, which isn't requested automatically in this build. Use
  **Share** instead of **Download** on those OS versions, or wire up a runtime permission request
  if you need the Download button there too.

## Possible next steps

- Add a cloud backend (FastAPI + Whisper Large + pyannote.audio, per the original spec) as an
  *optional* upgrade path, with a settings toggle to point the app at your own self-hosted server.
- Speaker diarization once a solid on-device (or opt-in cloud) option exists.
- Chunked/streaming transcription for very long recordings.
- DOCX/PDF export, translation, and AI summaries — all naturally cloud-only features (they need a
  language model beyond Whisper's transcription), so they'd hang off the same optional backend.

## License

App code: no license file has been added — add one if you plan to distribute this.
`app/src/main/cpp/whisper.cpp` is a submodule pointing at the upstream
[ggerganov/whisper.cpp](https://github.com/ggerganov/whisper.cpp) project, MIT licensed. The
JNI bridge in `app/src/main/cpp/jni.c` is adapted from whisper.cpp's own
`examples/whisper.android.java` sample.
