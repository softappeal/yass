package ch.softappeal.yass.transport.ws.echo;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.IOException;

public final class ServerEndpoint extends Endpoint {

  @Override public void onOpen(final Session session, final EndpointConfig config) {
    System.out.println("session '" + session.getId() + "' opened");
    final RemoteEndpoint.Basic remote = session.getBasicRemote();
    session.addMessageHandler(new MessageHandler.Whole<String>() {
      @Override public void onMessage(final String message) {
        System.out.println("session '" + session.getId() + "' echos message: '" + message + "'");
        if ("error".equals(message)) {
          throw new RuntimeException("error occurred");
        }
        try {
          remote.sendText("echo(" + message + ')');
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
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
