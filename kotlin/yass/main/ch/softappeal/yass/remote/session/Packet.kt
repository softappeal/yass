@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.remote.session

import ch.softappeal.yass.remote.Message
import java.io.Serializable

const val END_REQUEST_NUMBER = 0

fun isEndPacket(requestNumber: Int) = requestNumber == END_REQUEST_NUMBER

val END_PACKET = Packet()

class Packet : Serializable {
    private val requestNumber: Int
    private val message: Message?
    fun isEnd() = isEndPacket(requestNumber)

    constructor(requestNumber: Int, message: Message) {
        require(!isEndPacket(requestNumber))
        this.requestNumber = requestNumber
        this.message = message
    }

    internal constructor() {
        requestNumber = END_REQUEST_NUMBER
        message = null
    }

    fun requestNumber(): Int {
        check(!isEnd())
        return requestNumber
    }

    fun message(): Message = message!!

    companion object {
        private const val serialVersionUID = 1L
    }
}
