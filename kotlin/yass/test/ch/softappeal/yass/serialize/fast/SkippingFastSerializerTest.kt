package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.serialize.*
import java.io.*
import java.util.*
import kotlin.test.*

private fun copy(write: FastSerializer, read: FastSerializer, input: Any): Any {
    val buffer = ByteArrayOutputStream()
    val writer = writer(buffer)
    write.write(writer, input)
    val reader = reader(ByteArrayInputStream(buffer.toByteArray()))
    val output = read.read(reader)
    assertFailsWith<IllegalStateException> { reader.readByte() }
    return output!!
}

private fun copyTo2(input: Any) = copy(Serializer, Serializer2, input)
private fun copyFrom2(input: Any) = copy(Serializer2, Serializer, input)

class SkippingFastSerializerTest {
    @Test
    fun primitiveTypesTo2() {
        with(copyTo2(PrimitiveTypes()) as PrimitiveTypes2) {
            assertEquals(true, boolean)
            assertEquals(1, byte)
            assertEquals(2, short)
            assertEquals(3, int)
            assertEquals(4, long)
            assertEquals('5', char)
            assertEquals(6f, float)
            assertEquals(7.0, double)
            assertTrue(Arrays.equals(byteArrayOf(123), binary))
            assertEquals("string", string)
            assertEquals(Enum.C2, enum)

            assertEquals(false, boolean2)
            assertEquals(0, byte2)
            assertEquals(0, short2)
            assertEquals(0, int2)
            assertEquals(0, long2)
            assertEquals(0.toChar(), char2)
            assertEquals(0f, float2)
            assertEquals(0.0, double2)
            assertNull(binary2)
            assertNull(string2)
            assertNull(enum2)
        }
    }

    @Test
    fun primitiveTypesFrom2() {
        with(copyFrom2(PrimitiveTypes2()) as PrimitiveTypes) {
            assertEquals(true, boolean)
            assertEquals(1, byte)
            assertEquals(2, short)
            assertEquals(3, int)
            assertEquals(4, long)
            assertEquals('5', char)
            assertEquals(6f, float)
            assertEquals(7.0, double)
            assertTrue(Arrays.equals(byteArrayOf(123), binary))
            assertEquals("string", string)
            assertEquals(Enum.C2, enum)
        }
    }

    @Test
    fun nullablePrimitiveTypesTo2() {
        with(copyTo2(NullablePrimitiveTypes()) as NullablePrimitiveTypes2) {
            assertEquals(true, boolean)
            assertEquals(1, byte)
            assertEquals(2, short)
            assertEquals(3, int)
            assertEquals(4, long)
            assertEquals('5', char)
            assertEquals(6f, float)
            assertEquals(7.0, double)
            assertTrue(Arrays.equals(byteArrayOf(123), binary))
            assertEquals("string", string)
            assertEquals(Enum.C2, enum)

            assertNull(boolean2)
            assertNull(byte2)
            assertNull(short2)
            assertNull(int2)
            assertNull(long2)
            assertNull(char2)
            assertNull(float2)
            assertNull(double2)
            assertNull(binary2)
            assertNull(string2)
            assertNull(enum2)
        }
    }

    @Test
    fun nullablePrimitiveTypesFrom2() {
        with(copyFrom2(NullablePrimitiveTypes2()) as NullablePrimitiveTypes) {
            assertEquals(true, boolean)
            assertEquals(1, byte)
            assertEquals(2, short)
            assertEquals(3, int)
            assertEquals(4, long)
            assertEquals('5', char)
            assertEquals(6f, float)
            assertEquals(7.0, double)
            assertTrue(Arrays.equals(byteArrayOf(123), binary))
            assertEquals("string", string)
            assertEquals(Enum.C2, enum)
        }
    }

    private val cycle = Link(Link(null))

    init {
        cycle.next!!.next = cycle
    }

    @Test
    fun objectTypesTo2() {
        with(copyTo2(ObjectTypes(any = cycle)) as ObjectTypes2) {
            assertEquals(listOf(123), list)
            assertNull(link!!.next)
            assertTrue(any is Link)
            assertSame(any, any.next!!.next)

            assertNull(list2)
            assertNull(link2)
            assertNull(any2)
        }
    }

    @Test
    fun objectTypesFrom2() {
        with(copyFrom2(ObjectTypes2(any = cycle)) as ObjectTypes) {
            assertEquals(listOf(123), list)
            assertNull(link!!.next)
            assertTrue(any is Link)
            assertSame(any, any.next!!.next)
        }
    }

    private val cycle2 = NewLink(NewLink(null))

    init {
        cycle2.next!!.next = cycle2
    }

    private val list2 = listOf(
        false,
        true,
        1.toByte(),
        1.toShort(),
        1.toLong(),
        1.34f,
        345.34,
        'C',
        Enum.C1,
        Enum.C2,
        PrimitiveTypes2(),
        NullablePrimitiveTypes2(),
        null,
        123,
        "string",

        cycle2,
        NewBoolean(true),
        NewBoolean(false),
        NewEnum.C1,
        NewEnum.C2,
        NewClass(),
        ObjectTypes2(),
        listOf(
            123,
            null,
            1.toByte(),
            1.toShort(),
            1.toLong(),
            1.34f,
            345.34,
            'C',
            cycle2,
            "string",
            NewBoolean(false),
            ObjectTypes2(),
            NewEnum.C2,
            NewLink(null),
            NewClass()
        ),
        NewLink(null)
    )

    @Test
    fun skippingList() {
        val list = listOf(
            false,
            true,
            1.toByte(),
            1.toShort(),
            1.toLong(),
            1.34f,
            345.34,
            'C',
            Enum.C1,
            Enum.C2,
            PrimitiveTypes2(),
            NullablePrimitiveTypes2(),
            null,
            123,
            "string",

            listOf(123, null, "string", Link(null)),
            cycle,
            ObjectTypes2(link2 = null)
        )
        with(copyFrom2(ObjectTypes2(any = cycle, list = list, list2 = list2)) as ObjectTypes) {
            assertEquals(list.size, this.list!!.size)
            assertNull(link!!.next)
            assertTrue(any is Link)
            assertSame(any, any.next!!.next)
        }
    }

    @Test
    fun skippingAny() {
        list2.forEach {
            assertNull((copyFrom2(ObjectTypes2(any = cycle, any2 = it)) as ObjectTypes).link!!.next)
        }
    }
}
