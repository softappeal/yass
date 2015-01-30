package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.ProxyFactory;

public interface SessionProxyFactory extends AutoCloseable, ProxyFactory {

    @Override void close();

}
