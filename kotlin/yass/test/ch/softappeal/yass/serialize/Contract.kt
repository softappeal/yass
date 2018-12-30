package ch.softappeal.yass.serialize

import ch.softappeal.yass.*

@Tag(30)
enum class Color constructor(val text: String) { RED("red"), GREEN("green"), BLUE("blue") }

@Tag(42)
class IntException(
    @Tag(1) var value: Int
) : Exception()
