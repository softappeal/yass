package ch.softappeal.yass.generate

// $todo: check with Python and Typescript

import ch.softappeal.yass.generate.ts.*
import ch.softappeal.yass.serialize.fast.*
import java.nio.file.*

open class OpenClass
class FinalClass

class Demo(
    val llo: List<OpenClass>,
    val llf: List<FinalClass>,
    val lmo: MutableList<OpenClass>,
    val lmf: MutableList<FinalClass>,
    var rlo: List<OpenClass>,
    var rlf: List<FinalClass>,
    var rmo: MutableList<OpenClass>,
    var rmf: MutableList<FinalClass>
)

fun main() {
    Demo::class.java.declaredFields.forEach {
        println("${it.name}: ${it.genericType}")
    }
    /*
        llo: java.util.List<OpenClass>
        llf: java.util.List<FinalClass>
        lmo: java.util.List<OpenClass>
        lmf: java.util.List<FinalClass>
        rlo: java.util.List<? extends OpenClass>
        rlf: java.util.List<FinalClass>
        rmo: java.util.List<OpenClass>
        rmf: java.util.List<FinalClass>

        Why has only property 'rlo' a WildcardType?
    */
    TypeScriptGenerator(
        "",
        simpleFastSerializer(baseTypeSerializers(), listOf(Demo::class), listOf(), false),
        null, null,
        Paths.get("../../ts/tutorial/contract-include.txt"),
        mapOf(),
        Paths.get("build/test.ts")
    )
}
