package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.*
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.serialize.PrimitiveTypes
import ch.softappeal.yass.serialize.nested.*
import kotlinx.coroutines.*
import java.io.*
import kotlin.test.*

private fun taggedFastSerializer(skipping: Boolean) = sTaggedFastSerializer(
    listOf(
        STypeDesc(3, SBooleanSerializer),
        STypeDesc(5, SShortSerializer),
        STypeDesc(6, SIntSerializer),
        STypeDesc(7, SLongSerializer),
        STypeDesc(12, SBinarySerializer),
        STypeDesc(19, SStringSerializer)
    ),
    listOf(Color::class, PrimitiveTypes::class, AllTypes::class, IntException::class),
    listOf(Node::class),
    skipping
)

private val TAGGED_FAST_SERIALIZER = taggedFastSerializer(false)
private val TAGGED_FAST_SERIALIZER_SKIPPING = taggedFastSerializer(true)

private fun simpleFastSerializer(skipping: Boolean) = sSimpleFastSerializer(
    listOf(
        SBooleanSerializer,
        SShortSerializer,
        SIntSerializer,
        SLongSerializer,
        SBinarySerializer,
        SStringSerializer
    ),
    listOf(Color::class, PrimitiveTypes::class, AllTypes::class, IntException::class),
    listOf(Node::class),
    skipping
)

private val SIMPLE_FAST_SERIALIZER = simpleFastSerializer(false)
private val SIMPLE_FAST_SERIALIZER_SKIPPING = simpleFastSerializer(true)

class SFastSerializerTest {
    @Test
    fun simpleFast() {
        sTest(SIMPLE_FAST_SERIALIZER)
    }

    @Test
    fun simpleFastSkipping() {
        sTest(SIMPLE_FAST_SERIALIZER_SKIPPING)
    }

    @Test
    fun taggedFast() {
        sTest(TAGGED_FAST_SERIALIZER)
    }

    @Test
    fun taggedFastSkipping() {
        sTest(TAGGED_FAST_SERIALIZER_SKIPPING)
    }

    class A(val a: Int)

    @Test
    fun duplicatedField() = assertEquals(
        "duplicated field name 'private final int " +
            "ch.softappeal.yass.serialize.fast.SFastSerializerTest${'$'}A.a' and 'private final int " +
            "ch.softappeal.yass.serialize.fast.SFastSerializerTest${'$'}A.a' not allowed in class hierarchy",
        assertFailsWith<IllegalArgumentException> {
            val field = A::class.java.getDeclaredField("a")
            object : SFastSerializer(false) {
                init {
                    addClass(999, String::class, false, mapOf(1 to field, 2 to field))
                }
            }
        }.message
    )

    @Test
    fun notAnEnumeration() = assertEquals(
        "type 'java.lang.String' is not an enumeration",
        assertFailsWith<IllegalArgumentException> {
            object : SFastSerializer(false) {
                init {
                    addEnum(999, String::class)
                }
            }
        }.message
    )

    @Test
    fun baseEnumeration() = assertEquals(
        "base type 'ch.softappeal.yass.serialize.Color' is an enumeration",
        assertFailsWith<IllegalArgumentException> {
            sTaggedFastSerializer(
                listOf(STypeDesc(1, object : SBaseTypeSerializer<Color>(Color::class, SFieldType.VarInt) {
                    override suspend fun read(reader: SReader) = Color.BLUE
                    override suspend fun write(writer: SWriter, value: Color) {}
                })),
                listOf(),
                skipping = false
            )
        }.message
    )

    @Test
    fun duplicatedClass() = assertEquals(
        "type 'ch.softappeal.yass.serialize.Color' already added",
        assertFailsWith<IllegalArgumentException> {
            sTaggedFastSerializer(listOf(), listOf(Color::class, Color::class), skipping = false)
        }.message
    )

    @Test
    fun abstractClass() = assertEquals(
        "type 'ch.softappeal.yass.serialize.fast.SFastSerializer' is abstract",
        assertFailsWith<IllegalArgumentException> {
            sSimpleFastSerializer(listOf(), listOf(SFastSerializer::class), skipping = false)
        }.message
    )

    @Test
    fun illegalInterface() = assertEquals(
        "type 'java.lang.AutoCloseable' is abstract",
        assertFailsWith<IllegalArgumentException> {
            sSimpleFastSerializer(listOf(), listOf(AutoCloseable::class), skipping = false)
        }.message
    )

    @Test
    fun illegalAnnotation() = assertEquals(
        "type 'ch.softappeal.yass.Tag' is abstract",
        assertFailsWith<IllegalArgumentException> {
            sSimpleFastSerializer(listOf(), listOf(Tag::class), skipping = false)
        }.message
    )

    @Test
    fun illegalEnumeration() = assertEquals(
        "type 'ch.softappeal.yass.serialize.Color' is an enumeration",
        assertFailsWith<IllegalArgumentException> {
            sSimpleFastSerializer(listOf(), listOf(), listOf(Color::class), false)
        }.message
    )

    @Test
    fun missingType() {
        val serializer = sTaggedFastSerializer(listOf(), listOf(), skipping = false)
        assertEquals(
            "missing type 'ch.softappeal.yass.serialize.Color'",
            assertFailsWith<IllegalStateException> { sCopy(serializer, Color.BLUE) }.message
        )
    }

    @Test
    fun missingBaseClass() = assertEquals(
        "missing base class 'ch.softappeal.yass.serialize.PrimitiveTypes'",
        assertFailsWith<IllegalArgumentException> {
            sSimpleFastSerializer(listOf(), listOf(Color::class, AllTypes::class), skipping = false)
        }.message
    )

    class MissingClassTag {
        @Tag(1)
        internal var i: Int = 0
    }

    @Test
    fun missingClassTag() = assertEquals(
        "missing tag for " +
            "'class ch.softappeal.yass.serialize.fast.SFastSerializerTest${'$'}MissingClassTag'",
        assertFailsWith<IllegalStateException> {
            sTaggedFastSerializer(listOf(), listOf(MissingClassTag::class), skipping = false)
        }.message
    )

    @Tag(120)
    private class C1

    @Tag(120)
    private class C2

    @Test
    fun duplicatedTypeTag() = assertEquals(
        "type id 120 used for 'ch.softappeal.yass.serialize.fast.SFastSerializerTest.C2' and " +
            "'ch.softappeal.yass.serialize.fast.SFastSerializerTest.C1'",
        assertFailsWith<IllegalStateException> {
            sTaggedFastSerializer(listOf(), listOf(C1::class, C2::class), skipping = false)
        }.message
    )

    @Tag(-1)
    class InvalidTypeTag

    @Test
    fun invalidTypeTag() = assertEquals(
        "id -1 for type " +
            "'ch.softappeal.yass.serialize.fast.SFastSerializerTest.InvalidTypeTag' must be >= 0",
        assertFailsWith<IllegalArgumentException> {
            sTaggedFastSerializer(listOf(), listOf(InvalidTypeTag::class), skipping = false)
        }.message
    )

    @Tag(0)
    class InvalidFieldTag {
        @Tag(0)
        internal var i: Int = 0
    }

    @Test
    fun invalidFieldTag() = assertEquals(
        "id 0 for field " +
            "'private int ch.softappeal.yass.serialize.fast.SFastSerializerTest${'$'}InvalidFieldTag.i' " +
            "must be >= 1",
        assertFailsWith<IllegalArgumentException> {
            sTaggedFastSerializer(listOf(), listOf(InvalidFieldTag::class), skipping = false)
        }.message
    )

    @Tag(0)
    class DuplicatedFieldTag {
        @Tag(1)
        internal var i1: Int = 0
        @Tag(1)
        internal var i2: Int = 0
    }

    @Test
    fun duplicatedFieldTag() = assertEquals(
        "tag 1 used for fields 'private int " +
            "ch.softappeal.yass.serialize.fast.SFastSerializerTest${'$'}DuplicatedFieldTag.i2' and " +
            "'private int ch.softappeal.yass.serialize.fast.SFastSerializerTest${'$'}DuplicatedFieldTag.i1'",
        assertFailsWith<IllegalArgumentException> {
            sTaggedFastSerializer(listOf(), listOf(DuplicatedFieldTag::class), skipping = false)
        }.message
    )

    @Test
    fun taggedPrint() {
        compareFile("ch/softappeal/yass/serialize/fast/TaggedFastSerializerTest.numbers.txt") {
            TAGGED_FAST_SERIALIZER.print(it)
        }
    }

    @Test
    fun simplePrint() {
        compareFile("ch/softappeal/yass/serialize/fast/SimpleFastSerializerTest.numbers.txt") {
            SIMPLE_FAST_SERIALIZER.print(it)
        }
    }

    @Test
    fun bytes() = runBlocking {
        sCompareFile("ch/softappeal/yass/serialize/fast/TaggedFastSerializerTest.bytes.txt") { printer ->
            suspend fun write(printer: PrintWriter, value: Any) {
                val buffer = ByteArrayOutputStream()
                TAGGED_FAST_SERIALIZER.write(sWriter(buffer), value)
                val bytes = buffer.toByteArray()
                for (b in bytes) {
                    printer.print(" $b")
                }
                printer.println()
            }
            write(printer, createGraph())
            write(printer, createNulls())
            write(printer, createValues())
        }
    }
}
