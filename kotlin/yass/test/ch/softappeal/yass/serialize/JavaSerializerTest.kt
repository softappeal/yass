package ch.softappeal.yass.serialize

import ch.softappeal.yass.remote.ExceptionReply
import ch.softappeal.yass.remote.Request
import ch.softappeal.yass.remote.ValueReply
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

fun <T : Any?> copy(serializer: Serializer, value: T): T {
    val buffer = ByteArrayOutputStream()
    val writer = writer(buffer)
    serializer.write(writer, value)
    writer.writeByte(123.toByte()) // write sentinel
    val reader = reader(ByteArrayInputStream(buffer.toByteArray()))
    @Suppress("UNCHECKED_CAST") val result = serializer.read(reader) as T
    assertTrue(reader.readByte() == 123.toByte()) // check sentinel
    return result
}

private fun <T : Any?> copy(value: T): T = copy(JavaSerializer, value)

class JavaSerializerTest {

    @Test
    fun nullValue() {
        assertNull(copy(null))
    }

    @Test
    fun request() {
        val serviceId = 123
        val methodId = 1147
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

}
