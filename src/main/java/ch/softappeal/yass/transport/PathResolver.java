package ch.softappeal.yass.transport;

import ch.softappeal.yass.util.Check;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows having different contracts (and multiple versions of the same contract) on one listener.
 */
public final class PathResolver {

    private final Map<Object, TransportSetup> pathMappings = new HashMap<>(16);

    public PathResolver(final Map<?, TransportSetup> pathMappings) {
        pathMappings.forEach((path, setup) -> this.pathMappings.put(Check.notNull(path), Check.notNull(setup)));
    }

    public PathResolver(final Object path, final TransportSetup setup) {
        pathMappings.put(Check.notNull(path), Check.notNull(setup));
    }

    public TransportSetup resolvePath(final Object path) {
        final TransportSetup setup = pathMappings.get(Check.notNull(path));
        if (setup == null) {
            throw new RuntimeException("no mapping for path '" + path + '\'');
        }
        return setup;
    }

}
