package org.miker.tryi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.random.Random

class MultiParentEvolver(
    private val target: BufferedImage,
    private val previewPanel: GeneratedPreview,
    private val mutationChance: Double = 0.01,
    private val mutationAmount: Double = 0.10,
    private val selectionCutoff: Double = 0.15,
    private val populationSize: Int = 100,
    private val numTriangles: Int = NUM_TRIANGLES,
    private val fitnessThreshold: Double = FITNESS_THRESHOLD
) : Evolver(target, previewPanel) {

    /**
     * Generate the initial random population
     */
    private fun generateRandomPopulation(): List<Tryi> =
        List(populationSize) { Tryi(List(numTriangles) { Triangle.random() }) }

    /**
     * Merge two parents evenly with mutations.
     */
    private fun createChild(p1: List<Triangle>, p2: List<Triangle>): List<Triangle> =
        p1.zip(p2).map { parents ->
            // choose one gene (triangle) randomly from a parent
            parents.choose().let { base ->
                // decide to mutate that triangle or not
                when {
                    (Random.nextDouble(0.0, 1.0) <= mutationChance) -> base.mutate(mutationAmount)
                    else -> base
                }
            }
        }

    /**
     * Create the next generation. The most successful parents will be paired to breed a new generation the same size
     * as the previous generation.
     */
    private fun generateChildren(population: List<Tryi>): List<Tryi> {
        // take only the "good" part of the population
        val parents = population.take(max(1, (population.size * selectionCutoff).toInt()))
        val children = runBlocking {
            (0 until populationSize).map {
                async(Dispatchers.IO) {
                    // find two random members of the population
                    val p = Random.uniqueInt(2, parents.indices)

                    // create a child from the parents
                    Tryi(createChild(parents[p.first()].triangles, parents[p.last()].triangles))
                }
            }.awaitAll()
        }
        return children
    }

    override fun evolve(): TryiMatch {
        tailrec fun inner(population: List<TryiMatch>, generation: Long, start: Long): List<TryiMatch> =
            when {
                population.first().diff >= fitnessThreshold -> population
                else -> {
                    val children = generateChildren(population.map { it.tryi }).map { tryi ->
                        TryiMatch(tryi, imageDiff(target, tryi.image))
                    }.sortedBy { it.diff }

                    val best = children.first()
                    val rate = (generation.toDouble() / (System.currentTimeMillis() - start)) * 1000
                    val next = when {
                        best.diff < population.first().diff -> {
                            println("good, $generation, ${(1 - best.diff) * 100}, $rate")
                            previewPanel.update(best.image())
                            children
                        }
                        else -> {
                            println("bad, $generation, ${(1 - population.first().diff) * 100}, $rate")
                            population
                        }
                    }
                    if (generation % OUTPUT_RATE == 0L) {
                        println(best.tryi.serialize())
                    }
                    inner(next, generation + 1, start)
                }
            }

        // build initial population. population is always sorted from best to worst
        val population = generateRandomPopulation().map { tryi ->
            TryiMatch(tryi, imageDiff(target, tryi.image))
        }.sortedBy { it.diff }

        return inner(population, 0, System.currentTimeMillis()).first()
    }
}

fun <A> Pair<A, A>.choose(chance: Double = 0.5): A =
    when {
        Random.nextDouble() < chance -> this.first
        else -> this.second
    }
