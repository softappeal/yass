package ch.softappeal.yass.serialize

import ch.softappeal.yass.*

@Tag(120)
class C1(
    @Tag(1) val i1: Int
)

@Tag(120)
class C2(
    @Tag(1) val i1: Int,
    @Tag(2) val i2: Int?
) {
    fun i2(): Int = i2 ?: 13
}

@Tag(30)
enum class Color constructor(val text: String) { RED("red"), GREEN("green"), BLUE("blue") }

@Tag(200)
enum class E1 { c1, c2 }

@Tag(200)
enum class E2 { c1, c2, c3 }

class NoDefaultConstructor private constructor(val i: Int)

@Tag(42)
class IntException(
    @Tag(1) var value: Int
) : Exception()
