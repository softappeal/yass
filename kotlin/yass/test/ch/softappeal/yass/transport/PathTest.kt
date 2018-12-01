package ch.softappeal.yass.transport

import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Writer
import java.io.*
import kotlin.test.*

class PathTest {
    @Test
    fun missingMapping() = assertEquals(
        "invalid path 'null'",
        assertFailsWith<IllegalStateException> {
            ServerSetup(
                object : Serializer {
                    override fun read(reader: Reader): Any? = null
                    override fun write(writer: Writer, value: Any?) {}
                },
                mapOf()
            ).resolve(reader(ByteArrayInputStream(byteArrayOf())))
        }.message
    )

    @Test
    fun missingMappingSession() = assertEquals(
        "invalid path 'null'",
        assertFailsWith<IllegalStateException> {
            AcceptorSetup(
                object : Serializer {
                    override fun read(reader: Reader): Any? = null
                    override fun write(writer: Writer, value: Any?) {}
                },
                mapOf()
            ).resolve(reader(ByteArrayInputStream(byteArrayOf())))
        }.message
    )
}
