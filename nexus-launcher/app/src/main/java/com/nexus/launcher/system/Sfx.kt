package com.nexus.launcher.system

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Tiny synthesized UI sounds: a soft sine "blip" with a fast decay. No audio assets needed. */
object Sfx {
    private val executor = Executors.newSingleThreadExecutor()
    private const val RATE = 44_100

    fun tap() = blip(1_480.0, 28, 0.05f)
    fun open() = sweep(660.0, 1_320.0, 90, 0.06f)
    fun listen() = sweep(880.0, 1_760.0, 120, 0.07f)
    fun done() = sweep(1_320.0, 990.0, 110, 0.06f)

    private fun blip(freq: Double, ms: Int, vol: Float) = sweep(freq, freq, ms, vol)

    private fun sweep(from: Double, to: Double, ms: Int, vol: Float) {
        executor.execute {
            runCatching {
                val n = RATE * ms / 1000
                val pcm = ShortArray(n)
                var phase = 0.0
                for (i in 0 until n) {
                    val t = i.toDouble() / n
                    val f = from + (to - from) * t
                    phase += 2 * PI * f / RATE
                    val env = (1 - exp(-i / 60.0)) * exp(-4.0 * t)
                    pcm[i] = (sin(phase) * env * vol * Short.MAX_VALUE).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(n * 2)
                    .build()
                track.write(pcm, 0, n)
                track.play()
                Thread.sleep(ms + 40L)
                track.release()
            }
        }
    }
}
