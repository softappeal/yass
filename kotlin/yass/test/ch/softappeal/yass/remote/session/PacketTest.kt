package ch.softappeal.yass.remote.session

import ch.softappeal.yass.remote.ValueReply
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PacketTest {
    @Test
    fun end() {
        assertTrue(EndPacket.isEnd)
        assertTrue(isEndPacket(EndRequestNumber))
        assertFalse(isEndPacket(1))
        assertFailsWith<KotlinNullPointerException> { EndPacket.message }
        assertFailsWith<IllegalStateException> { EndPacket.requestNumber }
    }

    @Test
    fun normal() {
        val message = ValueReply(null)
        val packet = Packet(123, message)
        assertEquals(123, packet.requestNumber)
        assertSame(message, packet.message)
        assertFalse(packet.isEnd)
        assertFailsWith<IllegalArgumentException> { Packet(EndRequestNumber, message) }
    }
}
