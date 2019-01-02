package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.*
import ch.softappeal.yass.serialize.*
import java.io.*
import java.util.*
import kotlin.test.*

@Tag(21)
data class Value(
    @Tag(1) val i: Int
)

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

@Tag(22)
enum class Enum1 {
    E1_V1,
    E1_V2
}

@Tag(23)
class Class1(
    @Tag(111) val boolean1: Boolean = false,
    @Tag(112) val byte1: Byte = 0,
    @Tag(113) val short1: Short = 0,
    @Tag(114) val int1: Int = 0,
    @Tag(115) val long1: Long = 0,
    @Tag(116) val char1: Char = '0',
    @Tag(117) val float1: Float = 0.0f,
    @Tag(118) val double1: Double = 0.0,

    @Tag(121) val boolean1opt: Boolean? = null,
    @Tag(122) val byte1opt: Byte? = null,
    @Tag(123) val short1opt: Short? = null,
    @Tag(124) val int1opt: Int? = null,
    @Tag(125) val long1opt: Long? = null,
    @Tag(126) val char1opt: Char? = null,
    @Tag(127) val float1opt: Float? = null,
    @Tag(128) val double1opt: Double? = null,

    @Tag(131) val byteArray1: ByteArray = byteArrayOf(),
    @Tag(132) val string1: String = "",
    @Tag(133) val enum1: Enum1 = Enum1.E1_V2,
    @Tag(134) val value1: Any = Value(1),
    @Tag(135) val values1: List<Any?> = listOf(),

    @Tag(141) val byteArray1opt: ByteArray? = null,
    @Tag(142) val string1opt: String? = null,
    @Tag(143) val enum1opt: Enum1? = null,
    @Tag(144) val value1opt: Any? = null,
    @Tag(145) val values1opt: List<Any?>? = null
)

private val Serializer1 =
    taggedFastSerializer(
        BaseTypes,
        listOf(Value::class.java, Enum1::class.java, Class1::class.java),
        skipping = false
    )

@Tag(22)
enum class Enum2 {
    E1_V1,
    E1_V2,
    E2_V1
}

@Tag(23)
class Class2(
    @Tag(111) val boolean1: Boolean = false,
    @Tag(112) val byte1: Byte = 0,
    @Tag(113) val short1: Short = 0,
    @Tag(114) val int1: Int = 0,
    @Tag(115) val long1: Long = 0,
    @Tag(116) val char1: Char = '0',
    @Tag(117) val float1: Float = 0.0f,
    @Tag(118) val double1: Double = 0.0,

    @Tag(121) val boolean1opt: Boolean? = null,
    @Tag(122) val byte1opt: Byte? = null,
    @Tag(123) val short1opt: Short? = null,
    @Tag(124) val int1opt: Int? = null,
    @Tag(125) val long1opt: Long? = null,
    @Tag(126) val char1opt: Char? = null,
    @Tag(127) val float1opt: Float? = null,
    @Tag(128) val double1opt: Double? = null,

    @Tag(131) val byteArray1: ByteArray = byteArrayOf(),
    @Tag(132) val string1: String = "",
    @Tag(133) val enum1: Enum2 = Enum2.E1_V2,
    @Tag(134) val value1: Any = Value(1),
    @Tag(135) val values1: List<Any?> = listOf(),

    @Tag(141) val byteArray1opt: ByteArray? = null,
    @Tag(142) val string1opt: String? = null,
    @Tag(143) val enum1opt: Enum2? = null,
    @Tag(144) val value1opt: Any? = null,
    @Tag(145) val values1opt: List<Any?>? = null,

    @Tag(211) val boolean2: Boolean = true,
    @Tag(212) val byte2: Byte = 1,
    @Tag(213) val short2: Short = 2,
    @Tag(214) val int2: Int = 3,
    @Tag(215) val long2: Long = 4,
    @Tag(216) val char2: Char = '5',
    @Tag(217) val float2: Float = 6.0f,
    @Tag(218) val double2: Double = 7.0,

    @Tag(221) val boolean2opt: Boolean? = null,
    @Tag(222) val byte2opt: Byte? = null,
    @Tag(223) val short2opt: Short? = null,
    @Tag(224) val int2opt: Int? = null,
    @Tag(225) val long2opt: Long? = null,
    @Tag(226) val char2opt: Char? = null,
    @Tag(227) val float2opt: Float? = null,
    @Tag(228) val double2opt: Double? = null,

    @Tag(231) val byteArray2: ByteArray = byteArrayOf(1, 2, 3),
    @Tag(232) val string2: String = "s",
    @Tag(233) val enum2: Enum2 = Enum2.E1_V2,
    @Tag(234) val value2: Any = Value(1),
    @Tag(235) val values2: List<Any?> = listOf(123),

    @Tag(241) val byteArray2opt: ByteArray? = null,
    @Tag(242) val string2opt: String? = null,
    @Tag(243) val enum2opt: Enum2? = null,
    @Tag(244) val value2opt: Any? = null,
    @Tag(245) val values2opt: List<Any?>? = null
)

private fun Class2.checkNewFields() {
    assertEquals(false, boolean2)
    assertEquals(0, byte2)
    assertEquals(0, short2)
    assertEquals(0, int2)
    assertEquals(0, long2)
    assertEquals(0.toChar(), char2)
    assertEquals(0f, float2)
    assertEquals(0.0, double2)

    assertNull(boolean2opt)
    assertNull(byte2opt)
    assertNull(short2opt)
    assertNull(int2opt)
    assertNull(long2opt)
    assertNull(char2opt)
    assertNull(float2opt)
    assertNull(double2opt)

    assertNull(byteArray2)
    assertNull(string2)
    assertNull(enum2)
    assertNull(value2)
    assertNull(values2)

    assertNull(byteArray2opt)
    assertNull(string2opt)
    assertNull(enum2opt)
    assertNull(value2opt)
    assertNull(values2opt)
}

private val Serializer2 =
    taggedFastSerializer(
        BaseTypes,
        listOf(Value::class.java, Enum2::class.java, Class2::class.java),
        skipping = false
    )

private fun copy(write: FastSerializer, read: FastSerializer, input: Any): Any {
    val buffer = ByteArrayOutputStream()
    val writer = writer(buffer)
    write.write(writer, input)
    writer.writeByte(123.toByte()) // write sentinel
    val reader = reader(ByteArrayInputStream(buffer.toByteArray()))
    val output = read.read(reader)
    assertEquals(123.toByte(), reader.readByte()) // check sentinel
    return output!!
}

class SkippingFastSerializerTest {
    @Test
    fun version1To2() {
        fun copy(input: Any) = copy(Serializer1, Serializer2, input)
        with(
            copy(
                Class1(
                    boolean1 = true,
                    byte1 = 1,
                    short1 = 2,
                    int1 = 3,
                    long1 = 4,
                    char1 = '5',
                    float1 = 6f,
                    double1 = 7.0,

                    byteArray1 = byteArrayOf(1, 2, 3),
                    string1 = "s",
                    enum1 = Enum1.E1_V1,
                    value1 = Value(1),
                    values1 = listOf(null, Value(2))
                )
            ) as Class2
        ) {
            assertEquals(true, boolean1)
            assertEquals(1, byte1)
            assertEquals(2, short1)
            assertEquals(3, int1)
            assertEquals(4, long1)
            assertEquals('5', char1)
            assertEquals(6f, float1)
            assertEquals(7.0, double1)

            assertNull(boolean1opt)
            assertNull(byte1opt)
            assertNull(short1opt)
            assertNull(int1opt)
            assertNull(long1opt)
            assertNull(char1opt)
            assertNull(float1opt)
            assertNull(double1opt)

            assertTrue(Arrays.equals(byteArrayOf(1, 2, 3), byteArray1))
            assertEquals("s", string1)
            assertEquals(Enum2.E1_V1, enum1)
            assertEquals(Value(1), value1)
            assertEquals(listOf(null, Value(2)), values1)

            assertNull(byteArray1opt)
            assertNull(string1opt)
            assertNull(enum1opt)
            assertNull(value1opt)
            assertNull(values1opt)

            checkNewFields()
        }
        with(
            copy(
                Class1(
                    boolean1 = true,
                    byte1 = 1,
                    short1 = 2,
                    int1 = 3,
                    long1 = 4,
                    char1 = '5',
                    float1 = 6f,
                    double1 = 7.0,

                    boolean1opt = false,
                    byte1opt = 10,
                    short1opt = 11,
                    int1opt = 12,
                    long1opt = 13,
                    char1opt = '4',
                    float1opt = 15f,
                    double1opt = 16.0,

                    byteArray1 = byteArrayOf(1, 2, 3),
                    string1 = "s",
                    enum1 = Enum1.E1_V1,
                    value1 = Value(1),
                    values1 = listOf(null, Value(2)),

                    byteArray1opt = byteArrayOf(123),
                    string1opt = "so",
                    enum1opt = Enum1.E1_V2,
                    value1opt = Value(123),
                    values1opt = listOf(Value(321), null)
                )
            ) as Class2
        ) {
            assertEquals(true, boolean1)
            assertEquals(1, byte1)
            assertEquals(2, short1)
            assertEquals(3, int1)
            assertEquals(4, long1)
            assertEquals('5', char1)
            assertEquals(6f, float1)
            assertEquals(7.0, double1)

            assertEquals(false, boolean1opt)
            assertEquals(10, byte1opt)
            assertEquals(11, short1opt)
            assertEquals(12, int1opt)
            assertEquals(13, long1opt)
            assertEquals('4', char1opt)
            assertEquals(15f, float1opt)
            assertEquals(16.0, double1opt)

            assertTrue(Arrays.equals(byteArrayOf(1, 2, 3), byteArray1))
            assertEquals("s", string1)
            assertEquals(Enum2.E1_V1, enum1)
            assertEquals(Value(1), value1)
            assertEquals(listOf(null, Value(2)), values1)

            assertTrue(Arrays.equals(byteArrayOf(123), byteArray1opt))
            assertEquals("so", string1opt)
            assertEquals(Enum2.E1_V2, enum1opt)
            assertEquals(Value(123), value1opt)
            assertEquals(listOf(Value(321), null), values1opt)

            checkNewFields()
        }
    }

    @Ignore
    @Test
    fun version2To1() {
        fun copy(input: Any) = copy(Serializer2, Serializer1, input)
        with(
            copy(
                Class2(
                    boolean1 = true,
                    byte1 = 1,
                    short1 = 2,
                    int1 = 3,
                    long1 = 4,
                    char1 = '5',
                    float1 = 6f,
                    double1 = 7.0,

                    byteArray1 = byteArrayOf(1, 2, 3),
                    string1 = "s",
                    enum1 = Enum2.E1_V1,
                    value1 = Value(1),
                    values1 = listOf(null, Value(2))
                )
            ) as Class1
        ) {
            assertEquals(true, boolean1)
            assertEquals(1, byte1)
            assertEquals(2, short1)
            assertEquals(3, int1)
            assertEquals(4, long1)
            assertEquals('5', char1)
            assertEquals(6f, float1)
            assertEquals(7.0, double1)

            assertNull(boolean1opt)
            assertNull(byte1opt)
            assertNull(short1opt)
            assertNull(int1opt)
            assertNull(long1opt)
            assertNull(char1opt)
            assertNull(float1opt)
            assertNull(double1opt)

            assertTrue(Arrays.equals(byteArrayOf(1, 2, 3), byteArray1))
            assertEquals("s", string1)
            assertEquals(Enum1.E1_V1, enum1)
            assertEquals(Value(1), value1)
            assertEquals(listOf(null, Value(2)), values1)

            assertNull(byteArray1opt)
            assertNull(string1opt)
            assertNull(enum1opt)
            assertNull(value1opt)
            assertNull(values1opt)
        }
        with(
            copy(
                Class2(
                    boolean1 = true,
                    byte1 = 1,
                    short1 = 2,
                    int1 = 3,
                    long1 = 4,
                    char1 = '5',
                    float1 = 6f,
                    double1 = 7.0,

                    boolean1opt = false,
                    byte1opt = 10,
                    short1opt = 11,
                    int1opt = 12,
                    long1opt = 13,
                    char1opt = '4',
                    float1opt = 15f,
                    double1opt = 16.0,

                    byteArray1 = byteArrayOf(1, 2, 3),
                    string1 = "s",
                    enum1 = Enum2.E1_V1,
                    value1 = Value(1),
                    values1 = listOf(null, Value(2)),

                    byteArray1opt = byteArrayOf(123),
                    string1opt = "so",
                    enum1opt = Enum2.E1_V2,
                    value1opt = Value(123),
                    values1opt = listOf(Value(321), null)
                )
            ) as Class1
        ) {
            assertEquals(true, boolean1)
            assertEquals(1, byte1)
            assertEquals(2, short1)
            assertEquals(3, int1)
            assertEquals(4, long1)
            assertEquals('5', char1)
            assertEquals(6f, float1)
            assertEquals(7.0, double1)

            assertEquals(false, boolean1opt)
            assertEquals(10, byte1opt)
            assertEquals(11, short1opt)
            assertEquals(12, int1opt)
            assertEquals(13, long1opt)
            assertEquals('4', char1opt)
            assertEquals(15f, float1opt)
            assertEquals(16.0, double1opt)

            assertTrue(Arrays.equals(byteArrayOf(1, 2, 3), byteArray1))
            assertEquals("s", string1)
            assertEquals(Enum1.E1_V1, enum1)
            assertEquals(Value(1), value1)
            assertEquals(listOf(null, Value(2)), values1)

            assertTrue(Arrays.equals(byteArrayOf(123), byteArray1opt))
            assertEquals("so", string1opt)
            assertEquals(Enum1.E1_V2, enum1opt)
            assertEquals(Value(123), value1opt)
            assertEquals(listOf(Value(321), null), values1opt)
        }
    }
}
