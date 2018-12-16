package ch.softappeal.yass.serialize

import ch.softappeal.yass.*
import java.io.*

@Tag(50)
class Node(@Tag(1) var id: Int) : Serializable {
    @Tag(2)
    var link: Node? = null
}
