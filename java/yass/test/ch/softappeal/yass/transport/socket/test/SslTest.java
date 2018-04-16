package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.InputStreamSupplier;
import ch.softappeal.yass.NamedThreadFactory;
import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.remote.test.ContractIdTest;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.socket.SocketBinder;
import ch.softappeal.yass.transport.socket.SocketConnector;
import ch.softappeal.yass.transport.socket.SslSetup;
import ch.softappeal.yass.transport.test.TransportTest;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import java.security.KeyStore;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.transport.socket.test.SimpleSocketTransportTest.delayedShutdown;

public class SslTest extends TransportTest {

    private static void checkName(final String name) throws Exception {
        System.out.println("checkName");
        Assert.assertEquals(name, ((SSLSocket)SimpleSocketTransport.socket()).getSession().getPeerPrincipal().getName());
    }

    @SuppressWarnings("try")
    private static void test(final ServerSocketFactory serverSocketFactory, final SocketFactory socketFactory, final boolean needClientAuth) throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try (
            var closer = new SimpleSocketTransport(
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

    private static final char[] PASSWORD = "StorePass".toCharArray();

    private static KeyStore readKeyStore(final String name) {
        return SslSetup.readKeyStore(InputStreamSupplier.create("../../certificates/" + name), PASSWORD);
    }

    private static final KeyStore SERVER_KEY = readKeyStore("Server.key.pkcs12");
    private static final KeyStore SERVER_CERT = readKeyStore("Server.cert.pkcs12");
    private static final KeyStore CLIENTCA_CERT = readKeyStore("ClientCA.cert.pkcs12");
    private static final KeyStore CLIENT_KEY = readKeyStore("Client.key.pkcs12");

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
