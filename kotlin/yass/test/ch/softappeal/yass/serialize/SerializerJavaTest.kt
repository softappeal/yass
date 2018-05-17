package ch.softappeal.yass.serialize

import ch.softappeal.yass.serialize.contractj.Color
import ch.softappeal.yass.serialize.contractj.IntException
import ch.softappeal.yass.serialize.contractj.Node
import ch.softappeal.yass.serialize.contractj.PrimitiveTypes
import ch.softappeal.yass.serialize.contractj.nested.AllTypes
import ch.softappeal.yass.serialize.fast.BTH_BIGDECIMAL
import ch.softappeal.yass.serialize.fast.BTH_BIGINTEGER
import ch.softappeal.yass.serialize.fast.BTH_BOOLEAN
import ch.softappeal.yass.serialize.fast.BTH_BOOLEAN_ARRAY
import ch.softappeal.yass.serialize.fast.BTH_BYTE
import ch.softappeal.yass.serialize.fast.BTH_BYTE_ARRAY
import ch.softappeal.yass.serialize.fast.BTH_CHARACTER
import ch.softappeal.yass.serialize.fast.BTH_CHARACTER_ARRAY
import ch.softappeal.yass.serialize.fast.BTH_DATE
import ch.softappeal.yass.serialize.fast.BTH_DOUBLE
import ch.softappeal.yass.serialize.fast.BTH_DOUBLE_ARRAY
import ch.softappeal.yass.serialize.fast.BTH_FLOAT
import ch.softappeal.yass.serialize.fast.BTH_FLOAT_ARRAY
import ch.softappeal.yass.serialize.fast.BTH_INSTANT
import ch.softappeal.yass.serialize.fast.BTH_INTEGER
import ch.softappeal.yass.serialize.fast.BTH_INTEGER_ARRAY
import ch.softappeal.yass.serialize.fast.BTH_LONG
import ch.softappeal.yass.serialize.fast.BTH_LONG_ARRAY
import ch.softappeal.yass.serialize.fast.BTH_SHORT
import ch.softappeal.yass.serialize.fast.BTH_SHORT_ARRAY
import ch.softappeal.yass.serialize.fast.BTH_STRING
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer
import ch.softappeal.yass.serialize.fast.TaggedFastSerializer
import ch.softappeal.yass.serialize.fast.TypeDesc
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.util.Arrays
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

fun javaCreateNulls(): AllTypes {
    return AllTypes()
}

private fun checkNulls(allTypes: AllTypes) {
    assertFalse(allTypes.booleanField)
    assertTrue(allTypes.byteField.toInt() == 0)
    assertTrue(allTypes.shortField.toInt() == 0)
    assertTrue(allTypes.intField == 0)
    assertTrue(allTypes.longField == 0L)
    assertTrue(allTypes.charField == ' ')
    assertTrue(allTypes.floatField == 0f)
    assertTrue(allTypes.doubleField == 0.0)
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

fun javaCreateValues(): AllTypes {
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
    allTypes.primitiveTypesListField = Arrays.asList<PrimitiveTypes>(PrimitiveTypes(999), AllTypes("world"), null)
    allTypes.objectField = "bad"
    allTypes.objectListField = Arrays.asList<Any>("good", null, 123)
    allTypes.exception = IntException(123)
    return allTypes
}

private fun checkValues(allTypes: AllTypes) {
    assertFalse(allTypes.booleanField)
    assertTrue(allTypes.byteField.toInt() == 100)
    assertTrue(allTypes.shortField.toInt() == 101)
    assertTrue(allTypes.intField == 102)
    assertTrue(allTypes.longField == 103L)
    assertTrue(allTypes.charField == 'x')
    assertTrue(allTypes.floatField == 1.23f)
    assertTrue(allTypes.doubleField == 3.21)
    assertTrue(allTypes.booleanWrapperField == true)
    assertTrue(allTypes.byteWrapperField == (-100).toByte())
    assertTrue(allTypes.shortWrapperField == (-101).toShort())
    assertTrue(allTypes.intWrapperField == -102)
    assertTrue(allTypes.longWrapperField == -103L)
    assertTrue(allTypes.charWrapperField == 'y')
    assertTrue(allTypes.floatWrapperField == -1.23f)
    assertTrue(allTypes.doubleWrapperField == -3.21)
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
    assertTrue(allTypes.dateField.time == 123456789L)
    val instant = allTypes.instantField
    assertTrue(instant.epochSecond == 123L)
    assertTrue(instant.nano == 456789)
    assertEquals("hello", (allTypes.primitiveTypesField as AllTypes).stringField)
    val primitiveTypesListField = allTypes.primitiveTypesListField
    assertTrue(primitiveTypesListField.size == 3)
    assertEquals(999, primitiveTypesListField[0].intField.toLong())
    assertEquals("world", (primitiveTypesListField[1] as AllTypes).stringField)
    assertNull(primitiveTypesListField[2])
    assertEquals("bad", allTypes.objectField)
    val objectListField = allTypes.objectListField
    assertTrue(objectListField.size == 3)
    assertEquals("good", objectListField[0])
    assertNull(objectListField[1])
    assertEquals(123, objectListField[2])
    assertTrue((allTypes.exception as IntException).value == 123)
}

fun javaCreateGraph(): Node {
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
    val n3 = n2.link
    assertTrue(n1.id == 1)
    assertTrue(n2.id == 2)
    assertTrue(n3.id == 3)
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
    assertArrayEquals(byteArrayOf(1.toByte(), 2.toByte()), copy(serializer, byteArrayOf(1.toByte(), 2.toByte())))
    assertArrayEquals(shortArrayOf(1.toShort(), 2.toShort()), copy(serializer, shortArrayOf(1.toShort(), 2.toShort())))
    assertArrayEquals(intArrayOf(1, 2), copy(serializer, intArrayOf(1, 2)))
    assertArrayEquals(longArrayOf(1L, 2L), copy(serializer, longArrayOf(1L, 2L)))
    assertArrayEquals(charArrayOf('a', 'b'), copy(serializer, charArrayOf('a', 'b')))
    assertArrayEquals(floatArrayOf(1f, 2f), copy(serializer, floatArrayOf(1f, 2f)), 0f)
    assertArrayEquals(doubleArrayOf(1.0, 2.0), copy(serializer, doubleArrayOf(1.0, 2.0)), 0.0)
    assertEquals(emptyList(), copy(serializer, emptyList<Any>()))
    assertEquals(listOf(1, null, "2"), copy(serializer, listOf(1, null, "2")))
    assertEquals(listOf("1", null, "2"), copy(serializer, listOf("1", null, "2")))
    assertTrue(copy(serializer, AllTypes()).javaClass === AllTypes::class.java)
    assertTrue(copy(serializer, PrimitiveTypes()).javaClass === PrimitiveTypes::class.java)
    assertTrue(copy(serializer, IntException(123)).value == 123)
    val booleans = BooleanArray(10000)
    Arrays.fill(booleans, true)
    assertArrayEquals(copy(serializer, booleans), booleans)
    var bytes = ByteArray(10000)
    Arrays.fill(bytes, 123.toByte())
    assertArrayEquals(copy(serializer, bytes), bytes)
    bytes = ByteArray(0)
    assertArrayEquals(copy(serializer, bytes), bytes)
    bytes = byteArrayOf(1.toByte(), (-2).toByte(), 3.toByte())
    assertArrayEquals(copy(serializer, bytes), bytes)
    val shorts = ShortArray(10000)
    Arrays.fill(shorts, 12345.toShort())
    assertArrayEquals(copy(serializer, shorts), shorts)
    val ints = IntArray(10000)
    Arrays.fill(ints, 12345678)
    assertArrayEquals(copy(serializer, ints), ints)
    val longs = LongArray(10000)
    Arrays.fill(longs, 12345678901234585L)
    assertArrayEquals(copy(serializer, longs), longs)
    val chars = CharArray(10000)
    Arrays.fill(chars, 'x')
    assertArrayEquals(copy(serializer, chars), chars)
    val floats = FloatArray(10000)
    Arrays.fill(floats, 123.987f)
    assertArrayEquals(copy(serializer, floats), floats, 0f)
    val doubles = DoubleArray(10000)
    Arrays.fill(doubles, 123.987)
    assertArrayEquals(copy(serializer, doubles), doubles, 0.0)
}

private fun test(serializer: Serializer) {
    checkBaseTypes(serializer)
    checkNulls(copy(serializer, javaCreateNulls()))
    checkValues(copy(serializer, javaCreateValues()))
    checkGraph(copy(serializer, javaCreateGraph()))
}

val JAVA_TAGGED_FAST_SERIALIZER = TaggedFastSerializer(
    listOf(
        TypeDesc(3, BTH_BOOLEAN),
        TypeDesc(4, BTH_BYTE),
        TypeDesc(5, BTH_SHORT),
        TypeDesc(6, BTH_INTEGER),
        TypeDesc(7, BTH_LONG),
        TypeDesc(8, BTH_CHARACTER),
        TypeDesc(9, BTH_FLOAT),
        TypeDesc(10, BTH_DOUBLE),
        TypeDesc(11, BTH_BOOLEAN_ARRAY),
        TypeDesc(12, BTH_BYTE_ARRAY),
        TypeDesc(13, BTH_SHORT_ARRAY),
        TypeDesc(14, BTH_INTEGER_ARRAY),
        TypeDesc(15, BTH_LONG_ARRAY),
        TypeDesc(16, BTH_CHARACTER_ARRAY),
        TypeDesc(17, BTH_FLOAT_ARRAY),
        TypeDesc(18, BTH_DOUBLE_ARRAY),
        TypeDesc(19, BTH_STRING),
        TypeDesc(20, BTH_BIGINTEGER),
        TypeDesc(21, BTH_BIGDECIMAL),
        TypeDesc(22, BTH_DATE),
        TypeDesc(23, BTH_INSTANT)
    ),
    listOf(Color::class.java, PrimitiveTypes::class.java, AllTypes::class.java, IntException::class.java),
    listOf(Node::class.java)
)

val JAVA_SIMPLE_FAST_SERIALIZER = SimpleFastSerializer(
    listOf(
        BTH_BOOLEAN,
        BTH_BYTE,
        BTH_SHORT,
        BTH_INTEGER,
        BTH_LONG,
        BTH_CHARACTER,
        BTH_FLOAT,
        BTH_DOUBLE,
        BTH_BOOLEAN_ARRAY,
        BTH_BYTE_ARRAY,
        BTH_SHORT_ARRAY,
        BTH_INTEGER_ARRAY,
        BTH_LONG_ARRAY,
        BTH_CHARACTER_ARRAY,
        BTH_FLOAT_ARRAY,
        BTH_DOUBLE_ARRAY,
        BTH_STRING,
        BTH_BIGINTEGER,
        BTH_BIGDECIMAL,
        BTH_DATE,
        BTH_INSTANT
    ),
    listOf(Color::class.java, PrimitiveTypes::class.java, AllTypes::class.java, IntException::class.java),
    listOf(Node::class.java)
)

class SerializerJavaTest {

    @Test
    fun java() {
        test(JavaSerializer)
    }

    @Test
    fun simpleFast() {
        test(JAVA_SIMPLE_FAST_SERIALIZER)
    }

    @Test
    fun taggedFast() {
        test(JAVA_TAGGED_FAST_SERIALIZER)
    }

}
