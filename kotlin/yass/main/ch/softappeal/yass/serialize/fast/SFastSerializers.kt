package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.*
import java.io.*
import java.lang.reflect.*
import kotlin.reflect.*

fun sSimpleFastSerializer(
    baseTypeSerializers: List<SBaseTypeSerializer<*>>,
    treeConcreteClasses: List<KClass<*>>,
    graphConcreteClasses: List<KClass<*>> = listOf(),
    skipping: Boolean = true
) = object : SFastSerializer(skipping) {
    init {
        var id = FirstTypeId
        for (typeSerializer in baseTypeSerializers) addBaseType(STypeDesc(id++, typeSerializer))
        for (type in treeConcreteClasses) if (type.java.isEnum) addEnum(id++, type) else addClass(id++, type, false)
        for (type in graphConcreteClasses) {
            checkClass(type)
            addClass(id++, type, true)
        }
        fixup()
    }

    fun addClass(typeId: Int, type: KClass<*>, graph: Boolean) {
        val id2field = mutableMapOf<Int, Field>()
        var fieldId = FirstFieldId
        for (field in type.java.allFields) id2field[fieldId++] = field
        addClass(typeId, type, graph, id2field)
    }
}

fun sTaggedFastSerializer(
    baseTypeDescs: Collection<STypeDesc>,
    treeConcreteClasses: Collection<KClass<*>>,
    graphConcreteClasses: Collection<KClass<*>> = listOf(),
    skipping: Boolean = true
) = object : SFastSerializer(skipping) {
    init {
        baseTypeDescs.forEach(::addBaseType)
        treeConcreteClasses
            .forEach { type -> if (type.java.isEnum) addEnum(tag(type.java), type) else addClass(type, false) }
        graphConcreteClasses
            .forEach { type ->
                checkClass(type)
                addClass(type, true)
            }
        fixup()
    }

    fun addClass(type: KClass<*>, graph: Boolean) {
        val id2field = mutableMapOf<Int, Field>()
        for (field in type.java.allFields) {
            val id = tag(field)
            val oldField = id2field.put(id, field)
            require(oldField == null) { "tag $id used for fields '$field' and '$oldField'" }
        }
        addClass(tag(type.java), type, graph, id2field)
    }
}

fun SFastSerializer.print(printer: PrintWriter) {
    printer.println("skipping = $skipping")
    printer.println()
    id2typeSerializer.forEach { id, typeSerializer ->
        if (id < FirstTypeId) return@forEach
        with(typeSerializer.type) {
            printer.print("$id: $canonicalName")
            if (typeSerializer is SBaseTypeSerializer<*>) {
                printer.println(" ${typeSerializer.fieldType}")
                if (isEnum) for ((i, c) in enumConstants.withIndex()) printer.println("    $i: $c")
            } else if (typeSerializer is SFastSerializer.ClassTypeSerializer) {
                printer.println(" ${if (typeSerializer.graph) "graph" else "tree"}")
                for (fd in typeSerializer.fieldDescs)
                    printer.println("    ${fd.id}: ${fd.serializer.field.toGenericString()}")
            }
            printer.println()
        }
    }
}
