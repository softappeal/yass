package ch.softappeal.yass.kotlindir.serialize.contract

import ch.softappeal.yass.Tag

import java.io.Serializable

@Tag(50)
class Node : Serializable {
    @Tag(1)
    var id: Int = 0
    @Tag(2)
    var link: Node? = null

    constructor(id: Int) {
        this.id = id
    }

    constructor()

    companion object {
        private const val serialVersionUID = 1L
        var staticInt: Int = 0
    }
}
