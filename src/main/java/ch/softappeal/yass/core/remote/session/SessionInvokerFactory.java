package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.InvokerFactory;

public interface SessionInvokerFactory extends AutoCloseable, InvokerFactory {

    @Override void close();

}
