package ch.softappeal.yass

import java.lang.reflect.*

@MustBeDocumented
@Retention
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
annotation class Tag(val value: Int)

fun tag(element: AnnotatedElement): Int =
    (element.getAnnotation(Tag::class.java) ?: error("missing tag for '$element'")).value
