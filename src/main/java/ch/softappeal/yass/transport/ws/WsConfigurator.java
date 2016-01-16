package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.ArrayList;
import java.util.List;

public class WsConfigurator extends ServerEndpointConfig.Configurator {

    final WsConnection.Factory connectionFactory;
    final TransportSetup setup;
    final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public WsConfigurator(final WsConnection.Factory connectionFactory, final TransportSetup setup, final Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.connectionFactory = Check.notNull(connectionFactory);
        this.setup = Check.notNull(setup);
        this.uncaughtExceptionHandler = Check.notNull(uncaughtExceptionHandler);
    }

    @Override public final <T> T getEndpointInstance(final Class<T> endpointClass) {
        return endpointClass.cast(new Endpoint() {
            volatile WsConnection connection = null;
            @Override public void onOpen(final Session session, final EndpointConfig config) {
                try {
                    connection = WsConnection.create(WsConfigurator.this, session);
                } catch (final Throwable ignore) {
                    Exceptions.uncaughtException(uncaughtExceptionHandler, ignore);
                }
            }
            @Override public void onClose(final Session session, final CloseReason closeReason) {
                if (connection != null) {
                    connection.onClose(closeReason);
                }
            }
            @Override public void onError(final Session session, final Throwable throwable) {
                if (connection != null) {
                    connection.onError(throwable);
                }
            }
        });
    }

    public final Endpoint getEndpointInstance() {
        return getEndpointInstance(Endpoint.class);
    }

    @Override public String getNegotiatedSubprotocol(final List<String> supported, final List<String> requested) {
        for (final String r : requested) {
            if (supported.contains(r)) {
                return r;
            }
        }
        return "";
    }

    @Override public List<Extension> getNegotiatedExtensions(final List<Extension> installed, final List<Extension> requested) {
        final List<Extension> extensions = new ArrayList<>(requested.size());
        for (final Extension r : requested) {
            for (final Extension i : installed) {
                if (i.getName().equals(r.getName())) {
                    extensions.add(r);
                    break;
                }
            }
        }
        return extensions;
    }

    @Override public boolean checkOrigin(final String originHeaderValue) {
        return true;
    }

    @Override public void modifyHandshake(final ServerEndpointConfig sec, final HandshakeRequest request, final HandshakeResponse response) {
        // empty
    }

}
