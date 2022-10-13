package org.miker.tryi

import arrow.core.Option
import arrow.core.toOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.miker.tryi.ImageDiff.imageDiff
import org.miker.tryi.Triangle.MutationType
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
    private val previewPanel: Option<GeneratedPreview>,
    baseName: String,
    private val numTriangles: Int,
    private val mutationChance: Double,
    private val mutationAmount: Double,
    private val mutationType: MutationType,
    private val ogX: Int,
    private val ogY: Int,
    private val numChildren: Int = 50,
    private val fitnessThreshold: Double = FITNESS_THRESHOLD,
) : Evolver(numTriangles, target, baseName, ogX, ogY) {
    override fun evolve(): TryiMatch {
        fun mutate(parent: List<Triangle>): TryiMatch {
            val child = parent.map { triangle ->
                triangle.mutate(mutationType, mutationChance, mutationAmount)
            }
            val candidate = child.render()

            return TryiMatch(child, candidate, ogX, ogY, imageDiff(target, candidate))
        }

        tailrec fun inner(tryiMatch: TryiMatch, generation: Long, start: Long): TryiMatch =
            when {
                tryiMatch.diff >= fitnessThreshold -> tryiMatch
                else -> {
                    val children = runBlocking {
                        (0 until numChildren).map {
                            async(dispatcher) {
                                mutate(tryiMatch.triangles())
                            }
                        }.awaitAll()
                    }
                    val best = children.sortedBy { it.diff }.first()
                    val rate = (generation.toDouble() / (System.currentTimeMillis() - start)) * 1000
                    val next = when {
                        best.diff < tryiMatch.diff -> {
                            println("good, $generation, ${(1 - best.diff) * 100}, $rate")
                            previewPanel.map { it.update(best.image()) }
                            output(best.tryi, generation.toOption())
                            best
                        }
                        else -> {
                            println("bad, $generation, ${(1 - tryiMatch.diff) * 100}, $rate")
                            tryiMatch
                        }
                    }
                    inner(next, generation + 1, start)
                }
            }

        val tryi = buildInitialTriy(numChildren)

        val result = inner(
            TryiMatch(
                tryi,
                imageDiff(target, tryi.image)
            ),
            0,
            System.currentTimeMillis()
        )
        output(result.tryi)
        return result
    }
}
