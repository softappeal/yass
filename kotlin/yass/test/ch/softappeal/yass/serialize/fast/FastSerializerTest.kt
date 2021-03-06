package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.*
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.serialize.PrimitiveTypes
import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Writer
import ch.softappeal.yass.serialize.nested.*
import java.io.*
import kotlin.test.*

private fun taggedFastSerializer(skipping: Boolean) = taggedFastSerializer(
    listOf(
        TypeDesc(3, BooleanSerializer),
        TypeDesc(5, ShortSerializer),
        TypeDesc(6, IntSerializer),
        TypeDesc(7, LongSerializer),
        TypeDesc(12, BinarySerializer),
        TypeDesc(19, StringSerializer)
    ),
    listOf(Color::class, PrimitiveTypes::class, AllTypes::class, IntException::class),
    listOf(Node::class),
    skipping
)

private val TAGGED_FAST_SERIALIZER = taggedFastSerializer(false)
private val TAGGED_FAST_SERIALIZER_SKIPPING = taggedFastSerializer(true)

private fun simpleFastSerializer(skipping: Boolean) = simpleFastSerializer(
    listOf(
        BooleanSerializer,
        ShortSerializer,
        IntSerializer,
        LongSerializer,
        BinarySerializer,
        StringSerializer
    ),
    listOf(Color::class, PrimitiveTypes::class, AllTypes::class, IntException::class),
    listOf(Node::class),
    skipping
)

private val SIMPLE_FAST_SERIALIZER = simpleFastSerializer(false)
private val SIMPLE_FAST_SERIALIZER_SKIPPING = simpleFastSerializer(true)

class FastSerializerTest {
    @Test
    fun simpleFast() {
        test(SIMPLE_FAST_SERIALIZER)
    }

    @Test
    fun simpleFastSkipping() {
        test(SIMPLE_FAST_SERIALIZER_SKIPPING)
    }

    @Test
    fun taggedFast() {
        test(TAGGED_FAST_SERIALIZER)
    }

    @Test
    fun taggedFastSkipping() {
        test(TAGGED_FAST_SERIALIZER_SKIPPING)
    }

    class A(val a: Int)

    @Test
    fun duplicatedField() = assertEquals(
        "duplicated field name 'private final int " +
            "ch.softappeal.yass.serialize.fast.FastSerializerTest${'$'}A.a' and 'private final int " +
            "ch.softappeal.yass.serialize.fast.FastSerializerTest${'$'}A.a' not allowed in class hierarchy",
        assertFailsWith<IllegalArgumentException> {
            val field = A::class.java.getDeclaredField("a")
            object : FastSerializer(false) {
                init {
                    addClass(999, String::class.java, false, mapOf(1 to field, 2 to field))
                }
            }
        }.message
    )

    @Test
    fun notAnEnumeration() = assertEquals(
        "type 'java.lang.String' is not an enumeration",
        assertFailsWith<IllegalArgumentException> {
            object : FastSerializer(false) {
                init {
                    addEnum(999, String::class.java)
                }
            }
        }.message
    )

    @Test
    fun baseEnumeration() = assertEquals(
        "base type 'ch.softappeal.yass.serialize.Color' is an enumeration",
        assertFailsWith<IllegalArgumentException> {
            taggedFastSerializer(
                listOf(TypeDesc(1, object : BaseTypeSerializer<Color>(Color::class, FieldType.VarInt) {
                    override fun read(reader: Reader) = Color.BLUE
                    override fun write(writer: Writer, value: Color) {}
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
            taggedFastSerializer(listOf(), listOf(Color::class, Color::class), skipping = false)
        }.message
    )

    @Test
    fun abstractClass() = assertEquals(
        "type 'ch.softappeal.yass.serialize.fast.FastSerializer' is abstract",
        assertFailsWith<IllegalArgumentException> {
            simpleFastSerializer(listOf(), listOf(FastSerializer::class), skipping = false)
        }.message
    )

    @Test
    fun illegalInterface() = assertEquals(
        "type 'java.lang.AutoCloseable' is abstract",
        assertFailsWith<IllegalArgumentException> {
            simpleFastSerializer(listOf(), listOf(AutoCloseable::class), skipping = false)
        }.message
    )

    @Test
    fun illegalAnnotation() = assertEquals(
        "type 'ch.softappeal.yass.Tag' is abstract",
        assertFailsWith<IllegalArgumentException> {
            simpleFastSerializer(listOf(), listOf(Tag::class), skipping = false)
        }.message
    )

    @Test
    fun illegalEnumeration() = assertEquals(
        "type 'ch.softappeal.yass.serialize.Color' is an enumeration",
        assertFailsWith<IllegalArgumentException> {
            simpleFastSerializer(listOf(), listOf(), listOf(Color::class), false)
        }.message
    )

    @Test
    fun missingType() {
        val serializer = taggedFastSerializer(listOf(), listOf(), skipping = false)
        assertEquals(
            "missing type 'ch.softappeal.yass.serialize.Color'",
            assertFailsWith<IllegalStateException> { copy(serializer, Color.BLUE) }.message
        )
    }

    @Test
    fun missingBaseClass() = assertEquals(
        "missing base class 'ch.softappeal.yass.serialize.PrimitiveTypes'",
        assertFailsWith<IllegalArgumentException> {
            simpleFastSerializer(listOf(), listOf(Color::class, AllTypes::class), skipping = false)
        }.message
    )

    class MissingClassTag {
        @Tag(1)
        internal var i: Int = 0
    }

    @Test
    fun missingClassTag() = assertEquals(
        "missing tag for " +
            "'class ch.softappeal.yass.serialize.fast.FastSerializerTest${'$'}MissingClassTag'",
        assertFailsWith<IllegalStateException> {
            taggedFastSerializer(listOf(), listOf(MissingClassTag::class), skipping = false)
        }.message
    )

    @Tag(120)
    private class C1

    @Tag(120)
    private class C2

    @Test
    fun duplicatedTypeTag() = assertEquals(
        "type id 120 used for 'ch.softappeal.yass.serialize.fast.FastSerializerTest.C2' and " +
            "'ch.softappeal.yass.serialize.fast.FastSerializerTest.C1'",
        assertFailsWith<IllegalStateException> {
            taggedFastSerializer(listOf(), listOf(C1::class, C2::class), skipping = false)
        }.message
    )

    @Tag(-1)
    class InvalidTypeTag

    @Test
    fun invalidTypeTag() = assertEquals(
        "id -1 for type " +
            "'ch.softappeal.yass.serialize.fast.FastSerializerTest.InvalidTypeTag' must be >= 0",
        assertFailsWith<IllegalArgumentException> {
            taggedFastSerializer(listOf(), listOf(InvalidTypeTag::class), skipping = false)
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
            "'private int ch.softappeal.yass.serialize.fast.FastSerializerTest${'$'}InvalidFieldTag.i' " +
            "must be >= 1",
        assertFailsWith<IllegalArgumentException> {
            taggedFastSerializer(listOf(), listOf(InvalidFieldTag::class), skipping = false)
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
            "ch.softappeal.yass.serialize.fast.FastSerializerTest${'$'}DuplicatedFieldTag.i2' and " +
            "'private int ch.softappeal.yass.serialize.fast.FastSerializerTest${'$'}DuplicatedFieldTag.i1'",
        assertFailsWith<IllegalArgumentException> {
            taggedFastSerializer(listOf(), listOf(DuplicatedFieldTag::class), skipping = false)
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
    fun bytes() {
        compareFile("ch/softappeal/yass/serialize/fast/TaggedFastSerializerTest.bytes.txt") { printer ->
            fun write(printer: PrintWriter, value: Any) {
                val buffer = ByteArrayOutputStream()
                TAGGED_FAST_SERIALIZER.write(writer(buffer), value)
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
