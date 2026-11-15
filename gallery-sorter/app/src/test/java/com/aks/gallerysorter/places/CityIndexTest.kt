package com.aks.gallerysorter.places

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class CityIndexTest {
    // Gradle unit tests run with the module dir as working directory.
    private val index = CityIndex(File("src/main/assets/cities.bin").inputStream().buffered())

    @Test fun mumbai() {
        val c = index.nearest(19.07, 72.88)
        assertThat(c).isNotNull()
        assertThat(c!!.name).isEqualTo("Mumbai")
        assertThat(c.country).isEqualTo("IN")
    }

    @Test fun delhi() {
        val c = index.nearest(28.61, 77.21)
        assertThat(c!!.name).isAnyOf("Delhi", "New Delhi")
    }

    @Test fun midOceanIsNull() {
        assertThat(index.nearest(0.0, -30.0)).isNull()
    }

    @Test fun goaResolvesToAGoaCity() {
        val c = index.nearest(15.4989, 73.8278)
        assertThat(c).isNotNull()
        assertThat(c!!.country).isEqualTo("IN")
        assertThat(c.lat).isWithin(0.5).of(15.5)
    }

    @Test fun nearAntimeridianDoesNotCrash() {
        index.nearest(-17.7, 179.9)
        index.nearest(-17.7, -179.9)
    }
}
