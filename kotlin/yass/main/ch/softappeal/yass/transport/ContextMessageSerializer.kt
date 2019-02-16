package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.*

private val context = ThreadLocal<Any?>()

var cmsContext: Any?
    get() = context.get()
    set(value) = context.set(value)

class ContextMessageSerializer(
    private val contextSerializer: Serializer, private val messageSerializer: Serializer
) : Serializer {
    override fun read(reader: Reader): Message {
        cmsContext = contextSerializer.read(reader)
        return messageSerializer.read(reader) as Message
    }

    /** Sets context to null. */
    override fun write(writer: Writer, value: Any?) {
        try {
            contextSerializer.write(writer, cmsContext)
        } finally {
            cmsContext = null
        }
        messageSerializer.write(writer, value)
    }
}
