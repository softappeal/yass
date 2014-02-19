package ch.softappeal.yass.transport.ws.echo;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.util.Arrays;

public final class BinaryEchoClientEndpoint extends Endpoint {

  @Override public void onOpen(final Session session, final EndpointConfig config) {
    System.out.println("session '" + session.getId() + "' onOpen");
    session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
      @Override public void onMessage(final byte[] message) {
        System.out.println("session '" + session.getId() + "' onMessage: " + Arrays.toString(message));
      }
    });
  }

  @Override public void onClose(final Session session, final CloseReason closeReason) {
    System.out.println("session '" + session.getId() + "' onClose: " + closeReason);
  }

  @Override public void onError(final Session session, final Throwable thr) {
    System.out.println("session '" + session.getId() + "' onError: " + thr);
  }

}
