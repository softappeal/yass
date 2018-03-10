package ch.softappeal.yass.tutorial.initiator.web;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.SslConfig;
import ch.softappeal.yass.tutorial.shared.web.WebSetup;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public final class XhrInitiator extends WebSetup {

    private static Client xhr(final Serializer messageSerializer, final URL url, final boolean ssl) {
        Objects.requireNonNull(messageSerializer);
        Objects.requireNonNull(url);
        return new Client() {
            @Override public void invoke(final Client.Invocation invocation) throws Exception {
                final var connection = (HttpURLConnection)url.openConnection();
                try {
                    if (ssl) {
                        ((HttpsURLConnection)connection).setSSLSocketFactory(SslConfig.CLIENT.context.getSocketFactory());
                        ((HttpsURLConnection)connection).setHostnameVerifier((s, sslSession) -> true);
                    }
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    invocation.invoke(false, request -> {
                        messageSerializer.write(request, Writer.create(connection.getOutputStream()));
                        invocation.settle((Reply)messageSerializer.read(Reader.create(connection.getInputStream())));
                    });
                } finally {
                    connection.disconnect();
                }
            }
        };
    }

    private static void client(final String url, final boolean ssl) throws Exception {
        final var client = xhr(Config.MESSAGE_SERIALIZER, new URL(url), ssl);
        final var echoService = client.proxy(ACCEPTOR.echoService, new Logger(null, Logger.Side.CLIENT));
        System.out.println(echoService.echo("echo"));
    }

    public static void main(final String... args) throws Exception {
        client("http://localhost:" + PORT + XHR_PATH, false);
        client("https://localhost:" + (PORT + 1) + XHR_PATH, true);
    }

}
