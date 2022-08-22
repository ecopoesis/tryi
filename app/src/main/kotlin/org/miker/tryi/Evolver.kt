package org.miker.tryi;

import java.awt.image.BufferedImage

abstract class Evolver(
    private val target: BufferedImage,
    private val previewPanel: GeneratedPreview
) {

    companion object {
        const val FITNESS_THRESHOLD: Double = 0.99
        const val NUM_TRIANGLES: Int = 150
    }

    abstract fun evolve(): TryiMatch
}
