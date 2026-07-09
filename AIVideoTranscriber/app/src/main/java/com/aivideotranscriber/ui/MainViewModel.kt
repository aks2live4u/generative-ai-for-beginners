package com.aivideotranscriber.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aivideotranscriber.cleanup.CleanupManager
import com.aivideotranscriber.media.AudioExtractor
import com.aivideotranscriber.media.DownloadException
import com.aivideotranscriber.media.NoAudioTrackException
import com.aivideotranscriber.media.VideoDownloader
import com.aivideotranscriber.model.AccuracyTier
import com.aivideotranscriber.model.ModelDownloadException
import com.aivideotranscriber.model.ModelManager
import com.aivideotranscriber.util.LanguageOptions
import com.aivideotranscriber.whisper.WhisperContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val SUPPORTED_EXTENSIONS_MESSAGE =
    "Supported formats: MP4, MOV, MKV, AVI, WEBM, M4V, FLV, 3GP, MPEG."

class MainViewModel : ViewModel() {

    var inputMode by mutableStateOf(InputMode.FILE)
    var pickedFileUri by mutableStateOf<Uri?>(null)
    var pickedFileName by mutableStateOf<String?>(null)
    var urlText by mutableStateOf("")
    var language by mutableStateOf(LanguageOptions.AUTO.code)
    var accuracyTier by mutableStateOf(AccuracyTier.BEST)
    var timestampsEnabled by mutableStateOf(true)
    var fillerCleanupEnabled by mutableStateOf(false)

    var pipelineState by mutableStateOf<PipelineState>(PipelineState.Idle)
        private set

    val canStart: Boolean
        get() = when (inputMode) {
            InputMode.FILE -> pickedFileUri != null
            InputMode.URL -> urlText.isNotBlank()
        }

    private var whisperContext: WhisperContext? = null
    private var loadedTier: AccuracyTier? = null
    private var downloadedVideoFile: File? = null

    fun startTranscription(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                clearDownloadedVideo()
                val (samplePath, sampleFd, previewUri) = resolveAudioSource(appContext)

                pipelineState = PipelineState.ExtractingAudio(0)
                val pcm = withContext(Dispatchers.Default) {
                    try {
                        if (sampleFd != null) {
                            sampleFd.use {
                                AudioExtractor.extractMonoPcm16k(it.fileDescriptor) { p ->
                                    pipelineState = PipelineState.ExtractingAudio(p)
                                }
                            }
                        } else {
                            AudioExtractor.extractMonoPcm16k(samplePath!!) { p ->
                                pipelineState = PipelineState.ExtractingAudio(p)
                            }
                        }
                    } catch (e: NoAudioTrackException) {
                        throw PipelineException(e.message ?: "No audio track found.")
                    } catch (e: IOException) {
                        throw PipelineException("This file looks corrupted or isn't a supported video/audio format. $SUPPORTED_EXTENSIONS_MESSAGE")
                    }
                }

                if (pcm.isEmpty()) {
                    throw PipelineException("This file looks corrupted or isn't a supported video/audio format. $SUPPORTED_EXTENSIONS_MESSAGE")
                }

                val whisper = loadWhisperContext(appContext)

                pipelineState = PipelineState.Transcribing(0)
                val segments = whisper.transcribe(pcm, language) { p ->
                    pipelineState = PipelineState.Transcribing(p)
                }

                if (segments.isEmpty() || segments.all { it.text.isBlank() }) {
                    throw PipelineException("No speech was detected in this audio. Try a clearer recording or a different accuracy tier.")
                }

                pipelineState = PipelineState.Done(segments, previewUri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: PipelineException) {
                pipelineState = PipelineState.Failed(e.message ?: "Something went wrong.")
            } catch (e: DownloadException) {
                pipelineState = PipelineState.Failed(e.message ?: "Couldn't download this video.")
            } catch (e: ModelDownloadException) {
                pipelineState = PipelineState.Failed(e.message ?: "Couldn't download the on-device model. Check your internet connection.")
            } catch (e: OutOfMemoryError) {
                pipelineState = PipelineState.Failed("This device ran out of memory processing this video. Try a shorter clip or the \"Fast\" accuracy tier.")
            } catch (e: IOException) {
                pipelineState = PipelineState.Failed("A storage or network error occurred: ${e.message ?: "unknown error"}.")
            } catch (e: Exception) {
                pipelineState = PipelineState.Failed(e.message ?: "Something went wrong. Please try again.")
            }
        }
    }

    fun retry(context: Context) = startTranscription(context)

    fun reset(context: Context) {
        pipelineState = PipelineState.Idle
        pickedFileUri = null
        pickedFileName = null
        urlText = ""
        clearDownloadedVideo()
        CleanupManager.clearTempFiles(context.applicationContext)
    }

    /** Call when leaving the transcript screen or closing the app - deletes any video this app
     *  downloaded on the user's behalf. Files the user picked from their own device were never
     *  copied anywhere, so there is nothing of ours to delete for uploads. */
    fun clearDownloadedVideo() {
        downloadedVideoFile?.delete()
        downloadedVideoFile = null
    }

    override fun onCleared() {
        clearDownloadedVideo()
        // viewModelScope is already cancelled by the time onCleared() runs, so a fresh
        // runBlocking is used here to guarantee native memory is actually freed.
        whisperContext?.let { ctx -> kotlinx.coroutines.runBlocking { ctx.release() } }
    }

    private data class AudioSource(
        val path: String?,
        val fd: android.os.ParcelFileDescriptor?,
        val previewUri: Uri?,
    )

    private suspend fun resolveAudioSource(context: Context): AudioSource {
        return when (inputMode) {
            InputMode.FILE -> {
                val uri = pickedFileUri ?: throw PipelineException("Pick a video first.")
                val pfd = withContext(Dispatchers.IO) {
                    context.contentResolver.openFileDescriptor(uri, "r")
                        ?: throw PipelineException("Couldn't open this file.")
                }
                AudioSource(path = null, fd = pfd, previewUri = uri)
            }
            InputMode.URL -> {
                val url = urlText.trim()
                if (url.isBlank()) throw PipelineException("Paste a video link first.")
                pipelineState = PipelineState.Downloading(0)
                val dest = File(CleanupManager.tempDir(context), "download_${System.currentTimeMillis()}.mp4")
                val downloaded = withContext(Dispatchers.IO) {
                    VideoDownloader.download(url, dest) { p ->
                        pipelineState = PipelineState.Downloading(p)
                    }
                }
                downloadedVideoFile = downloaded
                val previewUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", downloaded)
                AudioSource(path = downloaded.absolutePath, fd = null, previewUri = previewUri)
            }
        }
    }

    private suspend fun loadWhisperContext(context: Context): WhisperContext {
        if (whisperContext != null && loadedTier == accuracyTier) {
            return whisperContext!!
        }
        whisperContext?.release()
        whisperContext = null

        pipelineState = PipelineState.LoadingModel(0)
        val modelManager = ModelManager(context)
        val modelFile = modelManager.ensureDownloaded(accuracyTier) { p ->
            pipelineState = PipelineState.LoadingModel(p)
        }
        val ctx = WhisperContext.load(modelFile.absolutePath)
        whisperContext = ctx
        loadedTier = accuracyTier
        return ctx
    }
}

private class PipelineException(message: String) : Exception(message)
