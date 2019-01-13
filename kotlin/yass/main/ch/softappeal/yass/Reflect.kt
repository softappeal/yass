package ch.softappeal.yass

import sun.misc.Unsafe
import java.lang.reflect.*

val Class<*>.ownFields: Sequence<Field>
    get() = declaredFields.asSequence()
        .filter { field ->
            val modifiers = field.modifiers
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) false else {
                if (!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers)) field.isAccessible = true
                true
            }
        }
        .sortedBy(Field::getName)

val Class<*>.allFields: List<Field>
    get() {
        val fields = mutableListOf<Field>()
        var type: Class<*>? = this
        while (type != null && type !== Throwable::class.java) {
            fields.addAll(type.ownFields)
            type = type.superclass
        }
        return fields
    }

private val Unsafe = {
    val field = Unsafe::class.java.getDeclaredField("theUnsafe")
    field.isAccessible = true
    field.get(null) as Unsafe
}()

val AllocatorFactory: (type: Class<*>) -> (() -> Any) =
    { type -> { Unsafe.allocateInstance(type) } }
