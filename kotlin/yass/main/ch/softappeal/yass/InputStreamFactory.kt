@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

typealias InputStreamFactory = () -> InputStream

fun inputStreamFactory(file: File): InputStreamFactory =
    { FileInputStream(file) }

fun inputStreamFactory(file: String): InputStreamFactory =
    inputStreamFactory(File(file))

fun inputStreamFactory(classLoader: ClassLoader, resource: String): InputStreamFactory =
    { classLoader.getResourceAsStream(resource) ?: error("resource '$resource' not found") }
