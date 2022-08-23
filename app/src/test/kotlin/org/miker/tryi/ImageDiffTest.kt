package org.miker.tryi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.miker.tryi.ImageDiff.ImageDiffAlgo
import org.miker.tryi.ImageDiff.imageDiff

class ImageDiffTest {

    companion object {
        const val ITERATIONS = 50
        val aList = List(ITERATIONS) { Tryi.random() }
        val bList = List(ITERATIONS) { Tryi.random() }
    }

    @ParameterizedTest
    @EnumSource(ImageDiffAlgo::class)
    fun `time algos`(algo: ImageDiffAlgo) {
        // warm up
        aList.zip(bList) { a, b ->
            imageDiff(a.image, b.image, algo)
        }

        // run for real
        val start = System.currentTimeMillis()
        val results = aList.zip(bList) { a, b ->
            imageDiff(a.image, b.image, algo)
        }
        val end = System.currentTimeMillis()
        println("$algo ${end - start} ms")
        assertEquals(ITERATIONS, results.size)
    }
}
