package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.*
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Writer
import ch.softappeal.yass.serialize.nested.*
import java.io.*
import kotlin.test.*

private val TAGGED_FAST_SERIALIZER = taggedFastSerializer(
    listOf(
        TypeDesc(3, BooleanSerializer),
        TypeDesc(4, ByteSerializer),
        TypeDesc(5, ShortSerializer),
        TypeDesc(6, IntSerializer),
        TypeDesc(7, LongSerializer),
        TypeDesc(8, CharSerializer),
        TypeDesc(9, FloatSerializer),
        TypeDesc(10, DoubleSerializer),
        TypeDesc(12, ByteArraySerializer),
        TypeDesc(19, StringSerializer)
    ),
    listOf(Color::class.java, PrimitiveTypes::class.java, AllTypes::class.java, IntException::class.java),
    listOf(Node::class.java),
    false
)

private val SIMPLE_FAST_SERIALIZER = simpleFastSerializer(
    listOf(
        BooleanSerializer,
        ByteSerializer,
        ShortSerializer,
        IntSerializer,
        LongSerializer,
        CharSerializer,
        FloatSerializer,
        DoubleSerializer,
        ByteArraySerializer,
        StringSerializer
    ),
    listOf(Color::class.java, PrimitiveTypes::class.java, AllTypes::class.java, IntException::class.java),
    listOf(Node::class.java),
    false
)

@Tag(120)
class C1(
    @Tag(1) val i1: Int
)

@Tag(120)
class C2(
    @Tag(1) val i1: Int,
    @Tag(2) val i2: Int?
) {
    fun i2(): Int = i2 ?: 13
}

@Tag(200)
enum class E1 { c1, c2 }

@Tag(200)
enum class E2 { c1, c2, c3 }

class FastSerializerTest {
    @Test
    fun simpleFast() {
        test(SIMPLE_FAST_SERIALIZER)
    }

    @Test
    fun taggedFast() {
        test(TAGGED_FAST_SERIALIZER)
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
                listOf(TypeDesc(1, object : BaseTypeSerializer<Color>(Color::class.java, WireType.Bytes1) {
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
            taggedFastSerializer(listOf(), listOf(Color::class.java, Color::class.java), skipping = false)
        }.message
    )

    @Test
    fun abstractClass() = assertEquals(
        "type 'ch.softappeal.yass.serialize.fast.FastSerializer' is abstract",
        assertFailsWith<IllegalArgumentException> {
            simpleFastSerializer(listOf(), listOf(FastSerializer::class.java), skipping = false)
        }.message
    )

    @Test
    fun illegalInterface() = assertEquals(
        "type 'java.lang.AutoCloseable' is abstract",
        assertFailsWith<IllegalArgumentException> {
            simpleFastSerializer(listOf(), listOf(AutoCloseable::class.java), skipping = false)
        }.message
    )

    @Test
    fun illegalAnnotation() = assertEquals(
        "type 'ch.softappeal.yass.Tag' is abstract",
        assertFailsWith<IllegalArgumentException> {
            simpleFastSerializer(listOf(), listOf(Tag::class.java), skipping = false)
        }.message
    )

    @Test
    fun illegalEnumeration() = assertEquals(
        "type 'ch.softappeal.yass.serialize.Color' is an enumeration",
        assertFailsWith<IllegalArgumentException> {
            simpleFastSerializer(listOf(), listOf(), listOf(Color::class.java), false)
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
            simpleFastSerializer(listOf(), listOf(Color::class.java, AllTypes::class.java), skipping = false)
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
            taggedFastSerializer(listOf(), listOf(MissingClassTag::class.java), skipping = false)
        }.message
    )

    @Test
    fun duplicatedTypeTag() = assertEquals(
        "type id 120 used for 'ch.softappeal.yass.serialize.fast.C2' and 'ch.softappeal.yass.serialize.fast.C1'",
        assertFailsWith<IllegalStateException> {
            taggedFastSerializer(listOf(), listOf(C1::class.java, C2::class.java), skipping = false)
        }.message
    )

    @Tag(-1)
    class InvalidTypeTag// empty

    @Test
    fun invalidTypeTag() = assertEquals(
        "id -1 for type " +
            "'ch.softappeal.yass.serialize.fast.FastSerializerTest.InvalidTypeTag' must be >= 0",
        assertFailsWith<IllegalArgumentException> {
            taggedFastSerializer(listOf(), listOf(InvalidTypeTag::class.java), skipping = false)
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
            taggedFastSerializer(listOf(), listOf(InvalidFieldTag::class.java), skipping = false)
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
            taggedFastSerializer(listOf(), listOf(DuplicatedFieldTag::class.java), skipping = false)
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

    @Test
    fun versioning() {
        val v1serializer =
            taggedFastSerializer(
                listOf(TypeDesc(3, IntSerializer)),
                listOf(E1::class.java, C1::class.java),
                skipping = false
            )
        val v2serializer =
            taggedFastSerializer(
                listOf(TypeDesc(3, IntSerializer)),
                listOf(E2::class.java, C2::class.java),
                skipping = false
            )

        fun copy(input: Any): Any {
            val buffer = ByteArrayOutputStream()
            val writer = writer(buffer)
            v1serializer.write(writer, input)
            writer.writeByte(123.toByte()) // write sentinel
            val reader = reader(ByteArrayInputStream(buffer.toByteArray()))
            val output = v2serializer.read(reader)
            assertEquals(123.toByte(), reader.readByte()) // check sentinel
            return output!!
        }

        val c2 = copy(C1(42)) as C2
        assertEquals(42, c2.i1)
        assertNull(c2.i2)
        assertEquals(13, c2.i2())
        assertSame(copy(E1.c1), E2.c1)
        assertSame(copy(E1.c2), E2.c2)
    }
}
