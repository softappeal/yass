package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.*

const val IntPathSerializerDefaultPath = 0

val IntPathSerializer = object : Serializer {
    override fun read(reader: Reader) = reader.readInt()
    override fun write(writer: Writer, value: Any?) = writer.writeInt(value as Int)
}

class ClientSetup(
    private val messageSerializer: Serializer,
    private val pathSerializer: Serializer,
    private val path: Any
) {
    constructor(messageSerializer: Serializer) : this(
        messageSerializer,
        IntPathSerializer,
        IntPathSerializerDefaultPath
    )

    fun read(reader: Reader) = messageSerializer.read(reader) as Reply
    fun write(writer: Writer, request: Request) {
        pathSerializer.write(writer, path)
        messageSerializer.write(writer, request)
    }
}

class ServerTransport(private val server: Server, private val messageSerializer: Serializer) {
    fun invocation(asyncSupported: Boolean, request: Request): ServerInvocation =
        server.invocation(asyncSupported, request)

    fun read(reader: Reader): Request = messageSerializer.read(reader) as Request
    fun write(writer: Writer, reply: Reply) = messageSerializer.write(writer, reply)
}

class ServerSetup(private val pathSerializer: Serializer, private val pathMappings: Map<out Any, ServerTransport>) {
    constructor(server: Server, messageSerializer: Serializer) :
        this(IntPathSerializer, mapOf(IntPathSerializerDefaultPath to ServerTransport(server, messageSerializer)))

    fun resolve(reader: Reader): ServerTransport {
        val path = pathSerializer.read(reader)
        return pathMappings[path] ?: error("invalid path '$path'")
    }
}
