package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.Socket;
import java.util.concurrent.Executor;

/**
 * Executor for socket connections.
 */
public final class SocketExecutor {

    public final Executor executor;
    public final UncaughtExceptionHandler exceptionHandler;

    /**
     * @param executor {@link Executor#execute(Runnable)} is called twice, once for reading from and once for writing to the socket
     * @param exceptionHandler called if executor rejects task
     */
    public SocketExecutor(final Executor executor, final UncaughtExceptionHandler exceptionHandler) {
        this.executor = Check.notNull(executor);
        this.exceptionHandler = Check.notNull(exceptionHandler);
    }

    void execute(final Socket socket, final SocketListener listener) {
        try {
            executor.execute(() -> {
                try {
                    listener.accept(socket, executor);
                } catch (final Exception e) {
                    SocketTransport.close(socket, e);
                    throw Exceptions.wrap(e);
                }
            });
        } catch (final Exception e) {
            SocketTransport.close(socket, e);
            Exceptions.uncaughtException(exceptionHandler, e);
        }
    }

}
