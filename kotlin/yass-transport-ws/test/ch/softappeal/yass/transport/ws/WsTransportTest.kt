package ch.softappeal.yass.transport.ws

import ch.softappeal.yass.remote.session.Connection
import ch.softappeal.yass.remote.session.createTestSession
import ch.softappeal.yass.remote.session.useExecutor
import ch.softappeal.yass.transport.SessionTransport
import ch.softappeal.yass.transport.socket.packetSerializer
import io.undertow.Undertow
import io.undertow.server.XnioByteBufferPool
import io.undertow.servlet.Servlets
import io.undertow.servlet.api.ThreadSetupHandler
import io.undertow.servlet.core.ContextClassLoaderSetupAction
import io.undertow.servlet.util.DefaultClassIntrospector
import io.undertow.websockets.jsr.ServerWebSocketContainer
import io.undertow.websockets.jsr.WebSocketDeploymentInfo
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.jsr356.ClientContainer
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer
import org.junit.Test
import org.xnio.ByteBufferSlicePool
import org.xnio.OptionMap
import org.xnio.Options
import org.xnio.Xnio
import java.net.URI
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.websocket.ClientEndpointConfig
import javax.websocket.Endpoint
import javax.websocket.WebSocketContainer
import javax.websocket.server.ServerEndpointConfig

private const val PORT = 9090
private const val PATH = "/test"
private val THE_URI = URI.create("ws://localhost:$PORT$PATH")

private fun connectionHandler(connection: Connection) {
    println(connection)
    println((connection as WsConnection).session)
}

private fun serverEndpointConfig(executor: Executor): ServerEndpointConfig = ServerEndpointConfig.Builder
    .create(Endpoint::class.java, PATH)
    .configurator(WsConfigurator(
        asyncWsConnectionFactory(100),
        SessionTransport(packetSerializer) { createTestSession(executor, null, ::connectionHandler) }
    ))
    .build()

private fun connect(container: WebSocketContainer, executor: Executor, done: () -> Unit) {
    container.connectToServer(
        WsConfigurator(
            SyncWsConnectionFactory,
            SessionTransport(packetSerializer) { createTestSession(executor, done, ::connectionHandler) }
        ).endpointInstance,
        ClientEndpointConfig.Builder.create().build(),
        THE_URI
    )
}

class WsTest {
    @Test
    fun jetty() = useExecutor { executor, done ->
        val server = Server()
        val serverConnector = ServerConnector(server)
        serverConnector.port = PORT
        server.addConnector(serverConnector)
        val contextHandler = ServletContextHandler(ServletContextHandler.NO_SESSIONS)
        contextHandler.contextPath = "/"
        server.handler = contextHandler
        WebSocketServerContainerInitializer.configureContext(contextHandler).addEndpoint(serverEndpointConfig(executor))
        server.start()
        val container = ClientContainer()
        container.start()
        connect(container, executor) {}
        TimeUnit.MILLISECONDS.sleep(200)
        container.stop()
        server.stop()
        done()
    }

    @Test
    fun undertow() = useExecutor { executor, done ->
        val xnio = Xnio.getInstance()
        val deployment = Servlets.defaultContainer()
            .addDeployment(
                Servlets.deployment()
                    .setClassLoader(WsTest::class.java.classLoader)
                    .setContextPath("/")
                    .setDeploymentName(WsTest::class.java.name)
                    .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        WebSocketDeploymentInfo()
                            .addEndpoint(serverEndpointConfig(executor))
                            .setWorker(xnio.createWorker(OptionMap.builder().map))
                            .setBuffers(XnioByteBufferPool(ByteBufferSlicePool(1024, 10240)))
                    )
            )
        deployment.deploy()
        val server = Undertow.builder()
            .addHttpListener(PORT, "localhost")
            .setHandler(deployment.start())
            .build()
        server.start()
        connect(
            ServerWebSocketContainer(
                DefaultClassIntrospector.INSTANCE,
                xnio.createWorker(OptionMap.create(Options.THREAD_DAEMON, true)),
                XnioByteBufferPool(ByteBufferSlicePool(1024, 10240)),
                listOf<ThreadSetupHandler>(ContextClassLoaderSetupAction(ClassLoader.getSystemClassLoader())),
                true,
                true
            ),
            executor
        ) {}
        TimeUnit.MILLISECONDS.sleep(200)
        server.stop()
        done()
    }
}
