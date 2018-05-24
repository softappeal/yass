package ch.softappeal.yass

import java.io.BufferedReader
import java.io.CharArrayReader
import java.io.CharArrayWriter
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

fun compareFile(file: String, printer: (writer: PrintWriter) -> Unit) {
    val writer = PrintWriter(System.out)
    printer(writer)
    writer.flush()
    val buffer = CharArrayWriter()
    PrintWriter(buffer).use { printer(it) }
    val testReader = BufferedReader(CharArrayReader(buffer.toCharArray()))
    BufferedReader(InputStreamReader(FileInputStream("test/$file"), StandardCharsets.UTF_8)).use { refReader ->
        while (true) {
            val testLine = testReader.readLine()
            val refLine = refReader.readLine()
            if ((testLine == null) && (refLine == null)) return
            check((testLine != null) && (refLine != null)) { "files don't have same length" }
            assertEquals(testLine, refLine)
        }
    }
}
