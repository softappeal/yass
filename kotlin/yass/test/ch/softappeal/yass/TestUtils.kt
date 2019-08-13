package ch.softappeal.yass

import java.io.*
import java.nio.charset.*
import kotlin.test.*

private fun compareFile(file: String, buffer: CharArrayWriter) {
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

fun compareFile(file: String, printer: (writer: PrintWriter) -> Unit) {
    val writer = PrintWriter(System.out)
    printer(writer)
    writer.flush()
    val buffer = CharArrayWriter()
    PrintWriter(buffer).use { printer(it) }
    compareFile(file, buffer)
}
