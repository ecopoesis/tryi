package org.miker.tryi

import org.miker.tryi.Triangle.Companion
import java.awt.image.BufferedImage
import java.util.*

/**
 * Triangles and their rendered image.
 */
data class Tryi(
    val triangles: List<Triangle>,
    val image: BufferedImage
) {
    constructor(triangles: List<Triangle>) : this(triangles, triangles.render())

    fun serialize(): String =
        Base64.getEncoder().encodeToString(triangles.flatMap { tri -> tri.asList }.map { it.toByte() }.toByteArray())

    companion object {
        @OptIn(ExperimentalUnsignedTypes::class)
        fun deserialize(s: String): Tryi {
            val bytes = Base64.getDecoder().decode(s).toUByteArray()
            if (bytes.size % 10 != 0) {
                throw IllegalArgumentException("Tryis must contain a multiple of 10 bytes")
            }

            val triangles = bytes.chunked(10).map { triBytes ->
                Triangle(
                    Point(triBytes[0], triBytes[1]),
                    Point(triBytes[2], triBytes[3]),
                    Point(triBytes[4], triBytes[5]),
                    TryiColor(triBytes[6], triBytes[7], triBytes[8], triBytes[9])
                )
            }

            return Tryi(triangles)
        }

        fun empty(): Tryi = Tryi(emptyList(), Utilities.emptyBufferedImage())

        fun random(numTriangles: Int = 100): Tryi = Tryi(List(numTriangles) { Triangle.random() })
    }
}
