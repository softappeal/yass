package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.socket.SocketBinder;
import ch.softappeal.yass.transport.socket.SocketConnector;
import ch.softappeal.yass.transport.socket.SslSetup;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.InputStreamSupplier;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.transport.socket.test.SimpleSocketTransportTest.delayedShutdown;

public class SslTest extends TransportTest {

    private static void checkName(final String name) throws Exception {
        System.out.println("checkName");
        Assert.assertEquals(name, ((SSLSocket)SimpleSocketTransport.socket().get()).getSession().getPeerPrincipal().getName());
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
                                checkName("CN=Client");
                            }
                            return invocation.proceed();
                        }
                    )
                )
            ).start(executor, SocketBinder.create(serverSocketFactory, SocketTransportTest.ADDRESS))
        ) {
            Assert.assertTrue(
                SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketConnector.create(socketFactory, SocketTransportTest.ADDRESS))
                    .proxy(
                        ContractIdTest.ID,
                        (method, arguments, invocation) -> {
                            checkName("CN=Server");
                            return invocation.proceed();
                        }
                    )
                    .divide(12, 4) == 3
            );
            System.out.println("ok");
        } finally {
            delayedShutdown(executor);
        }
    }

    private static final char[] PASSWORD = "KeyPass".toCharArray();

    private static KeyStore readKeyStore(final String name) {
        return SslSetup.readKeyStore(InputStreamSupplier.create("../../certificates/" + name), "StorePass".toCharArray());
    }

    private static final KeyStore SERVER_KEY = readKeyStore("Server.key.jks");
    private static final KeyStore SERVER_CERT = readKeyStore("Server.cert.jks");
    private static final KeyStore CLIENTCA_CERT = readKeyStore("ClientCA.cert.jks");
    private static final KeyStore CLIENT_KEY = readKeyStore("Client.key.jks");

    private static final String PROTOCOL = "TLSv1.2";
    private static final String CIPHER = "TLS_RSA_WITH_AES_128_CBC_SHA";

    @Test public void onlyServerAuthentication() throws Exception {
        test(
            new SslSetup(PROTOCOL, CIPHER, SERVER_KEY, PASSWORD, null).serverSocketFactory,
            new SslSetup(PROTOCOL, CIPHER, null, null, SERVER_CERT).socketFactory,
            false
        );
    }

    @Test public void clientAndServerAuthentication() throws Exception {
        test(
            new SslSetup(PROTOCOL, CIPHER, SERVER_KEY, PASSWORD, CLIENTCA_CERT).serverSocketFactory,
            new SslSetup(PROTOCOL, CIPHER, CLIENT_KEY, PASSWORD, SERVER_CERT).socketFactory,
            true
        );
    }

    @Test public void wrongServer() throws Exception {
        try {
            test(
                new SslSetup(PROTOCOL, CIPHER, SERVER_KEY, PASSWORD, null).serverSocketFactory,
                new SslSetup(PROTOCOL, CIPHER, null, null, CLIENTCA_CERT).socketFactory,
                false
            );
            Assert.fail();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Test public void wrongClientCA() throws Exception {
        try {
            test(
                new SslSetup(PROTOCOL, CIPHER, SERVER_KEY, PASSWORD, SERVER_CERT).serverSocketFactory,
                new SslSetup(PROTOCOL, CIPHER, CLIENT_KEY, PASSWORD, SERVER_CERT).socketFactory,
                true
            );
            Assert.fail();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
