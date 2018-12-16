package ch.softappeal.yass.serialize

import ch.softappeal.yass.*
import java.io.*

@Tag(40)
open class PrimitiveTypes : Serializable {
    @Tag(28)
    var booleanField: Boolean = false
    @Tag(1)
    var byteField: Byte = 0
    @Tag(2)
    var shortField: Short = 0
    @Tag(3)
    var intField: Int = 0
    @Tag(4)
    var longField: Long = 0
    @Tag(5)
    var charField = ' '
    @Tag(6)
    var floatField: Float = 0.toFloat()
    @Tag(7)
    var doubleField: Double = 0.toDouble()
    @Tag(10)
    var booleanArrayField: BooleanArray? = null
    @Tag(11)
    var byteArrayField: ByteArray? = null
    @Tag(12)
    var shortArrayField: ShortArray? = null
    @Tag(13)
    var intArrayField: IntArray? = null
    @Tag(14)
    var longArrayField: LongArray? = null
    @Tag(15)
    var charArrayField: CharArray? = null
    @Tag(16)
    var floatArrayField: FloatArray? = null
    @Tag(17)
    var doubleArrayField: DoubleArray? = null
    @Tag(20)
    var booleanWrapperField: Boolean? = null
    @Tag(21)
    var byteWrapperField: Byte? = null
    @Tag(22)
    var shortWrapperField: Short? = null
    @Tag(23)
    var intWrapperField: Int? = null
    @Tag(24)
    var longWrapperField: Long? = null
    @Tag(25)
    var charWrapperField: Char? = null
    @Tag(26)
    var floatWrapperField: Float? = null
    @Tag(27)
    var doubleWrapperField: Double? = null

    constructor(intField: Int) {
        this.intField = intField
    }

    constructor()
}
