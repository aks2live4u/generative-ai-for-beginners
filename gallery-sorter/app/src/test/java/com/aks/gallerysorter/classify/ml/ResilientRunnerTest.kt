package com.aks.gallerysorter.classify.ml

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test

class ResilientRunnerTest {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "model-thread") }

    @After fun tearDown() {
        executor.shutdownNow()
    }

    private class Fake(val name: String, var failRuns: Int = 0, val failSmoke: Boolean = false) : AutoCloseable {
        val threads = mutableListOf<String>()
        var closed = false
        fun call(): String {
            threads += Thread.currentThread().name
            if (failRuns > 0) {
                failRuns--
                throw IllegalStateException("$name failed")
            }
            return name
        }
        override fun close() {
            threads += Thread.currentThread().name
            closed = true
        }
    }

    private val smoke: (Fake) -> Unit = { if (it.failSmoke) throw IllegalStateException("smoke") else it.call() }

    @Test fun usesPreferredWhenItWorks() {
        val gpu = Fake("gpu")
        val r = ResilientRunner(executor, { gpu }, { Fake("cpu") }, smoke)
        assertThat(r.onPreferred).isTrue()
        assertThat(r.run { it.call() }).isEqualTo("gpu")
    }

    @Test fun preferredUnavailableUsesFallback() {
        val r = ResilientRunner(executor, { null }, { Fake("cpu") }, smoke)
        assertThat(r.onPreferred).isFalse()
        assertThat(r.run { it.call() }).isEqualTo("cpu")
    }

    @Test fun preferredConstructionThrowingUsesFallback() {
        val r = ResilientRunner<Fake>(executor, { throw UnsatisfiedLinkError("no gpu") }, { Fake("cpu") }, smoke)
        assertThat(r.run { it.call() }).isEqualTo("cpu")
    }

    @Test fun preferredFailingSmokeRunIsClosedAndFallbackUsed() {
        val gpu = Fake("gpu", failSmoke = true)
        val r = ResilientRunner(executor, { gpu }, { Fake("cpu") }, smoke)
        assertThat(gpu.closed).isTrue()
        assertThat(r.run { it.call() }).isEqualTo("cpu")
    }

    @Test fun firstInferenceFailureRebuildsOnFallbackAndRetriesOnce() {
        val gpu = Fake("gpu")
        var cpuBuilds = 0
        val r = ResilientRunner(executor, { gpu }, { cpuBuilds++; Fake("cpu") }, smoke)
        gpu.failRuns = 1
        assertThat(r.run { it.call() }).isEqualTo("cpu")
        assertThat(gpu.closed).isTrue()
        assertThat(r.onPreferred).isFalse()
        assertThat(r.run { it.call() }).isEqualTo("cpu")
        assertThat(cpuBuilds).isEqualTo(1)
    }

    @Test fun failureOnFallbackPropagatesWithoutRetryLoop() {
        val cpu = Fake("cpu")
        val r = ResilientRunner(executor, { null }, { cpu }, smoke)
        cpu.failRuns = 5
        val e = assertThrows(IllegalStateException::class.java) { r.run { it.call() } }
        assertThat(e).hasMessageThat().isEqualTo("cpu failed")
        assertThat(cpu.failRuns).isEqualTo(4) // tried exactly once
    }

    @Test fun fallbackSmokeFailureFailsConstruction() {
        assertThrows(IllegalStateException::class.java) {
            ResilientRunner(executor, { null }, { Fake("cpu", failSmoke = true) }, smoke)
        }
    }

    @Test fun createRunAndCloseAllHappenOnTheExecutorThread() {
        val gpu = Fake("gpu")
        var builtOn = ""
        val r = ResilientRunner(executor, { builtOn = Thread.currentThread().name; gpu }, { Fake("cpu") }, smoke)
        r.run { it.call() }
        r.close()
        assertThat(builtOn).isEqualTo("model-thread")
        assertThat(gpu.threads).containsExactly("model-thread", "model-thread", "model-thread")
        assertThat(gpu.closed).isTrue()
    }
}
