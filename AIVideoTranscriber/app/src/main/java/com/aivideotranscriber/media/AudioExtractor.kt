package com.aivideotranscriber.media

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.FileDescriptor
import java.nio.ByteOrder
import kotlin.math.roundToInt

class NoAudioTrackException : Exception("This file doesn't seem to contain any audio to transcribe.")

/**
 * Decodes the first audio track of a video/audio file into mono 16 kHz float32 PCM samples,
 * the exact format whisper.cpp expects. Uses Android's built-in MediaExtractor/MediaCodec so no
 * FFmpeg dependency is needed.
 */
object AudioExtractor {
    private const val TARGET_SAMPLE_RATE = 16_000

    fun extractMonoPcm16k(fd: FileDescriptor, onProgress: (Int) -> Unit = {}): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(fd)
        return extractFromExtractor(extractor, onProgress)
    }

    fun extractMonoPcm16k(path: String, onProgress: (Int) -> Unit = {}): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(path)
        return extractFromExtractor(extractor, onProgress)
    }

    private fun extractFromExtractor(extractor: MediaExtractor, onProgress: (Int) -> Unit): FloatArray {
        var audioTrackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                format = trackFormat
                break
            }
        }
        val audioFormat = format ?: run {
            extractor.release()
            throw NoAudioTrackException()
        }

        extractor.selectTrack(audioTrackIndex)
        val mime = audioFormat.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(audioFormat, null, null, 0)
        codec.start()

        val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) audioFormat.getLong(MediaFormat.KEY_DURATION) else 0L

        val pcmChunks = ArrayList<ShortArray>()
        var totalShorts = 0
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false

        try {
            while (!sawOutputEos) {
                if (!sawInputEos) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inBuffer = codec.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(inBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0)
                            extractor.advance()
                            if (durationUs > 0) {
                                onProgress((((presentationTimeUs * 100) / durationUs).toInt()).coerceIn(0, 100))
                            }
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outIndex >= 0) {
                    if (bufferInfo.size > 0) {
                        val outBuffer = codec.getOutputBuffer(outIndex)!!
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuffer = outBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                        val shorts = ShortArray(shortBuffer.remaining())
                        shortBuffer.get(shorts)
                        pcmChunks.add(shorts)
                        totalShorts += shorts.size
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEos = true
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        val interleaved = ShortArray(totalShorts)
        var offset = 0
        for (chunk in pcmChunks) {
            System.arraycopy(chunk, 0, interleaved, offset, chunk.size)
            offset += chunk.size
        }

        val mono = downmixToMono(interleaved, channelCount)
        onProgress(100)
        return resampleTo16k(mono, sampleRate)
    }

    private fun downmixToMono(samples: ShortArray, channelCount: Int): FloatArray {
        if (channelCount <= 1) {
            return FloatArray(samples.size) { samples[it] / 32768f }
        }
        val frameCount = samples.size / channelCount
        val mono = FloatArray(frameCount)
        for (i in 0 until frameCount) {
            var sum = 0
            for (c in 0 until channelCount) {
                sum += samples[i * channelCount + c]
            }
            mono[i] = (sum / channelCount) / 32768f
        }
        return mono
    }

    private fun resampleTo16k(input: FloatArray, inputSampleRate: Int): FloatArray {
        if (inputSampleRate == TARGET_SAMPLE_RATE || input.isEmpty()) return input
        val ratio = TARGET_SAMPLE_RATE.toDouble() / inputSampleRate
        val outputLength = (input.size * ratio).roundToInt()
        val output = FloatArray(outputLength)
        for (i in output.indices) {
            val srcPos = i / ratio
            val srcIndex = srcPos.toInt()
            val frac = (srcPos - srcIndex).toFloat()
            val s0 = input.getOrElse(srcIndex) { 0f }
            val s1 = input.getOrElse(srcIndex + 1) { s0 }
            output[i] = s0 + (s1 - s0) * frac
        }
        return output
    }
}
