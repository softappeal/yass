package ch.softappeal.yass.transport.socket;

import javax.net.ServerSocketFactory;
import java.net.InetAddress;
import java.net.ServerSocket;

public abstract class AbstractServerSocketFactory extends ServerSocketFactory {

    @Override public final ServerSocket createServerSocket(final int port) {
        throw new UnsupportedOperationException();
    }

    @Override public final ServerSocket createServerSocket(final int port, final int backlog) {
        throw new UnsupportedOperationException();
    }

    @Override public final ServerSocket createServerSocket(final int port, final int backlog, final InetAddress address) {
        throw new UnsupportedOperationException();
    }

}
