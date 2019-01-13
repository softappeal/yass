package ch.softappeal.yass

import kotlinx.coroutines.*
import java.io.*
import java.nio.charset.*
import kotlin.reflect.*
import kotlin.test.*

suspend fun sAssertFails(block: suspend () -> Unit): Exception {
    try {
        block()
    } catch (e: Exception) {
        return e
    }
    fail()
}

@PublishedApi
internal suspend fun <T : Exception> sAssertFailsWith(exceptionClass: KClass<T>, block: suspend () -> Unit): T {
    try {
        block()
    } catch (e: Exception) {
        if (exceptionClass.java.isInstance(e)) {
            @Suppress("UNCHECKED_CAST") return e as T
        }
        fail()
    }
    fail()
}

suspend inline fun <reified T : Exception> sAssertFailsWith(noinline block: suspend () -> Unit): T =
    sAssertFailsWith(T::class, block)

class TestUtilsTest {
    @Test
    fun sAssertFailsFailed() = runBlocking {
        try {
            sAssertFails {}
        } catch (ignore: Throwable) {
            return@runBlocking
        }
        fail()
    }

    @Test
    fun sAssertFails() = runBlocking {
        assertEquals("test", sAssertFails { throw Exception("test") }.message)
    }

    @Test
    fun sAssertFailsWithFailed() = runBlocking {
        try {
            sAssertFailsWith<RuntimeException> {}
        } catch (ignore: Throwable) {
            return@runBlocking
        }
        fail()
    }

    @Test
    fun sAssertFailsWithFailed2() = runBlocking {
        try {
            sAssertFailsWith<RuntimeException> { throw Exception() }
        } catch (ignore: Throwable) {
            return@runBlocking
        }
        fail()
    }

    @Test
    fun sAssertFailsWith() = runBlocking {
        assertEquals("test", sAssertFailsWith<Exception> { throw RuntimeException("test") }.message)
    }
}

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

suspend fun sCompareFile(file: String, printer: suspend (writer: PrintWriter) -> Unit) {
    val writer = PrintWriter(System.out)
    printer(writer)
    writer.flush()
    val buffer = CharArrayWriter()
    PrintWriter(buffer).use { printer(it) }
    compareFile(file, buffer)
}
