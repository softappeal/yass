package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.test.SerializerTest;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.PacketSerializer;
import ch.softappeal.yass.transport.socket.SocketConnection;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;
import ch.softappeal.yass.util.TestUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class WriterExecutorTest {

  public interface StringListener {
    @Tag(1) @OneWay void newString(String value);
  }

  private static final ContractId<StringListener> StringListenerId = ContractId.create(StringListener.class, 0);

  private static final Serializer PACKET_SERIALIZER = new PacketSerializer(new MessageSerializer(SerializerTest.TAGGED_FAST_SERIALIZER));

  private static final MethodMapper.Factory METHOD_MAPPER_FACTORY = TaggedMethodMapper.FACTORY;

  private static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

  private static Executor executor(final String name) {
    return Executors.newCachedThreadPool(new NamedThreadFactory(name, TestUtils.TERMINATE));
  }

  private static void server() {
    new SocketTransport(
      new SessionSetup(
        new Server(
          METHOD_MAPPER_FACTORY
        ),
        executor("server-request"),
        new SessionFactory() {
          @Override public Session create(final SessionSetup setup, final Connection connection) {
            return new Session(setup, connection) {
              @Override public void opened() {
                final SocketConnection socketConnection = (SocketConnection)connection;
                final StringListener stringListener = StringListenerId.invoker(this).proxy();
                socketConnection.awaitWriterQueueEmpty();
                final Executor worker = executor("server-worker");
                final String s = "hello";
                for (int i = 0; i < 20; i++) {
                  worker.execute(new Runnable() {
                    @Override public void run() {
                      while (true) {
                        stringListener.newString(s);
                        try {
                          TimeUnit.MILLISECONDS.sleep(1);
                        } catch (final InterruptedException e) {
                          throw new RuntimeException(e);
                        }
                      }
                    }
                  });
                }
              }
              @Override public void closed(@Nullable final Exception exception) {
                TestUtils.TERMINATE.uncaughtException(null, exception);
              }
            };
          }
        }
      ),
      PACKET_SERIALIZER,
      executor("server-reader"), executor("server-writer"),
      TestUtils.TERMINATE
    ).start(executor("server-listener"), ADDRESS);
  }

  public static void main(final String... args) {
    server();
    final AtomicInteger counter = new AtomicInteger(0);
    new SocketTransport(
      new SessionSetup(
        new Server(
          METHOD_MAPPER_FACTORY,
          StringListenerId.service(
            new StringListener() {
              @Override public void newString(final String value) {
                counter.incrementAndGet();
              }
            }
          )
        ),
        executor("client-request"),
        new SessionFactory() {
          @Override public Session create(final SessionSetup setup, final Connection connection) {
            return new Session(setup, connection) {
              @Override public void closed(@Nullable final Exception exception) {
                TestUtils.TERMINATE.uncaughtException(null, exception);
              }
            };
          }
        }
      ),
      PACKET_SERIALIZER,
      executor("client-reader"), executor("client-writer"),
      TestUtils.TERMINATE
    ).connect(ADDRESS);
    Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
      new Runnable() {
        @Override public void run() {
          System.out.println(counter.get());
        }
      },
      0,
      1,
      TimeUnit.SECONDS
    );
  }

}
