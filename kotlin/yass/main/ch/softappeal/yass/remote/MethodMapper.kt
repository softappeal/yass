@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.remote

import ch.softappeal.yass.tag
import java.lang.reflect.Method

@MustBeDocumented
@Retention
@Target(AnnotationTarget.FUNCTION)
annotation class OneWay

data class MethodMapping(val method: Method, val id: Int, val oneWay: Boolean = method.isAnnotationPresent(OneWay::class.java)) {
    init {
        if (oneWay) {
            check(method.returnType === Void.TYPE) { "OneWay method '$method' must return void" }
            check(method.exceptionTypes.isEmpty()) { "OneWay method '$method' must not throw exceptions" }
        }
    }
}

interface MethodMapper {
    fun map(id: Int): MethodMapping
    fun map(method: Method): MethodMapping
}

typealias MethodMapperFactory = (contract: Class<*>) -> MethodMapper

fun MethodMapperFactory.mappings(contract: Class<*>): Sequence<MethodMapping> {
    val methodMapper = this(contract)
    return contract.methods.asSequence()
        .map(methodMapper::map)
        .sortedBy(MethodMapping::id)
}

val SimpleMethodMapperFactory: MethodMapperFactory = { contract ->
    val mappings = mutableListOf<MethodMapping>()
    val name2mapping = mutableMapOf<String, MethodMapping>()
    for (method in contract.methods.sortedBy(Method::getName)) {
        val mapping = MethodMapping(method, mappings.size)
        val oldMapping = name2mapping.put(method.name, mapping)
        if (oldMapping != null) error("methods '$method' and '${oldMapping.method}' in contract '$contract' are overloaded")
        mappings.add(mapping)
    }
    object : MethodMapper {
        override fun map(id: Int) =
            if (id in 0 until mappings.size) mappings[id] else error("unexpected method id $id for contract '$contract'")

        override fun map(method: Method) =
            checkNotNull(name2mapping[method.name]) { "unexpected method '$method' for contract '$contract'" }
    }
}

val TaggedMethodMapperFactory: MethodMapperFactory = { contract ->
    val id2mapping = mutableMapOf<Int, MethodMapping>()
    for (method in contract.methods) {
        val id = tag(method)
        val oldMapping = id2mapping.put(id, MethodMapping(method, id))
        if (oldMapping != null) error("tag $id used for methods '$method' and '${oldMapping.method}' in contract '$contract'")
    }
    object : MethodMapper {
        override fun map(id: Int) =
            checkNotNull(id2mapping[id]) { "unexpected method id $id for contract '$contract'" }

        override fun map(method: Method) =
            checkNotNull(id2mapping[tag(method)]) { "unexpected method '$method' for contract '$contract'" }
    }
}
