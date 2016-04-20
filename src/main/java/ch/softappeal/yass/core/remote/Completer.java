package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

public interface Completer {

    void complete(@Nullable Object result);

    void completeExceptionally(Exception exception);

}
