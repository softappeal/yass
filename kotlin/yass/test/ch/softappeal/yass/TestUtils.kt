package ch.softappeal.yass

import java.io.*
import java.nio.charset.*
import kotlin.test.*

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
