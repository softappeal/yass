package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketConnection;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SslSetup;
import ch.softappeal.yass.util.ClassLoaderResource;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SslTest extends InvokeTest {

    private static void checkName(final SessionClient sessionClient) throws Exception {
        Assert.assertEquals("CN=Test", ((SSLSocket)((SocketConnection)sessionClient.connection).socket).getSession().getPeerPrincipal().getName());
    }

    private static void test(
        final ServerSocketFactory serverSocketFactory, final SocketFactory socketFactory, final boolean needClientAuth
    ) throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try {
            SocketTransport.listener(new TransportSetup(
                new Server(
                    PerformanceTest.METHOD_MAPPER_FACTORY,
                    new Service(PerformanceTest.CONTRACT_ID, new TestServiceImpl())
                ),
                executor,
                SocketPerformanceTest.PACKET_SERIALIZER,
                sessionClient -> {
                    if (needClientAuth) {
                        checkName(sessionClient);
                    }
                    return new Session(sessionClient) {
                        @Override protected void closed(final Throwable throwable) {
                            if (throwable != null) {
                                Exceptions.TERMINATE.uncaughtException(null, throwable);
                            }
                        }
                    };
                }
            )).start(executor, executor, serverSocketFactory, SocketListenerTest.ADDRESS);
            SocketTransport.connect(
                new TransportSetup(
                    new Server(PerformanceTest.METHOD_MAPPER_FACTORY),
                    executor,
                    SocketPerformanceTest.PACKET_SERIALIZER,
                    sessionClient -> {
                        checkName(sessionClient);
                        return new Session(sessionClient) {
                            @Override protected void opened() throws Exception {
                                final TestService testService = proxy(PerformanceTest.CONTRACT_ID);
                                Assert.assertTrue(testService.divide(12, 4) == 3);
                                close();
                            }
                            @Override protected void closed(final Throwable throwable) {
                                if (throwable != null) {
                                    Exceptions.TERMINATE.uncaughtException(null, throwable);
                                }
                            }
                        };
                    }
                ),
                executor,
                socketFactory, SocketListenerTest.ADDRESS
            );
        } finally {
            TimeUnit.MILLISECONDS.sleep(200);
            SocketListenerTest.shutdown(executor);
        }
    }

    private static final char[] PASSWORD = "changeit".toCharArray();

    private static KeyStore readKeyStore(final String name) {
        return SslSetup.readKeyStore(
            new ClassLoaderResource(
                SslTest.class.getClassLoader(),
                SslTest.class.getPackage().getName().replace('.', '/') + '/' + name + ".jks"
            ),
            PASSWORD
        );
    }

    private static final KeyStore TEST = readKeyStore("Test");
    private static final KeyStore TEST_EXPIRED = readKeyStore("TestExpired");
    private static final KeyStore TEST_CA = readKeyStore("TestCA");
    private static final KeyStore OTHER_CA = readKeyStore("OtherCA");

    private static final String PROTOCOL = "TLSv1.2";
    private static final String CIPHER = "TLS_RSA_WITH_AES_128_CBC_SHA";

    @Test public void onlyServerAuthentication() throws Exception {
        test(
            new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, null).serverSocketFactory,
            new SslSetup(PROTOCOL, CIPHER, null, null, TEST_CA).socketFactory,
            false
        );
    }

    @Test public void clientAndServerAuthentication() throws Exception {
        test(
            new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).serverSocketFactory,
            new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).socketFactory,
            true
        );
    }

    @Test public void wrongServerCA() throws Exception {
        test(
            new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, OTHER_CA).serverSocketFactory,
            new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).socketFactory,
            true
        );
    }

    @Test public void expiredServerCertificate() throws Exception {
        test(
            new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).serverSocketFactory,
            new SslSetup(PROTOCOL, CIPHER, TEST_EXPIRED, PASSWORD, TEST_CA).socketFactory,
            true
        );
    }

}
