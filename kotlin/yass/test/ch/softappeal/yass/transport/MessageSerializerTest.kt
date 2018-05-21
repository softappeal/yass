package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.ExceptionReply
import ch.softappeal.yass.remote.Message
import ch.softappeal.yass.remote.Request
import ch.softappeal.yass.remote.ValueReply
import ch.softappeal.yass.serialize.JavaSerializer
import ch.softappeal.yass.serialize.copy
import ch.softappeal.yass.serialize.reader
import ch.softappeal.yass.serialize.writer
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

private val SERIALIZER = messageSerializer(JavaSerializer)

private fun <T : Message?> copy(value: T): T = copy(SERIALIZER, value)

class MessageSerializerTest {

    @Test
    fun request() {
        val serviceId = 123456
        val methodId = 4711
        val request = copy(Request(serviceId, methodId, listOf()))
        assertEquals(serviceId, request.serviceId)
        assertEquals(methodId, request.methodId)
        assertTrue(request.arguments.isEmpty())
    }

    @Test
    fun value() {
        val value = "xyz"
        val reply = copy(ValueReply(value))
        assertEquals(value, reply.value)
    }

    @Test
    fun exception() {
        val reply = copy(ExceptionReply(EOFException()))
        assertTrue(reply.exception is EOFException)
    }

    @Test
    fun write1() = try {
        SERIALIZER.write(writer(ByteArrayOutputStream()), null)
        fail()
    } catch (e: IllegalStateException) {
        assertEquals("unexpected value 'null'", e.message)
    }

    @Test
    fun wite2() = try {
        SERIALIZER.write(writer(ByteArrayOutputStream()), "error")
        fail()
    } catch (e: IllegalStateException) {
        assertEquals("unexpected value 'error'", e.message)
    }

    @Test
    fun read() = try {
        SERIALIZER.read(reader(ByteArrayInputStream(byteArrayOf(123))))
        fail()
    } catch (e: IllegalStateException) {
        assertEquals("unexpected type 123", e.message)
    }

}
