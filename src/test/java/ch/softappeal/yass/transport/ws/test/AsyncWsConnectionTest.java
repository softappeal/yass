package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.session.Dispatcher;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.ws.AsyncWsConnection;
import ch.softappeal.yass.transport.ws.SyncWsConnection;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.util.Nullable;
import io.undertow.Undertow;
import io.undertow.server.XnioByteBufferPool;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.core.CompositeThreadSetupAction;
import io.undertow.servlet.util.DefaultClassIntrospector;
import io.undertow.websockets.jsr.DefaultContainerConfigurator;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public final class AsyncWsConnectionTest {

    public interface Busy {
        @OneWay void busy();
    }

    private static final ContractId<Busy> BUSY_ID = ContractId.create(Busy.class, 0);
    private static final Serializer PACKET_SERIALIZER = JavaSerializer.INSTANCE;
    private static final MethodMapper.Factory METHOD_MAPPER_FACTORY = SimpleMethodMapper.FACTORY;

    public static final String HOST = "0.0.0.0";
    public static final int PORT = 9090;
    public static final String PATH = "/test";

    private static final Dispatcher DISPATCHER = new Dispatcher() {
        @Override public void opened(final Session session, final Runnable runnable) {
            runnable.run();
        }
        @Override public void invoke(final Session session, final Server.Invocation invocation, final Runnable runnable) {
            runnable.run();
        }
    };

    public static final class ServerEndpoint extends WsEndpoint {
        @Override protected WsConnection createConnection(final javax.websocket.Session session) throws Exception {
            return WsConnection.create(
                SyncWsConnection.FACTORY,
                new TransportSetup(
                    new Server(
                        METHOD_MAPPER_FACTORY,
                        new Service(BUSY_ID, () -> {
                            System.out.println("busy");
                            try {
                                TimeUnit.MILLISECONDS.sleep(1_000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        })
                    ),
                    DISPATCHER,
                    PACKET_SERIALIZER,
                    sessionClient -> new Session(sessionClient) {
                        @Override protected void opened() {
                            System.out.println("server opened");
                        }
                        @Override public void closed(final @Nullable Throwable throwable) {
                            System.out.println("server closed");
                        }
                    }
                ),
                session
            );
        }
    }

    private static ServerEndpointConfig serverEndpointConfig(final ServerEndpointConfig.Configurator configurator) {
        return ServerEndpointConfig.Builder.create(ServerEndpoint.class, PATH).configurator(configurator).build();
    }

    public static final class ClientEndpoint extends WsEndpoint {
        @Override protected WsConnection createConnection(final javax.websocket.Session session) throws Exception {
            return WsConnection.create(
                AsyncWsConnection.factory(5_000),
                new TransportSetup(
                    new Server(
                        METHOD_MAPPER_FACTORY
                    ),
                    DISPATCHER,
                    PACKET_SERIALIZER,
                    sessionClient -> new Session(sessionClient) {
                        @Override protected void opened() {
                            System.out.println("client opened");
                            final Busy busy = proxy(BUSY_ID);
                            for (int i = 0; i < 100_000; i++) {
                                busy.busy();
                            }
                            System.out.println("client done");
                        }
                        @Override public void closed(final @Nullable Throwable throwable) {
                            System.out.println("client closed");
                            throwable.printStackTrace(System.out);
                            System.exit(1);
                        }
                    }
                ),
                session
            );
        }
    }

    private static void client(final WebSocketContainer container) throws Exception {
        container.connectToServer(
            new ClientEndpoint(),
            ClientEndpointConfig.Builder.create().build(),
            URI.create("ws://" + HOST + ":" + PORT + PATH)
        );
    }

    public static void main(final String... args) throws Exception {
        final DeploymentManager deployment = Servlets.defaultContainer()
            .addDeployment(
                Servlets.deployment()
                    .setClassLoader(AsyncWsConnectionTest.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName(AsyncWsConnectionTest.class.getName())
                    .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                            .addEndpoint(serverEndpointConfig(new DefaultContainerConfigurator()))
                            .setWorker(Xnio.getInstance().createWorker(OptionMap.builder().getMap()))
                            .setBuffers(new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)))
                    )
            );
        deployment.deploy();
        Undertow.builder()
            .addHttpListener(PORT, HOST)
            .setHandler(deployment.start())
            .build()
            .start();
        TimeUnit.SECONDS.sleep(1);
        if (true) {
            client(new ServerWebSocketContainer(
                DefaultClassIntrospector.INSTANCE,
                Xnio.getInstance().createWorker(OptionMap.create(Options.THREAD_DAEMON, true)),
                new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)),
                new CompositeThreadSetupAction(Collections.emptyList()),
                true,
                true
            ));
        } else {
            final ClientContainer container = new ClientContainer(); // $note: setSendTimeout not yet implemented in Jetty
            container.start();
            client(container);
        }
    }

}
