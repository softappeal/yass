package ch.softappeal.yass.serialize

import ch.softappeal.yass.serialize.nested.*
import java.io.*
import java.util.*
import kotlin.test.*

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
    assertNull(allTypes.byteArrayField)
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
    allTypes.byteArrayField = byteArrayOf(1.toByte(), (-2).toByte())
    allTypes.stringField = "999"
    allTypes.colorField = Color.BLUE
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
    assertTrue(Arrays.equals(allTypes.byteArrayField, byteArrayOf(1.toByte(), (-2).toByte())))
    assertEquals("999", allTypes.stringField)
    assertEquals(Color.BLUE, allTypes.colorField)
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
    assertTrue(
        Arrays.equals(byteArrayOf(1.toByte(), 2.toByte()), copy(serializer, byteArrayOf(1.toByte(), 2.toByte())))
    )
    assertEquals(listOf(), copy(serializer, listOf<Any>()))
    assertEquals(listOf(1, null, "2"), copy(serializer, listOf(1, null, "2")))
    assertEquals(listOf("1", null, "2"), copy(serializer, listOf("1", null, "2")))
    assertSame(AllTypes::class.java, copy(serializer, AllTypes()).javaClass)
    assertSame(PrimitiveTypes::class.java, copy(serializer, PrimitiveTypes()).javaClass)
    assertEquals(123, copy(serializer, IntException(123)).value)
    var bytes = ByteArray(10000)
    Arrays.fill(bytes, 123.toByte())
    assertTrue(Arrays.equals(copy(serializer, bytes), bytes))
    bytes = ByteArray(0)
    assertTrue(Arrays.equals(copy(serializer, bytes), bytes))
    bytes = byteArrayOf(1.toByte(), (-2).toByte(), 3.toByte())
    assertTrue(Arrays.equals(copy(serializer, bytes), bytes))
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
