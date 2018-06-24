package ch.softappeal.yass.transport

import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Serializer
import ch.softappeal.yass.serialize.Writer
import ch.softappeal.yass.serialize.reader
import org.junit.Test
import java.io.ByteArrayInputStream
import java.lang.IllegalStateException
import kotlin.test.assertEquals
import kotlin.test.fail

class PathTest {
    @Test
    fun missingMapping() {
        try {
            ServerSetup(
                object : Serializer {
                    override fun read(reader: Reader): Any? = null
                    override fun write(writer: Writer, value: Any?) {}
                },
                mapOf()
            ).resolve(reader(ByteArrayInputStream(byteArrayOf())))
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("invalid path 'null'", e.message)
        }
    }

    @Test
    fun missingMappingSession() {
        try {
            AcceptorSetup(
                object : Serializer {
                    override fun read(reader: Reader): Any? = null
                    override fun write(writer: Writer, value: Any?) {}
                },
                mapOf()
            ).resolve(reader(ByteArrayInputStream(byteArrayOf())))
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("invalid path 'null'", e.message)
        }
    }
}
