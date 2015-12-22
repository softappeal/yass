package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

public abstract class WsEndpoint extends Endpoint {

    private static final String CONNECTION = "]{*@+?'"; // should never clash ;-)

    protected abstract WsConnection createConnection(Session session) throws Exception;

    @Override public final void onOpen(final Session session, final EndpointConfig config) {
        try {
            session.getUserProperties().put(CONNECTION, createConnection(session));
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    private static WsConnection getConnection(final Session session) {
        return (WsConnection)session.getUserProperties().get(CONNECTION);
    }

    @Override public final void onClose(final Session session, final CloseReason closeReason) {
        getConnection(session).onClose(closeReason);
    }

    @Override public final void onError(final Session session, final Throwable throwable) {
        final @Nullable WsConnection connection = getConnection(session);
        if (connection != null) {
            connection.onError(throwable);
        }
    }

}
