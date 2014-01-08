package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.SslSetup;
import ch.softappeal.yass.transport.socket.StatelessTransport;
import ch.softappeal.yass.util.ClassLoaderResource;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SslTest extends InvokeTest {

  private static void test(final ServerSocketFactory serverSocketFactory, final SocketFactory socketFactory) throws Exception {
    final Interceptor peerPrincipalChecker = new Interceptor() {
      @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
        Assert.assertEquals("CN=Test", ((SSLSocket)StatelessTransport.socket()).getSession().getPeerPrincipal().getName());
        return invocation.proceed();
      }
    };
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    try {
      new StatelessTransport(
        new Server(
          PerformanceTest.METHOD_MAPPER_FACTORY,
          PerformanceTest.CONTRACT_ID.service(new TestServiceImpl(), peerPrincipalChecker)
        ),
        SocketPerformanceTest.MESSAGE_SERIALIZER,
        executor,
        Exceptions.STD_ERR
      ).start(executor, serverSocketFactory, SocketListenerTest.ADDRESS);
      final TestService testService = PerformanceTest.CONTRACT_ID.invoker(
        StatelessTransport.client(
          PerformanceTest.METHOD_MAPPER_FACTORY, SocketPerformanceTest.MESSAGE_SERIALIZER, socketFactory, SocketListenerTest.ADDRESS
        )
      ).proxy(peerPrincipalChecker);
      Assert.assertTrue(testService.divide(12, 4) == 3);
    } finally {
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

  @Test(expected = Exception.class) public void wrongServerCA() throws Exception {
    System.setProperty("javax.net.debug", "ssl");
    test(
      new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, OTHER_CA).serverSocketFactory,
      new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).socketFactory
    );
  }

  @Test(expected = Exception.class) public void expiredServerCertificate() throws Exception {
    System.setProperty("javax.net.debug", "ssl");
    test(
      new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).serverSocketFactory,
      new SslSetup(PROTOCOL, CIPHER, TEST_EXPIRED, PASSWORD, TEST_CA).socketFactory
    );
  }

}
