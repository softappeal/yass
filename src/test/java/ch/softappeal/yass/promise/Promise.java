package ch.softappeal.yass.promise;

import ch.softappeal.yass.util.Check;

import java.util.concurrent.CompletionStage;

public abstract class Promise<C> {

    @FunctionalInterface public interface Execute<R> {
        R execute() throws Exception;
    }

    public static <T> CompletionStage<T> create(final Execute<T> execute) {
        return null;
    }

    @FunctionalInterface public interface VoidExecute {
        void execute() throws Exception;
    }

    public static CompletionStage<Void> create(final VoidExecute execute) {
        return null;
    }

    protected final C proxy;

    protected Promise(final C proxy) {
        this.proxy = Check.notNull(proxy);
    }

}
