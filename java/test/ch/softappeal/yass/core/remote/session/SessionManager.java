package ch.softappeal.yass.core.remote.session;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager<S extends Session> {

    private final Set<S> modifiableSessions = ConcurrentHashMap.newKeySet(16);
    public final Set<S> sessions = Collections.unmodifiableSet(modifiableSessions);

    /**
     * Call in constructor of {@link Session}.
     */
    public final void add(final S session) {
        if (!modifiableSessions.add(Objects.requireNonNull(session))) {
            throw new RuntimeException("session already added");
        }
    }

    /**
     * Call in {@link Session#closed(Exception)}.
     */
    public final void remove(final S session) {
        if (!modifiableSessions.remove(Objects.requireNonNull(session))) {
            throw new RuntimeException("session already removed");
        }
    }

}
