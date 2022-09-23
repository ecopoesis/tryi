package org.miker.tryi

import java.awt.Color
import kotlin.random.Random
import kotlin.random.nextUBytes

data class TryiColor(val r: UByte, val g: UByte, val b: UByte, val a: UByte) {
    fun mutate(chance: Double, amount: Double): TryiColor {
        return TryiColor(
            r = if (Random.nextDouble(0.0, 1.0) <= chance) r.mutate(amount) else r,
            g = if (Random.nextDouble(0.0, 1.0) <= chance) g.mutate(amount) else g,
            b = if (Random.nextDouble(0.0, 1.0) <= chance) b.mutate(amount) else b,
            a = if (Random.nextDouble(0.0, 1.0) <= chance) a.mutate(amount) else a,
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
