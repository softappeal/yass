package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Exceptions;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executor;

public final class SocketUtils {

    private SocketUtils() {
        // disable
    }

    static void setForceImmediateSend(final Socket socket) throws SocketException {
        socket.setTcpNoDelay(true);
    }

    public static void close(final Socket socket, final Exception e) {
        try {
            socket.close();
        } catch (final Exception e2) {
            e.addSuppressed(e2);
        }
    }

    public static void close(final ServerSocket serverSocket, final Exception e) {
        try {
            serverSocket.close();
        } catch (final Exception e2) {
            e.addSuppressed(e2);
        }
    }

    @FunctionalInterface interface SocketExecutor {
        void execute(Socket socket) throws Exception;
    }

    static void execute(final Executor executor, final Socket socket, final SocketExecutor socketExecutor) {
        try {
            executor.execute(() -> {
                try {
                    setForceImmediateSend(socket);
                    socketExecutor.execute(socket);
                } catch (final Exception e) {
                    close(socket, e);
                    throw Exceptions.wrap(e);
                }
            });
        } catch (final Exception e) {
            close(socket, e);
            throw e;
        }
    }

}
