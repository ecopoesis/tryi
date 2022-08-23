package org.miker.tryi

import java.awt.Color
import kotlin.random.Random
import kotlin.random.nextUBytes

data class TryiColor(val r: UByte, val g: UByte, val b: UByte, val a: UByte) {
    fun mutate(amount: Double): TryiColor {
        return TryiColor(
            r = r.mutate(amount),
            g = g.mutate(amount),
            b = b.mutate(amount),
            a = a.mutate(amount),
        )
    }

    val asColor: Color by lazy { Color(r.toInt(), g.toInt(), b.toInt(), a.toInt()) }

    val asList: List<UByte> by lazy { listOf(r, g, b, a) }

    companion object {
        @OptIn(ExperimentalUnsignedTypes::class)
        fun random(): TryiColor {
            val bytes = Random.nextUBytes(4)
            return TryiColor(bytes[0], bytes[1], bytes[2], bytes[3])
        }
    }
}
