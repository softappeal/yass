package ch.softappeal.yass.transport.ws

import ch.softappeal.yass.remote.session.*
import ch.softappeal.yass.remote.session.Connection
import ch.softappeal.yass.transport.*
import ch.softappeal.yass.transport.socket.*
import io.undertow.*
import io.undertow.server.*
import io.undertow.servlet.*
import io.undertow.servlet.api.*
import io.undertow.servlet.core.*
import io.undertow.servlet.util.*
import io.undertow.websockets.jsr.*
import org.eclipse.jetty.server.*
import org.eclipse.jetty.servlet.*
import org.eclipse.jetty.websocket.jsr356.*
import org.eclipse.jetty.websocket.jsr356.server.deploy.*
import org.xnio.*
import java.net.*
import java.util.concurrent.*
import javax.websocket.*
import javax.websocket.server.*
import kotlin.test.*

private const val PORT = 9090
private const val PATH = "/test"
private val THE_URI = URI.create("ws://localhost:$PORT$PATH")

private fun connectionHandler(connection: Connection) {
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
    TimeUnit.MILLISECONDS.sleep(1_000)
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
                { xnio.createWorker(OptionMap.create(Options.THREAD_DAEMON, true)) },
                XnioByteBufferPool(ByteBufferSlicePool(1024, 10240)),
                listOf<ThreadSetupHandler>(ContextClassLoaderSetupAction(ClassLoader.getSystemClassLoader())),
                true,
                true
            ),
            executor
        ) {}
        server.stop()
        done()
    }
}
