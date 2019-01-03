package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.serialize.*

/* $$$
- remove Bytes 1..4 -> we would gain for both enums one bit
- enum consts with tags
- unknown enum consts map to required ordinal 0
 */

private const val ObjectTypeBits = 3
internal const val MaxTypeId = Int.MAX_VALUE shr ObjectTypeBits

internal fun typeIdFromSkippingId(skippingId: Int): Int = skippingId ushr ObjectTypeBits
private fun objectTypeFromSkippingId(skippingId: Int): ObjectType =
    ObjectType.values()[skippingId and ((1 shl ObjectTypeBits) - 1)]

internal enum class ObjectType {
    TreeClass {
        override fun skip(reader: Reader) {
            while (true) {
                val skippingId = reader.readVarInt()
                if (fieldIdFromSkippingId(skippingId) == EndFieldId) return
                fieldTypeFromSkippingId(skippingId).skip(reader)
            }
        }
    },
    GraphClass {
        override fun skip(reader: Reader) = error("skipping of graph classes not allowed") // $$$ why is this true?
    },
    Binary {
        override fun skip(reader: Reader) = FieldType.Binary.skip(reader)
    },
    VarInt {
        override fun skip(reader: Reader) = FieldType.VarInt.skip(reader)
    },
    Bytes1 {
        override fun skip(reader: Reader) = FieldType.Bytes1.skip(reader)
    },
    Bytes2 {
        override fun skip(reader: Reader) = FieldType.Bytes2.skip(reader)
    },
    Bytes4 {
        override fun skip(reader: Reader) = FieldType.Bytes4.skip(reader)
    },
    Bytes8 {
        override fun skip(reader: Reader) = FieldType.Bytes8.skip(reader)
    };

    fun skippingId(id: Int): Int = (id shl ObjectTypeBits) or ordinal
    abstract fun skip(reader: Reader)
}

private const val FieldTypeBits = 3
internal const val MaxFieldId = Int.MAX_VALUE shr FieldTypeBits

internal fun fieldIdFromSkippingId(skippingId: Int): Int = skippingId ushr FieldTypeBits

internal fun fieldTypeFromSkippingId(skippingId: Int): FieldType =
    FieldType.values()[skippingId and ((1 shl FieldTypeBits) - 1)]

enum class FieldType(internal val objectType: ObjectType) {
    ClassOrReference(ObjectType.TreeClass) {
        override fun skip(reader: Reader) {
            val skippingId = reader.readVarInt()
            when (typeIdFromSkippingId(skippingId)) {
                NullTypeDesc.id -> {
                    println("$$$ null")
                }
                ReferenceTypeDesc.id -> {
                    println("$$$ reference")
                    reader.readVarInt()
                }
                ListTypeDesc.id -> {
                    println("$$$ list")
                    List.skip(reader)
                }
                else -> objectTypeFromSkippingId(skippingId).skip(reader)
            }
        }
    },
    List(ObjectType.TreeClass) {
        override fun skip(reader: Reader) {
            var length = reader.readVarInt()
            while (length-- > 0) FieldType.ClassOrReference.skip(reader)
        }
    },
    Binary(ObjectType.Binary) {
        override fun skip(reader: Reader) {
            repeat(reader.readVarInt()) { reader.readByte() }
        }
    },
    VarInt(ObjectType.VarInt) {
        override fun skip(reader: Reader) {
            reader.readVarLong()
        }
    },
    Bytes1(ObjectType.Bytes1) {
        override fun skip(reader: Reader) {
            reader.readByte()
        }
    },
    Bytes2(ObjectType.Bytes2) {
        override fun skip(reader: Reader) {
            repeat(2) { reader.readByte() }
        }
    },
    Bytes4(ObjectType.Bytes4) {
        override fun skip(reader: Reader) {
            repeat(4) { reader.readByte() }
        }
    },
    Bytes8(ObjectType.Bytes8) {
        override fun skip(reader: Reader) {
            repeat(8) { reader.readByte() }
        }
    };

    internal fun skippingId(id: Int): Int = (id shl FieldTypeBits) or ordinal
    internal abstract fun skip(reader: Reader)
}
