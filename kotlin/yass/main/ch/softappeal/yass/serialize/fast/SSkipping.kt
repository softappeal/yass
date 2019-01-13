package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.serialize.fast.SFastSerializer.*

private fun objectTypeFromSkippingId(skippingId: Int): SObjectType =
    SObjectType.values()[skippingId and ((1 shl ObjectTypeBits) - 1)]

internal enum class SObjectType {
    TreeClass {
        override suspend fun skip(input: Input) {
            while (true) {
                val skippingId = input.reader.readVarInt()
                if (fieldIdFromSkippingId(skippingId) == EndFieldId) return
                sFieldTypeFromSkippingId(skippingId).skip(input)
            }
        }
    },
    GraphClass {
        override suspend fun skip(input: Input) {
            input.addToObjects(SkippedGraphObject)
            TreeClass.skip(input)
        }
    },
    Binary {
        override suspend fun skip(input: Input) = SFieldType.Binary.skip(input)
    },
    VarInt {
        override suspend fun skip(input: Input) = SFieldType.VarInt.skip(input)
    };

    fun skippingId(id: Int): Int = (id shl ObjectTypeBits) or ordinal
    abstract suspend fun skip(input: Input)
}

internal fun sFieldTypeFromSkippingId(skippingId: Int): SFieldType =
    SFieldType.values()[skippingId and ((1 shl FieldTypeBits) - 1)]

enum class SFieldType(internal val objectType: SObjectType) {
    Class(SObjectType.TreeClass) {
        override suspend fun skip(input: Input) {
            val skippingId = input.reader.readVarInt()
            when (typeIdFromSkippingId(skippingId)) {
                NullTypeDesc.id -> Unit
                ReferenceTypeDesc.id -> input.reader.skipVar()
                ListTypeDesc.id -> List.skip(input)
                else -> objectTypeFromSkippingId(skippingId).skip(input)
            }
        }
    },
    List(SObjectType.TreeClass) {
        override suspend fun skip(input: Input) = repeat(input.reader.readVarInt()) { SFieldType.Class.skip(input) }
    },
    Binary(SObjectType.Binary) {
        override suspend fun skip(input: Input) = skip(input.reader)
        override suspend fun skip(reader: SReader) = repeat(reader.readVarInt()) { reader.readByte() }
    },
    VarInt(SObjectType.VarInt) {
        override suspend fun skip(input: Input) = skip(input.reader)
        override suspend fun skip(reader: SReader) = reader.skipVar()
    };

    internal fun skippingId(id: Int): Int = (id shl FieldTypeBits) or ordinal
    internal abstract suspend fun skip(input: Input)
    internal open suspend fun skip(reader: SReader): Unit = error("only needed by tests")
}
