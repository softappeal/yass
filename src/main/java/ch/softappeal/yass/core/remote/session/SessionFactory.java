package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Invoker;

public interface SessionFactory {

  /**
   * Creates a new session.
   * It's allowed to call {@link Invoker#proxy(Interceptor...)} during this method,
   * but the proxies can be used not before {@link Session#opened()} is called.
   * If this method throws an exception, the connection is rejected ({@link Session#closed(Exception)} won't be called).
   */
  Session create(SessionSetup setup, Connection connection) throws Exception;

}
