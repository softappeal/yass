package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows having different contracts (and multiple versions of the same contract) on one socket listener.
 */
public final class PathResolver {

  private final Map<Object, SocketTransport> pathMappings = new HashMap<>(16);

  public PathResolver(final Map<?, SocketTransport> pathMappings) {
    for (final Map.Entry<?, SocketTransport> pathMapping : pathMappings.entrySet()) {
      this.pathMappings.put(Check.notNull(pathMapping.getKey()), Check.notNull(pathMapping.getValue()));
    }
  }

  public PathResolver(final Object path, final SocketTransport transport) {
    pathMappings.put(Check.notNull(path), Check.notNull(transport));
  }

  public SocketTransport resolvePath(final Object path) throws Exception {
    final SocketTransport transport = pathMappings.get(Check.notNull(path));
    if (transport == null) {
      throw new RuntimeException("no mapping for path '" + path + '\'');
    }
    return transport;
  }

}
