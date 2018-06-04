package ch.softappeal.yass.tutorial.generate

import ch.softappeal.yass.generate.ts.ExternalDesc
import ch.softappeal.yass.generate.ts.TypeScriptGenerator
import ch.softappeal.yass.tutorial.contract.Acceptor
import ch.softappeal.yass.tutorial.contract.ContractSerializer
import ch.softappeal.yass.tutorial.contract.Initiator
import java.nio.file.Paths

fun main(args: Array<String>) {
    TypeScriptGenerator(
        "ch.softappeal.yass.tutorial.contract",
        ContractSerializer,
        Initiator,
        Acceptor,
        Paths.get("../../ts/tutorial/contract-include.txt"),
        mapOf(Integer::class.java to ExternalDesc("Integer", "IntegerHandler")), // shows how to use a contract external base type
        Paths.get("build/generated/ts/contract.ts")
        // , "protected readonly __TYPE_KEY__!: never;"
    )
}
