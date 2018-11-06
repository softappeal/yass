package ch.softappeal.yass

import java.lang.reflect.Array
import java.lang.reflect.Field
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

/** Dumps a value class (these should not reference other objects) to out (should be an one-liner). */
typealias ValueDumper = (out: StringBuilder, value: Any) -> Unit

val EmptyValueDumper: ValueDumper = { _, _ -> }

typealias Dumper = (out: StringBuilder, value: Any?) -> StringBuilder

@JvmOverloads
fun treeDumper(compact: Boolean, valueDumper: ValueDumper = EmptyValueDumper): Dumper =
    dumper(compact, false, emptySet(), valueDumper)

@JvmOverloads
fun graphDumper(
    compact: Boolean,
    concreteValueClasses: Set<Class<*>> = emptySet(),
    valueDumper: ValueDumper = EmptyValueDumper
): Dumper =
    dumper(compact, true, concreteValueClasses, valueDumper)

fun Dumper.dump(value: Any?): StringBuilder = this(StringBuilder(256), value)

fun Dumper.toString(value: Any?): String = this.dump(value).toString()

private val PrimitiveWrapperClasses = setOf(
    Boolean::class::javaObjectType.get(),
    Byte::class::javaObjectType.get(),
    Short::class::javaObjectType.get(),
    Int::class::javaObjectType.get(),
    Long::class::javaObjectType.get(),
    Float::class::javaObjectType.get(),
    Double::class::javaObjectType.get()
)

private fun dumper(compact: Boolean, graph: Boolean, concreteValueClasses: Set<Class<*>>, valueDumper: ValueDumper): Dumper {
    val class2fields = ConcurrentHashMap<Class<*>, List<Field>>()
    return { out, value ->
        var tabs = 0
        val alreadyDumpedObjects = if (graph) IdentityHashMap<Any, Int>(16) else null
        fun appendTabs() {
            for (t in tabs downTo 1) out.append("    ")
        }

        fun appendLine() {
            out.append('\n')
        }

        fun inc(s: CharSequence) {
            out.append(s)
            appendLine()
            tabs++
        }

        fun dec(s: CharSequence) {
            tabs--
            appendTabs()
            out.append(s)
        }

        fun append(s: CharSequence) {
            out.append(s)
        }

        fun append(c: Char) {
            out.append(c)
        }

        fun dump(value: Any?) {
            fun dumpArray(array: Any) {
                val length = Array.getLength(array)
                if (compact) {
                    append('[')
                    var first = true
                    for (i in 0 until length) {
                        if (!first) append(',')
                        first = false
                        dump(Array.get(array, i))
                    }
                    append(']')
                } else {
                    inc("[")
                    for (i in 0 until length) {
                        appendTabs()
                        dump(Array.get(array, i))
                        appendLine()
                    }
                    dec("]")
                }
            }

            fun dumpCollection(collection: Collection<*>) {
                if (compact) {
                    append('[')
                    var first = true
                    for (e in collection) {
                        if (!first) append(',')
                        first = false
                        dump(e)
                    }
                    append(']')
                } else {
                    inc("[")
                    for (e in collection) {
                        appendTabs()
                        dump(e)
                        appendLine()
                    }
                    dec("]")
                }
            }

            fun dumpMap(map: Map<*, *>) {
                if (compact) {
                    append('{')
                    var first = true
                    for ((k, v) in map) {
                        if (!first) append(',')
                        first = false
                        dump(k)
                        append('>')
                        dump(v)
                    }
                    append('}')
                } else {
                    inc("{")
                    for ((k, v) in map) {
                        appendTabs()
                        dump(k)
                        append(" -> ")
                        dump(v)
                        appendLine()
                    }
                    dec("}")
                }
            }

            fun dumpClass(type: Class<*>, obj: Any) {
                val checkDumped = graph && !concreteValueClasses.contains(type)
                val index: Int
                if (checkDumped) {
                    val reference = alreadyDumpedObjects!![obj]
                    if (reference != null) {
                        append("#$reference")
                        return
                    }
                    index = alreadyDumpedObjects.size
                    alreadyDumpedObjects[obj] = index
                } else
                    index = 0
                val oldLength = out.length
                valueDumper(out, obj)
                if (oldLength == out.length) {
                    val fields = class2fields.computeIfAbsent(type) { allFields(it) }
                    fun dumpFields() {
                        var first = true
                        for (field in fields) {
                            val f = field.get(obj)
                            if (f != null) {
                                if (compact) {
                                    if (!first) append(',')
                                    first = false
                                    append("${field.name}=")
                                    dump(f)
                                } else {
                                    appendTabs()
                                    append("${field.name} = ")
                                    dump(f)
                                    appendLine()
                                }
                            }
                        }
                    }
                    if (compact) {
                        append("${type.simpleName}(")
                        dumpFields()
                        append(')')
                    } else {
                        inc("${type.simpleName}(")
                        dumpFields()
                        dec(")")
                    }
                }
                if (checkDumped) append("#$index")
            }
            when (value) {
                null -> append("null")
                is CharSequence -> append("\"$value\"")
                is Collection<*> -> dumpCollection(value)
                is Map<*, *> -> dumpMap(value)
                else -> {
                    val type = value.javaClass
                    when {
                        type.isEnum || PrimitiveWrapperClasses.contains(type) -> out.append(value)
                        type.isArray -> dumpArray(value)
                        type === Character::class.java -> append("'$value'")
                        else -> dumpClass(type, value)
                    }
                }
            }
        }
        dump(value)
        out
    }
}
