package ch.softappeal.yass.tutorial.initiator.web;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Tunnel;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.util.Check;

import java.net.HttpURLConnection;
import java.net.URL;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public final class XhrInitiator {

    private static Client xhr(final Serializer messageSerializer, final URL url) {
        Check.notNull(messageSerializer);
        Check.notNull(url);
        return new Client() {
            @Override public Object invoke(final Client.Invocation invocation) throws Exception {
                final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                try {
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    return invocation.invoke(new Tunnel() {
                        @Override public Reply invoke(final Request request) throws Exception {
                            messageSerializer.write(request, Writer.create(connection.getOutputStream()));
                            if (invocation.methodMapping.oneWay) {
                                throw new IllegalArgumentException("xhr not allowed for oneWay method (serviceId " + request.serviceId + ", methodId " + request.methodId + ')');
                            }
                            return (Reply)messageSerializer.read(Reader.create(connection.getInputStream()));
                        }
                    });
                } finally {
                    connection.disconnect();
                }
            }
        };
    }

    public static void main(final String... args) throws Exception {
        final Client client = xhr(Config.MESSAGE_SERIALIZER, new URL("http://localhost:9090/xhr"));
        final EchoService echoService = client.proxy(ACCEPTOR.echoService, new Logger(null, Logger.Side.CLIENT));
        System.out.println(echoService.echo("echo"));
    }

}
