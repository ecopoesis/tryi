package org.miker.tryi

import arrow.core.Option
import arrow.core.toOption
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.miker.tryi.ImageDiff.imageDiff
import org.miker.tryi.Triangle.MutationType
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.random.Random

class MultiParentEvolver(
    private val target: BufferedImage,
    private val previewPanel: Option<GeneratedPreview>,
    baseName: String,
    private val numTriangles: Int,
    private val mutationChance: Double,
    private val mutationAmount: Double,
    private val mutationType: MutationType,
    private val select: (population: List<TryiMatch>) -> Pair<Tryi, Tryi> = { tournament(it,2) },
    private val populationSize: Int = 50,
    private val fitnessThreshold: Double = FITNESS_THRESHOLD
) : Evolver(numTriangles, target, baseName) {

    /**
     * Generate the initial random population
     */
    private fun generateRandomPopulation(): List<Tryi> =
        List(populationSize) { Tryi(List(numTriangles) { Triangle.random() }) }

    private fun generateFitPopulation(): List<Tryi> = List(populationSize) { buildInitialTriy(populationSize) }

    /**
     * Merge two parents evenly with mutations.
     */
    private fun createChild(p1: List<Triangle>, p2: List<Triangle>): List<Triangle> =
        p1.zip(p2).map { parents ->
            // choose one gene (triangle) randomly from a parent
            parents.choose().mutate(mutationType, mutationChance, mutationAmount)
        }

    /**
     * Create the next generation. The most successful parents will be paired to breed a new generation the same size
     * as the previous generation.
     */
    private fun generateChildren(population: List<TryiMatch>): List<Tryi> {
        val children = runBlocking {
            (0 until populationSize).map {
                async(dispatcher) {
                    val parents = select(population)

                    // create a child from the parents
                    Tryi(createChild(parents.first.triangles, parents.second.triangles))
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
                    val next = runBlocking {
                        val children = generateChildren(population).pmap { tryi ->
                            TryiMatch(tryi, imageDiff(target, tryi.image))
                        }.sortedBy { it.diff }

                        val best = children.first()
                        val rate = (generation.toDouble() / (System.currentTimeMillis() - start)) * 1000
                        val next = when {
                            best.diff < population.first().diff -> {
                                println("good, $generation, ${(1 - best.diff) * 100}, $rate")
                                previewPanel.map { it.update(best.image()) }
                                output(best.tryi, generation.toOption())
                                children
                            }

                            else -> {
                                println("bad, $generation, ${(1 - population.first().diff) * 100}, $rate")
                                population
                            }
                        }
                        next
                    }
                    inner(next, generation + 1, start)
                }
            }

        // build initial population. population is always sorted from best to worst
        val population = generateRandomPopulation().map { tryi ->
            TryiMatch(tryi, imageDiff(target, tryi.image))
        }.sortedBy { it.diff }

        val result = inner(population, 0, System.currentTimeMillis()).first()
        output(result.tryi)
        return result
    }
}

fun <A> Pair<A, A>.choose(chance: Double = 0.5): A =
    when {
        Random.nextDouble() < chance -> this.first
        else -> this.second
    }

/**
 * Take only the top [selectionCutoff] of the population.
 */
fun cutoff(population: List<TryiMatch>, selectionCutoff: Double): Pair<Tryi, Tryi> {
    val possible = population.take(max(1, (population.size * selectionCutoff).toInt())).map { it.tryi }
    val parents = possible.random(2)
    return Pair(parents.first(), parents.last())
}

/**
 * Tournament selection: choose [k] individuals from the population. Choose the best. Repeat to get another parent.
 */
fun tournament(population: List<TryiMatch>, k: Int = 2): Pair<Tryi, Tryi> {
    // run a single tournament
    fun singleTournament(): Tryi = population.random(k).minByOrNull { it.diff }!!.tryi

    return Pair(singleTournament(), singleTournament())
}
