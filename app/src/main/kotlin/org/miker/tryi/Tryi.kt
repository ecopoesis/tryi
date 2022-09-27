package org.miker.tryi

import org.miker.tryi.Utilities.compositeWhite
import org.miker.tryi.Utilities.decompositeBlack
import org.miker.tryi.Utilities.decompositeWhite
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

        /**
         * Read Altered Qualia format: vertices polys r g b a x0 y0 x1 y1 x2 y2
         */
        fun readDna(s: String): Tryi {
            fun String.readByte(): UByte {
                val i = this.toInt()
                if (i < 0 || i > 255) throw IllegalArgumentException("Points and RGB must be 0..255")
                return i.toUByte()
            }

            fun String.readAlpha(): UByte {
                val f = this.toFloat()
                if (f < 0 || f > 1) throw IllegalArgumentException("Points and RGB must be 0..1")
                return ((f * 255)).toInt().toUByte()
            }

            val parts = s.split(" ")
            if (parts[0] != "3") {
                throw IllegalArgumentException("Only triangle DNA is supported.")
            }

            val polys = parts[1].toInt()
            if (parts.size != (polys * 10) + 2) {
                throw IllegalArgumentException("Invalid format. Expected $polys triangles, found ${parts.size - 2}")
            }

            val triangles = parts.drop(2).chunked(10).map { tri ->
                val a = tri[3].readAlpha()
                val r = tri[0].readByte()
                val g = tri[1].readByte()
                val b = tri[2].readByte()

                Triangle(
                    Point(tri[4].readByte(), tri[5].readByte()),
                    Point(tri[6].readByte(), tri[7].readByte()),
                    Point(tri[8].readByte(), tri[9].readByte()),
                    TryiColor(r, g, b, a)
                )
            }
            return Tryi(triangles)
        }

        fun empty(): Tryi = Tryi(emptyList(), Utilities.emptyBufferedImage())

        fun random(numTriangles: Int = 100): Tryi = Tryi(List(numTriangles) { Triangle.random() })
    }
}
