package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.*
import ch.softappeal.yass.serialize.*
import kotlinx.coroutines.*
import java.io.*
import java.util.*
import kotlin.test.*

private fun <T : Any> SBaseTypeSerializer<T>.check(value: T, bytes: Int, skipping: Boolean = true) = runBlocking {
    // test serializing
    var buffer = ByteArrayOutputStream()
    write(sWriter(buffer), value)
    assertEquals(bytes, buffer.size())
    var reader = sReader(ByteArrayInputStream(buffer.toByteArray()))
    if (value is ByteArray)
        assertTrue(Arrays.equals(value, read(reader) as ByteArray))
    else
        assertEquals(value, read(reader))
    sAssertFailsWith<IllegalStateException> { reader.readByte() }

    // test skipping
    if (!skipping) return@runBlocking
    buffer = ByteArrayOutputStream()
    write(sWriter(buffer), value)
    reader = sReader(ByteArrayInputStream(buffer.toByteArray()))
    fieldType.skip(reader)
    sAssertFailsWith<IllegalStateException> { reader.readByte() }
}

private fun <T : Any> SBaseTypeSerializer<T>.checkNoSkipping(value: T, bytes: Int) = check(value, bytes, false)

class SBaseTypeSerializersTest {
    @Test
    fun test() {
        with(SBooleanSerializer) {
            check(false, 1)
            check(true, 1)
        }
        with(SByteSerializer) {
            check(0, 1)
            check(1, 1)
            check(-1, 1)
            check(63, 1)
            check(-64, 1)
            check(Byte.MAX_VALUE, 2)
            check(Byte.MIN_VALUE, 2)
        }
        with(SShortSerializer) {
            check(0, 1)
            check(1, 1)
            check(-1, 1)
            check(Short.MAX_VALUE, 3)
            check(Short.MIN_VALUE, 3)
        }
        with(SIntSerializer) {
            check(0, 1)
            check(1, 1)
            check(-1, 1)
            check(Int.MAX_VALUE, 5)
            check(Int.MIN_VALUE, 5)
        }
        with(SLongSerializer) {
            check(0, 1)
            check(1, 1)
            check(-1, 1)
            check(Long.MAX_VALUE, 10)
            check(Long.MIN_VALUE, 10)
        }
        with(SCharSerializer) {
            check('\u0000', 1)
            check('x', 1)
            check('\u007f', 1)
            check('\u0080', 2)
            check('\u3fff', 2)
            check('\uffff', 3)
        }
        with(SFloatSerializer) {
            check(123.3f, 5)
            check(Float.MIN_VALUE, 5)
            check(Float.MAX_VALUE, 5)
            check(Float.POSITIVE_INFINITY, 5)
            check(Float.NEGATIVE_INFINITY, 5)
            check(Float.NaN, 5)
        }
        with(SFloatSerializerNoSkipping) {
            checkNoSkipping(123.3f, 4)
            checkNoSkipping(Float.MIN_VALUE, 4)
            checkNoSkipping(Float.MAX_VALUE, 4)
            checkNoSkipping(Float.POSITIVE_INFINITY, 4)
            checkNoSkipping(Float.NEGATIVE_INFINITY, 4)
            checkNoSkipping(Float.NaN, 4)
        }
        with(SDoubleSerializer) {
            check(123.3, 9)
            check(Double.MIN_VALUE, 9)
            check(Double.MAX_VALUE, 9)
            check(Double.POSITIVE_INFINITY, 9)
            check(Double.NEGATIVE_INFINITY, 9)
            check(Double.NaN, 9)
        }
        with(SDoubleSerializerNoSkipping) {
            checkNoSkipping(123.3, 8)
            checkNoSkipping(Double.MIN_VALUE, 8)
            checkNoSkipping(Double.MAX_VALUE, 8)
            checkNoSkipping(Double.POSITIVE_INFINITY, 8)
            checkNoSkipping(Double.NEGATIVE_INFINITY, 8)
            checkNoSkipping(Double.NaN, 8)
        }
        with(SBinarySerializer) {
            check(byteArrayOf(), 1)
            check(byteArrayOf(1, 2, 3), 4)
            check(ByteArray(10_000), 10_002)
        }
        with(SStringSerializer) {
            check("", 1)
            check("hello", 6)
        }
    }
}
