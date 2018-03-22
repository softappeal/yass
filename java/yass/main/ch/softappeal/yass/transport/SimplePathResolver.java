package ch.softappeal.yass.transport;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Allows having different contracts (and multiple versions of the same contract) on one listener.
 */
public final class SimplePathResolver {

    private final Map<Object, SimpleTransportSetup> pathMappings = new HashMap<>(16);
    private void put(final Object path, final SimpleTransportSetup setup) {
        pathMappings.put(Objects.requireNonNull(path), Objects.requireNonNull(setup));
    }

    public SimplePathResolver(final Map<?, SimpleTransportSetup> pathMappings) {
        pathMappings.forEach(this::put);
    }

    public SimplePathResolver(final Object path, final SimpleTransportSetup setup) {
        put(path, setup);
    }

    public SimpleTransportSetup resolvePath(final Object path) {
        return Optional.ofNullable(pathMappings.get(Objects.requireNonNull(path)))
            .orElseThrow(() -> new RuntimeException("no mapping for path '" + path + '\''));
    }

}
