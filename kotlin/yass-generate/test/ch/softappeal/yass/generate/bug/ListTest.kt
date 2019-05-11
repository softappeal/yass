package ch.softappeal.yass.generate.bug

// $todo: check with Python and Typescript

import ch.softappeal.yass.generate.ts.*
import ch.softappeal.yass.serialize.fast.*
import java.nio.file.*

class X(
    var // fails
    // val // works
    x:
    List // fails
    // MutableList // works
    <JavaClass> // fails
    // <String> // works
)

fun main() {
    TypeScriptGenerator(
        "",
        simpleFastSerializer(baseTypeSerializers(), listOf(X::class), listOf(), false),
        null, null,
        Paths.get("../../ts/tutorial/contract-include.txt"),
        mapOf(),
        Paths.get("build/test.ts")
    )
}
