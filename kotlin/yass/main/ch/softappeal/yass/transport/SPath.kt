package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.*

const val SIntPathSerializerDefaultPath = 0

val SIntPathSerializer = object : SSerializer {
    override suspend fun read(reader: SReader) = reader.readInt()
    override suspend fun write(writer: SWriter, value: Any?) = writer.writeInt(value as Int)
}

class SClientSetup(
    private val messageSerializer: SSerializer,
    private val pathSerializer: SSerializer,
    private val path: Any
) {
    constructor(messageSerializer: SSerializer) : this(
        messageSerializer,
        SIntPathSerializer,
        SIntPathSerializerDefaultPath
    )

    suspend fun read(reader: SReader) = messageSerializer.read(reader) as Reply
    suspend fun write(writer: SWriter, request: Request) {
        pathSerializer.write(writer, path)
        messageSerializer.write(writer, request)
    }
}

class SServerTransport(private val server: SServer, private val messageSerializer: SSerializer) {
    fun invocation(request: Request): SServiceInvocation =
        server.invocation(request)

    suspend fun read(reader: SReader): Request = messageSerializer.read(reader) as Request
    suspend fun write(writer: SWriter, reply: Reply) = messageSerializer.write(writer, reply)
}

class SServerSetup(private val pathSerializer: SSerializer, private val pathMappings: Map<out Any, SServerTransport>) {
    constructor(server: SServer, messageSerializer: SSerializer) :
        this(SIntPathSerializer, mapOf(SIntPathSerializerDefaultPath to SServerTransport(server, messageSerializer)))

    suspend fun resolve(reader: SReader): SServerTransport {
        val path = pathSerializer.read(reader)
        return pathMappings[path] ?: error("invalid path '$path'")
    }
}
