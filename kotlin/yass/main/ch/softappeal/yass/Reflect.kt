@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass

import sun.misc.Unsafe
import java.lang.reflect.Field
import java.lang.reflect.Modifier

fun ownFields(type: Class<*>): List<Field> {
    val fields = mutableListOf<Field>()
    for (field in type.declaredFields) {
        val modifiers = field.modifiers
        if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
            fields.add(field)
            if (!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers)) field.isAccessible = true
        }
    }
    fields.sortBy { it.name }
    return fields
}

fun allFields(type: Class<*>): List<Field> {
    val fields = mutableListOf<Field>()
    var t: Class<*>? = type
    while ((t != null) && (t != Throwable::class.java)) {
        fields.addAll(ownFields(t))
        t = t.superclass
    }
    return fields
}

private val UNSAFE = {
    val field = Unsafe::class.java.getDeclaredField("theUnsafe")
    field.isAccessible = true
    field.get(null) as Unsafe
}()

val Allocate: (type: Class<*>) -> (() -> Any) = { type -> { UNSAFE.allocateInstance(type) } }
