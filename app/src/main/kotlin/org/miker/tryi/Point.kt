package org.miker.tryi

import kotlin.random.Random
import kotlin.random.nextUBytes

data class Point(val x: UByte, val y: UByte) {
    fun mutate(amount: Double): Point {
        return Point(
            x = x.mutate(amount),
            y = y.mutate(amount)
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
