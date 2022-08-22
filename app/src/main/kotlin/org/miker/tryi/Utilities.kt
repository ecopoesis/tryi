package org.miker.tryi

import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Generate [n] positive, unique [Int]s in the [range].
 */
fun Random.uniqueInt(n: Int, range: IntRange = 0..Int.MAX_VALUE): Set<Int> {
    val randoms: MutableSet<Int> = mutableSetOf()
    while (randoms.size < n) {
        randoms.add(this.nextInt(range))
    }
    return randoms
}

/**
 * mutate a [UByte] randomly by [amount].
 */
fun UByte.mutate(amount: Double): UByte {
    val i = this.toInt()
    val change = max((i * amount).roundToInt(), 1)
    return Random.nextInt(i - change, i + change).coerceIn(UByte.MIN_VALUE.toInt(), UByte.MAX_VALUE.toInt()).toUByte()
}

object Utilities {
    fun emptyBufferedImage(): BufferedImage =
        BufferedImage(UByte.MAX_VALUE.toInt(), UByte.MAX_VALUE.toInt(), BufferedImage.TYPE_4BYTE_ABGR)
}
