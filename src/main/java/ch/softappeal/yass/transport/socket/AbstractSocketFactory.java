package ch.softappeal.yass.transport.socket;

import javax.net.SocketFactory;
import java.net.InetAddress;
import java.net.Socket;

public abstract class AbstractSocketFactory extends SocketFactory {

  @Override public final Socket createSocket(final String host, final int port) {
    throw new UnsupportedOperationException();
  }

  @Override public final Socket createSocket(final String host, final int port, final InetAddress localHost, final int localPort) {
    throw new UnsupportedOperationException();
  }

  @Override public final Socket createSocket(final InetAddress host, final int port) {
    throw new UnsupportedOperationException();
  }

  @Override public final Socket createSocket(final InetAddress host, final int port, final InetAddress localHost, final int localPort) {
    throw new UnsupportedOperationException();
  }

}
