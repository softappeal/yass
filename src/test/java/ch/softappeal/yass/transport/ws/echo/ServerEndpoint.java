package ch.softappeal.yass.transport.ws.echo;

import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServerEndpoint extends Endpoint {

  private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(10, new NamedThreadFactory("Timer", Exceptions.STD_ERR));

  @Override public void onOpen(final Session session, final EndpointConfig config) {
    System.out.println("session '" + session.getId() + "' onOpen");
    final RemoteEndpoint.Basic remote = session.getBasicRemote();
    session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
      @Override public void onMessage(final byte[] message) {
        System.out.println("session '" + session.getId() + "' onMessage: " + Arrays.toString(message));
        try {
          if (message.length == 1) {
            final byte command = message[0];
            if (command == 0) {
              session.close();
            } else if (command == 1) {
              session.close(new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "command 1"));
            } else if (command == 2) {
              throw new RuntimeException("command 2");
            } else if (command == 99) {
              TIMER.scheduleWithFixedDelay(new Runnable() {
                final AtomicInteger counter = new AtomicInteger();
                @Override public void run() {
                  try {
                    remote.sendBinary(ByteBuffer.wrap(new byte[] {(byte)counter.incrementAndGet()}));
                  } catch (final IOException e) {
                    throw new RuntimeException(e);
                  }
                }
              }, 5000, 100, TimeUnit.MILLISECONDS);
            }
          } else {
            remote.sendBinary(ByteBuffer.wrap(message));
          }
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
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
