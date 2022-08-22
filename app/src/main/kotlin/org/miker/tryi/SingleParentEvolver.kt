package org.miker.tryi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import kotlin.random.Random

/**
 * Evolves an image that matches [target] within [fitnessThreshold].
 * Starts by building an image one triangle at a time until there are [numTriangles]. For each triangle added,
 * [numChildren] are tried, and the best is chosen.
 *
 * Once initialized, evolution:
 * Each generation [numChildren] children are created from the parent. Each child's genes have [mutationChancePct] of
 * changing, and each gene will mutate by [mutationChancePct].
 */
class SingleParentEvolver(
    private val target: BufferedImage,
    private val previewPanel: GeneratedPreview,
    private val mutationChance: Double = 0.01,
    private val mutationAmount: Double = 0.05,
    private val numChildren: Int = 50,
    private val numTriangles: Int = NUM_TRIANGLES,
    private val fitnessThreshold: Double = FITNESS_THRESHOLD,
) : Evolver(target, previewPanel) {
    /**
     * Create the initial triangles. Randomly generates triangles until we have [numTriangles].
     * For each new triangle, [numChildren] are generated and the best is chosen.
     */
    fun initialize(): Tryi {
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
                    previewPanel.update(best.image())

                    inner(best.tryi)
                }
            }

        return inner(Tryi.empty())
    }

    override fun evolve(): TryiMatch {
        fun mutate(parent: List<Triangle>): TryiMatch {
            val child = parent.map { triangle ->
                when {
                    (Random.nextDouble(0.0, 1.0) <= mutationChance) -> triangle.mutate(mutationAmount)
                    else -> triangle
                }
            }
            val candidate = child.render()

            return TryiMatch(child, candidate, imageDiff(target, candidate))
        }

        tailrec fun inner(tryiMatch: TryiMatch, generation: Long, start: Long): TryiMatch =
            when {
                tryiMatch.diff >= fitnessThreshold -> tryiMatch
                else -> {
                    val children = runBlocking {
                        (0 until numChildren).map {
                            async(Dispatchers.IO) {
                                mutate(tryiMatch.triangles())
                            }
                        }.awaitAll()
                    }
                    val best = children.sortedBy { it.diff }.first()
                    val rate = (generation.toDouble() / (System.currentTimeMillis() - start)) * 1000
                    val next = when {
                        best.diff < tryiMatch.diff -> {
                            println("good, $generation, ${(1 - best.diff) * 100}, $rate")
                            previewPanel.update(best.image())
                            best
                        }
                        else -> {
                            println("bad, $generation, ${(1 - tryiMatch.diff) * 100}, $rate")
                            tryiMatch
                        }
                    }
                    if (generation % OUTPUT_RATE == 0L) {
                        println(next.tryi.serialize())
                    }

                    inner(next, generation + 1, start)
                }
            }

        val tryi = initialize()

        return inner(
            TryiMatch(
                tryi,
                imageDiff(target, tryi.image)
            ),
            0,
            System.currentTimeMillis()
        )
    }
}
