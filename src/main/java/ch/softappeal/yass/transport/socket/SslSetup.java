package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Resource;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;

public final class SslSetup {

  public static KeyStore readKeyStore(final Resource keyStoreResource, @Nullable final char[] keyStorePwd) {
    try {
      final KeyStore keyStore = KeyStore.getInstance("JKS");
      try (InputStream in = keyStoreResource.create()) {
        keyStore.load(in, keyStorePwd);
      }
      return keyStore;
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
  }

  private final SSLContext context;
  private final String[] protocols;
  private final String[] cipherSuites;

  @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
  public SslSetup(
    final String protocol, final String cipher, final KeyStore keyStore, final char[] keyStorePwd, final KeyStore trustStore,
    @Nullable final SecureRandom random, final String keyManagerFactoryAlgorithm, final String trustManagerFactoryAlgorithm
  ) {
    protocols = new String[] {Check.notNull(protocol)};
    cipherSuites = new String[] {Check.notNull(cipher)};
    try {
      context = SSLContext.getInstance(protocol);
      final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithm);
      keyManagerFactory.init(keyStore, keyStorePwd);
      final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustManagerFactoryAlgorithm);
      trustManagerFactory.init(trustStore);
      context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), random);
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
  }

  public SslSetup(final String protocol, final String cipher, final KeyStore keyStore, final char[] keyStorePwd, final KeyStore trustStore) {
    this(protocol, cipher, keyStore, keyStorePwd, trustStore, null, "SunX509", "SunX509");
  }

  public final SocketFactory socketFactory = new AbstractSocketFactory() {
    @Override public Socket createSocket() throws IOException {
      final SSLSocket socket = (SSLSocket)context.getSocketFactory().createSocket();
      try {
        socket.setEnabledProtocols(protocols);
        socket.setEnabledCipherSuites(cipherSuites);
      } catch (final Exception e) {
        SocketListener.closeWithAddSuppressed(socket, e);
        throw e;
      }
      return socket;
    }
  };

  public final ServerSocketFactory serverSocketFactory = new AbstractServerSocketFactory() {
    @Override public ServerSocket createServerSocket() throws IOException {
      final SSLServerSocket serverSocket = (SSLServerSocket)context.getServerSocketFactory().createServerSocket();
      try {
        serverSocket.setNeedClientAuth(true);
        serverSocket.setEnabledProtocols(protocols);
        serverSocket.setEnabledCipherSuites(cipherSuites);
      } catch (final Exception e) {
        SocketListener.closeWithAddSuppressed(serverSocket, e);
        throw e;
      }
      return serverSocket;
    }
  };

}
