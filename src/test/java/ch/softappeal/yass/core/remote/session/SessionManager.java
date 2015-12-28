package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Check;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager<S extends Session> {

    private final Set<S> modifiableSessions = Collections.newSetFromMap(new ConcurrentHashMap<>(16));
    public final Set<S> sessions = Collections.unmodifiableSet(modifiableSessions);

    /**
     * Call in constructor of {@link Session}.
     */
    public final void add(final S session) {
        if (!modifiableSessions.add(Check.notNull(session))) {
            throw new RuntimeException("session already added");
        }
    }

    /**
     * Call in {@link Session#closed(boolean)}.
     */
    public final void remove(final S session) {
        if (!modifiableSessions.remove(Check.notNull(session))) {
            throw new RuntimeException("session already removed");
        }
    }

}
