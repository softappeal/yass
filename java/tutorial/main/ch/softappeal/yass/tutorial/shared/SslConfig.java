package ch.softappeal.yass.tutorial.shared;

import ch.softappeal.yass.transport.socket.SslSetup;
import ch.softappeal.yass.util.InputStreamSupplier;

import java.security.KeyStore;

public final class SslConfig {

    private static final char[] PASSWORD = "StorePass".toCharArray();

    private static KeyStore readKeyStore(final String name) {
        return SslSetup.readKeyStore(InputStreamSupplier.create("certificates/" + name), PASSWORD);
    }

    private static SslSetup sslSetup(final String keyStore, final String trustStore) {
        return new SslSetup("TLSv1.2", "TLS_RSA_WITH_AES_128_CBC_SHA", readKeyStore(keyStore), PASSWORD, readKeyStore(trustStore));
    }

    public static final SslSetup SERVER = sslSetup("Server.key.pkcs12", "ClientCA.cert.pkcs12");
    public static final SslSetup CLIENT = sslSetup("Client.key.pkcs12", "Server.cert.pkcs12");

}
