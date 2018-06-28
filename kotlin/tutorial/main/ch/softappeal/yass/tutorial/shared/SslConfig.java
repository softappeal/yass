package ch.softappeal.yass.tutorial.shared;

import ch.softappeal.yass.transport.socket.SslSetup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;

import static ch.softappeal.yass.transport.socket.SslSetupKt.readKeyStore;

public final class SslConfig {

    private static final char[] PASSWORD = "StorePass".toCharArray();

    private static KeyStore keyStore(final String name) {
        try (InputStream keyStore = Files.newInputStream(Paths.get("certificates", name))) {
            return readKeyStore(keyStore, PASSWORD);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static SslSetup sslSetup(final String keyStore, final String trustStore) {
        return new SslSetup("TLSv1.2", "TLS_RSA_WITH_AES_128_CBC_SHA", keyStore(keyStore), PASSWORD, keyStore(trustStore));
    }

    public static final SslSetup SERVER = sslSetup("Server.key.pkcs12", "ClientCA.cert.pkcs12");
    public static final SslSetup CLIENT = sslSetup("Client.key.pkcs12", "Server.cert.pkcs12");

}
