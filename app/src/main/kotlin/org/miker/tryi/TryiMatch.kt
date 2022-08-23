package org.miker.tryi

import java.awt.image.BufferedImage

/**
 * A tryi and how well it matches.
 */
data class TryiMatch(
    val tryi: Tryi,
    val diff: Double
) {
    constructor(triangles: List<Triangle>, image: BufferedImage, match: Double) : this(Tryi(triangles, image), match)

    fun image(): BufferedImage = tryi.image
    fun triangles(): List<Triangle> = tryi.triangles
}
