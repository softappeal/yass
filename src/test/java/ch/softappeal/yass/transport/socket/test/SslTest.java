package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.SimpleSession;
import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketConnection;
import ch.softappeal.yass.transport.socket.SocketHelper;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SslSetup;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
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

    private static void checkName(final Connection connection) throws Exception {
        Assert.assertEquals("CN=Test", ((SSLSocket)((SocketConnection)connection).socket).getSession().getPeerPrincipal().getName());
    }

    private static void test(final ServerSocketFactory serverSocketFactory, final SocketFactory socketFactory, final boolean needClientAuth) throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try {
            new SocketTransport(executor, SyncSocketConnection.FACTORY).start(
                TransportSetup.ofPacketSerializer(
                    JavaSerializer.INSTANCE,
                    connection -> {
                        if (needClientAuth) {
                            checkName(connection);
                        }
                        return new SimpleSession(connection, executor) {
                            @Override protected Server server() {
                                return new Server(
                                    PerformanceTest.CONTRACT_ID.service(new TestServiceImpl())
                                );
                            }
                        };
                    }
                ),
                executor,
                serverSocketFactory,
                SocketHelper.ADDRESS
            );
            new SocketTransport(executor, SyncSocketConnection.FACTORY).connect(
                TransportSetup.ofPacketSerializer(
                    JavaSerializer.INSTANCE,
                    connection -> {
                        checkName(connection);
                        return new SimpleSession(connection, executor) {
                            @Override protected Server server() {
                                return Server.EMPTY;
                            }
                            @Override protected void opened() throws Exception {
                                final TestService testService = proxy(PerformanceTest.CONTRACT_ID);
                                Assert.assertTrue(testService.divide(12, 4) == 3);
                                close();
                            }
                        };
                    }
                ),
                socketFactory, SocketHelper.ADDRESS
            );
        } finally {
            TimeUnit.MILLISECONDS.sleep(200);
            SocketHelper.shutdown(executor);
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
