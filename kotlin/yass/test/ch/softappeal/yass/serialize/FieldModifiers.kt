package ch.softappeal.yass.serialize

var CONSTRUCTOR_CALLED: Boolean = false

class FieldModifiers private constructor() {
    private val privateField: Int = 0
    private val privateFinalField: Int
    var publicField: Int = 0
    val publicFinalField: Int

    init {
        CONSTRUCTOR_CALLED = true
        privateFinalField = 100
        publicFinalField = 101
    }
}
