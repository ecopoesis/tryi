package org.miker.tryi

import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Generate [n] positive, unique [Int]s in the [range].
 */
fun Random.uniqueInt(n: Int, range: IntRange = 0..Int.MAX_VALUE): Set<Int> {
    val randoms: MutableSet<Int> = mutableSetOf()
    while (randoms.size < n) {
        randoms.add(Random.nextInt(range))
    }
    return randoms
}
