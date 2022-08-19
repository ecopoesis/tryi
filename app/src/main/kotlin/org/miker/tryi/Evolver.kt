package org.miker.tryi;

import java.awt.image.BufferedImage

abstract class Evolver(
    private val target: BufferedImage,
    private val previewPanel: GeneratedPreview
) {

    companion object {
        const val MUTATION_CHANCE_PCT: Double = 1.0
        const val MUTATION_AMOUNT_PCT: Double = 10 / 200.0
        const val FITNESS_THRESHOLD: Double = 0.99
        const val NUM_TRIANGLES: Int = 150
        const val NUM_CHILDREN: Int = 50
    }
}
