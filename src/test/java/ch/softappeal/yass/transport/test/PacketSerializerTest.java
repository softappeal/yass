package ch.softappeal.yass.transport.test;

import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.ValueReply;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.test.JavaSerializerTest;
import ch.softappeal.yass.transport.PacketSerializer;
import org.junit.Assert;
import org.junit.Test;

public class PacketSerializerTest {

    public static final Serializer SERIALIZER = new PacketSerializer(MessageSerializerTest.SERIALIZER);

    private static Packet copy(final Packet value) throws Exception {
        return JavaSerializerTest.copy(SERIALIZER, value);
    }

    @Test public void end() throws Exception {
        Assert.assertTrue(copy(Packet.END).isEnd());
    }

    @Test public void normal() throws Exception {
        final Message message = new ValueReply(null);
        final Packet packet = copy(new Packet(123, message));
        Assert.assertTrue(packet.requestNumber() == 123);
        Assert.assertNull(((ValueReply)packet.message()).value);
    }

}
