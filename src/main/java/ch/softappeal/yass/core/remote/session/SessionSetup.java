package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;

public class SessionSetup {

    public final Server server;
    public final Executor requestExecutor;
    public final SessionFactory sessionFactory;

    public SessionSetup(final Server server, final Executor requestExecutor, final SessionFactory sessionFactory) {
        this.server = Check.notNull(server);
        this.requestExecutor = Check.notNull(requestExecutor);
        this.sessionFactory = Check.notNull(sessionFactory);
    }

}
