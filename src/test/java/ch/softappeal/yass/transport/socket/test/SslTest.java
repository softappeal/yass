package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.transport.socket.SimpleSocketBinder;
import ch.softappeal.yass.transport.socket.SimpleSocketConnector;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.socket.SslSetup;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.util.ClassLoaderResource;
import ch.softappeal.yass.util.Closer;
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

public class SslTest extends TransportTest {

    private static void checkName() throws Exception {
        System.out.println("checkName");
        Assert.assertEquals("CN=Test", ((SSLSocket)SimpleSocketTransport.socket().get()).getSession().getPeerPrincipal().getName());
    }

    @SuppressWarnings("try")
    private static void test(final ServerSocketFactory serverSocketFactory, final SocketFactory socketFactory, final boolean needClientAuth) throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try (
            Closer closer = new SimpleSocketTransport(
                executor,
                MESSAGE_SERIALIZER,
                new Server(
                    ContractIdTest.ID.service(
                        new TestServiceImpl(),
                        (method, arguments, invocation) -> {
                            if (needClientAuth) {
                                checkName();
                            }
                            return invocation.proceed();
                        }
                    )
                )
            ).start(executor, new SimpleSocketBinder(serverSocketFactory, SocketTransportTest.ADDRESS))
        ) {
            Assert.assertTrue(
                SimpleSocketTransport.client(MESSAGE_SERIALIZER, new SimpleSocketConnector(socketFactory, SocketTransportTest.ADDRESS))
                    .proxy(
                        ContractIdTest.ID,
                        (method, arguments, invocation) -> {
                            checkName();
                            return invocation.proceed();
                        }
                    )
                    .divide(12, 4) == 3
            );
            System.out.println("ok");
        } finally {
            TimeUnit.MILLISECONDS.sleep(200);
            executor.shutdown();
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
        try {
            test(
                new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, OTHER_CA).serverSocketFactory,
                new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).socketFactory,
                true
            );
            Assert.fail();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Test public void expiredServerCertificate() throws Exception {
        try {
            test(
                new SslSetup(PROTOCOL, CIPHER, TEST, PASSWORD, TEST_CA).serverSocketFactory,
                new SslSetup(PROTOCOL, CIPHER, TEST_EXPIRED, PASSWORD, TEST_CA).socketFactory,
                true
            );
            Assert.fail();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
