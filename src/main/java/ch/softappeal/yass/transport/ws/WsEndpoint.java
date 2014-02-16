package ch.softappeal.yass.transport.ws;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

public abstract class WsEndpoint extends Endpoint { // $todo: review

  private static final String CONNECTION = "xkj{{/&@]59QQw53";

  protected abstract WsConnection createConnection(Session session);

  @Override public final void onOpen(final Session session, final EndpointConfig config) {
    session.getUserProperties().put(CONNECTION, createConnection(session));
  }

  @Override public final void onClose(final Session session, final CloseReason closeReason) {
    ((WsConnection)session.getUserProperties().get(CONNECTION)).onClose(closeReason);
  }

  @Override public final void onError(final Session session, final Throwable throwable) {
    ((WsConnection)session.getUserProperties().get(CONNECTION)).onError(throwable);
  }

}
