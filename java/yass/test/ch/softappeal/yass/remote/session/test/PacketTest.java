package ch.softappeal.yass.remote.session.test;

import ch.softappeal.yass.remote.ValueReply;
import ch.softappeal.yass.remote.session.Packet;
import ch.softappeal.yass.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

public class PacketTest extends InvokeTest {

    @Test public void end() {
        Assert.assertTrue(Packet.END.isEnd());
        Assert.assertTrue(Packet.isEnd(Packet.END_REQUEST_NUMBER));
        Assert.assertFalse(Packet.isEnd(1));
        try {
            Packet.END.message();
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertEquals("not allowed if isEnd", e.getMessage());
        }
        try {
            Packet.END.requestNumber();
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertEquals("not allowed if isEnd", e.getMessage());
        }
    }

    @Test public void normal() {
        final var message = new ValueReply(null);
        final var packet = new Packet(123, message);
        Assert.assertTrue(packet.requestNumber() == 123);
        Assert.assertSame(message, packet.message());
        Assert.assertFalse(packet.isEnd());
        try {
            new Packet(123, null);
            Assert.fail();
        } catch (final NullPointerException ignore) {
            // empty
        }
        try {
            new Packet(Packet.END_REQUEST_NUMBER, message);
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertEquals("use END", e.getMessage());
        }
    }

}
