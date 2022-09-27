package org.miker.tryi

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

fun <A> List<A>.random(n: Int): List<A> =
    this.asSequence().shuffled().take(n).toList()

/**
 * mutate a [UByte] randomly by [amount].
 */
fun UByte.mutate(amount: Double): UByte {
    val i = this.toInt()
    val change = max((i * amount).roundToInt(), 1)
    return Random.nextInt(i - change, i + change).coerceIn(UByte.MIN_VALUE.toInt(), UByte.MAX_VALUE.toInt()).toUByte()
}

fun BufferedImage.deepCopy(): BufferedImage =
    BufferedImage(this.colorModel, this.copyData(null), this.isAlphaPremultiplied, null)

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

object Utilities {
    fun emptyBufferedImage(): BufferedImage =
        BufferedImage(UByte.MAX_VALUE.toInt(), UByte.MAX_VALUE.toInt(), BufferedImage.TYPE_4BYTE_ABGR)

    /**
     * adjust [color] by [alpha] for a black background
     */
    fun compositeBlack(alpha: UByte, color: UByte): Int {
        // normalize 0..1
        val a = alpha.toLong() / 255.0
        val c = color.toLong() / 255.0

        return  ((a * c) * 255).toInt().coerceIn(0, 255)
    }

    /**
     * adjust [color] by [alpha] for a white background
     */
    fun compositeWhite(alpha: UByte, color: UByte): Int {
        // normalize 0..1
        val a = alpha.toLong() / 255.0
        val c = color.toLong() / 255.0

        return (((1 - a) + (a * c)) * 255).toInt().coerceIn(0, 255)
    }

    /**
     * adjust [color] so it has [targetAlpha] for a black background
     */
    fun decompositeBlack(color: UByte, targetAlpha: UByte): Int {
        // normalize 0..1
        val a = targetAlpha.toLong() / 255.0
        val c = color.toLong() / 255.0

        return ((c / a) * 255).toInt().coerceIn(0, 255)
    }


    /**
     * adjust [color] so it has [targetAlpha] for a white background
     */
    fun decompositeWhite(color: UByte, targetAlpha: UByte): Int {
        // normalize 0..1
        val a = targetAlpha.toLong() / 255.0
        val c = color.toLong() / 255.0

        return ((a * (c - 1 + a)) * 255).toInt().coerceIn(0, 255)
    }
}
