package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.session.*
import ch.softappeal.yass.serialize.*

class SSessionTransport(private val packetSerializer: SSerializer, val sessionFactory: SSessionFactory) {
    suspend fun read(reader: SReader) = packetSerializer.read(reader) as Packet
    suspend fun write(writer: SWriter, packet: Packet) = packetSerializer.write(writer, packet)
}

class SInitiatorSetup(
    val transport: SSessionTransport,
    private val pathSerializer: SSerializer,
    private val path: Any
) {
    constructor(packetSerializer: SSerializer, sessionFactory: SSessionFactory) :
        this(SSessionTransport(packetSerializer, sessionFactory), SIntPathSerializer, SIntPathSerializerDefaultPath)

    suspend fun writePath(writer: SWriter) = pathSerializer.write(writer, path)
}

class SAcceptorSetup(
    private val pathSerializer: SSerializer,
    private val pathMappings: Map<out Any, SSessionTransport>
) {
    constructor(packetSerializer: SSerializer, sessionFactory: SSessionFactory) : this(
        SIntPathSerializer,
        mapOf(SIntPathSerializerDefaultPath to SSessionTransport(packetSerializer, sessionFactory))
    )

    suspend fun resolve(reader: SReader): SSessionTransport {
        val path = pathSerializer.read(reader)
        return pathMappings[path] ?: error("invalid path '$path'")
    }
}
