package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Exceptions;

public final class LocalConnection implements Connection {

    private Session session;

    private LocalConnection() {
        // disable
    }

    @Override public void write(final Packet packet) throws Exception {
        Session.received(session, packet);
    }

    @Override public void closed() {
        session.close();
    }

    /**
     * Creates two connected local sessions.
     */
    public static void connect(final SessionFactory sessionFactory1, final SessionFactory sessionFactory2) {
        final LocalConnection connection1 = new LocalConnection();
        final LocalConnection connection2 = new LocalConnection();
        try {
            connection2.session = Session.create(sessionFactory1, connection1);
            try {
                connection1.session = Session.create(sessionFactory2, connection2);
            } catch (final Exception e) {
                Session.close(connection2.session, e);
                throw e;
            }
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

}
