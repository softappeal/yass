package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.*
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Writer
import java.io.*
import java.util.*
import kotlin.test.*

private val BaseTypes = listOf(
    TypeDesc(3, BooleanSerializer),
    TypeDesc(4, ByteSerializer),
    TypeDesc(5, ShortSerializer),
    TypeDesc(6, IntSerializer),
    TypeDesc(7, LongSerializer),
    TypeDesc(8, CharSerializer),
    TypeDesc(9, FloatSerializer),
    TypeDesc(10, DoubleSerializer),
    TypeDesc(11, ByteArraySerializer),
    TypeDesc(12, StringSerializer)
)

@Tag(21)
enum class Enum {
    C1,
    C2
}

@Tag(22)
class Link(
    @Tag(1) var next: Link?
)

@Tag(268_435_455)
class PrimitiveTypes(
    @Tag(268_435_455) val boolean: Boolean = true,
    @Tag(2) val byte: Byte = 1,
    @Tag(3) val short: Short = 2,
    @Tag(4) val int: Int = 3,
    @Tag(5) val long: Long = 4,
    @Tag(6) val char: Char = '5',
    @Tag(7) val float: Float = 6.0f,
    @Tag(8) val double: Double = 7.0,
    @Tag(9) val binary: ByteArray = byteArrayOf(123),
    @Tag(10) val string: String = "string",
    @Tag(11) val enum: Enum = Enum.C2
)

@Tag(31)
class NullablePrimitiveTypes(
    @Tag(1) val boolean: Boolean? = true,
    @Tag(2) val byte: Byte? = 1,
    @Tag(3) val short: Short? = 2,
    @Tag(4) val int: Int? = 3,
    @Tag(5) val long: Long? = 4,
    @Tag(6) val char: Char? = '5',
    @Tag(7) val float: Float? = 6f,
    @Tag(8) val double: Double? = 7.0,
    @Tag(9) val binary: ByteArray? = byteArrayOf(123),
    @Tag(10) val string: String? = "string",
    @Tag(11) val enum: Enum? = Enum.C2
)

@Tag(32)
class ObjectTypes(
    @Tag(1) val list: List<Any?>? = listOf(123),
    @Tag(2) val link: Link? = Link(null),
    @Tag(3) val any: Any? = PrimitiveTypes()
)

private val Serializer = taggedFastSerializer(
    BaseTypes,
    listOf(
        Enum::class.java,
        PrimitiveTypes::class.java,
        NullablePrimitiveTypes::class.java,
        ObjectTypes::class.java
    ),
    listOf(
        Link::class.java
    )
)

@Tag(268_435_455)
class PrimitiveTypes2(
    @Tag(268_435_455) val boolean: Boolean = true,
    @Tag(2) val byte: Byte = 1,
    @Tag(3) val short: Short = 2,
    @Tag(4) val int: Int = 3,
    @Tag(5) val long: Long = 4,
    @Tag(6) val char: Char = '5',
    @Tag(7) val float: Float = 6.0f,
    @Tag(8) val double: Double = 7.0,
    @Tag(9) val binary: ByteArray = byteArrayOf(123),
    @Tag(10) val string: String = "string",
    @Tag(11) val enum: Enum = Enum.C2,

    @Tag(21) val boolean2: Boolean = true,
    @Tag(22) val byte2: Byte = 1,
    @Tag(23) val short2: Short = 2,
    @Tag(24) val int2: Int = 3,
    @Tag(25) val long2: Long = 4,
    @Tag(26) val char2: Char = '5',
    @Tag(27) val float2: Float = 6.0f,
    @Tag(28) val double2: Double = 7.0,
    @Tag(29) val binary2: ByteArray = byteArrayOf(123),
    @Tag(30) val string2: String = "string",
    @Tag(31) val enum2: Enum = Enum.C2
)

@Tag(31)
class NullablePrimitiveTypes2(
    @Tag(1) val boolean: Boolean? = true,
    @Tag(2) val byte: Byte? = 1,
    @Tag(3) val short: Short? = 2,
    @Tag(4) val int: Int? = 3,
    @Tag(5) val long: Long? = 4,
    @Tag(6) val char: Char? = '5',
    @Tag(7) val float: Float? = 6f,
    @Tag(8) val double: Double? = 7.0,
    @Tag(9) val binary: ByteArray? = byteArrayOf(123),
    @Tag(10) val string: String? = "string",
    @Tag(11) val enum: Enum? = Enum.C2,

    @Tag(21) val boolean2: Boolean? = true,
    @Tag(22) val byte2: Byte? = 1,
    @Tag(23) val short2: Short? = 2,
    @Tag(24) val int2: Int? = 3,
    @Tag(25) val long2: Long? = 4,
    @Tag(26) val char2: Char? = '5',
    @Tag(27) val float2: Float? = 6f,
    @Tag(28) val double2: Double? = 7.0,
    @Tag(29) val binary2: ByteArray? = byteArrayOf(123),
    @Tag(30) val string2: String? = "string",
    @Tag(31) val enum2: Enum? = Enum.C2
)

@Tag(32)
class ObjectTypes2(
    @Tag(1) val list: List<Any?>? = listOf(123),
    @Tag(2) val link: Link? = Link(null),
    @Tag(3) val any: Any? = PrimitiveTypes2(),

    @Tag(11) val list2: List<Any?>? = listOf(123),
    @Tag(12) val link2: Link? = Link(null),
    @Tag(13) val any2: Any? = PrimitiveTypes2()
)

class NewBoolean(val value: Boolean)

private val NewBooleanSerializer =
    object : BaseTypeSerializer<NewBoolean>(NewBoolean::class.javaObjectType, FieldType.Bytes1) {
        override fun read(reader: Reader) =
            NewBoolean(reader.readByte().toInt() != 0)

        override fun write(writer: Writer, value: NewBoolean) =
            writer.writeByte((if (value.value) 1 else 0).toByte())
    }

@Tag(343)
class NewClass

@Tag(763)
enum class NewEnum {
    C1,
    C2
}

private val Serializer2 = taggedFastSerializer(
    BaseTypes.toMutableList().apply { add(TypeDesc(999, NewBooleanSerializer)) },
    listOf(
        Enum::class.java,
        PrimitiveTypes2::class.java,
        NullablePrimitiveTypes2::class.java,
        ObjectTypes2::class.java,
        NewClass::class.java,
        NewEnum::class.java
    ),
    listOf(
        Link::class.java
    )
)

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
        with(copyFrom2(ObjectTypes2(any = cycle, link2 = null)) as ObjectTypes) {
            assertEquals(listOf(123), list)
            assertNull(link!!.next)
            assertTrue(any is Link)
            assertSame(any, any.next!!.next)
        }
    }

    private val skipList2 = listOf(
        false,
        true,
        Enum.C1,
        Enum.C2,
        PrimitiveTypes2(),
        NullablePrimitiveTypes2(),
        null,
        123,
        "string",

        NewBoolean(true),
        NewBoolean(false),
        NewEnum.C1,
        NewEnum.C2,
        NewClass(),
        ObjectTypes2(link = null, link2 = null),
        listOf(
            123,
            null,
            "string",
            NewBoolean(false),
            NewEnum.C2,
            NewClass()
        )
    )

    @Test
    fun skippingList() {
        val list = listOf(
            false,
            true,
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
        with(copyFrom2(ObjectTypes2(any = cycle, list = list, list2 = skipList2, link2 = null)) as ObjectTypes) {
            assertEquals(list.size, this.list!!.size)
            assertNull(link!!.next)
            assertTrue(any is Link)
            assertSame(any, any.next!!.next)
        }
    }

    @Test
    fun skippingAny() {
        skipList2.forEach {
            assertNull((copyFrom2(ObjectTypes2(any2 = it, link2 = null)) as ObjectTypes).link!!.next)
        }
    }

    @Test
    fun skippingReferences() {
        copyFrom2(ObjectTypes2(any = cycle, any2 = cycle, link2 = null))
        copyFrom2(ObjectTypes2(any = cycle, list2 = listOf(cycle), link2 = null))
    }

    @Test
    fun illegalSkippingGraphClasses() {
        assertFails { copyFrom2(ObjectTypes2(link2 = cycle)) }
        assertFails { copyFrom2(ObjectTypes2(link2 = null, list2 = listOf(cycle))) }
    }
}
