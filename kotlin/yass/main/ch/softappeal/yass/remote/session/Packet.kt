package ch.softappeal.yass.remote.session

import ch.softappeal.yass.remote.Message

const val EndRequestNumber = 0

fun isEndPacket(requestNumber: Int): Boolean =
    requestNumber == EndRequestNumber

val EndPacket = Packet()

class Packet {
    private val _requestNumber: Int
    private val _message: Message?

    val isEnd: Boolean
        get() = isEndPacket(_requestNumber)

    val requestNumber: Int
        get() {
            check(!isEnd)
            return _requestNumber
        }

    val message: Message
        get() = _message!!

    constructor(requestNumber: Int, message: Message) {
        require(!isEndPacket(requestNumber))
        _requestNumber = requestNumber
        _message = message
    }

    internal constructor() {
        _requestNumber = EndRequestNumber
        _message = null
    }
}
