package ch.softappeal.yass.remote.session.test;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.remote.session.Connection;
import ch.softappeal.yass.remote.session.Packet;
import ch.softappeal.yass.remote.session.Session;
import ch.softappeal.yass.remote.session.SessionFactory;

public final class LocalConnection implements Connection {

    private Session session;

    private LocalConnection() {
        // disable
    }

    @Override public void write(final Packet packet) throws Exception {
        Session.received(session, packet);
    }

    @Override public void closed() {
        if (session != null) {
            session.close();
        }
    }

    public static void connect(final SessionFactory sessionFactory1, final SessionFactory sessionFactory2) {
        final var connection1 = new LocalConnection();
        final var connection2 = new LocalConnection();
        try {
            connection2.session = Session.create(sessionFactory1, connection1);
            try {
                connection1.session = Session.create(sessionFactory2, connection2);
            } catch (final Exception e) {
                Session.closeThrow(connection2.session, e);
            }
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

}
