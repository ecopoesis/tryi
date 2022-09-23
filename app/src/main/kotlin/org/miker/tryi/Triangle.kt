package org.miker.tryi

import org.miker.tryi.Triangle.MutationType.FULL
import org.miker.tryi.Triangle.MutationType.GENE
import java.awt.image.BufferedImage
import kotlin.random.Random

data class Triangle(val p1: Point, val p2: Point, val p3: Point, val color: TryiColor) {
    enum class MutationType {
        /**
         * if the [chance] passes, all 10 genes are mutated by [amount]
         */
        FULL,

        /**
         * chance is run per gene, and if passes each is mutated by [amount]
         */
        GENE
    }

    val x: IntArray by lazy { arrayOf(p1.x, p2.x, p3.x).map { it.toInt() }.toIntArray() }
    val y: IntArray by lazy { arrayOf(p1.y, p2.y, p3.y).map { it.toInt() }.toIntArray() }

    val asList: List<UByte> by lazy { listOf(p1.asList, p2.asList, p3.asList, color.asList).flatten() }

    fun mutate(type: MutationType, chance: Double, amount: Double): Triangle = when (type) {
        FULL -> if (Random.nextDouble(0.0, 1.0) <= chance) {
            Triangle(
                p1 = this.p1.mutate(1.0, amount),
                p2 = this.p2.mutate(1.0, amount),
                p3 = this.p3.mutate(1.0, amount),
                color = this.color.mutate(1.0, amount)
            )
        } else {
            this
        }

        GENE -> Triangle(
            p1 = this.p1.mutate(chance, amount),
            p2 = this.p2.mutate(chance, amount),
            p3 = this.p3.mutate(chance, amount),
            color = this.color.mutate(chance, amount)
        )
    }


    companion object {
        fun random(): Triangle = Triangle(
            Point.random(),
            Point.random(),
            Point.random(),
            TryiColor.random()
        )
    }
}

fun List<Triangle>.render(): BufferedImage {
    val out = BufferedImage(UByte.MAX_VALUE.toInt(), UByte.MAX_VALUE.toInt(), BufferedImage.TYPE_4BYTE_ABGR)
    val g2d = out.createGraphics()
    this.forEach { triangle ->
        g2d.color = triangle.color.asColor
        g2d.fillPolygon(triangle.x, triangle.y, 3)
    }
    g2d.dispose()
    return out
}
