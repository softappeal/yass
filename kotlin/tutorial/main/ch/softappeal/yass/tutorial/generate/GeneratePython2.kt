package ch.softappeal.yass.tutorial.generate

import ch.softappeal.yass.generate.py.ExternalDesc
import ch.softappeal.yass.generate.py.PythonGenerator
import ch.softappeal.yass.tutorial.contract.Initiator
import ch.softappeal.yass.tutorial.contract.PyAcceptor
import ch.softappeal.yass.tutorial.contract.PyContractSerializer
import java.nio.file.Paths

fun main(args: Array<String>) {
    PythonGenerator(
        "ch.softappeal.yass.tutorial.contract",
        PyContractSerializer,
        Initiator,
        PyAcceptor,
        false,
        Paths.get("../../py2/tutorial/contract_include_each_module.txt"),
        mapOf("" to Paths.get("../../py2/tutorial/contract_include_root_module.txt")),
        mapOf(Integer::class.java to ExternalDesc("Integer", "Integer.TYPE_DESC")), // shows how to use a contract external base type
        Paths.get("build/generated/py2")
    )
}
