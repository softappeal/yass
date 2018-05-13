package ch.softappeal.yass.generate

import ch.softappeal.yass.remote.ContractId
import ch.softappeal.yass.remote.MethodMapper
import ch.softappeal.yass.remote.Services
import ch.softappeal.yass.serialize.fast.FastSerializer
import ch.softappeal.yass.serialize.fast.TypeHandler
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.util.ArrayList
import java.util.Arrays
import java.util.Comparator
import java.util.HashSet
import java.util.SortedMap
import java.util.SortedSet
import java.util.TreeSet
import java.util.function.Consumer
import java.util.stream.Collectors

class ServiceDesc internal constructor(val name: String, val contractId: ContractId<*>)

private val ROOT_CLASSES = HashSet<Type>(listOf(
    Any::class.java,
    Exception::class.java,
    RuntimeException::class.java,
    Error::class.java,
    Throwable::class.java
))

fun isRootClass(type: Class<*>) = ROOT_CLASSES.contains(type)

fun getServiceDescs(services: Services): List<ServiceDesc> {
    val serviceDescs = ArrayList<ServiceDesc>()
    for (field in services.javaClass.fields) {
        if (!Modifier.isStatic(field.modifiers) && (field.type === ContractId::class.java))
            serviceDescs.add(ServiceDesc(field.name, field.get(services) as ContractId<*>))
    }
    serviceDescs.sortWith(Comparator.comparing<ServiceDesc, Int> { sd -> sd.contractId.id })
    return serviceDescs
}

private fun getInterfaces(services: Services?): Set<Class<*>> = if (services == null) {
    emptySet()
} else getServiceDescs(services).stream().map { serviceDesc -> serviceDesc.contractId.contract }.collect(Collectors.toSet())

fun getMethods(type: Class<*>): Array<Method> {
    val methods = type.methods
    Arrays.sort(methods, Comparator.comparing<Method, String>({ it.name }))
    return methods
}

fun <E> iterate(iterable: Iterable<E>, notFirstAction: Runnable, alwaysAction: Consumer<E>) {
    var first = true
    for (element in iterable) {
        if (!first) notFirstAction.run()
        first = false
        alwaysAction.accept(element)
    }
}

abstract class Generator protected constructor(
    rootPackage: String, serializer: FastSerializer, protected val initiator: Services?, protected val acceptor: Services?
) {
    private val rootPackage: String = if (rootPackage.isEmpty()) "" else "$rootPackage."
    protected val id2typeHandler: SortedMap<Int, TypeHandler> = serializer.id2typeHandler()
    private var methodMapperFactory: Function1<Class<*>, MethodMapper>? = null
    protected val interfaces: SortedSet<Class<*>> = TreeSet(Comparator.comparing<Class<*>, String>({ c -> c.canonicalName }))

    init {
        check((initiator == null) || (acceptor == null) || (initiator.methodMapperFactory === acceptor.methodMapperFactory)) {
            "initiator and acceptor must have same methodMapperFactory"
        }
        if (initiator != null) methodMapperFactory = initiator.methodMapperFactory
        if (acceptor != null) methodMapperFactory = acceptor.methodMapperFactory
        interfaces.addAll(getInterfaces(initiator))
        interfaces.addAll(getInterfaces(acceptor))
        interfaces.forEach(Consumer { this.checkType(it) })
    }

    protected fun checkType(type: Class<*>) {
        check(type.canonicalName.startsWith(rootPackage)) {
            "type '" + type.canonicalName + "' doesn't have root package '" + rootPackage + "'"
        }
    }

    protected fun qualifiedName(type: Class<*>): String = type.canonicalName.substring(rootPackage.length)
    protected fun methodMapper(type: Class<*>): MethodMapper = methodMapperFactory!!.invoke(type)
}
