package org.miker.tryi;

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage

abstract class Evolver(
    private val numTriangles: Int,
    private val target: BufferedImage,
    private val previewPanel: GeneratedPreview
) {

    companion object {
        const val FITNESS_THRESHOLD: Double = 0.99
        const val NUM_TRIANGLES: Int = 150
    }

    abstract fun evolve(): TryiMatch

    /**
     * Create the initial triangles. Randomly generates triangles until we have [numTriangles].
     * For each new triangle, [numChildren] are generated and the best is chosen.
     */
    fun buildInitialTriy(numChildren: Int): Tryi {
        fun addTriangle(tryi: Tryi): TryiMatch {
            val triangle = Triangle.random()

            val candidate = tryi.image.deepCopy()
            val g2d = candidate.createGraphics()
            g2d.color = triangle.color.asColor
            g2d.fillPolygon(triangle.x, triangle.y, 3)
            g2d.dispose()

            return TryiMatch(tryi.triangles + triangle, candidate, imageDiff(target, candidate))
        }

        tailrec fun inner(tryi: Tryi): Tryi =
            when (tryi.triangles.size) {
                numTriangles -> tryi
                else -> {
                    val children = runBlocking {
                        (0 until numChildren).map {
                            async(Dispatchers.IO) {
                                addTriangle(tryi)
                            }
                        }.awaitAll()
                    }
                    val best = children.sortedBy { it.diff }.first()
                    println("gen, ${tryi.triangles.size}, ${(1 - best.diff) * 100}")
                    //previewPanel.update(best.image())

                    inner(best.tryi)
                }
            }

        return inner(Tryi.empty())
    }
}
