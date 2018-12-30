package ch.softappeal.yass.serialize.nested

import ch.softappeal.yass.*
import ch.softappeal.yass.serialize.*

@Tag(41)
class AllTypes : PrimitiveTypes {
    @Tag(100)
    var stringField: String? = null
    @Tag(101)
    var colorField: Color? = null
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
}
