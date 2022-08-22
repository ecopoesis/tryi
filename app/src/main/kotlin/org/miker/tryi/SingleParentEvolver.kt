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
    private val numTriangles: Int = NUM_TRIANGLES,
    private val numChildren: Int = NUM_CHILDREN,
    private val fitnessThreshold: Double = FITNESS_THRESHOLD,
) : Evolver(target, previewPanel) {

    /**
     * Our best image so far.
     */
    var tryi: Tryi

    /**
     * Create the initial triangles. Randomly generates triangles until we have [numTriangles].
     * For each new triangle, [numChildren] are generated and the best is chosen.
     */
    init {
        fun addTriangle(tryi: Tryi): TryiMatch {
            val triangle =
                Triangle(
                    Point.random(),
                    Point.random(),
                    Point.random(),
                    TryiColor.random()
                )

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
                    println("Generated ${tryi.triangles.size}: ${(1 - best.diff) * 100}% correct")
                    previewPanel.update(best.image())

                    inner(best.tryi)
                }
            }

        val (triangles, image) = inner(
            Tryi(emptyList(), Utilities.emptyBufferedImage())
        )
        tryi = Tryi(triangles, image)
    }

    fun evolve() {
        fun mutate(parent: List<Triangle>): TryiMatch {
            val child = parent.map { triangle ->
                when (Random.nextInt(1, 101) <= MUTATION_CHANCE_PCT) {
                    false -> triangle
                    else -> triangle.mutate(MUTATION_AMOUNT_PCT)
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
                    if (best.diff < tryiMatch.diff) {
                        val msg = "Evolved ($generation): ${(1 - best.diff) * 100}% correct"
                        if (generation % UPDATE_RATE == 0L) {
                            previewPanel.update(best.image())
                            println("$msg - ${(generation.toDouble() / (System.currentTimeMillis() - start)) * 1000} generations/sec")
                        } else {
                            println(msg)
                        }
                        inner(best, generation + 1, start)
                    } else {
                        println("Bad ($generation): ${(1 - tryiMatch.diff) * 100}% correct")
                        inner(tryiMatch, generation + 1, start)
                    }
                }
            }

        val (evolved, _) = inner(
            TryiMatch(
                tryi,
                imageDiff(target, tryi.image)
            ),
            0,
            System.currentTimeMillis()
        )

        tryi = evolved
    }
}
