package ch.softappeal.yass.remote

import java.io.Serializable

sealed class Message : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

class Request(val serviceId: Int, val methodId: Int, val arguments: List<Any?>) : Message() {
    companion object {
        private const val serialVersionUID = 1L
    }
}

abstract class Reply : Message() {
    internal abstract fun process(): Any?

    companion object {
        private const val serialVersionUID = 1L
    }
}

class ValueReply(val value: Any?) : Reply() {
    override fun process() = value

    companion object {
        private const val serialVersionUID = 1L
    }
}

class ExceptionReply(val exception: Exception) : Reply() {
    override fun process() = throw exception

    companion object {
        private const val serialVersionUID = 1L
    }
}
