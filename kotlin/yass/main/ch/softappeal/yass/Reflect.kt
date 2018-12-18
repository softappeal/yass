package ch.softappeal.yass

import sun.misc.Unsafe
import java.lang.reflect.*

fun ownFields(type: Class<*>): Sequence<Field> = type.declaredFields.asSequence()
    .filter { field ->
        val modifiers = field.modifiers
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) false else {
            if (!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers)) field.isAccessible = true
            true
        }
    }
    .sortedBy(Field::getName)

fun allFields(type: Class<*>): Sequence<Field> = emptySequence<Field>().apply {
    var t: Class<*>? = type
    while (t != null && t !== Throwable::class.java) {
        plus(ownFields(t))
        t = t.superclass
    }
}

private val Unsafe = {
    val field = Unsafe::class.java.getDeclaredField("theUnsafe")
    field.isAccessible = true
    field.get(null) as Unsafe
}()

val AllocatorFactory: (type: Class<*>) -> (() -> Any) =
    { type -> { Unsafe.allocateInstance(type) } }
