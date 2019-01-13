package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.*
import ch.softappeal.yass.serialize.*

@Tag(536_870_911)
class PrimitiveTypes2(
    @Tag(536_870_911) val boolean: Boolean = true,
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
    object : BaseTypeSerializer<NewBoolean>(NewBoolean::class, FieldType.VarInt) {
        override fun read(reader: Reader) = NewBoolean(reader.readByte().toInt() != 0)
        override fun write(writer: Writer, value: NewBoolean) = writer.writeByte((if (value.value) 1 else 0).toByte())
    }

private val SNewBooleanSerializer =
    object : SBaseTypeSerializer<NewBoolean>(NewBoolean::class, SFieldType.VarInt) {
        override suspend fun read(reader: SReader) =
            NewBoolean(reader.readByte().toInt() != 0)

        override suspend fun write(writer: SWriter, value: NewBoolean) =
            writer.writeByte((if (value.value) 1 else 0).toByte())
    }

@Tag(343)
class NewClass

@Tag(763)
enum class NewEnum {
    C1,
    C2
}

@Tag(41)
class NewLink(
    @Tag(1) var next: NewLink?
)

val Serializer2 = taggedFastSerializer(
    BaseTypes.toMutableList().apply { add(TypeDesc(999, NewBooleanSerializer)) },
    listOf(
        Enum::class,
        PrimitiveTypes2::class,
        NullablePrimitiveTypes2::class,
        ObjectTypes2::class,
        NewClass::class,
        NewEnum::class
    ),
    listOf(
        Link::class,
        NewLink::class
    )
)

val SSerializer2 = sTaggedFastSerializer(
    SBaseTypes.toMutableList().apply { add(STypeDesc(999, SNewBooleanSerializer)) },
    listOf(
        Enum::class,
        PrimitiveTypes2::class,
        NullablePrimitiveTypes2::class,
        ObjectTypes2::class,
        NewClass::class,
        NewEnum::class
    ),
    listOf(
        Link::class,
        NewLink::class
    )
)
