package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.util.Check;

public class SessionSetup {

    public final Server server;
    public final Dispatcher dispatcher;
    public final SessionFactory sessionFactory;

    public SessionSetup(final Server server, final Dispatcher dispatcher, final SessionFactory sessionFactory) {
        this.server = Check.notNull(server);
        this.dispatcher = Check.notNull(dispatcher);
        this.sessionFactory = Check.notNull(sessionFactory);
    }

}
