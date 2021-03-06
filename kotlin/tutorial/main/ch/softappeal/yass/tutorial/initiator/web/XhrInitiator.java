package ch.softappeal.yass.tutorial.initiator.web;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.serialize.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.shared.*;
import ch.softappeal.yass.tutorial.shared.web.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.util.*;

import static ch.softappeal.yass.serialize.ReaderKt.*;
import static ch.softappeal.yass.serialize.WriterKt.*;
import static ch.softappeal.yass.tutorial.contract.Config.*;

public final class XhrInitiator extends WebSetup {

    private static Client xhr(final Serializer messageSerializer, final URL url, final boolean ssl) {
        Objects.requireNonNull(messageSerializer);
        Objects.requireNonNull(url);
        return new Client() {
            @Override
            public void invoke(final ClientInvocation invocation) throws Exception {
                final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                try {
                    if (ssl) {
                        ((HttpsURLConnection) connection)
                            .setSSLSocketFactory(SslConfig.CLIENT.getContext().getSocketFactory());
                        ((HttpsURLConnection) connection).setHostnameVerifier((s, sslSession) -> true);
                    }
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    invocation.invoke(false, request -> {
                        try {
                            messageSerializer.write(writer(connection.getOutputStream()), request);
                            try (InputStream in = connection.getInputStream()) {
                                // note: early closing of input seams to make none ssl invocations faster
                                invocation.settle((Reply) messageSerializer.read(reader(in)));
                            }
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    });
                } finally {
                    connection.disconnect();
                }
            }
        };
    }

    private static void client(final String url, final boolean ssl) throws Exception {
        final Client client = xhr(Config.MESSAGE_SERIALIZER, new URL(url), ssl);
        final EchoService echoService = client.proxy(ACCEPTOR.echoService, new Logger(null, Logger.Side.CLIENT));
        System.out.println(echoService.echo("echo"));
    }

    public static void main(final String... args) throws Exception {
        client("http://localhost:" + PORT + XHR_PATH, false);
        client("https://localhost:" + (PORT + 1) + XHR_PATH, true);
    }

}
