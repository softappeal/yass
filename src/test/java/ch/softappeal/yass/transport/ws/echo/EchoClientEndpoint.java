package ch.softappeal.yass.transport.ws.echo;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

public final class EchoClientEndpoint extends Endpoint {

  @Override public void onOpen(final Session session, final EndpointConfig config) {
    System.out.println("session '" + session.getId() + "' opened");
    session.addMessageHandler(new MessageHandler.Whole<String>() {
      @Override public void onMessage(final String message) {
        System.out.println("session '" + session.getId() + "' received message: '" + message + "'");
      }
    });
  }

  @Override public void onClose(final Session session, final CloseReason closeReason) {
    System.out.println("session '" + session.getId() + "' close: " + closeReason);
  }

  @Override public void onError(final Session session, final Throwable thr) {
    System.out.println("session '" + session.getId() + "' error: " + thr);
  }

}
