package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.*
import kotlin.coroutines.*

class ContextCCE : AbstractCoroutineContextElement(ContextCCE) {
    var context: Any? = null

    companion object Key : CoroutineContext.Key<ContextCCE>
}

suspend fun contextCCE(): ContextCCE = coroutineContext[ContextCCE]!!

class SContextMessageSerializer(
    private val contextSerializer: SSerializer, private val messageSerializer: SSerializer
) : SSerializer {
    override suspend fun read(reader: SReader): Message {
        contextCCE().context = contextSerializer.read(reader)
        return messageSerializer.read(reader) as Message
    }

    /** Sets context to null. */
    override suspend fun write(writer: SWriter, value: Any?) {
        try {
            contextSerializer.write(writer, contextCCE().context)
        } finally {
            contextCCE().context = null
        }
        messageSerializer.write(writer, value)
    }
}
