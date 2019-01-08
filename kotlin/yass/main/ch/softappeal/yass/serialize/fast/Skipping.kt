package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.serialize.fast.FastSerializer.*

private const val ObjectTypeBits = 2
internal const val MaxTypeId = Int.MAX_VALUE shr ObjectTypeBits
internal fun typeIdFromSkippingId(skippingId: Int): Int = skippingId ushr ObjectTypeBits
private fun objectTypeFromSkippingId(skippingId: Int): ObjectType =
    ObjectType.values()[skippingId and ((1 shl ObjectTypeBits) - 1)]

private val SkippedGraphObject = Any()

internal enum class ObjectType {
    TreeClass {
        override fun skip(input: Input) {
            while (true) {
                val skippingId = input.reader.readVarInt()
                if (fieldIdFromSkippingId(skippingId) == EndFieldId) return
                fieldTypeFromSkippingId(skippingId).skip(input)
            }
        }
    },
    GraphClass {
        override fun skip(input: Input) {
            input.addToObjects(SkippedGraphObject)
            TreeClass.skip(input)
        }
    },
    Binary {
        override fun skip(input: Input) = FieldType.Binary.skip(input)
    },
    VarInt {
        override fun skip(input: Input) = FieldType.VarInt.skip(input)
    };

    fun skippingId(id: Int): Int = (id shl ObjectTypeBits) or ordinal
    abstract fun skip(input: Input)
}

private const val FieldTypeBits = 2
internal const val MaxFieldId = Int.MAX_VALUE shr FieldTypeBits
internal fun fieldIdFromSkippingId(skippingId: Int): Int = skippingId ushr FieldTypeBits
internal fun fieldTypeFromSkippingId(skippingId: Int): FieldType =
    FieldType.values()[skippingId and ((1 shl FieldTypeBits) - 1)]

enum class FieldType(internal val objectType: ObjectType) {
    Class(ObjectType.TreeClass) {
        override fun skip(input: Input) {
            val skippingId = input.reader.readVarInt()
            when (typeIdFromSkippingId(skippingId)) {
                NullTypeDesc.id -> Unit
                ReferenceTypeDesc.id -> input.reader.skipVar()
                ListTypeDesc.id -> List.skip(input)
                else -> objectTypeFromSkippingId(skippingId).skip(input)
            }
        }
    },
    List(ObjectType.TreeClass) {
        override fun skip(input: Input) = repeat(input.reader.readVarInt()) { FieldType.Class.skip(input) }
    },
    Binary(ObjectType.Binary) {
        override fun skip(input: Input) = skip(input.reader)
        override fun skip(reader: Reader) = repeat(reader.readVarInt()) { reader.readByte() }
    },
    VarInt(ObjectType.VarInt) {
        override fun skip(input: Input) = skip(input.reader)
        override fun skip(reader: Reader) = reader.skipVar()
    };

    internal fun skippingId(id: Int): Int = (id shl FieldTypeBits) or ordinal
    internal abstract fun skip(input: Input)
    internal open fun skip(reader: Reader): Unit = error("only needed by tests")
}
