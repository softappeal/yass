package ch.softappeal.yass.serialize.fast

/* $$$
- enum consts with tags
- unknown enum consts map to required ordinal 0
 */

private const val ObjectTypeBits = 2
internal const val MaxTypeId = Int.MAX_VALUE shr ObjectTypeBits

internal fun typeIdFromSkippingId(skippingId: Int): Int = skippingId ushr ObjectTypeBits
private fun objectTypeFromSkippingId(skippingId: Int): ObjectType =
    ObjectType.values()[skippingId and ((1 shl ObjectTypeBits) - 1)]

private val SkippedGraphObject = Any()

internal enum class ObjectType {
    TreeClass {
        override fun skip(input: FastSerializer.Input) {
            while (true) {
                val skippingId = input.reader.readVarInt()
                if (fieldIdFromSkippingId(skippingId) == EndFieldId) return
                fieldTypeFromSkippingId(skippingId).skip(input)
            }
        }
    },
    GraphClass {
        override fun skip(input: FastSerializer.Input) {
            println("$$$ GraphClass")
            if (input.objects == null) input.objects = mutableListOf()
            input.objects!!.add(SkippedGraphObject)
            TreeClass.skip(input)
        }
    },
    Binary {
        override fun skip(input: FastSerializer.Input) = FieldType.Binary.skip(input)
    },
    VarInt {
        override fun skip(input: FastSerializer.Input) = FieldType.VarInt.skip(input)
    };

    fun skippingId(id: Int): Int = (id shl ObjectTypeBits) or ordinal
    abstract fun skip(input: FastSerializer.Input)
}

private const val FieldTypeBits = 2
internal const val MaxFieldId = Int.MAX_VALUE shr FieldTypeBits

internal fun fieldIdFromSkippingId(skippingId: Int): Int = skippingId ushr FieldTypeBits

internal fun fieldTypeFromSkippingId(skippingId: Int): FieldType =
    FieldType.values()[skippingId and ((1 shl FieldTypeBits) - 1)]

enum class FieldType(internal val objectType: ObjectType) {
    ClassOrReference(ObjectType.TreeClass) {
        override fun skip(input: FastSerializer.Input) {
            val skippingId = input.reader.readVarInt()
            when (typeIdFromSkippingId(skippingId)) {
                NullTypeDesc.id -> {
                    println("$$$ null")
                }
                ReferenceTypeDesc.id -> {
                    println("$$$ reference")
                    input.objects!![input.reader.readVarInt()] // $$$ paranoia check
                }
                ListTypeDesc.id -> {
                    println("$$$ list")
                    List.skip(input)
                }
                else -> objectTypeFromSkippingId(skippingId).skip(input)
            }
        }
    },
    List(ObjectType.TreeClass) {
        override fun skip(input: FastSerializer.Input) {
            var length = input.reader.readVarInt()
            while (length-- > 0) FieldType.ClassOrReference.skip(input)
        }
    },
    Binary(ObjectType.Binary) {
        override fun skip(input: FastSerializer.Input) {
            repeat(input.reader.readVarInt()) { input.reader.readByte() }
        }
    },
    VarInt(ObjectType.VarInt) {
        override fun skip(input: FastSerializer.Input) {
            input.reader.readVarLong()
        }
    };

    internal fun skippingId(id: Int): Int = (id shl FieldTypeBits) or ordinal
    internal abstract fun skip(input: FastSerializer.Input)
}
