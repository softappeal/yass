package ch.softappeal.yass.remote;

import ch.softappeal.yass.Nullable;

public abstract class Completer {

    public abstract void complete(@Nullable Object result);

    /**
     * Calls {@link #complete(Object) complete(null)}
     */
    public final void complete() {
        complete(null);
    }

    public abstract void completeExceptionally(Exception exception);

}
