package ch.softappeal.yass.generate

import ch.softappeal.yass.serialize.fast.IntSerializer
import ch.softappeal.yass.serialize.fast.TypeDesc
import org.junit.Test
import kotlin.test.assertEquals
import ch.softappeal.yass.generate.py.baseTypeDescs as pyBaseTypeDescs
import ch.softappeal.yass.generate.ts.baseTypeDescs as tsBaseTypeDescs

class GenerateTest {
    @Test
    fun typeScript() {
        assertEquals(5, tsBaseTypeDescs(TypeDesc(100, IntSerializer)).size)
    }

    @Test
    fun python() {
        assertEquals(5, pyBaseTypeDescs(TypeDesc(100, IntSerializer)).size)
    }
}
