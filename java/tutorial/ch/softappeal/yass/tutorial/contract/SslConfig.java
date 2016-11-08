package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.transport.socket.SslSetup;
import ch.softappeal.yass.util.FileResource;

import java.security.KeyStore;

public final class SslConfig {

    private static KeyStore readKeyStore(final String name) {
        return SslSetup.readKeyStore(new FileResource("certificates/" + name), "StorePass".toCharArray());
    }

    private static SslSetup sslSetup(final String keyStore, final String trustStore) {
        return new SslSetup("TLSv1.2", "TLS_RSA_WITH_AES_128_CBC_SHA", readKeyStore(keyStore), "KeyPass".toCharArray(), readKeyStore(trustStore));
    }

    public static final SslSetup SERVER = sslSetup("Server.key.jks", "ClientCA.cert.jks");
    public static final SslSetup CLIENT = sslSetup("Client.key.jks", "Server.cert.jks");

}
