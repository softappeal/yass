package ch.softappeal.yass

import org.junit.Test
import java.io.FileNotFoundException
import kotlin.test.assertEquals
import kotlin.test.fail

class InputStreamFactoryTest {
    @Test
    fun file() {
        inputStreamFactory("test/" + InputStreamFactoryTest::class.java.name.replace('.', '/') + ".kt")().use {}
    }

    @Test
    fun fileFailed() = try {
        inputStreamFactory("INVALID")()
        fail()
    } catch (e: FileNotFoundException) {
        println(e.message)
    }

    @Test
    fun classLoader() {
        inputStreamFactory(
            InputStreamFactoryTest::class.java.classLoader,
            InputStreamFactoryTest::class.java.name.replace('.', '/') + ".class"
        )().use {}
    }

    @Test
    fun classLoaderFailed() = try {
        inputStreamFactory(
            InputStreamFactoryTest::class.java.classLoader,
            "INVALID"
        )()
        fail()
    } catch (e: IllegalStateException) {
        assertEquals("resource 'INVALID' not found", e.message)
    }
}
