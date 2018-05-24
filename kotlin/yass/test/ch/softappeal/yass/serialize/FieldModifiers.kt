package ch.softappeal.yass.serialize

class FieldModifiers private constructor() {
    @Transient
    var transientField: Int = 0
    private val privateField: Int = 0
    private val privateFinalField: Int
    var publicField: Int = 0
    val publicFinalField: Int

    init {
        CONSTRUCTOR_CALLED = true
        privateFinalField = 100
        publicFinalField = 101
    }

    companion object {
        var CONSTRUCTOR_CALLED: Boolean = false
        var STATIC_FIELD: Int = 0
    }
}
