package ch.softappeal.yass.remote

sealed class Message

class Request(val serviceId: Int, val methodId: Int, val arguments: List<Any?>) : Message()

abstract class Reply : Message() {
    internal abstract fun process(): Any?
}

class ValueReply(val value: Any?) : Reply() {
    override fun process() = value
}

class ExceptionReply(val exception: Exception) : Reply() {
    override fun process() = throw exception
}
