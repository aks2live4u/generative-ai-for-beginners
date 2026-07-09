package com.aivideotranscriber.whisper

import java.io.File

internal object WhisperCpuConfig {

    /**
     * Nearly every modern phone SoC is big.LITTLE (or big.MID.LITTLE): a handful of fast
     * "performance" cores alongside slower "efficiency" cores. whisper.cpp splits its workload
     * evenly across however many threads you give it, so including the slow efficiency cores in
     * the thread count drags the fast cores down to their speed instead of speeding things up.
     * This reads each core's max clock speed from sysfs and only counts cores in the fastest
     * cluster(s) towards the thread count, ignoring the efficiency cluster.
     */
    fun preferredThreadCount(): Int {
        val totalCores = Runtime.getRuntime().availableProcessors()
        val bigCoreCount = countHighPerformanceCores(totalCores)
        val threads = if (bigCoreCount in 1 until totalCores) bigCoreCount else totalCores - 1
        return threads.coerceAtLeast(2)
    }

    private fun countHighPerformanceCores(totalCores: Int): Int = try {
        val maxFreqsKHz = (0 until totalCores).mapNotNull { cpu -> readMaxFreqKHz(cpu) }
        val fastest = maxFreqsKHz.maxOrNull()
        if (fastest == null || fastest <= 0L) {
            0
        } else {
            // Cores within 75% of the fastest core's clock are treated as the "big" cluster
            // (this keeps prime + performance cores together on 3-cluster chips like Dimensity
            // 9x00/Snapdragon 8 Gen/Elite, while excluding the much slower efficiency cluster).
            val threshold = (fastest * 0.75).toLong()
            maxFreqsKHz.count { it >= threshold }
        }
    } catch (e: Exception) {
        0
    }

    private fun readMaxFreqKHz(cpu: Int): Long? = try {
        File("/sys/devices/system/cpu/cpu$cpu/cpufreq/cpuinfo_max_freq")
            .readText()
            .trim()
            .toLongOrNull()
    } catch (e: Exception) {
        null
    }
}
