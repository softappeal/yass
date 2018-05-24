@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.remote.session

import ch.softappeal.yass.remote.Message

const val EndRequestNumber = 0

fun isEndPacket(requestNumber: Int) = requestNumber == EndRequestNumber

val EndPacket = Packet()

class Packet {
    private val requestNumber: Int
    private val message: Message?
    fun isEnd() = isEndPacket(requestNumber)

    constructor(requestNumber: Int, message: Message) {
        require(!isEndPacket(requestNumber))
        this.requestNumber = requestNumber
        this.message = message
    }

    internal constructor() {
        requestNumber = EndRequestNumber
        message = null
    }

    fun requestNumber(): Int {
        check(!isEnd())
        return requestNumber
    }

    fun message(): Message =
        message!!
}
