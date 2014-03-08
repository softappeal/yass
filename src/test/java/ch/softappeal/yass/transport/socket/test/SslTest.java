package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketConnection;
import ch.softappeal.yass.transport.socket.SocketExecutor;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SslSetup;
import ch.softappeal.yass.util.ClassLoaderResource;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SslTest extends InvokeTest {

  private static void test(final ServerSocketFactory serverSocketFactory, final SocketFactory socketFactory) throws Exception {
    final Interceptor peerPrincipalChecker = new Interceptor() {
      @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
        Assert.assertEquals("CN=Test", ((SSLSocket)((SocketConnection)Session.get().connection).socket).getSession().getPeerPrincipal().getName());
        return invocation.proceed();
      }
    };
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    try {
      SocketTransport.listener(
        SocketTransportTest.PATH_SERIALIZER,
        new PathResolver(
          SocketTransportTest.PATH,
          new TransportSetup(
            new Server(
              PerformanceTest.METHOD_MAPPER_FACTORY,
              PerformanceTest.CONTRACT_ID.service(new TestServiceImpl(), peerPrincipalChecker)
            ),
            SocketPerformanceTest.PACKET_SERIALIZER,
            executor,
            new SessionFactory() {
              @Override public Session create(SessionSetup setup, Connection connection) throws Exception {
                return new Session(setup, connection) {
                  @Override protected void closed(final Throwable throwable) {
                    if (throwable != null) {
                      TestUtils.TERMINATE.uncaughtException(null, throwable);
                    }
                  }
                };
              }
            }
          )
        )
      ).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), serverSocketFactory, SocketListenerTest.ADDRESS);
      SocketTransport.connect(
        new TransportSetup(
          new Server(PerformanceTest.METHOD_MAPPER_FACTORY),
          SocketPerformanceTest.PACKET_SERIALIZER,
          executor,
          new SessionFactory() {
            @Override public Session create(final SessionSetup setup, final Connection connection) {
              return new Session(setup, connection) {
                @Override protected void opened() throws Exception {
                  final TestService testService = PerformanceTest.CONTRACT_ID.invoker(this).proxy(peerPrincipalChecker);
                  Assert.assertTrue(testService.divide(12, 4) == 3);
                  close();
                }
                @Override protected void closed(final Throwable throwable) {
                  if (throwable != null) {
                    TestUtils.TERMINATE.uncaughtException(null, throwable);
                  }
                }
              };
            }
          }
        ),
        new SocketExecutor(executor, TestUtils.TERMINATE),
        SocketTransportTest.PATH_SERIALIZER, SocketTransportTest.PATH,
        socketFactory, SocketListenerTest.ADDRESS
      );
    } finally {
      TimeUnit.MILLISECONDS.sleep(200);
      SocketListenerTest.shutdown(executor);
    }
  }

  private static final char[] PASSWORD = "changeit".toCharArray();

  private static KeyStore readKeyStore(final String name) {
    return SslSetup.readKeyStore(new ClassLoaderResource(SslTest.class.getClassLoader(), SslTest.class.getPackage().getName().replace('.', '/') + '/' + name + ".jks"), PASSWORD);
  }

  private static final KeyStore TEST = readKeyStore("Test");
  private static final KeyStore TEST_EXPIRED = readKeyStore("TestExpired");
  private static final KeyStore TEST_CA = readKeyStore("TestCA");
  private static final KeyStore OTHER_CA = readKeyStore("OtherCA");

  private static final String PROTOCOL = "TLSv1.2";
  private static final String CIPHER = "TLS_RSA_WITH_AES_128_CBC_SHA";

  @Test public void clientAndServerAuthentication() throws Exception {
    System.setProperty("javax.net.debug", "ssl");
    test(
      new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).serverSocketFactory,
      new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).socketFactory
    );
  }

  @Test public void wrongServerCA() throws Exception {
    System.setProperty("javax.net.debug", "ssl");
    test(
      new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, OTHER_CA).serverSocketFactory,
      new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).socketFactory
    );
  }

  @Test public void expiredServerCertificate() throws Exception {
    System.setProperty("javax.net.debug", "ssl");
    test(
      new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).serverSocketFactory,
      new SslSetup(PROTOCOL, CIPHER, TEST_EXPIRED, PASSWORD, TEST_CA).socketFactory
    );
  }

}
