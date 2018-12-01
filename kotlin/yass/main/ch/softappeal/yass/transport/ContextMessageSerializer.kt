package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.*

class ContextMessageSerializer(
    private val contextSerializer: Serializer, private val messageSerializer: Serializer
) : Serializer {
    private val _context = ThreadLocal<Any?>()

    var context: Any?
        get() = _context.get()
        set(value) = _context.set(value)

    override fun read(reader: Reader): Message {
        context = contextSerializer.read(reader)
        return messageSerializer.read(reader) as Message
    }

    /** Sets context to null. */
    override fun write(writer: Writer, value: Any?) {
        try {
            contextSerializer.write(writer, context)
        } finally {
            context = null
        }
        messageSerializer.write(writer, value)
    }
}
