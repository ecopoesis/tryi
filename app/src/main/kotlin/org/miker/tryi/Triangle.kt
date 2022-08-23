package org.miker.tryi

import java.awt.image.BufferedImage

data class Triangle(val p1: Point, val p2: Point, val p3: Point, val color: TryiColor) {
    val x: IntArray by lazy { arrayOf(p1.x, p2.x, p3.x).map { it.toInt() }.toIntArray() }
    val y: IntArray by lazy { arrayOf(p1.y, p2.y, p3.y).map { it.toInt() }.toIntArray() }

    val asList: List<UByte> by lazy { listOf(p1.asList, p2.asList, p3.asList, color.asList).flatten() }

    fun mutate(amount: Double): Triangle =
        Triangle(
            p1 = this.p1.mutate(amount),
            p2 = this.p2.mutate(amount),
            p3 = this.p3.mutate(amount),
            color = this.color.mutate(amount)
        )

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
