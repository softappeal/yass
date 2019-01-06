package ch.softappeal.yass.serialize

import ch.softappeal.yass.*
import java.io.*

@Tag(40)
open class PrimitiveTypes : Serializable {
    @Tag(28)
    var booleanField: Boolean = false
    @Tag(2)
    var shortField: Short = 0
    @Tag(3)
    var intField: Int = 0
    @Tag(4)
    var longField: Long = 0
    @Tag(11)
    var byteArrayField: ByteArray? = null
    @Tag(20)
    var booleanWrapperField: Boolean? = null
    @Tag(22)
    var shortWrapperField: Short? = null
    @Tag(23)
    var intWrapperField: Int? = null
    @Tag(24)
    var longWrapperField: Long? = null

    constructor(intField: Int) {
        this.intField = intField
    }

    constructor()
}
