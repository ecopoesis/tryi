
package org.miker.tryi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TryiTest {
    @Test
    fun sederLoop() {
        val triangles = List(100) { Triangle.random() }
        val tryi = Tryi(triangles, triangles.render())

        val serialized = tryi.serialize()
        val deserialized = Tryi.deserialize(serialized)

        assertEquals(serialized, deserialized.serialize())
        assertEquals(tryi.triangles.size, deserialized.triangles.size)

        tryi.triangles.zip(deserialized.triangles) { a, b ->
            assertEquals(a, b)
        }
    }
}
