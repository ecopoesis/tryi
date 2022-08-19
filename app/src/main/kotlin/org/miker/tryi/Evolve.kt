package org.miker.tryi

import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.random.Random

class Evolve(
    private val mutationChance: Int,
    private val mutationAmount: Double,
    private val successCutoff: Double,
    private val fitnessThreshold: Double
) {

    /**
     * Merge two parents evenly with mutations.
     */
    private fun createChild(p1: List<Triangle>, p2: List<Triangle>): List<Triangle> =
        p1.zip(p2).map { parents ->
            parents.choose().let { base ->
                when {
                    (Random.nextInt(1, 101) <= mutationChance) -> base
                    else -> base.mutate(mutationAmount)
                }
            }
        }

    /**
     * Create the next generation. The most successful parents will be paired to breed a new generation the same size
     * as the previous generation.
     */
    private fun generateChildren(population: List<List<Triangle>>): List<List<Triangle>> {
        val parents = population.take(max(1, (population.size * successCutoff).toInt()))
        return List(population.size) {
            val p = Random.uniqueInt(2, population.indices)
            createChild(parents[p.first()], parents[p.last()])
        }
    }

    fun evolver(population: List<Triple<List<Triangle>, BufferedImage, Double>>, target: BufferedImage, preview: GeneratedPreview, generation: Long = 0, start: Long = System.currentTimeMillis()): List<Triangle> =
        when {
            population.first().third >= FITNESS_THRESHOLD -> population.first().first
            else -> {
                val children = generateChildren(population.map { it.first }).map { child ->
                    child.render().let { img ->
                        Triple(child, img, imageDiff(target, img))
                    }
                }.sortedBy { it.third }

                if (children.first().third < population.first().third) {
                    println("Evolved ($generation): ${(1 - children.first().third) * 100}% correct - ${(generation.toDouble() / (System.currentTimeMillis() - start)) * 1000} generations/sec")
                    evolver(children, target, preview, generation + 1, start)
                } else {
                    println("Bad ($generation): ${(1 - population.first().third) * 100}% correct")
                    evolver(population, target, preview, generation + 1, start)
                }
            }
        }
}

fun <A> Pair<A, A>.choose(chance: Double = 0.5): A =
    when {
        Random.nextDouble() < chance -> this.first
        else -> this.second
    }
