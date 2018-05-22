@file:JvmName("Kt")
@file:JvmMultifileClass

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
fun treeDumper(compact: Boolean, valueDumper: ValueDumper = EmptyValueDumper) =
    dumper(compact, false, emptySet(), valueDumper)

@JvmOverloads
fun graphDumper(compact: Boolean, concreteValueClasses: Set<Class<*>> = emptySet(), valueDumper: ValueDumper = EmptyValueDumper) =
    dumper(compact, true, concreteValueClasses, valueDumper)

fun Dumper.dump(value: Any?): StringBuilder = this(StringBuilder(256), value)

fun Dumper.println(value: Any?) {
    System.out.println(this.dump(value))
}

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

        fun inc(s: String) {
            out.append(s)
            appendLine()
            tabs++
        }

        fun dec(s: String) {
            tabs--
            appendTabs()
            out.append(s)
        }

        fun dump(value: Any?) {
            fun dumpArray(array: Any) {
                val length = Array.getLength(array)
                if (compact) {
                    out.append("[ ")
                    for (i in 0 until length) {
                        dump(Array.get(array, i))
                        out.append(' ')
                    }
                    out.append(']')
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
                    out.append("[ ")
                    for (e in collection) {
                        dump(e)
                        out.append(' ')
                    }
                    out.append(']')
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
                    out.append("{ ")
                    for ((k, v) in map) {
                        dump(k)
                        out.append("->")
                        dump(v)
                        out.append(' ')
                    }
                    out.append('}')
                } else {
                    inc("{")
                    for ((k, v) in map) {
                        appendTabs()
                        dump(k)
                        out.append(" -> ")
                        dump(v)
                        appendLine()
                    }
                    dec("}")
                }
            }

            fun dumpClassFields(fields: List<Field>, value: Any) {
                for (field in fields) {
                    val f = field.get(value)
                    if (f != null) {
                        if (compact) {
                            out.append(field.name).append('=')
                            dump(f)
                            out.append(' ')
                        } else {
                            appendTabs()
                            out.append(field.name).append(" = ")
                            dump(f)
                            appendLine()
                        }
                    }
                }
            }

            fun dumpClass(type: Class<*>, value: Any) {
                val g = graph && !concreteValueClasses.contains(type)
                val index: Int
                if (g) {
                    val reference = alreadyDumpedObjects!![value]
                    if (reference != null) {
                        out.append('#').append(reference)
                        return
                    }
                    index = alreadyDumpedObjects.size
                    alreadyDumpedObjects[value] = index
                } else index = 0
                val oldLength = out.length
                valueDumper(out, value)
                if (oldLength == out.length) {
                    val fields = class2fields.computeIfAbsent(type) { allFields(it) }
                    if (compact) {
                        out.append(type.simpleName).append("( ")
                        dumpClassFields(fields, value)
                        out.append(')')
                    } else {
                        inc(type.simpleName + '(')
                        dumpClassFields(fields, value)
                        dec(")")
                    }
                }
                if (g) out.append('#').append(index)
            }
            when (value) {
                null -> out.append("null")
                is CharSequence -> out.append('"').append(value).append('"')
                is Collection<*> -> dumpCollection(value)
                is Map<*, *> -> dumpMap(value)
                else -> {
                    val type = value.javaClass
                    when {
                        type.isEnum || PrimitiveWrapperClasses.contains(type) -> out.append(value)
                        type.isArray -> dumpArray(value)
                        type === Character::class.java -> out.append('\'').append(value).append('\'')
                        else -> dumpClass(type, value)
                    }
                }
            }
        }
        dump(value)
        out
    }
}
