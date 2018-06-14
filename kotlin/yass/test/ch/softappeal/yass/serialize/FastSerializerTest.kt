package ch.softappeal.yass.serialize

import ch.softappeal.yass.Tag
import ch.softappeal.yass.compareFile
import ch.softappeal.yass.serialize.fast.BaseTypeSerializer
import ch.softappeal.yass.serialize.fast.FastSerializer
import ch.softappeal.yass.serialize.fast.IntSerializer
import ch.softappeal.yass.serialize.fast.TypeDesc
import ch.softappeal.yass.serialize.fast.print
import ch.softappeal.yass.serialize.fast.simpleFastSerializer
import ch.softappeal.yass.serialize.fast.taggedFastSerializer
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class FastSerializerTest {

    class A(val a: Int)

    @Test
    fun duplicatedField() = try {
        val field = A::class.java.getDeclaredField("a")
        object : FastSerializer() {
            init {
                addClass(999, String::class.java, false, mapOf(1 to field, 2 to field))
            }
        }
        fail()
    } catch (e: IllegalArgumentException) {
        assertEquals(
            "duplicated field name 'private final int ch.softappeal.yass.serialize.FastSerializerTest${'$'}A.a' and 'private final int ch.softappeal.yass.serialize.FastSerializerTest${'$'}A.a' not allowed in class hierarchy",
            e.message
        )
    }

    @Test
    fun notAnEnumeration() = try {
        object : FastSerializer() {
            init {
                addEnum(999, String::class.java)
            }
        }
        fail()
    } catch (e: IllegalArgumentException) {
        assertEquals("type 'java.lang.String' is not an enumeration", e.message)
    }

    @Test
    fun baseEnumeration() = try {
        taggedFastSerializer(
            listOf(TypeDesc(1, object : BaseTypeSerializer<Color>(Color::class.java) {
                override fun read(reader: Reader) = Color.BLUE
                override fun write(writer: Writer, value: Color) {}
            })),
            listOf()
        )
        fail()
    } catch (e: IllegalArgumentException) {
        assertEquals("base type 'ch.softappeal.yass.serialize.Color' is an enumeration", e.message)
    }

    @Test
    fun duplicatedClass() = try {
        taggedFastSerializer(listOf(), listOf(Color::class.java, Color::class.java))
        fail()
    } catch (e: IllegalArgumentException) {
        assertEquals("type 'ch.softappeal.yass.serialize.Color' already added", e.message)
    }

    @Test
    fun abstractClass() = try {
        simpleFastSerializer(listOf(), listOf(FastSerializer::class.java))
        fail()
    } catch (e: IllegalArgumentException) {
        assertEquals("type 'ch.softappeal.yass.serialize.fast.FastSerializer' is abstract", e.message)
    }

    @Test
    fun illegalInterface() = try {
        simpleFastSerializer(listOf(), listOf(AutoCloseable::class.java))
        fail()
    } catch (e: IllegalArgumentException) {
        assertEquals("type 'java.lang.AutoCloseable' is abstract", e.message)
    }

    @Test
    fun illegalAnnotation() = try {
        simpleFastSerializer(listOf(), listOf(Tag::class.java))
        fail()
    } catch (e: IllegalArgumentException) {
        assertEquals("type 'ch.softappeal.yass.Tag' is abstract", e.message)
    }

    @Test
    fun illegalEnumeration() = try {
        simpleFastSerializer(listOf(), listOf(), listOf(Color::class.java))
        fail()
    } catch (e: IllegalArgumentException) {
        assertEquals("type 'ch.softappeal.yass.serialize.Color' is an enumeration", e.message)
    }

    @Test
    fun missingType() {
        val serializer = taggedFastSerializer(listOf(), listOf())
        try {
            copy(serializer, Color.BLUE)
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("missing type 'ch.softappeal.yass.serialize.Color'", e.message)
        }

    }

    class MissingClassTag {
        @Tag(1)
        internal var i: Int = 0
    }

    @Test
    fun missingClassTag() = try {
        taggedFastSerializer(listOf(), listOf(MissingClassTag::class.java))
        fail()
    } catch (e: IllegalStateException) {
        assertEquals("missing tag for 'class ch.softappeal.yass.serialize.FastSerializerTest${'$'}MissingClassTag'", e.message)
    }

    @Test
    fun duplicatedTypeTag() = try {
        taggedFastSerializer(listOf(), listOf(C1::class.java, C2::class.java))
        fail()
    } catch (e: IllegalStateException) {
        assertEquals(
            "type id 120 used for 'ch.softappeal.yass.serialize.C2' and 'ch.softappeal.yass.serialize.C1'",
            e.message
        )
    }

    @Tag(-1)
    class InvalidTypeTag// empty

    @Test
    fun invalidTypeTag() = try {
        taggedFastSerializer(listOf(), listOf(InvalidTypeTag::class.java))
        fail()
    } catch (e: IllegalArgumentException) {
        assertEquals("id -1 for type 'ch.softappeal.yass.serialize.FastSerializerTest.InvalidTypeTag' must be >= 0", e.message)
    }

    @Tag(0)
    class InvalidFieldTag {
        @Tag(0)
        internal var i: Int = 0
    }

    @Test
    fun invalidFieldTag() = try {
        taggedFastSerializer(listOf(), listOf(InvalidFieldTag::class.java))
        fail()
    } catch (e: IllegalArgumentException) {
        assertEquals(
            "id 0 for field 'private int ch.softappeal.yass.serialize.FastSerializerTest${'$'}InvalidFieldTag.i' must be >= 1",
            e.message
        )
    }

    @Tag(0)
    class DuplicatedFieldTag {
        @Tag(1)
        internal var i1: Int = 0
        @Tag(1)
        internal var i2: Int = 0
    }

    @Test
    fun duplicatedFieldTag() = try {
        taggedFastSerializer(listOf(), listOf(DuplicatedFieldTag::class.java))
        fail()
    } catch (e: IllegalArgumentException) {
        assertEquals(
            "tag 1 used for fields 'private int ch.softappeal.yass.serialize.FastSerializerTest${'$'}DuplicatedFieldTag.i2' and 'private int ch.softappeal.yass.serialize.FastSerializerTest${'$'}DuplicatedFieldTag.i1'",
            e.message
        )
    }

    @Test
    fun taggedPrint() {
        compareFile("ch/softappeal/yass/serialize/TaggedFastSerializerTest.numbers.txt") { TAGGED_FAST_SERIALIZER.print(it) }
    }

    @Test
    fun simplePrint() {
        compareFile("ch/softappeal/yass/serialize/SimpleFastSerializerTest.numbers.txt") { SIMPLE_FAST_SERIALIZER.print(it) }
    }

    @Test
    fun bytes() {
        compareFile("ch/softappeal/yass/serialize/TaggedFastSerializerTest.bytes.txt") { printer ->
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
        val v1serializer = taggedFastSerializer(listOf(TypeDesc(3, IntSerializer)), listOf(E1::class.java, C1::class.java))
        val v2serializer = taggedFastSerializer(listOf(TypeDesc(3, IntSerializer)), listOf(E2::class.java, C2::class.java))
        fun copy(input: Any): Any {
            val buffer = ByteArrayOutputStream()
            val writer = writer(buffer)
            v1serializer.write(writer, input)
            writer.writeByte(123.toByte()) // write sentinel
            val reader = reader(ByteArrayInputStream(buffer.toByteArray()))
            val output = v2serializer.read(reader)
            assertTrue(reader.readByte() == 123.toByte()) // check sentinel
            return output!!
        }

        val c2 = copy(C1(42)) as C2
        assertTrue(c2.i1 == 42)
        assertNull(c2.i2)
        assertTrue(c2.i2() == 13)
        assertSame(copy(E1.c1), E2.c1)
        assertSame(copy(E1.c2), E2.c2)
    }

}
