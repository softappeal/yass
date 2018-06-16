@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.Message
import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Serializer
import ch.softappeal.yass.serialize.Writer

class ContextMessageSerializer internal constructor(
    private val contextSerializer: Serializer, private val messageSerializer: Serializer,
    private val read: Boolean, private val write: Boolean
) : Serializer {
    private val threadLocal = ThreadLocal<Any?>()

    var context: Any?
        get() = threadLocal.get()
        set(value) = threadLocal.set(value)

    override fun read(reader: Reader): Message {
        if (read)
            context = contextSerializer.read(reader)
        return messageSerializer.read(reader) as Message
    }

    override fun write(writer: Writer, value: Any?) {
        if (write) {
            try {
                contextSerializer.write(writer, context)
            } finally {
                context = null
            }
        }
        messageSerializer.write(writer, value)
    }
}

fun readContextMessageSerializer(contextSerializer: Serializer, messageSerializer: Serializer) =
    ContextMessageSerializer(contextSerializer, messageSerializer, true, false)

fun writeContextMessageSerializer(contextSerializer: Serializer, messageSerializer: Serializer) =
    ContextMessageSerializer(contextSerializer, messageSerializer, false, true)

fun readWriteContextMessageSerializer(contextSerializer: Serializer, messageSerializer: Serializer) =
    ContextMessageSerializer(contextSerializer, messageSerializer, true, true)
