package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.session.*
import ch.softappeal.yass.serialize.*

class SessionTransport(private val packetSerializer: Serializer, val sessionFactory: SessionFactory) {
    fun read(reader: Reader) = packetSerializer.read(reader) as Packet
    fun write(writer: Writer, packet: Packet) = packetSerializer.write(writer, packet)
}

class InitiatorSetup(
    val transport: SessionTransport,
    private val pathSerializer: Serializer,
    private val path: Any
) {
    constructor(packetSerializer: Serializer, sessionFactory: SessionFactory) :
        this(SessionTransport(packetSerializer, sessionFactory), IntPathSerializer, IntPathSerializerDefaultPath)

    fun writePath(writer: Writer) = pathSerializer.write(writer, path)
}

class AcceptorSetup(private val pathSerializer: Serializer, private val pathMappings: Map<out Any, SessionTransport>) {
    constructor(packetSerializer: Serializer, sessionFactory: SessionFactory) : this(
        IntPathSerializer,
        mapOf(IntPathSerializerDefaultPath to SessionTransport(packetSerializer, sessionFactory))
    )

    fun resolve(reader: Reader): SessionTransport {
        val path = pathSerializer.read(reader)
        return pathMappings[path] ?: error("invalid path '$path'")
    }
}
