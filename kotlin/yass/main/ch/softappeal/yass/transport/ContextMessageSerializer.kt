@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.Message
import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Serializer
import ch.softappeal.yass.serialize.Writer

class ContextMessageSerializer(private val contextSerializer: Serializer, private val messageSerializer: Serializer) : Serializer {
    private val context_ = ThreadLocal<Any?>()

    var context: Any?
        get() = context_.get()
        set(value) = context_.set(value)

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
