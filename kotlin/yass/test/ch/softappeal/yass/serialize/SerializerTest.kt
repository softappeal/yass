package ch.softappeal.yass.serialize

import ch.softappeal.yass.serialize.nested.AllTypes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.util.Arrays
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

fun <T : Any?> copy(serializer: Serializer, value: T): T {
    val buffer = ByteArrayOutputStream()
    val writer = writer(buffer)
    serializer.write(writer, value)
    writer.writeByte(123.toByte()) // write sentinel
    val reader = reader(ByteArrayInputStream(buffer.toByteArray()))
    @Suppress("UNCHECKED_CAST") val result = serializer.read(reader) as T
    assertEquals(reader.readByte(), 123.toByte()) // check sentinel
    return result
}

fun createNulls(): AllTypes {
    return AllTypes()
}

private fun checkNulls(allTypes: AllTypes) {
    assertFalse(allTypes.booleanField)
    assertEquals(0, allTypes.byteField.toInt())
    assertEquals(0, allTypes.shortField.toInt())
    assertEquals(0, allTypes.intField)
    assertEquals(0L, allTypes.longField)
    assertEquals(' ', allTypes.charField)
    assertEquals(0f, allTypes.floatField)
    assertEquals(0.0, allTypes.doubleField)
    assertNull(allTypes.booleanArrayField)
    assertNull(allTypes.byteArrayField)
    assertNull(allTypes.shortArrayField)
    assertNull(allTypes.intArrayField)
    assertNull(allTypes.longArrayField)
    assertNull(allTypes.charArrayField)
    assertNull(allTypes.floatArrayField)
    assertNull(allTypes.doubleArrayField)
    assertNull(allTypes.booleanWrapperField)
    assertNull(allTypes.byteWrapperField)
    assertNull(allTypes.shortWrapperField)
    assertNull(allTypes.intWrapperField)
    assertNull(allTypes.longWrapperField)
    assertNull(allTypes.charWrapperField)
    assertNull(allTypes.floatWrapperField)
    assertNull(allTypes.doubleWrapperField)
    assertNull(allTypes.stringField)
    assertNull(allTypes.colorField)
    assertNull(allTypes.bigDecimalField)
    assertNull(allTypes.bigIntegerField)
    assertNull(allTypes.dateField)
    assertNull(allTypes.instantField)
    assertNull(allTypes.primitiveTypesField)
    assertNull(allTypes.primitiveTypesListField)
    assertNull(allTypes.objectField)
    assertNull(allTypes.objectListField)
    assertNull(allTypes.exception)
}

fun createValues(): AllTypes {
    val allTypes = AllTypes()
    allTypes.booleanField = false
    allTypes.byteField = 100.toByte()
    allTypes.shortField = 101.toShort()
    allTypes.intField = 102
    allTypes.longField = 103L
    allTypes.charField = 'x'
    allTypes.floatField = 1.23f
    allTypes.doubleField = 3.21
    allTypes.booleanWrapperField = true
    allTypes.byteWrapperField = (-100).toByte()
    allTypes.shortWrapperField = (-101).toShort()
    allTypes.intWrapperField = -102
    allTypes.longWrapperField = -103L
    allTypes.charWrapperField = 'y'
    allTypes.floatWrapperField = -1.23f
    allTypes.doubleWrapperField = -3.21
    allTypes.booleanArrayField = booleanArrayOf(false, true, false)
    allTypes.byteArrayField = byteArrayOf(1.toByte(), (-2).toByte())
    allTypes.shortArrayField = shortArrayOf((-1).toShort(), 2.toShort())
    allTypes.intArrayField = intArrayOf(1, -2)
    allTypes.longArrayField = longArrayOf(-1L, 2L)
    allTypes.charArrayField = charArrayOf('x', 'y')
    allTypes.floatArrayField = floatArrayOf(-1f, 2f)
    allTypes.doubleArrayField = doubleArrayOf(1.0, -2.0)
    allTypes.stringField = "999"
    allTypes.colorField = Color.BLUE
    allTypes.bigDecimalField = BigDecimal("98.7")
    allTypes.bigIntegerField = BigInteger("987")
    allTypes.dateField = Date(123456789L)
    allTypes.instantField = Instant.ofEpochSecond(123, 456789)
    allTypes.primitiveTypesField = AllTypes("hello")
    allTypes.primitiveTypesListField =
        Arrays.asList<PrimitiveTypes>(PrimitiveTypes(999), AllTypes("world"), null)
    allTypes.objectField = "bad"
    allTypes.objectListField = Arrays.asList<Any>("good", null, 123)
    allTypes.exception = IntException(123)
    return allTypes
}

private fun checkValues(allTypes: AllTypes) {
    assertFalse(allTypes.booleanField)
    assertEquals(100, allTypes.byteField.toInt())
    assertEquals(101, allTypes.shortField.toInt())
    assertEquals(102, allTypes.intField)
    assertEquals(103L, allTypes.longField)
    assertEquals('x', allTypes.charField)
    assertEquals(1.23f, allTypes.floatField)
    assertEquals(3.21, allTypes.doubleField)
    assertEquals(true, allTypes.booleanWrapperField)
    assertEquals((-100).toByte(), allTypes.byteWrapperField)
    assertEquals((-101).toShort(), allTypes.shortWrapperField)
    assertEquals(-102, allTypes.intWrapperField)
    assertEquals(-103L, allTypes.longWrapperField)
    assertEquals('y', allTypes.charWrapperField)
    assertEquals(-1.23f, allTypes.floatWrapperField)
    assertEquals(-3.21, allTypes.doubleWrapperField)
    assertTrue(Arrays.equals(allTypes.booleanArrayField, booleanArrayOf(false, true, false)))
    assertTrue(Arrays.equals(allTypes.byteArrayField, byteArrayOf(1.toByte(), (-2).toByte())))
    assertTrue(Arrays.equals(allTypes.shortArrayField, shortArrayOf((-1).toShort(), 2.toShort())))
    assertTrue(Arrays.equals(allTypes.intArrayField, intArrayOf(1, -2)))
    assertTrue(Arrays.equals(allTypes.longArrayField, longArrayOf(-1L, 2L)))
    assertTrue(Arrays.equals(allTypes.charArrayField, charArrayOf('x', 'y')))
    assertTrue(Arrays.equals(allTypes.floatArrayField, floatArrayOf(-1f, 2f)))
    assertTrue(Arrays.equals(allTypes.doubleArrayField, doubleArrayOf(1.0, -2.0)))
    assertEquals("999", allTypes.stringField)
    assertEquals(Color.BLUE, allTypes.colorField)
    assertEquals(BigDecimal("98.7"), allTypes.bigDecimalField)
    assertEquals(BigInteger("987"), allTypes.bigIntegerField)
    assertEquals(123456789L, allTypes.dateField!!.time)
    val instant = allTypes.instantField!!
    assertEquals(123L, instant.epochSecond)
    assertEquals(456789, instant.nano)
    assertEquals("hello", (allTypes.primitiveTypesField as AllTypes).stringField)
    val primitiveTypesListField = allTypes.primitiveTypesListField!!
    assertEquals(3, primitiveTypesListField.size)
    assertEquals(999, primitiveTypesListField[0]!!.intField.toLong())
    assertEquals("world", (primitiveTypesListField[1] as AllTypes).stringField)
    assertNull(primitiveTypesListField[2])
    assertEquals("bad", allTypes.objectField)
    val objectListField = allTypes.objectListField!!
    assertEquals(3, objectListField.size)
    assertEquals("good", objectListField[0])
    assertNull(objectListField[1])
    assertEquals(123, objectListField[2])
    assertEquals(123, (allTypes.exception as IntException).value)
}

fun createGraph(): Node {
    val n1 = Node(1)
    val n2 = Node(2)
    val n3 = Node(3)
    n1.link = n2
    n2.link = n3
    n3.link = n2
    return n1
}

private fun checkGraph(n1: Node) {
    val n2 = n1.link
    val n3 = n2!!.link
    assertEquals(n1.id, 1)
    assertEquals(n2.id, 2)
    assertEquals(n3!!.id, 3)
    assertSame(n3.link, n2)
}

private fun checkBaseTypes(serializer: Serializer) {
    assertNull(copy(serializer, null))
    assertEquals(false, copy(serializer, false))
    assertEquals(true, copy(serializer, true))
    assertEquals(123.toByte(), copy(serializer, 123.toByte()))
    assertEquals(123.toShort(), copy(serializer, 123.toShort()))
    assertEquals(123, copy(serializer, 123))
    assertEquals(123L, copy(serializer, 123L))
    assertEquals('x', copy(serializer, 'x'))
    assertEquals(1.23f, copy(serializer, 1.23f))
    assertEquals(1.23, copy(serializer, 1.23))
    assertEquals("123", copy(serializer, "123"))
    assertEquals(Color.RED, copy(serializer, Color.RED))
    assertEquals(BigInteger("123"), copy(serializer, BigInteger("123")))
    assertEquals(BigDecimal("1.23"), copy(serializer, BigDecimal("1.23")))
    assertEquals(Date(9876543210L), copy(serializer, Date(9876543210L)))
    assertTrue(Arrays.equals(booleanArrayOf(true, false), copy(serializer, booleanArrayOf(true, false))))
    assertTrue(
        Arrays.equals(byteArrayOf(1.toByte(), 2.toByte()), copy(serializer, byteArrayOf(1.toByte(), 2.toByte())))
    )
    assertTrue(
        Arrays.equals(shortArrayOf(1.toShort(), 2.toShort()), copy(serializer, shortArrayOf(1.toShort(), 2.toShort())))
    )
    assertTrue(Arrays.equals(intArrayOf(1, 2), copy(serializer, intArrayOf(1, 2))))
    assertTrue(Arrays.equals(longArrayOf(1L, 2L), copy(serializer, longArrayOf(1L, 2L))))
    assertTrue(Arrays.equals(charArrayOf('a', 'b'), copy(serializer, charArrayOf('a', 'b'))))
    assertTrue(Arrays.equals(floatArrayOf(1f, 2f), copy(serializer, floatArrayOf(1.0f, 2f))))
    assertTrue(Arrays.equals(doubleArrayOf(1.0, 2.0), copy(serializer, doubleArrayOf(1.0, 2.0))))
    assertEquals(emptyList(), copy(serializer, emptyList<Any>()))
    assertEquals(listOf(1, null, "2"), copy(serializer, listOf(1, null, "2")))
    assertEquals(listOf("1", null, "2"), copy(serializer, listOf("1", null, "2")))
    assertSame(AllTypes::class.java, copy(serializer, AllTypes()).javaClass)
    assertSame(PrimitiveTypes::class.java, copy(serializer, PrimitiveTypes()).javaClass)
    assertEquals(123, copy(serializer, IntException(123)).value)
    val booleans = BooleanArray(10000)
    Arrays.fill(booleans, true)
    assertTrue(Arrays.equals(copy(serializer, booleans), booleans))
    var bytes = ByteArray(10000)
    Arrays.fill(bytes, 123.toByte())
    assertTrue(Arrays.equals(copy(serializer, bytes), bytes))
    bytes = ByteArray(0)
    assertTrue(Arrays.equals(copy(serializer, bytes), bytes))
    bytes = byteArrayOf(1.toByte(), (-2).toByte(), 3.toByte())
    assertTrue(Arrays.equals(copy(serializer, bytes), bytes))
    val shorts = ShortArray(10000)
    Arrays.fill(shorts, 12345.toShort())
    assertTrue(Arrays.equals(copy(serializer, shorts), shorts))
    val ints = IntArray(10000)
    Arrays.fill(ints, 12345678)
    assertTrue(Arrays.equals(copy(serializer, ints), ints))
    val longs = LongArray(10000)
    Arrays.fill(longs, 12345678901234585L)
    assertTrue(Arrays.equals(copy(serializer, longs), longs))
    val chars = CharArray(10000)
    Arrays.fill(chars, 'x')
    assertTrue(Arrays.equals(copy(serializer, chars), chars))
    val floats = FloatArray(10000)
    Arrays.fill(floats, 123.987f)
    assertTrue(Arrays.equals(copy(serializer, floats), floats))
    val doubles = DoubleArray(10000)
    Arrays.fill(doubles, 123.987)
    assertTrue(Arrays.equals(copy(serializer, doubles), doubles))
}

fun test(serializer: Serializer) {
    checkBaseTypes(serializer)
    checkNulls(copy(serializer, createNulls()))
    checkValues(copy(serializer, createValues()))
    checkGraph(copy(serializer, createGraph()))
}

class SerializerTest {
    @Test
    fun java() {
        test(JavaSerializer)
    }
}
