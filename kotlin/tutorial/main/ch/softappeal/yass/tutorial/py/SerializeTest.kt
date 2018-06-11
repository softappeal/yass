package ch.softappeal.yass.tutorial.py

import ch.softappeal.yass.serialize.writer
import java.io.ByteArrayOutputStream
import java.util.Arrays

fun main(args: Array<String>) {
    val out = ByteArrayOutputStream()
    Serializer.write(writer(out), createObjects())
    val bytes = Arrays.toString(out.toByteArray())
    println(bytes)
    if ("[2, 15, 0, 3, 0, 3, 1, 7, -128, -119, 15, 7, -117, -56, 120, 4, 84, 79, 126, -85, -80, 59, 20, -52, 5, 5, 72, 101, 108, 108, 111, 5, 20, 62, 1, 18, 127, -62, -128, -56, -76, -33, -65, -32, -96, -128, -28, -116, -95, -17, -65, -65, 60, 6, 5, 0, 127, -1, 10, -45, 8, -62, 31, 22, 58, 9, 1, 9, 0, 11, 1, 1, 2, -10, 1, 3, 4, 89, 65, 83, 83, 0, 14, 1, 3, 7, 2, 7, 4, 7, 6, 0, 15, 1, 63, -16, 0, 0, 0, 0, 0, 0, 2, 2, 1, 0, 15, 1, 64, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0]" != bytes) {
        throw Exception()
    }
}
