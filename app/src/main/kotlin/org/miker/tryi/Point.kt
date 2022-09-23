package org.miker.tryi

import javax.swing.text.html.HTML.Tag.P
import kotlin.random.Random
import kotlin.random.nextUBytes

data class Point(val x: UByte, val y: UByte) {
    fun mutate(chance: Double, amount: Double): Point {
        return Point(
            x = if (Random.nextDouble(0.0, 1.0) <= chance) x.mutate(amount) else x,
            y = if (Random.nextDouble(0.0, 1.0) <= chance) y.mutate(amount) else y
        )
    }

    val asList: List<UByte> by lazy { listOf(x, y) }

    companion object {
        @OptIn(ExperimentalUnsignedTypes::class)
        fun random(): Point {
            val bytes = Random.nextUBytes(2)
            return Point(bytes[0], bytes[1])
        }
    }
}
