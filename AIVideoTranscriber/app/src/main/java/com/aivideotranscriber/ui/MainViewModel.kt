package com.aivideotranscriber.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aivideotranscriber.media.AudioExtractor
import com.aivideotranscriber.media.NoAudioTrackException
import com.aivideotranscriber.model.AccuracyTier
import com.aivideotranscriber.model.ModelDownloadException
import com.aivideotranscriber.model.ModelManager
import com.aivideotranscriber.util.LanguageOptions
import com.aivideotranscriber.whisper.WhisperContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val SUPPORTED_EXTENSIONS_MESSAGE =
    "Supported formats: MP4, MOV, MKV, AVI, WEBM, M4V, FLV, 3GP, MPEG."

class MainViewModel : ViewModel() {

    var pickedFileUri by mutableStateOf<Uri?>(null)
    var pickedFileName by mutableStateOf<String?>(null)
    var language by mutableStateOf(LanguageOptions.AUTO.code)
    var accuracyTier by mutableStateOf(AccuracyTier.BEST)
    var timestampsEnabled by mutableStateOf(true)
    var fillerCleanupEnabled by mutableStateOf(false)

    var pipelineState by mutableStateOf<PipelineState>(PipelineState.Idle)
        private set

    val canStart: Boolean
        get() = pickedFileUri != null

    private var whisperContext: WhisperContext? = null
    private var loadedTier: AccuracyTier? = null

    fun startTranscription(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val uri = pickedFileUri ?: throw PipelineException("Pick a video first.")
                val pfd = withContext(Dispatchers.IO) {
                    appContext.contentResolver.openFileDescriptor(uri, "r")
                        ?: throw PipelineException("Couldn't open this file.")
                }

                pipelineState = PipelineState.ExtractingAudio(0)
                val pcm = withContext(Dispatchers.Default) {
                    try {
                        pfd.use {
                            AudioExtractor.extractMonoPcm16k(it.fileDescriptor) { p ->
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

                pipelineState = PipelineState.Done(segments, uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: PipelineException) {
                pipelineState = PipelineState.Failed(e.message ?: "Something went wrong.")
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

    fun reset() {
        pipelineState = PipelineState.Idle
        pickedFileUri = null
        pickedFileName = null
    }

    override fun onCleared() {
        // viewModelScope is already cancelled by the time onCleared() runs, so a fresh
        // runBlocking is used here to guarantee native memory is actually freed.
        whisperContext?.let { ctx -> kotlinx.coroutines.runBlocking { ctx.release() } }
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
