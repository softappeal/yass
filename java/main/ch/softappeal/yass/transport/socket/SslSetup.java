package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Resource;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;

public final class SslSetup {

    public final SSLContext context;
    public final String protocol;
    public final String cipher;
    private final String[] protocols;
    private final String[] cipherSuites;
    public final boolean needClientAuth;

    public String[] protocols() {
        return new String[] {protocol};
    }

    public String[] cipherSuites() {
        return new String[] {cipher};
    }

    public SslSetup(
        final String protocol,
        final String cipher,
        final @Nullable KeyStore keyStore,
        final @Nullable char[] keyStorePwd,
        final @Nullable KeyStore trustStore,
        final @Nullable SecureRandom random,
        final String keyManagerFactoryAlgorithm,
        final String trustManagerFactoryAlgorithm
    ) {
        if ((keyStore == null) && (trustStore == null)) {
            throw new IllegalArgumentException("at least one of keyStore or trustStore must be defined");
        }
        this.protocol = Check.notNull(protocol);
        this.cipher = Check.notNull(cipher);
        protocols = protocols();
        cipherSuites = cipherSuites();
        try {
            context = SSLContext.getInstance(protocol);
            KeyManager[] keyManagers = new KeyManager[0];
            if (keyStore != null) {
                final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithm);
                keyManagerFactory.init(keyStore, keyStorePwd);
                keyManagers = keyManagerFactory.getKeyManagers();
            }
            boolean needClientAuth = false;
            TrustManager[] trustManagers = new TrustManager[0];
            if (trustStore != null) {
                final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustManagerFactoryAlgorithm);
                trustManagerFactory.init(trustStore);
                trustManagers = trustManagerFactory.getTrustManagers();
                needClientAuth = true;
            }
            context.init(keyManagers, trustManagers, random);
            this.needClientAuth = needClientAuth;
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    public SslSetup(
        final String protocol,
        final String cipher,
        final @Nullable KeyStore keyStore,
        final @Nullable char[] keyStorePwd,
        final @Nullable KeyStore trustStore
    ) {
        this(protocol, cipher, keyStore, keyStorePwd, trustStore, null, "SunX509", "SunX509");
    }

    public final SocketFactory socketFactory = new AbstractSocketFactory() {
        @Override public Socket createSocket() throws IOException {
            final SSLSocket socket = (SSLSocket)context.getSocketFactory().createSocket();
            try {
                socket.setEnabledProtocols(protocols);
                socket.setEnabledCipherSuites(cipherSuites);
            } catch (final Exception e) {
                SocketUtils.close(socket, e);
                throw e;
            }
            return socket;
        }
    };

    public final ServerSocketFactory serverSocketFactory = new AbstractServerSocketFactory() {
        @Override public ServerSocket createServerSocket() throws IOException {
            final SSLServerSocket serverSocket = (SSLServerSocket)context.getServerSocketFactory().createServerSocket();
            try {
                serverSocket.setNeedClientAuth(needClientAuth);
                serverSocket.setEnabledProtocols(protocols);
                serverSocket.setEnabledCipherSuites(cipherSuites);
            } catch (final Exception e) {
                SocketUtils.close(serverSocket, e);
                throw e;
            }
            return serverSocket;
        }
    };

    public static KeyStore readKeyStore(final Resource keyStoreResource, final @Nullable char[] keyStorePwd) {
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

}
