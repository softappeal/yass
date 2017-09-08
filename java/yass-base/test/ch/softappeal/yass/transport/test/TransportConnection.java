package ch.softappeal.yass.transport.test;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Exceptions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class TransportConnection implements Connection {

    private Session session;
    private Serializer serializer;

    private TransportConnection() {
        // disable
    }

    @Override public void write(final Packet packet) throws Exception {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(128);
        serializer.write(packet, Writer.create(buffer));
        Session.received(session, (Packet)serializer.read(Reader.create(new ByteArrayInputStream(buffer.toByteArray()))));
    }

    @Override public void closed() {
        if (session != null) {
            session.close();
        }
    }

    public static void connect(final TransportSetup setup1, final TransportSetup setup2) {
        final TransportConnection connection1 = new TransportConnection();
        final TransportConnection connection2 = new TransportConnection();
        try {
            connection2.session = Session.create(setup1.sessionFactory, connection1);
            connection2.serializer = setup1.packetSerializer;
            try {
                connection1.session = Session.create(setup2.sessionFactory, connection2);
                connection1.serializer = setup2.packetSerializer;
            } catch (final Exception e) {
                Session.closeThrow(connection2.session, e);
            }
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

}
