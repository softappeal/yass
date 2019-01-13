package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.*

val BaseTypes = listOf(
    TypeDesc(3, BooleanSerializer),
    TypeDesc(4, ByteSerializer),
    TypeDesc(5, ShortSerializer),
    TypeDesc(6, IntSerializer),
    TypeDesc(7, LongSerializer),
    TypeDesc(8, CharSerializer),
    TypeDesc(9, FloatSerializer),
    TypeDesc(10, DoubleSerializer),
    TypeDesc(11, BinarySerializer),
    TypeDesc(12, StringSerializer)
)

val SBaseTypes = listOf(
    STypeDesc(3, SBooleanSerializer),
    STypeDesc(4, SByteSerializer),
    STypeDesc(5, SShortSerializer),
    STypeDesc(6, SIntSerializer),
    STypeDesc(7, SLongSerializer),
    STypeDesc(8, SCharSerializer),
    STypeDesc(9, SFloatSerializer),
    STypeDesc(10, SDoubleSerializer),
    STypeDesc(11, SBinarySerializer),
    STypeDesc(12, SStringSerializer)
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

@Tag(536_870_911)
class PrimitiveTypes(
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

val Serializer = taggedFastSerializer(
    BaseTypes,
    listOf(
        Enum::class,
        PrimitiveTypes::class,
        NullablePrimitiveTypes::class,
        ObjectTypes::class
    ),
    listOf(
        Link::class
    )
)

val SSerializer = sTaggedFastSerializer(
    SBaseTypes,
    listOf(
        Enum::class,
        PrimitiveTypes::class,
        NullablePrimitiveTypes::class,
        ObjectTypes::class
    ),
    listOf(
        Link::class
    )
)
