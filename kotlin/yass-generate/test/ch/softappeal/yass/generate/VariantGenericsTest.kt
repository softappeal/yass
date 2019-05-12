package ch.softappeal.yass.generate

import ch.softappeal.yass.generate.ts.*
import ch.softappeal.yass.serialize.fast.*
import java.nio.file.*

// see https://discuss.kotlinlang.org/t/reflection-bug/12694

open class OpenClass
class FinalClass

class Demo(
    val llo: List<OpenClass>,
    val llf: List<FinalClass>,
    val lmo: MutableList<OpenClass>,
    val lmf: MutableList<FinalClass>,
    @JvmSuppressWildcards // see https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#variant-generics
    var rlo: List<OpenClass>,
    var rlf: List<FinalClass>,
    var rmo: MutableList<OpenClass>,
    var rmf: MutableList<FinalClass>
)

fun main() {
    Demo::class.java.declaredFields.forEach {
        println("${it.name}: ${it.genericType}")
    }
    TypeScriptGenerator(
        "",
        simpleFastSerializer(baseTypeSerializers(), listOf(Demo::class), listOf(), false),
        null, null,
        Paths.get("../../ts/tutorial/contract-include.txt"),
        mapOf(),
        Paths.get("build/test.ts")
    )
}
