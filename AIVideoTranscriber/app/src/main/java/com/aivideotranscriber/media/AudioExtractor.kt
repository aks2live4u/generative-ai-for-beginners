package com.aivideotranscriber.media

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import java.io.FileDescriptor
import java.nio.ByteOrder

class NoAudioTrackException : Exception("This file doesn't seem to contain any audio to transcribe.")

class AudioDecodeTimeoutException : Exception(
    "Audio decoding got stuck and timed out. This can happen with certain files on some devices " +
        "- try picking the file again, or try a different file.",
)

/**
 * Decodes the first audio track of a video/audio file into mono 16 kHz float32 PCM samples,
 * the exact format whisper.cpp expects. Uses Android's built-in MediaExtractor/MediaCodec so no
 * FFmpeg dependency is needed.
 *
 * Downmixing and resampling happen incrementally, one decoded chunk at a time, instead of
 * buffering the whole file at full resolution first - a 15-minute 44.1kHz stereo recording is
 * ~300MB at full resolution vs. ~55MB as 16kHz mono, and materializing the full-resolution
 * version (plus intermediate copies) was blowing past the heap on real devices.
 */
object AudioExtractor {
    private const val TARGET_SAMPLE_RATE = 16_000

    // Decoding should always be fast - seconds, at most low tens of seconds even for long files,
    // since it's a straight decode, not ML inference. Some devices' hardware decoders are known
    // to occasionally never signal end-of-stream for a particular file, which would otherwise
    // hang the (synchronous, non-cancellable) decode loop below forever with no way out.
    private const val MAX_DECODE_DURATION_MS = 3 * 60 * 1000L

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

        // These are the *container's* declared format, used only to size the output buffer and
        // as a fallback. The decoder's actual output format - read below via
        // INFO_OUTPUT_FORMAT_CHANGED - is what's authoritative, and can legitimately differ (HE-AAC
        // streams commonly decode at 2x the sample rate declared in the container, for example).
        var sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) audioFormat.getLong(MediaFormat.KEY_DURATION) else 0L

        // Pre-size the output buffer from the file's reported duration so it rarely (if ever)
        // needs to grow/copy - falls back to a modest guess if duration isn't reported.
        val estimatedOutputSamples = if (durationUs > 0) {
            ((durationUs / 1_000_000.0) * TARGET_SAMPLE_RATE).toInt() + TARGET_SAMPLE_RATE
        } else {
            TARGET_SAMPLE_RATE * 60
        }
        val output = GrowableFloatBuffer(estimatedOutputSamples.coerceAtLeast(1024))
        var resampler = StreamingResampler(sampleRate, TARGET_SAMPLE_RATE)
        var producedAnyOutput = false

        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false
        val decodeDeadline = SystemClock.elapsedRealtime() + MAX_DECODE_DURATION_MS

        try {
            while (!sawOutputEos) {
                if (SystemClock.elapsedRealtime() > decodeDeadline) {
                    throw AudioDecodeTimeoutException()
                }
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
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val actualFormat = codec.outputFormat
                        val actualSampleRate = actualFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        val actualChannelCount = actualFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        if (actualSampleRate != sampleRate || actualChannelCount != channelCount) {
                            if (producedAnyOutput) {
                                // Format changed mid-stream (rare) - flush what the old format
                                // produced so far and continue fresh rather than corrupting it.
                                resampler.finish(output)
                            }
                            sampleRate = actualSampleRate
                            channelCount = actualChannelCount
                            resampler = StreamingResampler(sampleRate, TARGET_SAMPLE_RATE)
                        }
                    }
                    outIndex >= 0 -> {
                        if (bufferInfo.size > 0) {
                            val outBuffer = codec.getOutputBuffer(outIndex)!!
                            outBuffer.position(bufferInfo.offset)
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val shortBuffer = outBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                            val shorts = ShortArray(shortBuffer.remaining())
                            shortBuffer.get(shorts)

                            val monoChunk = downmixToMono(shorts, channelCount)
                            resampler.process(monoChunk, output)
                            producedAnyOutput = true
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            sawOutputEos = true
                        }
                    }
                    // INFO_TRY_AGAIN_LATER (-1) or the deprecated INFO_OUTPUT_BUFFERS_CHANGED (-3):
                    // nothing to do, loop again.
                }
            }
            resampler.finish(output)
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        onProgress(100)
        return output.toFloatArray()
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
}

/** A growable primitive float array - like ArrayList<Float> but without the boxing overhead,
 *  which matters at the scale of tens of millions of audio samples. */
private class GrowableFloatBuffer(initialCapacity: Int) {
    private var array = FloatArray(initialCapacity.coerceAtLeast(16))
    private var size = 0

    fun append(value: Float) {
        if (size == array.size) {
            array = array.copyOf(array.size * 2)
        }
        array[size] = value
        size++
    }

    fun toFloatArray(): FloatArray = if (size == array.size) array else array.copyOf(size)
}

/**
 * Linear-interpolation resampler that consumes mono input one decoded chunk at a time and
 * appends the resampled output as it goes, so the full-resolution audio is never held in memory
 * all at once. Interpolating across a chunk boundary needs the last sample of the previous
 * chunk, which is carried over between calls to [process].
 */
private class StreamingResampler(private val sourceSampleRate: Int, private val targetSampleRate: Int) {
    private val ratio = targetSampleRate.toDouble() / sourceSampleRate
    private var globalInputSamplesConsumed = 0L
    private var nextOutputIndex = 0L
    private var prevSample = 0f

    fun process(chunk: FloatArray, dest: GrowableFloatBuffer) {
        if (chunk.isEmpty()) return

        if (sourceSampleRate == targetSampleRate) {
            for (sample in chunk) dest.append(sample)
            globalInputSamplesConsumed += chunk.size
            prevSample = chunk.last()
            return
        }

        val chunkStart = globalInputSamplesConsumed
        val chunkEnd = chunkStart + chunk.size

        while (true) {
            val srcPosGlobal = nextOutputIndex / ratio
            // Stop once producing the next sample would require data past this chunk - the
            // remainder is resolved once the next chunk (or finish()) arrives.
            if (srcPosGlobal >= chunkEnd - 1) break

            val srcIndexGlobal = srcPosGlobal.toLong()
            val frac = (srcPosGlobal - srcIndexGlobal).toFloat()
            val localIndex0 = (srcIndexGlobal - chunkStart).toInt()
            val s0 = if (localIndex0 < 0) prevSample else chunk[localIndex0]
            val s1 = chunk[localIndex0 + 1]
            dest.append(s0 + (s1 - s0) * frac)
            nextOutputIndex++
        }

        globalInputSamplesConsumed = chunkEnd
        prevSample = chunk.last()
    }

    /** Flushes any trailing output sample(s) that only needed data up to the very last input
     *  sample (holds the tail steady rather than interpolating towards silence). */
    fun finish(dest: GrowableFloatBuffer) {
        if (sourceSampleRate == targetSampleRate) return
        while (nextOutputIndex / ratio < globalInputSamplesConsumed) {
            dest.append(prevSample)
            nextOutputIndex++
        }
    }
}
