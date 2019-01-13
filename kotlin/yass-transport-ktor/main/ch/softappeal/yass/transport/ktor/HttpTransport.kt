package ch.softappeal.yass.transport.ktor

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.transport.*
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.*
import kotlin.coroutines.*

fun httpClient(messageSerializer: SSerializer, url: String, httpClientFactory: () -> HttpClient) = object : SClient() {
    override suspend fun invoke(request: Request, oneWay: Boolean): Reply? = httpClientFactory().use { client ->
        val result = client.call(url) {
            method = HttpMethod.Post
            body = object : WriteChannelContent() {
                override suspend fun writeTo(channel: ByteWriteChannel) {
                    messageSerializer.write(channel.writer(), request)
                }
            }
        }
        if (oneWay) return null
        return messageSerializer.read(result.response.content.reader()) as Reply
    }
}

class CallCCE(val call: ApplicationCall) : AbstractCoroutineContextElement(CallCCE) {
    companion object Key : CoroutineContext.Key<CallCCE>
}

fun Route.route(path: String, transport: SServerTransport) {
    route(path) {
        post {
            val reader = call.receiveChannel().reader()
            val invocation = transport.invocation(transport.read(reader))
            withContext(CallCCE(call)) {
                val reply = invocation.invoke()
                if (invocation.oneWay) return@withContext
                call.respond(object : WriteChannelContent() {
                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        transport.write(channel.writer(), reply)
                    }
                })
            }
        }
    }
}
