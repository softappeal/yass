package ch.softappeal.yass.serialize.nested

import ch.softappeal.yass.*
import ch.softappeal.yass.serialize.*
import java.math.*
import java.time.*
import java.util.*

@Tag(41)
class AllTypes : PrimitiveTypes {
    @Tag(100)
    var stringField: String? = null
    @Tag(101)
    var colorField: Color? = null
    @Tag(102)
    var bigDecimalField: BigDecimal? = null
    @Tag(103)
    var bigIntegerField: BigInteger? = null
    @Tag(104)
    var dateField: Date? = null
    @Tag(105)
    var instantField: Instant? = null
    @Tag(106)
    var primitiveTypesField: PrimitiveTypes? = null
    @Tag(107)
    var primitiveTypesListField: List<PrimitiveTypes?>? = null
    @Tag(108)
    var objectField: Any? = null
    @Tag(109)
    var objectListField: List<Any?>? = null
    @Tag(110)
    var exception: Exception? = null

    constructor(stringField: String) {
        this.stringField = stringField
    }

    constructor()

    companion object {
        private const val serialVersionUID = 1L
    }
}
